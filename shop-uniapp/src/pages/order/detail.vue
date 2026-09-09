<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { cancelOrder, confirmReceipt, getOrder, getOrderTracks } from '@/api'

/**
 * 订单详情。对应原型 docs/prototype/h5/order-detail.html。
 * 后端 detail 一次性返回 order/goodsList/address/packages（见 ConsumerOrderController#detail）。
 */
const orderId = ref(null)
const loading = ref(false)
const data = ref(null)
const tracks = ref([])
const tracksLoading = ref(false)
const activePackageId = ref('')

onLoad((query) => {
  orderId.value = query.id
  load()
})

async function load() {
  loading.value = true
  try {
    data.value = await getOrder(orderId.value)
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
}

async function loadTracks(packageId) {
  tracksLoading.value = true
  try {
    tracks.value = await getOrderTracks(orderId.value, packageId)
  } catch (e) {
    tracks.value = []
  } finally {
    tracksLoading.value = false
  }
}

function fmtPrice(v) {
  return `¥${Number(v ?? 0).toFixed(2)}`
}

function fmtDateTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '—'
}

function statusText() {
  const o = data.value?.order
  if (!o) return ''
  if (o.orderStatus === 'cancelled') return '已取消'
  if (o.orderStatus === 'finished') return '已完成'
  if (o.payStatus === 'unpaid') return '待付款'
  if (o.deliveryType === 'pickup' && o.deliveryStatus === 'pending') return '待自提'
  if (o.deliveryStatus === 'pending') return '待发货'
  if (o.deliveryStatus === 'shipped') return '待收货'
  return '处理中'
}

function onConfirm() {
  uni.showModal({
    title: '确认收货',
    content: '确认已收到商品？确认后将无法撤销',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await confirmReceipt(orderId.value)
        uni.showToast({ title: '已确认收货' })
        await load()
      } catch (e) {
        // 已由 request.js 提示
      }
    },
  })
}

function onCancel() {
  uni.showModal({
    title: '取消订单',
    content: '确认取消该订单？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelOrder(orderId.value)
        uni.showToast({ title: '已取消' })
        await load()
      } catch (e) {
        // 已由 request.js 提示
      }
    },
  })
}

function onAfterSale() {
  uni.navigateTo({ url: `/pages/afterSale/apply?orderId=${orderId.value}` })
}

function onComment() {
  uni.navigateTo({ url: `/pages/goods/comment?orderId=${orderId.value}` })
}

const showTracks = ref(false)
async function onToggleTracks(pkg) {
  const id = pkg?.id == null ? '' : String(pkg.id)
  if (showTracks.value && activePackageId.value === id) {
    showTracks.value = false
    return
  }
  activePackageId.value = id
  showTracks.value = true
  tracks.value = []
  await loadTracks(pkg?.id)
}

function goodsNameOf(id) {
  const g = (data.value?.goodsList || []).find((row) => String(row.id) === String(id))
  return g ? g.goodsName : ''
}

function packageGoodsText(pkg) {
  // orderGoodsIds 是后端 List<Long> 序列化的裸数字 JSON，JSON.parse 会把 19 位雪花 id
  // 取整成另一个 id 导致名称匹配不上，这里只按数字串提取（见 CONTRIBUTING.md）
  const ids = String(pkg.orderGoodsIds || '').match(/\d+/g) || []
  if (!ids.length) return '整单'
  return ids.map((id) => goodsNameOf(id) || `#${id}`).join('、')
}
</script>

<template>
  <view class="page">
    <view v-if="data" class="hero" :class="{ dim: data.order.orderStatus === 'cancelled' }">
      <view class="hero-title">{{ statusText() }}</view>
    </view>

    <view v-if="data" class="card">
      <template v-if="data.order.deliveryType === 'pickup'">
        <view class="row">
          <view class="addr-name">{{ data.pickupStore?.name || '自提门店' }}</view>
          <view class="addr-detail">{{ [data.pickupStore?.region, data.pickupStore?.detail].filter(Boolean).join(' ') || '门店地址待完善' }}</view>
        </view>
        <view v-if="data.order.payStatus === 'paid' && data.order.deliveryStatus === 'pending'" class="pickup-code-box">
          <view class="pickup-code-label">自提核销码</view>
          <view class="pickup-code-value">{{ data.order.pickupCode }}</view>
          <view class="pickup-code-hint">到店出示此码，由店员核销</view>
        </view>
      </template>
      <template v-else>
        <view class="row" v-if="data.address && data.address.name">
          <view class="addr-name">{{ data.address.name }} {{ data.address.phone }}</view>
          <view class="addr-detail">{{ data.address.province }}{{ data.address.city }}{{ data.address.region }}{{ data.address.detail }}</view>
        </view>
        <view v-else class="row muted">门店自提或虚拟商品，无收货地址</view>
      </template>
    </view>

    <view v-if="data" class="card">
      <view v-for="g in data.goodsList" :key="g.id" class="goods-row">
        <view class="ph">📦</view>
        <view class="mid">
          <view class="g-name">{{ g.goodsName }}</view>
          <view class="g-spec">{{ g.specText || '默认规格' }} ×{{ g.totalNum }}</view>
        </view>
        <view class="price">{{ fmtPrice(g.totalPrice) }}</view>
      </view>
    </view>

    <view v-if="data" class="card">
      <view class="kv-row"><text>商品总额</text><text>{{ fmtPrice(data.order.totalPrice) }}</text></view>
      <view class="kv-row"><text>运费</text><text>{{ fmtPrice(data.order.expressPrice) }}</text></view>
      <view class="kv-row" style="font-weight: 700; color: #0b0b0b">
        <text>实付金额</text><text style="color: #e34948">{{ fmtPrice(data.order.payPrice) }}</text>
      </view>
    </view>

    <view v-if="data" class="card">
      <view class="kv-row"><text>订单编号</text><text>{{ data.order.orderNo }}</text></view>
      <view class="kv-row"><text>下单时间</text><text>{{ fmtDateTime(data.order.createTime) }}</text></view>
      <view class="kv-row"><text>支付方式</text><text>{{ data.order.payMethod || '—' }}</text></view>
      <view
        v-for="pkg in (data.packages || [])"
        :key="pkg.id"
        class="kv-row"
        @click="onToggleTracks(pkg)"
      >
        <text>{{ pkg.expressCompany }}</text>
        <text>{{ pkg.expressNo }} · {{ packageGoodsText(pkg) }} ›</text>
      </view>
    </view>

    <view v-if="showTracks" class="card">
      <view v-if="tracksLoading" class="row muted">加载中…</view>
      <view v-else-if="tracks.length === 0" class="row muted">暂无物流轨迹</view>
      <view v-else v-for="(t, i) in tracks" :key="i" class="track-row">
        <view class="track-desc">{{ t.description }}</view>
        <view class="track-meta">{{ t.location }} · {{ fmtDateTime(t.time) }}</view>
      </view>
    </view>

    <view v-if="data" class="bottom-bar">
      <button v-if="data.order.payStatus === 'unpaid'" class="m-btn" @click="onCancel">取消订单</button>
      <button v-if="data.order.payStatus === 'unpaid'" class="m-btn m-btn-warm">去支付</button>
      <button v-if="data.order.deliveryStatus === 'shipped'" class="m-btn" @click="onAfterSale">申请售后</button>
      <button v-if="data.order.deliveryStatus === 'shipped'" class="m-btn m-btn-warm" @click="onConfirm">确认收货</button>
      <button v-if="data.order.orderStatus === 'finished'" class="m-btn m-btn-warm" @click="onComment">评价</button>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
  padding-bottom: 140rpx;
}
.hero {
  background: linear-gradient(135deg, #ff9143, #e34948);
  padding: 40rpx 32rpx;
}
.hero.dim {
  background: #898781;
}
.hero-title {
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
}
.card {
  background: #fff;
  margin: 20rpx 28rpx;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}
.row {
  padding: 8rpx 0;
}
.row.muted {
  color: #898781;
  font-size: 24rpx;
}
.addr-name {
  font-size: 28rpx;
  font-weight: 600;
}
.addr-detail {
  font-size: 24rpx;
  color: #52514e;
  margin-top: 6rpx;
}
.pickup-code-box {
  margin-top: 20rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #fff5f0;
  text-align: center;
}
.pickup-code-label {
  font-size: 22rpx;
  color: #898781;
}
.pickup-code-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #d4380d;
  letter-spacing: 8rpx;
  margin: 10rpx 0;
}
.pickup-code-hint {
  font-size: 20rpx;
  color: #898781;
}
.goods-row {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 12rpx 0;
}
.ph {
  width: 88rpx;
  height: 88rpx;
  border-radius: 14rpx;
  background: linear-gradient(135deg, #f3e7e0, #eadad0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  flex-shrink: 0;
}
.mid {
  flex: 1;
}
.g-name {
  font-size: 26rpx;
}
.g-spec {
  font-size: 22rpx;
  color: #898781;
  margin-top: 6rpx;
}
.price {
  font-size: 26rpx;
  font-weight: 600;
}
.kv-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  font-size: 24rpx;
  color: #52514e;
}
.track-row {
  padding: 14rpx 0;
  border-bottom: 2rpx dashed #e1e0d9;
}
.track-row:last-child {
  border-bottom: none;
}
.track-desc {
  font-size: 24rpx;
}
.track-meta {
  font-size: 20rpx;
  color: #898781;
  margin-top: 4rpx;
}
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 16rpx 28rpx;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  box-shadow: 0 -2rpx 8rpx rgba(11, 11, 11, 0.06);
}
.m-btn {
  font-size: 26rpx;
  margin: 0;
  padding: 0 28rpx;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: 34rpx;
  background: #fff;
  border: 2rpx solid rgba(11, 11, 11, 0.16);
}
.m-btn-warm {
  background: #e34948;
  color: #fff;
  border-color: #e34948;
}
</style>
