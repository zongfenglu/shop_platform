<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getGroupActive, getGroupRecord } from '@/api/index'

/**
 * 拼团详情。对应原型 h5/group-buy-detail.html。
 * - 选 SKU 后"开团"：跳结算页，groupRecordId 留空，后端 CheckoutAppService 创建团并回写。
 * - 从分享链接进入（带 recordId）：展示拼团进度，"参团"跳结算页带 groupRecordId。
 */
const active = ref(null)
const record = ref(null)
const selectedSkuId = ref(null)
const loading = ref(true)
const now = ref(Date.now())
let timer = null

onLoad(async (query) => {
  const id = Number(query.id)
  const recordId = query.recordId ? Number(query.recordId) : null
  try {
    active.value = await getGroupActive(id)
    const skus = active.value?.skus || []
    if (skus.length) selectedSkuId.value = skus[0].skuId
    if (recordId) {
      record.value = await getGroupRecord(recordId).catch(() => null)
      startClock()
    }
  } finally {
    loading.value = false
  }
})

onShow(() => { now.value = Date.now() })

function startClock() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => { now.value = Date.now() }, 1000)
}

const selectedSku = computed(() => {
  const skus = active.value?.skus || []
  return skus.find((s) => s.skuId === selectedSkuId.value) || null
})

function fmtPrice(v) { return Number(v ?? 0).toFixed(2) }

const expireMs = computed(() => {
  if (!record.value?.expireTime) return null
  const t = new Date(String(record.value.expireTime).replace(' ', 'T')).getTime()
  if (!t) return null
  return Math.max(0, t - now.value)
})

function fmtMs(ms) {
  if (ms == null) return '--:--:--'
  const s = Math.floor(ms / 1000)
  const h = String(Math.floor(s / 3600)).padStart(2, '0')
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
  const sec = String(s % 60).padStart(2, '0')
  return `${h}:${m}:${sec}`
}

const recordStatusText = computed(() => {
  const st = record.value?.status
  return { pending: '拼团中', success: '已成团', fail: '未成团' }[st] || st || ''
})

function goCheckout(join) {
  if (!selectedSku.value) {
    uni.showToast({ title: '请选择规格', icon: 'none' })
    return
  }
  const a = active.value
  const sku = selectedSku.value
  let url = `/pages/order/checkout?skuId=${sku.skuId}&goodsId=${a.goodsId}&quantity=1`
    + `&activityType=group&activityId=${a.id}`
  if (join && record.value?.recordId) {
    url += `&groupRecordId=${record.value.recordId}`
  }
  uni.navigateTo({ url })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="active">
      <!-- 商品头 -->
      <view class="goods-hd">
        <image v-if="active.goodsImage" class="goods-img" :src="active.goodsImage" mode="aspectFill" />
        <view v-else class="goods-img goods-img-ph">无图</view>
        <view class="goods-info">
          <view class="goods-name">{{ active.goodsName || ('商品' + active.goodsId) }}</view>
          <view class="price-row">
            <text class="group-price">¥{{ fmtPrice(selectedSku?.groupPrice) }}</text>
            <text class="origin">¥{{ fmtPrice(selectedSku?.price) }}</text>
          </view>
          <view class="badges">
            <text class="badge">{{ active.groupNum }}人团</text>
            <text class="badge">{{ active.validHours }}小时成团</text>
          </view>
        </view>
      </view>

      <!-- 拼团进度（从分享链接进入时） -->
      <view v-if="record" class="card progress-card">
        <view class="prog-hd">
          <text class="prog-title">{{ recordStatusText }}</text>
          <text v-if="record.status === 'pending' && expireMs != null" class="prog-cd">剩 {{ fmtMs(expireMs) }}</text>
        </view>
        <view class="prog-bar">
          <view class="prog-fill" :style="{ width: Math.min(100, (record.actualNum / record.groupNum) * 100) + '%' }" />
        </view>
        <view class="prog-meta">
          已参团 {{ record.actualNum }}/{{ record.groupNum }} 人
          <text v-if="record.status === 'pending'">· 还差 {{ Math.max(0, record.groupNum - record.actualNum) }} 人成团</text>
        </view>
      </view>

      <!-- SKU 选择 -->
      <view class="card">
        <view class="card-title">选择规格</view>
        <view class="sku-list">
          <view
            v-for="s in active.skus"
            :key="s.skuId"
            class="sku-item"
            :class="{ active: selectedSkuId === s.skuId }"
            @click="selectedSkuId = s.skuId"
          >
            <text class="sku-price">¥{{ fmtPrice(s.groupPrice) }}</text>
            <text class="sku-origin">¥{{ fmtPrice(s.price) }}</text>
            <text class="sku-stock">库存 {{ s.stock ?? 0 }}</text>
          </view>
        </view>
      </view>

      <view class="bottom-spacer" />

      <!-- 操作栏 -->
      <view class="bar">
        <view class="bar-price">
          <text class="bar-label">拼团价</text>
          <text class="bar-amount">¥{{ fmtPrice(selectedSku?.groupPrice) }}</text>
        </view>
        <button v-if="record && record.status === 'pending'" class="bar-btn join" @click="goCheckout(true)">
          参团
        </button>
        <button v-else class="bar-btn" @click="goCheckout(false)">
          开团
        </button>
      </view>
    </view>
    <view v-else class="empty">活动不存在或已结束</view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f3f1ec; padding: 20rpx; padding-bottom: 160rpx; }
.empty { text-align: center; color: #898781; padding: 120rpx 0; font-size: 26rpx; }

.goods-hd {
  display: flex; gap: 20rpx; background: #fff; border-radius: 18rpx; padding: 24rpx; margin-bottom: 18rpx;
}
.goods-img { width: 200rpx; height: 200rpx; border-radius: 12rpx; background: #f0efec; flex-shrink: 0; }
.goods-img-ph { display: flex; align-items: center; justify-content: center; color: #b5b3ad; font-size: 22rpx; }
.goods-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 12rpx; }
.goods-name { font-size: 30rpx; font-weight: 600; line-height: 1.4; }
.price-row { display: flex; align-items: baseline; gap: 14rpx; }
.group-price { color: #d4380d; font-size: 40rpx; font-weight: 800; }
.origin { color: #b5b3ad; font-size: 24rpx; text-decoration: line-through; }
.badges { display: flex; gap: 12rpx; }
.badge { font-size: 22rpx; color: #2a78d6; background: #eaf2fb; padding: 4rpx 14rpx; border-radius: 8rpx; }

.card { background: #fff; border-radius: 18rpx; padding: 24rpx; margin-bottom: 18rpx; }
.card-title { font-size: 28rpx; font-weight: 600; margin-bottom: 18rpx; }

.progress-card { background: linear-gradient(135deg, #fff5f0, #fff); }
.prog-hd { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 14rpx; }
.prog-title { font-size: 30rpx; font-weight: 700; color: #d4380d; }
.prog-cd { font-size: 26rpx; color: #d4380d; font-variant-numeric: tabular-nums; }
.prog-bar { height: 18rpx; border-radius: 9rpx; background: #f0efec; overflow: hidden; }
.prog-fill { height: 100%; background: linear-gradient(90deg, #ff8a3d, #d4380d); }
.prog-meta { font-size: 24rpx; color: #4a4844; margin-top: 12rpx; }

.sku-list { display: flex; flex-wrap: wrap; gap: 16rpx; }
.sku-item {
  display: flex; flex-direction: column; gap: 6rpx;
  border: 2rpx solid rgba(11,11,11,0.1); border-radius: 12rpx; padding: 16rpx 22rpx;
  min-width: 180rpx;
}
.sku-item.active { border-color: #d4380d; background: #fff5f0; }
.sku-price { color: #d4380d; font-size: 28rpx; font-weight: 700; }
.sku-origin { color: #b5b3ad; font-size: 20rpx; text-decoration: line-through; }
.sku-stock { font-size: 20rpx; color: #898781; }

.bottom-spacer { height: 40rpx; }
.bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-top: 2rpx solid rgba(11,11,11,0.08);
  padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.bar-price { display: flex; align-items: baseline; gap: 10rpx; }
.bar-label { font-size: 24rpx; color: #898781; }
.bar-amount { color: #d4380d; font-size: 38rpx; font-weight: 800; }
.bar-btn {
  background: #d4380d; color: #fff; border-radius: 38rpx;
  height: 76rpx; line-height: 76rpx; font-size: 28rpx; padding: 0 56rpx; margin: 0;
}
.bar-btn.join { background: #2a78d6; }
</style>
