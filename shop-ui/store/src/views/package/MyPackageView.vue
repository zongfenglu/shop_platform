<script setup>
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { applyStoreInvoice, getStorePackage, listStoreInvoices, placePackageOrder } from '@/api/package'

/**
 * 我的套餐。对照 docs/prototype/store/my-package.html。
 * 续费/升级生成线下待确认订购单；在线支付后续接入，由超管「确认到账」后延长有效期。
 */
const loading = ref(false)
const submitting = ref(false)
const invoices = ref([])
const invoiceOpen = ref(false)
const invoiceSubmitting = ref(false)
const invoiceForm = ref({ shopOrderId: null, title: '', taxNo: '' })
const data = ref({
  shopName: '',
  shopStatus: '',
  expireTime: null,
  daysLeft: 0,
  currentPackage: null,
  quotas: [],
  plans: [],
  orders: [],
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    data.value = (await getStorePackage()) || data.value
    invoices.value = (await listStoreInvoices()) || []
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

const currentTplId = computed(() => data.value.currentPackage?.packageTplId)
const expireSoon = computed(() => data.value.daysLeft <= 30)

function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 10)
}

function money(v) {
  return Number(v || 0).toFixed(2)
}

function quotaText(q) {
  const max = Number(q.max)
  const used = Number(q.used)
  if (max < 0) return `${used} / 不限`
  return `${used} / ${max}`
}

function quotaCls(q) {
  if (q.percent >= 90) return 'danger'
  if (q.percent >= 70) return 'warn'
  return ''
}

function planPrice(plan) {
  try {
    const p = typeof plan.price === 'string' ? JSON.parse(plan.price) : plan.price
    return p?.year ?? p?.month ?? 0
  } catch (e) {
    return 0
  }
}

function planFeatures(plan) {
  try {
    const q = typeof plan.quota === 'string' ? JSON.parse(plan.quota) : plan.quota
    return [
      q?.goods_max < 0 ? '商品数不限' : `商品数 ${q?.goods_max ?? '—'}`,
      `员工账号 ${q?.staff_max ?? '—'}`,
      q?.store_max < 0 ? '门店不限' : `门店 ${q?.store_max ?? 0}`,
    ]
  } catch (e) {
    return []
  }
}

function isCurrent(plan) {
  return String(plan.id) === String(currentTplId.value)
}

function typeText(row) {
  return { new: '新开通', renew: '续费', upgrade: '升级', addon: '增值服务' }[row.type] || row.type
}

function statusText(row) {
  if (row.payStatus === 'pending') return '待确认到账'
  if (row.payStatus === 'paid') return '已完成'
  if (row.payStatus === 'refunded') return '已退款'
  if (row.payStatus === 'closed') return '已关闭'
  return row.payStatus
}

function statusClass(row) {
  if (row.payStatus === 'pending') return 'tag-warning'
  if (row.payStatus === 'paid') return 'tag-good'
  if (row.payStatus === 'refunded') return 'tag-critical'
  return 'tag-muted'
}

function invoiceOf(orderId) {
  return (invoices.value || []).find((row) => String(row.shopOrderId) === String(orderId))
}

function invoiceStatusText(status) {
  return { applying: '待开票', issued: '已开票', rejected: '已驳回' }[status] || status || '—'
}

function openInvoice(order) {
  invoiceForm.value = { shopOrderId: order.id, title: '', taxNo: '' }
  invoiceOpen.value = true
}

async function submitInvoice() {
  if (!invoiceForm.value.title.trim() || !invoiceForm.value.taxNo.trim()) {
    message.error('请填写发票抬头和税号')
    return
  }
  invoiceSubmitting.value = true
  try {
    await applyStoreInvoice({
      shopOrderId: invoiceForm.value.shopOrderId,
      title: invoiceForm.value.title.trim(),
      taxNo: invoiceForm.value.taxNo.trim(),
    })
    message.success('已提交发票申请')
    invoiceOpen.value = false
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    invoiceSubmitting.value = false
  }
}

function planName(row) {
  const plan = (data.value.plans || []).find((p) => String(p.id) === String(row.packageTplId))
  return plan?.name || data.value.currentPackage?.name || '—'
}

async function submit(type, packageTplId) {
  submitting.value = true
  try {
    await placePackageOrder({ type, packageTplId, durationMonth: 12 })
    message.success('已提交 12 个月订购单（线下转账），请等待平台确认到账')
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function onRenew() {
  Modal.confirm({
    title: '确认续费 12 个月？',
    content: '将生成一笔线下待确认订购单，平台确认到账后延长有效期。在线支付后续接入。',
    okText: '提交',
    cancelText: '取消',
    onOk: () => submit('renew', currentTplId.value),
  })
}

function onUpgrade(plan) {
  Modal.confirm({
    title: `升级到「${plan.name}」12 个月？`,
    content: `金额 ¥${money(planPrice(plan))}，线下转账，平台确认到账后生效。`,
    okText: '提交',
    cancelText: '取消',
    onOk: () => submit('upgrade', plan.id),
  })
}
</script>

<template>
  <a-spin :spinning="loading">
    <div v-if="expireSoon" class="banner-expire">
      <span>
        当前套餐将于 <b>{{ fmtTime(data.expireTime) }}</b> 到期，剩余 {{ data.daysLeft }} 天
        {{ data.daysLeft < 0 ? '（已过期）' : '，建议提前续费' }}
      </span>
      <button class="btn btn-sm btn-primary" :disabled="submitting" @click="onRenew">立即续费</button>
    </div>

    <div class="grid grid-2" style="margin-bottom: 20px">
      <div class="card card-pad">
        <div class="card-title">当前套餐</div>
        <div style="display: flex; align-items: baseline; gap: 8px; margin: 10px 0">
          <span style="font-size: 22px; font-weight: 800">{{ data.currentPackage?.name || '未开通套餐' }}</span>
          <span class="tag tag-good tag-dot">{{ data.shopStatus === 'normal' ? '正常使用' : (data.shopStatus || '—') }}</span>
        </div>
        <div style="font-size: 12.5px; color: var(--text-muted); margin-bottom: 16px">
          到期时间：{{ fmtTime(data.expireTime) }} · 剩余 {{ data.daysLeft }} 天
        </div>
        <div style="display: flex; gap: 10px">
          <button class="btn btn-primary" :disabled="submitting || !data.currentPackage" @click="onRenew">续费 12 个月</button>
        </div>
        <div class="form-hint" style="margin-top: 12px; margin-bottom: 0">微信支付收款按排期后续接入，当前为线下转账 + 超管确认到账。</div>
      </div>

      <div class="card card-pad">
        <div class="card-title">配额用量</div>
        <div v-if="!(data.quotas || []).length" class="form-hint">暂无套餐快照</div>
        <div v-for="item in data.quotas" :key="item.key" class="quota-row">
          <div class="quota-hd"><span>{{ item.label }}</span><span class="n">{{ quotaText(item) }}</span></div>
          <div class="quota-track"><div class="quota-fill" :class="quotaCls(item)" :style="{ width: `${item.percent}%` }"></div></div>
        </div>
      </div>
    </div>

    <div class="card-title" style="font-size: 16px; margin-bottom: 14px">升级套餐</div>
    <div class="grid grid-3" style="margin-bottom: 20px">
      <div v-for="plan in data.plans" :key="plan.id" class="plan-card" :class="{ current: isCurrent(plan) }">
        <div v-if="isCurrent(plan)" class="badge-current">当前套餐</div>
        <div style="font-weight: 700; font-size: 15px">{{ plan.name }}</div>
        <div class="plan-price">¥{{ money(planPrice(plan)) }}<span class="unit">/年</span></div>
        <div class="form-hint" style="margin-bottom: 8px">{{ plan.intro }}</div>
        <div v-for="feature in planFeatures(plan)" :key="feature" class="plan-feat">✓ {{ feature }}</div>
        <button
          class="btn btn-block"
          :class="{ 'btn-primary': !isCurrent(plan) }"
          :disabled="submitting || isCurrent(plan)"
          style="margin-top: 14px"
          @click="onUpgrade(plan)"
        >
          {{ isCurrent(plan) ? '当前套餐' : '升级并提交订购单' }}
        </button>
      </div>
    </div>

    <div class="card">
      <div class="card-pad" style="padding-bottom: 0"><div class="card-title" style="margin-bottom: 0">订购记录</div></div>
      <table class="table" style="margin-top: 8px">
        <thead>
          <tr>
            <th>订单号</th>
            <th>类型</th>
            <th>套餐</th>
            <th class="num">金额</th>
            <th>支付方式</th>
            <th>时间</th>
            <th>状态</th>
            <th>发票</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!(data.orders || []).length">
            <td colspan="8" style="text-align: center; color: var(--text-muted)">暂无订购记录</td>
          </tr>
          <tr v-for="item in data.orders" :key="item.id">
            <td>{{ item.orderNo }}</td>
            <td>{{ typeText(item) }}</td>
            <td>{{ planName(item) }} · {{ item.durationMonth }}个月</td>
            <td class="num">¥{{ money(item.amount) }}</td>
            <td>{{ item.payMethod === 'offline' ? '线下转账' : (item.payMethod || '—') }}</td>
            <td>{{ fmtTime(item.createTime) }}</td>
            <td><span class="tag" :class="statusClass(item)">{{ statusText(item) }}</span></td>
            <td>
              <template v-if="invoiceOf(item.id)">
                <span class="tag" :class="invoiceOf(item.id).status === 'issued' ? 'tag-good' : (invoiceOf(item.id).status === 'rejected' ? 'tag-critical' : 'tag-warning')">
                  {{ invoiceStatusText(invoiceOf(item.id).status) }}
                </span>
                <div v-if="invoiceOf(item.id).invoiceNo" style="font-size: 12px; color: var(--text-muted)">{{ invoiceOf(item.id).invoiceNo }}</div>
                <button v-if="invoiceOf(item.id).status === 'rejected'" class="btn btn-sm" style="margin-top: 4px" @click="openInvoice(item)">重新申请</button>
              </template>
              <button v-else-if="item.payStatus === 'paid'" class="btn btn-sm" @click="openInvoice(item)">申请发票</button>
              <span v-else class="form-hint" style="margin: 0">到账后可开</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <a-modal v-model:open="invoiceOpen" title="申请发票" :confirm-loading="invoiceSubmitting" ok-text="提交" cancel-text="取消" @ok="submitInvoice">
      <div class="form-hint">仅已支付订购单可开票，由平台审核后开具。</div>
      <label class="form-label">发票抬头</label>
      <input v-model="invoiceForm.title" class="form-input" placeholder="公司全称或个人姓名" />
      <label class="form-label" style="margin-top: 12px">税号</label>
      <input v-model="invoiceForm.taxNo" class="form-input" placeholder="纳税人识别号" />
    </a-modal>
  </a-spin>
</template>

<style scoped>
.banner-expire {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--status-warning-bg);
  background: var(--status-warning-bg);
  color: var(--status-warning-text);
  margin-bottom: 18px;
}
.quota-row { margin-bottom: 18px; }
.quota-row:last-child { margin-bottom: 0; }
.quota-hd { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px; }
.quota-hd .n { font-variant-numeric: tabular-nums; color: var(--text-secondary); }
.quota-track { height: 8px; border-radius: 4px; background: var(--surface-2); overflow: hidden; }
.quota-fill { height: 100%; border-radius: 4px; background: var(--series-1); }
.quota-fill.warn { background: var(--status-warning); }
.quota-fill.danger { background: var(--status-critical); }
.plan-card {
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 20px;
  position: relative;
}
.plan-card.current {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-bg-2);
}
.plan-card .badge-current {
  position: absolute;
  top: -10px;
  left: 16px;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 999px;
}
.plan-price { font-size: 26px; font-weight: 800; margin: 10px 0; }
.plan-price .unit { font-size: 12px; color: var(--text-muted); font-weight: 500; }
.plan-feat {
  font-size: 12.5px;
  color: var(--text-secondary);
  padding: 5px 0;
  display: flex;
  gap: 6px;
}
</style>
