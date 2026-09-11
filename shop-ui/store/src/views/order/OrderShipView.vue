<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getOrder, shipOrder } from '@/api/order'
import { listExpressCompanies } from '@/api/operationSettings'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const data = ref(null)
const expressCompanies = ref([])
const selectedGoodsIds = ref([])
const form = reactive({ expressCompany: '', expressNo: '' })
const errors = reactive({ expressCompany: '', expressNo: '', goods: '' })

function parsePackageGoodsIds(pkg) {
  return new Set(String(pkg?.orderGoodsIds || '').match(/\d+/g) || [])
}

const shippedGoodsIds = computed(() => {
  const ids = new Set()
  const allIds = (data.value?.goodsList || []).map((item) => String(item.id))
  for (const pkg of data.value?.packages || []) {
    const packageIds = parsePackageGoodsIds(pkg)
    if (packageIds.size === 0) return new Set(allIds)
    packageIds.forEach((id) => ids.add(id))
  }
  return ids
})

const unshippedGoods = computed(() => (data.value?.goodsList || [])
  .filter((item) => !shippedGoodsIds.value.has(String(item.id))))

const canShip = computed(() => {
  const order = data.value?.order
  return order && order.payStatus === 'paid' && order.deliveryType !== 'pickup'
    && !['cancelled', 'finished'].includes(order.orderStatus)
    && order.deliveryStatus !== 'received' && unshippedGoods.value.length > 0
})

async function load() {
  loading.value = true
  try {
    const [detail, companies] = await Promise.all([
      getOrder(route.params.id),
      listExpressCompanies(true),
    ])
    data.value = detail
    expressCompanies.value = companies || []
    selectedGoodsIds.value = unshippedGoods.value.map((item) => String(item.id))
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)

function fmtPrice(value) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function fullAddress() {
  const address = data.value?.address
  if (!address?.name) return '无收货地址'
  return `${address.province || ''}${address.city || ''}${address.region || ''}${address.detail || ''}`
}

async function submit() {
  errors.expressCompany = form.expressCompany ? '' : '请选择物流公司'
  errors.expressNo = form.expressNo.trim() ? '' : '请输入物流单号'
  errors.goods = selectedGoodsIds.value.length ? '' : '请选择本次发出的商品'
  if (errors.expressCompany || errors.expressNo || errors.goods) return

  submitting.value = true
  try {
    await shipOrder(route.params.id, {
      expressCompany: form.expressCompany,
      expressNo: form.expressNo.trim(),
      orderGoodsIds: selectedGoodsIds.value,
    })
    message.success('发货成功')
    router.replace({ name: 'order-detail', params: { id: route.params.id } })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="breadcrumb">
        <a @click="router.push({ name: 'orders' })">订单</a>
        <span>／</span>
        <a @click="router.push({ name: 'order-detail', params: { id: route.params.id } })">{{ data?.order?.orderNo || '订单详情' }}</a>
        <span>／ 发货</span>
      </div>
      <div class="page-title">订单发货</div>
    </div>
  </div>

  <a-spin :spinning="loading">
    <template v-if="data && canShip">
      <section class="section-band">
        <h2>订单信息</h2>
        <div class="summary-grid">
          <div><span>订单号</span><strong>{{ data.order.orderNo }}</strong></div>
          <div><span>实付金额</span><strong>{{ fmtPrice(data.order.payPrice) }}</strong></div>
          <div><span>收货人</span><strong>{{ data.address?.name || '—' }} {{ data.address?.phone || '' }}</strong></div>
          <div class="address"><span>收货地址</span><strong>{{ fullAddress() }}</strong></div>
        </div>
      </section>

      <section class="section-band">
        <h2>本次发出商品</h2>
        <div class="card goods-table">
          <table class="table">
            <thead><tr><th class="check-col"></th><th>商品</th><th>规格</th><th class="num">数量</th><th class="num">小计</th></tr></thead>
            <tbody>
              <tr v-for="item in unshippedGoods" :key="item.id">
                <td><input v-model="selectedGoodsIds" type="checkbox" :value="String(item.id)" /></td>
                <td class="goods-name"><img v-if="item.image" :src="item.image" alt="" /><span>{{ item.goodsName }}</span></td>
                <td>{{ item.specText || '默认规格' }}</td>
                <td class="num">{{ item.totalNum }}</td>
                <td class="num">{{ fmtPrice(item.totalPrice) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="errors.goods" class="field-error">{{ errors.goods }}</div>
      </section>

      <section class="section-band shipping-form">
        <h2>发货信息</h2>
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>物流公司</label>
          <select v-model="form.expressCompany" class="form-select">
            <option value="">请选择物流公司</option>
            <option v-for="company in expressCompanies" :key="company.id" :value="company.name">{{ company.name }}</option>
          </select>
          <div v-if="errors.expressCompany" class="field-error">{{ errors.expressCompany }}</div>
        </div>
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>物流单号</label>
          <input v-model.trim="form.expressNo" class="form-input" maxlength="64" autocomplete="off" />
          <div v-if="errors.expressNo" class="field-error">{{ errors.expressNo }}</div>
        </div>
        <div class="form-actions">
          <button class="btn" @click="router.push({ name: 'order-detail', params: { id: route.params.id } })">取消</button>
          <button class="btn btn-primary" :disabled="submitting" @click="submit">{{ submitting ? '发货中…' : '确认发货' }}</button>
        </div>
      </section>
    </template>
    <div v-else-if="data && !canShip" class="card card-pad empty-state">
      <div>该订单当前不能发货</div>
      <button class="btn" @click="router.replace({ name: 'order-detail', params: { id: route.params.id } })">返回订单详情</button>
    </div>
    <div v-else-if="!loading" class="card card-pad empty-state">订单不存在</div>
  </a-spin>
</template>

<style scoped>
.breadcrumb { display: flex; gap: 6px; margin-bottom: 6px; font-size: 13px; color: var(--text-muted); }
.breadcrumb a { cursor: pointer; color: var(--primary); }
.section-band { padding: 0 0 24px; margin-bottom: 24px; border-bottom: 1px solid var(--gridline); }
.section-band h2 { margin: 0 0 16px; padding-left: 10px; border-left: 3px solid var(--primary); font-size: 16px; letter-spacing: 0; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1px; background: var(--gridline); border: 1px solid var(--gridline); }
.summary-grid > div { display: flex; flex-direction: column; gap: 6px; min-width: 0; padding: 14px 16px; background: var(--surface); }
.summary-grid span { color: var(--text-muted); font-size: 12px; }
.summary-grid strong { overflow-wrap: anywhere; font-size: 13px; }
.summary-grid .address { grid-column: span 3; }
.goods-table { overflow-x: auto; }
.goods-table table { min-width: 640px; }
.check-col { width: 44px; }
.goods-name { display: flex; align-items: center; gap: 10px; }
.goods-name img { width: 42px; height: 42px; flex-shrink: 0; border-radius: 6px; object-fit: cover; }
.shipping-form { max-width: 720px; border-bottom: 0; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 24px; }
.field-error { margin-top: 5px; color: var(--status-critical); font-size: 12px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 16px; color: var(--text-muted); }
@media (max-width: 760px) {
  .summary-grid { grid-template-columns: 1fr; }
  .summary-grid .address { grid-column: auto; }
}
</style>
