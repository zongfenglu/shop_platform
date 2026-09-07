<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { getPayConfig, savePayConfig } from '@/api/payConfig'
import { createFreightTemplate, deleteFreightTemplate, listFreightTemplates, updateFreightTemplate } from '@/api/goods'

/**
 * 设置页。对照 docs/prototype/store/settings.html，支付设置和运费模板接真实接口。
 *
 * 原型还有「基本信息」「交易设置」「客服设置」等 tab，这些字段在后端 Shop/相关实体上都不存在
 * （Shop 只有 name/logo/industry/contact/mobile，没有经营地址/自动确认收货天数这些字段），
 * 画出来保存了也没有地方落库，是假交互。支付设置是唯一有真实后端支撑的部分——
 * 而且这个页面直接关系到售后退款能否执行（StoreAfterSaleController#refund 依赖这里配置的
 * 微信支付商户号，没配置会在执行退款时报"商户尚未配置微信支付"）。
 */
const loading = ref(false)
const config = ref(null)
const submitting = ref(false)
const activeTab = ref('pay')
const freightTemplates = ref([])
const freightLoading = ref(false)
const freightModalOpen = ref(false)
const freightSubmitting = ref(false)
const editingFreightId = ref(null)

const freightForm = reactive({
  name: '',
  method: 'count',
  first: 1,
  firstFee: 8,
  additional: 1,
  additionalFee: 5,
  freeMinPrice: '',
})

async function load() {
  loading.value = true
  try {
    config.value = await getPayConfig()
  } catch (e) {
    config.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function loadFreightTemplates() {
  freightLoading.value = true
  try {
    freightTemplates.value = await listFreightTemplates()
  } catch (e) {
    freightTemplates.value = []
  } finally {
    freightLoading.value = false
  }
}

onMounted(loadFreightTemplates)

const form = reactive({
  appId: '',
  mchId: '',
  mchCertSerialNo: '',
  apiV3Key: '',
  mchPrivateKeyPem: '',
})

const errors = reactive({})

function validate() {
  errors.appId = form.appId.trim() ? '' : '请输入APPID'
  errors.mchId = form.mchId.trim() ? '' : '请输入商户号'
  errors.mchCertSerialNo = form.mchCertSerialNo.trim() ? '' : '请输入证书序列号'
  errors.apiV3Key = form.apiV3Key.trim() ? '' : '请输入APIv3密钥'
  errors.mchPrivateKeyPem = form.mchPrivateKeyPem.trim() ? '' : '请输入商户私钥'
  return !errors.appId && !errors.mchId && !errors.mchCertSerialNo && !errors.apiV3Key && !errors.mchPrivateKeyPem
}

async function onSave() {
  if (!validate()) {
    message.error('请检查表单中标红的必填项')
    return
  }
  submitting.value = true
  try {
    await savePayConfig({ ...form })
    message.success('保存成功，密钥已加密存储')
    // 密钥只进不出：保存成功后清空表单里的敏感字段，绝不在页面上残留明文
    form.apiV3Key = ''
    form.mchPrivateKeyPem = ''
    await load()
  } catch (e) {
    // 已由拦截器提示
  } finally {
    submitting.value = false
  }
}

function resetFreightForm() {
  Object.assign(freightForm, {
    name: '', method: 'count', first: 1, firstFee: 8,
    additional: 1, additionalFee: 5, freeMinPrice: '',
  })
}

function openFreightModal() {
  resetFreightForm()
  editingFreightId.value = null
  freightModalOpen.value = true
}

function openEditFreight(template) {
  resetFreightForm()
  editingFreightId.value = template.id
  try {
    const rule = JSON.parse(template.rules || '[]')[0] || {}
    const freeRules = template.freeRules ? JSON.parse(template.freeRules) : {}
    Object.assign(freightForm, {
      name: template.name || '',
      method: template.method || 'count',
      first: rule.first ?? 1,
      firstFee: rule.firstFee ?? 0,
      additional: rule.additional ?? 1,
      additionalFee: rule.additionalFee ?? 0,
      freeMinPrice: freeRules.minPrice === undefined ? '' : String(freeRules.minPrice),
    })
    freightModalOpen.value = true
  } catch (e) {
    message.error('运费模板规则格式无效，无法编辑')
  }
}

function closeFreightModal() {
  if (!freightSubmitting.value) freightModalOpen.value = false
}

function methodLabel(method) {
  return { count: '件数', weight: '重量', volume: '体积' }[method] || method || '—'
}

function ruleSummary(template) {
  try {
    const rule = JSON.parse(template.rules || '[]')[0]
    if (!rule) return '未配置阶梯规则'
    const suffix = template.method === 'count' ? '件' : template.method === 'weight' ? 'kg' : 'm³'
    return `首${rule.first || 1}${suffix} ¥${Number(rule.firstFee || 0).toFixed(2)}，续${rule.additional || 1}${suffix} ¥${Number(rule.additionalFee || 0).toFixed(2)}`
  } catch (e) {
    return '规则格式无效'
  }
}

function freeShippingLabel(template) {
  if (!template.freeRules) return '—'
  try {
    const minPrice = JSON.parse(template.freeRules).minPrice
    return minPrice === undefined || minPrice === null || minPrice === ''
      ? '—'
      : `满 ¥${Number(minPrice).toFixed(2)} 包邮`
  } catch (e) {
    return '—'
  }
}

async function onCreateFreight() {
  if (!freightForm.name.trim()) {
    message.error('请输入模板名称')
    return
  }
  const freeMinPrice = freightForm.freeMinPrice.trim() === '' ? null : Number(freightForm.freeMinPrice)
  if (!Number.isFinite(freightForm.first) || !Number.isFinite(freightForm.additional)
    || !Number.isFinite(freightForm.firstFee) || !Number.isFinite(freightForm.additionalFee)
    || freightForm.first <= 0 || freightForm.additional <= 0 || freightForm.firstFee < 0
    || freightForm.additionalFee < 0 || (freeMinPrice !== null && (!Number.isFinite(freeMinPrice) || freeMinPrice < 0))) {
    message.error('首段、续段必须大于 0，费用不能为负数')
    return
  }
  freightSubmitting.value = true
  try {
    const payload = {
      name: freightForm.name.trim(),
      method: freightForm.method,
      rules: JSON.stringify([{
        region: ['*'], first: freightForm.first, firstFee: freightForm.firstFee,
        additional: freightForm.additional, additionalFee: freightForm.additionalFee,
      }]),
      freeRules: freeMinPrice === null ? null : JSON.stringify({ minPrice: freeMinPrice }),
    }
    if (editingFreightId.value) {
      await updateFreightTemplate(editingFreightId.value, payload)
      message.success('运费模板已更新')
    } else {
      await createFreightTemplate(payload)
      message.success('运费模板已创建')
    }
    freightModalOpen.value = false
    await loadFreightTemplates()
  } catch (e) {
    // 错误提示由 HTTP 拦截器统一处理
  } finally {
    freightSubmitting.value = false
  }
}

function confirmDeleteFreight(template) {
  Modal.confirm({
    title: '删除运费模板？',
    content: `删除后不能再用于新商品，已有商品的历史订单不受影响。\n模板：${template.name}`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      await deleteFreightTemplate(template.id)
      message.success('运费模板已删除')
      await loadFreightTemplates()
    },
  })
}

function maskMchId(id) {
  if (!id || id.length < 6) return id || '—'
  return `${id.slice(0, 4)}****${id.slice(-4)}`
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">设置</div>
      <div class="page-desc">商城信息、交易规则、支付物流与协议配置</div>
    </div>
  </div>

  <a-spin :spinning="loading">
    <div class="settings-shell">
      <div class="settings-nav">
        <div class="group-title">基础</div>
        <button class="settings-item" :class="{ active: activeTab === 'freight' }" @click="activeTab = 'freight'">运费模板</button>
        <div class="group-title">收款与配送</div>
        <button class="settings-item" :class="{ active: activeTab === 'pay' }" @click="activeTab = 'pay'">支付设置</button>
      </div>

      <div v-if="activeTab === 'freight'" class="settings-panel">
        <div class="page-header panel-header">
          <div>
            <div class="card-title" style="margin: 0">运费模板</div>
            <div class="card-sub" style="margin: 4px 0 0">按件数、重量或体积配置阶梯运费与满额包邮规则</div>
          </div>
          <button class="btn btn-sm btn-primary" @click="openFreightModal">＋ 新建模板</button>
        </div>
        <a-spin :spinning="freightLoading">
          <div class="card">
            <table v-if="freightTemplates.length" class="table">
              <thead><tr><th>模板名称</th><th>计费方式</th><th>说明</th><th>包邮门槛</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="template in freightTemplates" :key="template.id">
                  <td style="font-weight: 600">{{ template.name }}</td>
                  <td><span class="tag tag-primary">{{ methodLabel(template.method) }}</span></td>
                  <td>{{ ruleSummary(template) }}</td>
                  <td>{{ freeShippingLabel(template) }}</td>
                  <td><button class="btn btn-sm" @click="openEditFreight(template)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDeleteFreight(template)">删除</button></td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-state"><div class="icon">▧</div><div>暂无运费模板</div></div>
          </div>
        </a-spin>
      </div>

      <div v-else class="card card-pad payment-panel">
      <p class="card-title">微信支付</p>
      <p class="card-sub">配置租户自有商户号，用于收款和售后退款；密钥全程加密存储，保存后不可回显明文</p>

      <template v-if="config">
        <div class="kv-row"><div class="k">当前状态</div><div class="v"><span class="tag" :class="config.status === 'enabled' ? 'tag-good' : 'tag-muted'">{{ config.status === 'enabled' ? '已启用' : '未启用' }}</span></div></div>
        <div class="kv-row"><div class="k">APPID</div><div class="v">{{ config.appId || '—' }}</div></div>
        <div class="kv-row"><div class="k">商户号</div><div class="v">{{ maskMchId(config.mchId) }}</div></div>
        <div class="kv-row"><div class="k">证书序列号</div><div class="v">{{ config.mchCertSerialNo || '—' }}</div></div>
        <div class="kv-row"><div class="k">APIv3密钥</div><div class="v">{{ config.apiV3KeySet ? '●●●●●●●● 已设置' : '未设置' }}</div></div>
        <div class="kv-row"><div class="k">商户私钥</div><div class="v">{{ config.mchPrivateKeySet ? '●●●●●●●● 已设置' : '未设置' }}</div></div>
      </template>
      <div v-else style="color: var(--text-muted); font-size: 13px; margin-bottom: 16px">尚未配置微信支付</div>

      <p class="card-title" style="margin-top: 24px">{{ config ? '更新配置' : '新增配置' }}</p>
      <div class="form-item">
        <label class="form-label"><span class="req">*</span>APPID</label>
        <input v-model="form.appId" class="form-input" />
        <div v-if="errors.appId" class="field-error">{{ errors.appId }}</div>
      </div>
      <div class="form-item">
        <label class="form-label"><span class="req">*</span>商户号 mchId</label>
        <input v-model="form.mchId" class="form-input" />
        <div v-if="errors.mchId" class="field-error">{{ errors.mchId }}</div>
      </div>
      <div class="form-item">
        <label class="form-label"><span class="req">*</span>证书序列号</label>
        <input v-model="form.mchCertSerialNo" class="form-input" />
        <div v-if="errors.mchCertSerialNo" class="field-error">{{ errors.mchCertSerialNo }}</div>
      </div>
      <div class="form-item">
        <label class="form-label"><span class="req">*</span>APIv3 密钥</label>
        <input v-model="form.apiV3Key" type="password" class="form-input" autocomplete="new-password" />
        <div v-if="errors.apiV3Key" class="field-error">{{ errors.apiV3Key }}</div>
      </div>
      <div class="form-item">
        <label class="form-label"><span class="req">*</span>商户私钥（PEM格式）</label>
        <textarea v-model="form.mchPrivateKeyPem" class="form-textarea" rows="4" autocomplete="new-password" placeholder="-----BEGIN PRIVATE KEY-----..."></textarea>
        <div v-if="errors.mchPrivateKeyPem" class="field-error">{{ errors.mchPrivateKeyPem }}</div>
      </div>

      <div style="display: flex; justify-content: flex-end; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--gridline)">
        <button class="btn btn-primary" :disabled="submitting" @click="onSave">
          {{ submitting ? '保存中…' : '保存' }}
        </button>
      </div>
      </div>
    </div>
  </a-spin>

  <div v-if="freightModalOpen" class="modal-mask" @click.self="closeFreightModal">
    <div class="modal freight-modal">
      <div class="modal-header"><span>{{ editingFreightId ? '编辑运费模板' : '新建运费模板' }}</span><button class="modal-close" aria-label="关闭" @click="closeFreightModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>模板名称</label><input v-model="freightForm.name" class="form-input" placeholder="如：按重量计费" /></div>
        <div class="form-item"><label class="form-label">计费方式</label><select v-model="freightForm.method" class="form-select"><option value="count">按件数</option><option value="weight">按重量（kg）</option><option value="volume">按体积（m³）</option></select></div>
        <div class="form-row form-item"><div><label class="form-label">首段数量</label><input v-model.number="freightForm.first" type="number" min="0.001" step="0.001" class="form-input" /></div><div><label class="form-label">首段费用</label><input v-model.number="freightForm.firstFee" type="number" min="0" step="0.01" class="form-input" /></div></div>
        <div class="form-row form-item"><div><label class="form-label">续段数量</label><input v-model.number="freightForm.additional" type="number" min="0.001" step="0.001" class="form-input" /></div><div><label class="form-label">续段费用</label><input v-model.number="freightForm.additionalFee" type="number" min="0" step="0.01" class="form-input" /></div></div>
        <div class="form-item"><label class="form-label">满额包邮（可选）</label><input v-model="freightForm.freeMinPrice" type="number" min="0" step="0.01" class="form-input" placeholder="不填则不设置" /></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeFreightModal">取消</button><button class="btn btn-primary" :disabled="freightSubmitting" @click="onCreateFreight">{{ freightSubmitting ? '保存中…' : (editingFreightId ? '保存修改' : '创建模板') }}</button></div>
    </div>
  </div>
</template>

<style scoped>
.kv-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px dashed var(--gridline);
  font-size: 13px;
}
.kv-row .k {
  width: 100px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.kv-row .v {
  font-weight: 500;
}
.settings-shell {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.settings-nav {
  width: 180px;
  flex-shrink: 0;
}
.group-title {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  margin: 14px 0 6px 14px;
}
.group-title:first-child { margin-top: 0; }
.settings-item {
  display: block;
  width: 100%;
  padding: 9px 14px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  text-align: left;
  font-size: 13px;
  cursor: pointer;
}
.settings-item.active { background: var(--primary-bg); color: var(--primary-hover); font-weight: 600; }
.settings-panel { flex: 1; min-width: 0; }
.panel-header { margin-bottom: 14px; }
.payment-panel { max-width: 640px; }
.freight-modal { width: 560px; }
.btn-danger-outline { color: var(--status-critical); border-color: var(--status-critical); margin-left: 6px; }
.field-error {
  font-size: 12px;
  color: var(--status-critical);
  margin-top: 4px;
}
</style>
