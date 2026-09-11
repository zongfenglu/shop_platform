<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { cancelOrder, getOrder, shipOrder } from '@/api/order'
import { listExpressCompanies } from '@/api/operationSettings'

/**
 * 订单详情。对应原型 docs/prototype/store/order-detail.html。
 * 后端 detail 接口一次性返回 order/goodsList/address/packages，见 StoreOrderController#detail。
 */
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const data = ref(null)
const expressCompanies = ref([])

async function load() {
  loading.value = true
  try {
    data.value = await getOrder(route.params.id)
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    load(),
    listExpressCompanies(true).then((items) => { expressCompanies.value = items || [] }).catch(() => {}),
  ])
})

function fmtPrice(v) {
  if (v === null || v === undefined) return '—'
  return `¥${Number(v).toFixed(2)}`
}

function fmtDateTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '—'
}

function parsePkgIds(pkg) {
  try {
    const ids = JSON.parse(pkg?.orderGoodsIds || '[]')
    return Array.isArray(ids) ? ids.map((id) => String(id)) : []
  } catch (e) {
    return []
  }
}

function shippedIdSet() {
  const goodsIds = (data.value?.goodsList || []).map((g) => String(g.id))
  const set = new Set()
  for (const pkg of data.value?.packages || []) {
    const ids = parsePkgIds(pkg)
    if (ids.length === 0) {
      return new Set(goodsIds)
    }
    ids.forEach((id) => set.add(id))
  }
  return set
}

function unshippedGoods() {
  const shipped = shippedIdSet()
  return (data.value?.goodsList || []).filter((g) => !shipped.has(String(g.id)))
}

function isShipped(g) {
  return shippedIdSet().has(String(g.id))
}

function packageGoodsText(pkg) {
  const ids = parsePkgIds(pkg)
  if (!ids.length) return '整单'
  const names = (data.value?.goodsList || [])
    .filter((g) => ids.includes(String(g.id)))
    .map((g) => g.goodsName)
  return names.length ? names.join('、') : `${ids.length} 件商品`
}

function canShip() {
  const o = data.value?.order
  return o
    && o.payStatus === 'paid'
    && o.deliveryType !== 'pickup'
    && o.deliveryStatus !== 'received'
    && o.orderStatus !== 'cancelled'
    && o.orderStatus !== 'finished'
    && unshippedGoods().length > 0
}

function canCancel() {
  const o = data.value?.order
  return o && o.payStatus === 'unpaid'
}

// ---------- 发货弹窗 ----------
const shipModalOpen = ref(false)
const shipping = ref(false)
const shipForm = reactive({ expressCompany: '', expressNo: '' })
const shipErrors = reactive({ expressCompany: '', expressNo: '' })
const selectedGoodsIds = ref([])

function openShipModal() {
  shipForm.expressCompany = ''
  shipForm.expressNo = ''
  shipErrors.expressCompany = ''
  shipErrors.expressNo = ''
  selectedGoodsIds.value = unshippedGoods().map((g) => String(g.id))
  shipModalOpen.value = true
}

async function onShip() {
  shipErrors.expressCompany = shipForm.expressCompany.trim() ? '' : '请选择快递公司'
  shipErrors.expressNo = shipForm.expressNo.trim() ? '' : '请输入快递单号'
  if (shipErrors.expressCompany || shipErrors.expressNo) return
  if (!selectedGoodsIds.value.length) {
    message.warning('请选择要发出的商品')
    return
  }

  shipping.value = true
  try {
    await shipOrder(route.params.id, {
      expressCompany: shipForm.expressCompany.trim(),
      expressNo: shipForm.expressNo.trim(),
      orderGoodsIds: selectedGoodsIds.value,
    })
    message.success('发货成功')
    shipModalOpen.value = false
    await load()
  } catch (e) {
    // 已由拦截器提示
  } finally {
    shipping.value = false
  }
}

function onCancel() {
  Modal.confirm({
    title: '确认关闭该订单？',
    content: '关单后会自动回补库存，且不可撤销',
    okText: '关闭订单',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await cancelOrder(route.params.id, '商户关闭订单')
        message.success('订单已关闭')
        await load()
      } catch (e) {
        // 已由拦截器提示
      }
    },
  })
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="breadcrumb" style="margin-bottom: 6px">
        <a @click="router.push({ name: 'orders' })" style="cursor: pointer">订单</a> ／ <b>{{ data?.order?.orderNo || '订单详情' }}</b>
      </div>
      <div class="page-title">{{ data?.order?.orderNo || '加载中…' }}</div>
    </div>
    <div v-if="data" style="display: flex; gap: 10px">
      <button v-if="canCancel()" class="btn" @click="onCancel">关闭订单</button>
      <button v-if="canShip()" class="btn btn-primary" @click="openShipModal">
        {{ data.packages?.length ? '继续发货' : '发货' }}
      </button>
    </div>
  </div>

  <a-spin :spinning="loading">
    <template v-if="data">
      <div class="card card-pad" style="margin-bottom: 14px">
        <p class="card-title">订单信息</p>
        <div class="kv-row"><div class="k">订单号</div><div class="v">{{ data.order.orderNo }}</div></div>
        <div class="kv-row"><div class="k">下单时间</div><div class="v">{{ fmtDateTime(data.order.createTime) }}</div></div>
        <div class="kv-row"><div class="k">支付方式</div><div class="v">{{ data.order.payMethod || '—' }}</div></div>
        <div class="kv-row"><div class="k">支付时间</div><div class="v">{{ fmtDateTime(data.order.payTime) }}</div></div>
        <div class="kv-row"><div class="k">买家备注</div><div class="v">{{ data.order.buyerRemark || '—' }}</div></div>
      </div>

      <div class="card card-pad" style="margin-bottom: 14px">
        <p class="card-title">收货地址</p>
        <template v-if="data.address && data.address.name">
          <div class="kv-row"><div class="k">收货人</div><div class="v">{{ data.address.name }} {{ data.address.phone }}</div></div>
          <div class="kv-row"><div class="k">收货地址</div><div class="v">{{ data.address.province }}{{ data.address.city }}{{ data.address.region }}{{ data.address.detail }}</div></div>
        </template>
        <div v-else style="color: var(--text-muted); font-size: 13px">门店自提或虚拟商品，无收货地址</div>
      </div>

      <div class="card" style="margin-bottom: 14px">
        <table class="table">
          <thead>
            <tr>
              <th>商品</th>
              <th>规格</th>
              <th class="num">单价</th>
              <th class="num">数量</th>
              <th class="num">小计</th>
              <th>发货</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="g in data.goodsList" :key="g.id">
              <td>
                <div style="display: flex; align-items: center; gap: 10px">
                  <img v-if="g.image" :src="g.image" alt="" style="width: 40px; height: 40px; border-radius: 6px; object-fit: cover" />
                  <div>{{ g.goodsName }}</div>
                </div>
              </td>
              <td>{{ g.specText || '默认规格' }}</td>
              <td class="num">{{ fmtPrice(g.goodsPrice) }}</td>
              <td class="num">{{ g.totalNum }}</td>
              <td class="num">{{ fmtPrice(g.totalPrice) }}</td>
              <td>
                <span class="tag" :class="isShipped(g) ? 'tag-good' : 'tag-warning'">
                  {{ isShipped(g) ? '已发' : '待发' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card card-pad" style="margin-bottom: 14px; max-width: 400px; margin-left: auto">
        <div class="kv-row"><div class="k">商品总额</div><div class="v">{{ fmtPrice(data.order.totalPrice) }}</div></div>
        <div class="kv-row"><div class="k">运费</div><div class="v">{{ fmtPrice(data.order.expressPrice) }}</div></div>
        <div class="kv-row"><div class="k">优惠</div><div class="v">-{{ fmtPrice(data.order.discountPrice) }}</div></div>
        <div class="kv-row"><div class="k" style="font-weight: 600; color: var(--text-primary)">实付金额</div><div class="v" style="color: var(--price); font-weight: 700">{{ fmtPrice(data.order.payPrice) }}</div></div>
      </div>

      <div v-if="data.packages && data.packages.length" class="card card-pad">
        <p class="card-title">物流信息</p>
        <div v-for="(pkg, idx) in data.packages" :key="pkg.id" class="kv-row">
          <div class="k">包裹 {{ idx + 1 }}</div>
          <div class="v">{{ pkg.expressCompany }} {{ pkg.expressNo }} · {{ packageGoodsText(pkg) }}</div>
        </div>
      </div>
    </template>
    <div v-else-if="!loading" class="card card-pad" style="text-align: center; color: var(--text-muted)">
      订单不存在
    </div>
  </a-spin>

  <a-modal
    v-model:open="shipModalOpen"
    title="订单发货"
    :confirm-loading="shipping"
    ok-text="确认发货"
    cancel-text="取消"
    @ok="onShip"
  >
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>快递公司</label>
      <select v-model="shipForm.expressCompany" class="form-select">
        <option value="">请选择快递公司</option>
        <option v-for="company in expressCompanies" :key="company.id" :value="company.name">{{ company.name }}</option>
      </select>
      <div v-if="shipErrors.expressCompany" class="field-error">{{ shipErrors.expressCompany }}</div>
    </div>
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>快递单号</label>
      <input v-model="shipForm.expressNo" class="form-input" />
      <div v-if="shipErrors.expressNo" class="field-error">{{ shipErrors.expressNo }}</div>
    </div>
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>本次发出</label>
      <label v-for="g in unshippedGoods()" :key="g.id" class="ship-line">
        <input type="checkbox" :value="String(g.id)" v-model="selectedGoodsIds" />
        <span>{{ g.goodsName }} {{ g.specText || '默认规格' }} ×{{ g.totalNum }}</span>
      </label>
      <div class="form-hint">不勾选的商品留在本单，可之后继续发货，每次生成一个包裹。</div>
    </div>
  </a-modal>
</template>

<style scoped>
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
  width: 100px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.kv-row .v {
  font-weight: 500;
  flex: 1;
}
.field-error {
  font-size: 12px;
  color: var(--status-critical);
  margin-top: 4px;
}
.ship-line {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}
</style>
