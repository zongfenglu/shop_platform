<script setup>
import { onMounted, ref } from 'vue'
import { cancelOrder, pageOrders } from '@/api'

/**
 * 我的订单。对应原型 docs/prototype/h5/order-list.html。
 *
 * 后端 ConsumerOrderController#list 只支持按 orderStatus（normal/cancelled/finished）筛选，
 * 不支持按 payStatus/deliveryStatus 筛选，所以 tab 只能做"全部/进行中/已完成/已取消"三档，
 * 不能像原型那样细分"待付款/待发货/待收货"——那需要 payStatus+deliveryStatus 组合查询，
 * 后端接口目前不支持这种组合。前端拿到列表后仍按单条记录判断展示态（待付款/待发货/待收货文案与按钮），
 * 只是筛选粒度做不到那么细，这是接口能力决定的，不是没做。
 */
const TABS = [
  { key: 'all', label: '全部', orderStatus: '' },
  { key: 'normal', label: '进行中', orderStatus: 'normal' },
  { key: 'finished', label: '已完成', orderStatus: 'finished' },
  { key: 'cancelled', label: '已取消', orderStatus: 'cancelled' },
]

const activeTab = ref('all')
const loading = ref(false)
const rows = ref([])

async function load() {
  loading.value = true
  try {
    const tab = TABS.find((t) => t.key === activeTab.value)
    const page = await pageOrders({ orderStatus: tab.orderStatus || undefined, pageNum: 1, pageSize: 50 })
    rows.value = page.records || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onTabChange(key) {
  activeTab.value = key
  load()
}

function statusText(row) {
  if (row.orderStatus === 'cancelled') return { text: '已取消', cls: 'good' }
  if (row.orderStatus === 'finished') return { text: '已完成', cls: 'good' }
  if (row.payStatus === 'unpaid') return { text: '待付款', cls: 'warning' }
  if (row.deliveryStatus === 'pending') return { text: '待发货', cls: '' }
  if (row.deliveryStatus === 'shipped') return { text: '待收货', cls: '' }
  return { text: '进行中', cls: '' }
}

function fmtPrice(v) {
  return `¥${Number(v ?? 0).toFixed(2)}`
}

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : ''
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/order/detail?id=${id}` })
}

async function onCancel(row) {
  uni.showModal({
    title: '取消订单',
    content: '确认取消该订单？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelOrder(row.id)
        uni.showToast({ title: '已取消' })
        await load()
      } catch (e) {
        // 已由 request.js 统一提示
      }
    },
  })
}

function onAfterSale(row) {
  uni.navigateTo({ url: `/pages/afterSale/apply?orderId=${row.id}` })
}

function onComment(row) {
  uni.navigateTo({ url: `/pages/goods/comment?orderId=${row.id}` })
}
</script>

<template>
  <view class="page">
    <view class="tabs">
      <view v-for="tab in TABS" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }" @click="onTabChange(tab.key)">
        {{ tab.label }}
      </view>
    </view>

    <view class="list">
      <view v-if="!loading && rows.length === 0" class="empty">暂无订单</view>

      <view v-for="row in rows" :key="row.id" class="card" @click="goDetail(row.id)">
        <view class="card-hd">
          <text>{{ fmtDate(row.createTime) }}</text>
          <text class="st" :class="statusText(row).cls">{{ statusText(row).text }}</text>
        </view>
        <view class="goods-row">
          <view class="ph">📦</view>
          <view class="mid">
            <view class="order-no">{{ row.orderNo }}</view>
          </view>
          <view class="price">{{ fmtPrice(row.payPrice) }}</view>
        </view>
        <view class="foot" @click.stop>
          <text class="pay-hint" v-if="row.payStatus === 'unpaid'">应付 {{ fmtPrice(row.payPrice) }}</text>
          <view style="flex: 1"></view>
          <button v-if="row.payStatus === 'unpaid'" class="btn" size="mini" @click="onCancel(row)">取消订单</button>
          <button v-if="row.payStatus === 'unpaid'" class="btn btn-primary" size="mini" @click="goDetail(row.id)">去支付</button>
          <button v-if="row.deliveryStatus === 'shipped'" class="btn" size="mini" @click="onAfterSale(row)">申请售后</button>
          <button v-if="row.deliveryStatus === 'shipped'" class="btn btn-primary" size="mini" @click="goDetail(row.id)">确认收货</button>
          <button v-if="row.orderStatus === 'finished'" class="btn" size="mini" @click="onComment(row)">评价</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
}
.tabs {
  display: flex;
  background: #fff;
  border-bottom: 2rpx solid #e1e0d9;
  position: sticky;
  top: 0;
  z-index: 4;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  font-size: 26rpx;
  color: #52514e;
}
.tab.active {
  color: #2a78d6;
  font-weight: 600;
  border-bottom: 4rpx solid #2a78d6;
}
.list {
  padding: 20rpx 0;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 100rpx 0;
}
.card {
  background: #fff;
  margin: 0 28rpx 20rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(11, 11, 11, 0.06);
}
.card-hd {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 28rpx;
  font-size: 24rpx;
  color: #898781;
  border-bottom: 2rpx solid #e1e0d9;
}
.card-hd .st {
  font-weight: 600;
  color: #898781;
}
.card-hd .st.warning {
  color: #b57a00;
}
.card-hd .st.good {
  color: #898781;
}
.goods-row {
  display: flex;
  gap: 20rpx;
  padding: 24rpx 28rpx;
  align-items: center;
}
.ph {
  width: 96rpx;
  height: 96rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #f3e7e0, #eadad0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  flex-shrink: 0;
}
.mid {
  flex: 1;
}
.order-no {
  font-size: 24rpx;
  color: #52514e;
}
.price {
  font-size: 28rpx;
  font-weight: 600;
}
.foot {
  display: flex;
  align-items: center;
  padding: 0 28rpx 24rpx;
  gap: 16rpx;
}
.pay-hint {
  font-size: 24rpx;
  color: #898781;
}
.btn {
  font-size: 24rpx;
  margin: 0;
  padding: 0 24rpx;
  height: 60rpx;
  line-height: 60rpx;
  border-radius: 30rpx;
  background: #fff;
  border: 2rpx solid rgba(11, 11, 11, 0.16);
}
.btn-primary {
  background: #e34948;
  color: #fff;
  border-color: #e34948;
}
</style>
