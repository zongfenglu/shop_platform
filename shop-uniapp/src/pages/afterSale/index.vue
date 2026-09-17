<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import { listMyAfterSales, pageOrders } from '@/api'

const TABS = [
  { key: 'apply', label: '售后申请' },
  { key: 'processing', label: '处理中' },
  { key: 'review', label: '待评价' },
  { key: 'records', label: '申请记录' },
]

const activeTab = ref('apply')
const keyword = ref('')
const loading = ref(false)
const rows = ref([])

const STATUS_META = {
  applying: { text: '等待商家处理', cls: 'warning' },
  approved: { text: '商家已同意', cls: 'good' },
  return_shipped: { text: '等待商家收货', cls: 'warning' },
  refunding: { text: '退款处理中', cls: 'warning' },
  refunded: { text: '退款完成', cls: 'muted' },
  rejected: { text: '商家已拒绝', cls: 'critical' },
  closed: { text: '申请已关闭', cls: 'muted' },
}

const visibleRows = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return rows.value
  return rows.value.filter((row) => [row.goodsName, row.orderNo, row.specText]
    .some((value) => String(value || '').toLowerCase().includes(text)))
})

onShow(load)
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function load() {
  loading.value = true
  try {
    if (activeTab.value === 'apply') {
      const page = await pageOrders({ pageNum: 1, pageSize: 50 })
      rows.value = (page.records || []).filter((row) => (
        row.payStatus === 'paid'
        && row.orderStatus !== 'cancelled'
        && row.canApplyAfterSale === true
      ))
    } else {
      const page = await listMyAfterSales({ scope: activeTab.value, pageNum: 1, pageSize: 50 })
      rows.value = page.records || []
    }
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function changeTab(key) {
  if (activeTab.value === key) return
  activeTab.value = key
  keyword.value = ''
  load()
}

function statusOf(row) {
  return STATUS_META[row.status] || { text: row.status || '售后处理中', cls: 'muted' }
}

function fmtPrice(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function goApply(row) {
  uni.navigateTo({ url: `/pages/afterSale/apply?orderId=${row.id}` })
}

function goOrder(row) {
  uni.navigateTo({ url: `/pages/order/detail?id=${row.id}` })
}

function goAfterSale(row) {
  uni.navigateTo({ url: `/pages/afterSale/detail?id=${row.id}` })
}
</script>

<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="tab in TABS"
        :key="tab.key"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        @click="changeTab(tab.key)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="search-row">
      <view class="search-box">
        <text class="search-mark">⌕</text>
        <input v-model="keyword" class="search-input" placeholder="商品名称/订单编号" confirm-type="search" />
      </view>
    </view>

    <view class="list">
      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="visibleRows.length === 0" class="empty">
        {{ keyword ? '没有匹配的记录' : (activeTab === 'apply' ? '暂无可申请售后的订单' : '暂无售后记录') }}
      </view>

      <view v-for="row in visibleRows" :key="row.id" class="sale-card">
        <view class="card-head">
          <view class="store-name"><text class="store-symbol">店</text><text>本店订单</text></view>
          <text v-if="activeTab !== 'apply'" class="status" :class="statusOf(row).cls">{{ statusOf(row).text }}</text>
          <text v-else class="order-link" @click="goOrder(row)">订单详情 ›</text>
        </view>

        <view class="goods-row" @click="activeTab === 'apply' ? goOrder(row) : goAfterSale(row)">
          <image v-if="row.goodsImage" class="goods-image" :src="row.goodsImage" mode="aspectFill" />
          <view v-else class="goods-image placeholder"><AppIcon name="package-open-muted" :size="25" /></view>
          <view class="goods-body">
            <view class="goods-name">{{ row.goodsName || '订单商品' }}</view>
            <view class="goods-spec">{{ row.specText || '默认规格' }} · 数量 {{ row.goodsNum || row.goodsCount || 1 }}</view>
            <view class="order-no">订单号 {{ row.orderNo || '—' }}</view>
            <view v-if="activeTab !== 'apply'" class="reason">{{ row.applyReason || '售后申请' }} · {{ fmtPrice(row.refundAmount) }}</view>
          </view>
        </view>

        <view class="card-actions">
          <button v-if="activeTab === 'apply'" class="action-btn" @click="goApply(row)">退款/售后</button>
          <button v-else class="action-btn secondary" @click="goOrder({ id: row.orderId })">订单详情</button>
          <button v-if="activeTab !== 'apply'" class="action-btn" @click="goAfterSale(row)">售后详情</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f4f5f3; color: #20262e; }
.tabs { position: sticky; top: 0; z-index: 5; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); background: #fff; border-bottom: 1rpx solid #e4e6e3; }
.tab { position: relative; height: 86rpx; display: flex; align-items: center; justify-content: center; color: #555c61; font-size: 25rpx; white-space: nowrap; }
.tab.active { color: #20262e; font-weight: 700; }
.tab.active::after { content: ''; position: absolute; bottom: 0; left: 24%; right: 24%; height: 5rpx; border-radius: 4rpx; background: #e34948; }
.search-row { padding: 18rpx 24rpx; background: #fff; }
.search-box { height: 68rpx; display: flex; align-items: center; gap: 12rpx; padding: 0 22rpx; border-radius: 8rpx; background: #f4f5f3; }
.search-mark { color: #8f969a; font-size: 35rpx; line-height: 1; }
.search-input { flex: 1; min-width: 0; height: 68rpx; color: #30363b; font-size: 24rpx; }
.list { padding: 20rpx 24rpx 40rpx; }
.empty { padding: 120rpx 20rpx; text-align: center; color: #969ca0; font-size: 24rpx; }
.sale-card { margin-bottom: 20rpx; overflow: hidden; border: 1rpx solid #e4e6e3; border-radius: 12rpx; background: #fff; }
.card-head { min-height: 72rpx; padding: 0 22rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 1rpx solid #eceeeb; }
.store-name { display: flex; align-items: center; gap: 10rpx; min-width: 0; font-size: 23rpx; color: #596066; }
.store-symbol { width: 34rpx; height: 34rpx; display: flex; align-items: center; justify-content: center; border-radius: 8rpx; color: #fff; background: #e86a4a; font-size: 18rpx; }
.order-link { color: #8c9397; font-size: 21rpx; }
.status { font-size: 22rpx; color: #287e62; }.status.warning { color: #a66d13; }.status.critical { color: #c94d45; }.status.muted { color: #969ca0; }
.goods-row { display: flex; gap: 18rpx; padding: 24rpx 22rpx 18rpx; }
.goods-image { width: 128rpx; height: 128rpx; flex: 0 0 128rpx; border-radius: 8rpx; background: #f0f2ef; }
.placeholder { display: flex; align-items: center; justify-content: center; }
.goods-body { flex: 1; min-width: 0; }
.goods-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #252b30; font-size: 25rpx; font-weight: 600; }
.goods-spec, .order-no { margin-top: 8rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #92989c; font-size: 21rpx; }
.reason { margin-top: 9rpx; color: #a66d13; font-size: 21rpx; }
.card-actions { min-height: 76rpx; padding: 0 22rpx 18rpx; display: flex; justify-content: flex-end; align-items: center; gap: 12rpx; }
.action-btn { min-width: 146rpx; height: 58rpx; margin: 0; padding: 0 22rpx; border: 2rpx solid #d84b3e; border-radius: 8rpx; background: #fff; color: #c53f35; font-size: 22rpx; line-height: 54rpx; }
.action-btn::after { border: 0; }.action-btn.secondary { border-color: #d9dcda; color: #555c61; }
</style>
