<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { createShop } from '@/api/shop'
import { listPackages } from '@/api/package'

/**
 * 新建商城向导。对照原型 admin/shop-new.html，但**改成了 4 步而不是原型的 5 步**：
 *
 * 原型的 5 步是「基本信息 / 套餐周期 / 店主账号 / 访问域名 / 完成」，其中"店主账号"和
 * "访问域名"被拆成了两步。但后端 CreateShopRequest 只有一个 code 字段（二级域名前缀），
 * 店主账号是后端按 `code + "_admin"` 自动生成的（见 ShopServiceImpl），没有让运营手动填账号的入口，
 * 也没有"通知方式勾选""小程序接入方式"这些配置项——这些字段不存在，画出来是假交互。
 * 所以这里把域名步骤挪到店主账号预览之前（域名决定了账号名，逻辑上域名必须先填），
 * 合并成「基本信息 / 套餐与周期 / 访问域名与账号 / 完成」四步，每一步的字段都对应真实存在的
 * CreateShopRequest 字段，不多不少。
 */
const router = useRouter()
const step = ref(1)
const TOTAL = 4
const submitting = ref(false)
const result = ref(null)

const form = reactive({
  name: '',
  industry: '服饰鞋包',
  contact: '',
  mobile: '',
  remark: '',
  packageTplId: null,
  durationMonth: 12,
  code: '',
})

const errors = reactive({})

const packages = ref([])
const packagesLoading = ref(false)

onMounted(async () => {
  packagesLoading.value = true
  try {
    const all = await listPackages()
    // 试用版不参与人工开店选择——试用套餐是系统自动灌入的默认套餐，不是运营手动开店时的选项；
    // 已下架的套餐不应该出现在"新开店"的可选列表里（下架只影响新开店，不影响已开通商城，见套餐管理页注释）。
    packages.value = all.filter((p) => p.isShow && !p.isTrial)
  } catch (e) {
    packages.value = []
  } finally {
    packagesLoading.value = false
  }
})

const DURATION_OPTIONS = [
  { value: 1, label: '1 个月' },
  { value: 3, label: '3 个月' },
  { value: 12, label: '12 个月（首月赠送）' },
]

const selectedPackage = computed(() => packages.value.find((p) => p.id === form.packageTplId))

function priceOf(pkg) {
  if (!pkg) return {}
  try {
    return JSON.parse(pkg.price)
  } catch (e) {
    return {}
  }
}

const estimatedFee = computed(() => {
  const price = priceOf(selectedPackage.value)
  if (form.durationMonth === 1) return price.month ?? 0
  if (form.durationMonth === 3) return price.quarter ?? 0
  return price.year ?? 0
})

const previewUsername = computed(() => (form.code ? `${form.code}_admin` : ''))

function validateStep1() {
  errors.name = form.name.trim() ? '' : '请输入商城名称'
  errors.contact = form.contact.trim() ? '' : '请输入联系人'
  errors.mobile = /^1[3-9]\d{9}$/.test(form.mobile) ? '' : '请输入正确的联系手机号'
  return !errors.name && !errors.contact && !errors.mobile
}

function validateStep2() {
  errors.packageTplId = form.packageTplId ? '' : '请选择套餐'
  return !errors.packageTplId
}

function validateStep3() {
  errors.code = /^[a-z0-9-]{3,32}$/.test(form.code) ? '' : '二级域名前缀仅支持小写字母、数字、短横线，长度3-32'
  return !errors.code
}

async function onNext() {
  if (step.value === 1 && !validateStep1()) return
  if (step.value === 2 && !validateStep2()) return
  if (step.value === 3) {
    if (!validateStep3()) return
    await onSubmit()
    return
  }
  step.value += 1
}

function onPrev() {
  if (step.value > 1) step.value -= 1
}

async function onSubmit() {
  submitting.value = true
  try {
    result.value = await createShop({
      name: form.name.trim(),
      code: form.code.trim(),
      industry: form.industry,
      contact: form.contact.trim(),
      mobile: form.mobile.trim(),
      remark: form.remark.trim(),
      packageTplId: form.packageTplId,
      durationMonth: form.durationMonth,
    })
    step.value = 4
  } catch (e) {
    // 后端校验失败（如域名前缀已被占用）已由 http 拦截器弹出提示，留在当前步骤修改重试
  } finally {
    submitting.value = false
  }
}

function goList() {
  router.push({ name: 'shops' })
}

function goDetail() {
  router.push({ name: 'shop-detail', params: { id: result.value.shopId } })
}

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : '—'
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="breadcrumb" style="margin-bottom: 6px">
        <a @click="goList" style="cursor: pointer">商城管理</a> ／ <b>新建商城</b>
      </div>
      <div class="page-title">新建商城</div>
    </div>
  </div>

  <div class="wizard-wrap">
    <div class="card card-pad">
      <div class="steps">
        <template v-for="(label, i) in ['基本信息', '套餐与周期', '访问域名', '完成']" :key="label">
          <div class="step" :class="{ active: step === i + 1, done: step > i + 1 }">
            <div class="step-num">{{ i + 1 }}</div>
            <div class="step-label">{{ label }}</div>
          </div>
          <div v-if="i < 3" class="step-line"></div>
        </template>
      </div>

      <!-- Step 1: 基本信息 -->
      <div v-if="step === 1" class="wizard-step active">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>商城名称</label>
          <input v-model="form.name" class="form-input" placeholder="例如：花间集女装旗舰店" />
          <div v-if="errors.name" class="field-error">{{ errors.name }}</div>
        </div>
        <div class="form-row form-item">
          <div>
            <label class="form-label">所属行业</label>
            <select v-model="form.industry" class="form-select">
              <option>服饰鞋包</option>
              <option>生鲜食品</option>
              <option>美妆个护</option>
              <option>3C数码</option>
              <option>其他</option>
            </select>
          </div>
        </div>
        <div class="form-row form-item">
          <div>
            <label class="form-label"><span class="req">*</span>联系人</label>
            <input v-model="form.contact" class="form-input" />
            <div v-if="errors.contact" class="field-error">{{ errors.contact }}</div>
          </div>
          <div>
            <label class="form-label"><span class="req">*</span>联系手机</label>
            <input v-model="form.mobile" class="form-input" maxlength="11" />
            <div v-if="errors.mobile" class="field-error">{{ errors.mobile }}</div>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">备注</label>
          <textarea v-model="form.remark" class="form-textarea" rows="2" placeholder="内部备注，租户不可见"></textarea>
        </div>
      </div>

      <!-- Step 2: 套餐与周期 -->
      <div v-if="step === 2" class="wizard-step active">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>选择套餐</label>
          <a-spin :spinning="packagesLoading">
            <div class="form-radio-group">
              <div
                v-for="pkg in packages"
                :key="pkg.id"
                class="form-radio-card"
                :class="{ selected: form.packageTplId === pkg.id }"
                @click="form.packageTplId = pkg.id"
              >
                <div class="title">{{ pkg.name }}</div>
                <div class="desc">{{ pkg.intro }} · ¥{{ priceOf(pkg).month ?? 0 }}/月</div>
              </div>
            </div>
          </a-spin>
          <div v-if="errors.packageTplId" class="field-error">{{ errors.packageTplId }}</div>
          <div v-if="!packagesLoading && packages.length === 0" class="form-hint">
            暂无可选套餐，请先在「套餐管理」上架至少一个非试用套餐
          </div>
        </div>
        <div class="form-row form-item">
          <div>
            <label class="form-label"><span class="req">*</span>订购周期</label>
            <select v-model.number="form.durationMonth" class="form-select">
              <option v-for="d in DURATION_OPTIONS" :key="d.value" :value="d.value">{{ d.label }}</option>
            </select>
          </div>
        </div>
        <div v-if="selectedPackage" class="card" style="background: var(--surface-2); border: none; padding: 12px 16px; font-size: 12.5px; color: var(--text-secondary)">
          本次开通：{{ selectedPackage.name }} · {{ form.durationMonth }} 个月 · 应收 ¥{{ estimatedFee }}
          （线下开通，不产生支付订单，实际到期时间由后端计算）
        </div>
      </div>

      <!-- Step 3: 访问域名与账号 -->
      <div v-if="step === 3" class="wizard-step active">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>二级域名前缀</label>
          <input v-model="form.code" class="form-input" style="width: 280px" placeholder="如 huajianji" />
          <div v-if="errors.code" class="field-error">{{ errors.code }}</div>
          <div class="form-hint">
            仅支持小写字母、数字、短横线，3-32 位；实际访问域名 = 该前缀 + 平台配置的基础域名（部署环境不同而不同）
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">店主登录账号（自动生成）</label>
          <input class="form-input" disabled :value="previewUsername || '请先填写二级域名前缀'" />
          <div class="form-hint">初始密码由系统随机生成，创建成功后展示（生产环境应通过短信下发，当前尚未接入短信通道）</div>
        </div>
      </div>

      <!-- Step 4: 完成 -->
      <div v-if="step === 4" class="wizard-step active">
        <div class="result-box">
          <div class="result-icon">✓</div>
          <div style="font-size: 16px; font-weight: 700; margin-bottom: 4px">商城创建成功</div>
          <div style="font-size: 12.5px; color: var(--text-muted); margin-bottom: 22px">
            种子数据初始化已完成：默认分类、运费模板、装修页、交易设置、会员等级、协议均已就绪
          </div>
        </div>
        <div v-if="result" class="card" style="background: var(--surface-2); border: none; padding: 6px 20px; margin-bottom: 20px">
          <div class="kv-row"><div class="k">商城编号</div><div class="v">{{ result.shopId }}</div></div>
          <div class="kv-row"><div class="k">商城名称</div><div class="v">{{ result.name }}</div></div>
          <div class="kv-row"><div class="k">域名前缀</div><div class="v">{{ result.code }}</div></div>
          <div class="kv-row"><div class="k">店主账号</div><div class="v">{{ result.storeAdminUsername }}</div></div>
          <div class="kv-row"><div class="k">套餐到期</div><div class="v">{{ fmtDate(result.expireTime) }}</div></div>
        </div>
        <div style="display: flex; gap: 10px; justify-content: center">
          <button class="btn" @click="goList">返回列表</button>
          <button class="btn btn-primary" @click="goDetail">查看商城详情</button>
        </div>
      </div>

      <div v-if="step < 4" style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 26px; padding-top: 20px; border-top: 1px solid var(--gridline)">
        <button class="btn btn-ghost" @click="goList">取消</button>
        <button class="btn" :disabled="step === 1" @click="onPrev">上一步</button>
        <button class="btn btn-primary" :disabled="submitting" @click="onNext">
          {{ submitting ? '创建中…' : step === 3 ? '确认创建' : '下一步' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 向导专用样式，同样只存在于原型 shop-new.html 的页内 <style>，不在共享样式表里 */
.wizard-wrap {
  max-width: 760px;
  margin: 0 auto;
}
.result-box {
  text-align: center;
  padding: 30px 20px 10px;
}
.result-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--status-good-bg);
  color: var(--status-good);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  margin: 0 auto 16px;
}
.kv-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px dashed var(--gridline);
  font-size: 13px;
}
.kv-row:last-child {
  border-bottom: none;
}
.kv-row .k {
  width: 130px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.kv-row .v {
  font-weight: 500;
}
.field-error {
  font-size: 12px;
  color: var(--status-critical);
  margin-top: 4px;
}
</style>
