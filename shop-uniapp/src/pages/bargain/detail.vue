<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getBargainActive, startBargain, helpBargain, getBargainRecord } from '@/api/index'

/**
 * 砍价详情。对应原型 h5/bargain-detail.html。
 * - 选 SKU → "发起砍价"创建 bargain_record（current_price=SKU 原价）。
 * - "请好友砍一刀"调用 help（演示环境自己点即助力），current_price 递减至 floor_price。
 * - "用当前价下单"跳结算页：activityType=bargain，activityId=bargain_record.id。
 * - 从分享链接进入（带 recordId）直接拉进度，可助力。
 */
const active = ref(null)
const record = ref(null)
const selectedSkuId = ref(null)
const loading = ref(true)
const helping = ref(false)
const starting = ref(false)
const now = ref(Date.now())
let timer = null

onLoad(async (query) => {
  const id = Number(query.id)
  const recordId = query.recordId ? Number(query.recordId) : null
  try {
    active.value = await getBargainActive(id)
    const skus = active.value?.skus || []
    if (skus.length) selectedSkuId.value = skus[0].skuId
    if (recordId) {
      record.value = await getBargainRecord(recordId).catch(() => null)
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

// 砍价进度：从原价砍到底价，已砍比例
const progressPct = computed(() => {
  if (!record.value || !active.value) return 0
  const origin = Number(selectedSku.value?.price || record.value.currentPrice)
  const floor = Number(active.value.floorPrice)
  const cur = Number(record.value.currentPrice)
  if (origin <= floor) return 100
  return Math.min(100, Math.max(0, Math.round(((origin - cur) / (origin - floor)) * 100)))
})

const reachedFloor = computed(() => {
  if (!record.value || !active.value) return false
  return Number(record.value.currentPrice) <= Number(active.value.floorPrice)
})

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
  return { ongoing: '砍价中', done: '已砍到底价', expired: '已过期', ordered: '已下单' }[st] || st || ''
})

async function onStart() {
  if (!selectedSku.value) {
    uni.showToast({ title: '请选择规格', icon: 'none' })
    return
  }
  starting.value = true
  try {
    record.value = await startBargain(active.value.id, selectedSku.value.skuId)
    startClock()
    uni.showToast({ title: '砍价已发起', icon: 'success' })
  } catch (e) {
    // request.js 统一提示
  } finally {
    starting.value = false
  }
}

async function onHelp() {
  if (!record.value) return
  helping.value = true
  try {
    record.value = await helpBargain(record.value.recordId)
  } catch (e) {
    // 已到底价/过期等统一提示
  } finally {
    helping.value = false
  }
}

function goCheckout() {
  if (!record.value) return
  if (record.value.status === 'expired') {
    uni.showToast({ title: '砍价已过期', icon: 'none' })
    return
  }
  if (record.value.status === 'ordered') {
    uni.showToast({ title: '该砍价已下单', icon: 'none' })
    return
  }
  const a = active.value
  uni.navigateTo({
    url: `/pages/order/checkout?skuId=${selectedSku.value?.skuId}&goodsId=${a.goodsId}&quantity=1`
      + `&activityType=bargain&activityId=${record.value.recordId}`,
  })
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
            <text class="floor-price">底价 ¥{{ fmtPrice(active.floorPrice) }}</text>
            <text v-if="selectedSku" class="origin">原价 ¥{{ fmtPrice(selectedSku.price) }}</text>
          </view>
          <view class="badges">
            <text class="badge">{{ active.validHours }}小时有效</text>
            <text v-if="active.helpLimit > 0" class="badge">最多 {{ active.helpLimit }} 刀</text>
          </view>
        </view>
      </view>

      <!-- 未发起：选 SKU + 发起 -->
      <view v-if="!record" class="card">
        <view class="card-title">选择规格</view>
        <view class="sku-list">
          <view
            v-for="s in active.skus"
            :key="s.skuId"
            class="sku-item"
            :class="{ active: selectedSkuId === s.skuId }"
            @click="selectedSkuId = s.skuId"
          >
            <text class="sku-price">¥{{ fmtPrice(s.price) }}</text>
            <text class="sku-stock">库存 {{ s.stock ?? 0 }}</text>
          </view>
        </view>
      </view>

      <!-- 已发起：砍价进度 -->
      <view v-else class="card bargain-card">
        <view class="prog-hd">
          <text class="prog-title">{{ recordStatusText }}</text>
          <text v-if="record.status === 'ongoing' && expireMs != null" class="prog-cd">剩 {{ fmtMs(expireMs) }}</text>
        </view>

        <view class="price-big">
          <text class="cur-label">当前价</text>
          <text class="cur-price">¥{{ fmtPrice(record.currentPrice) }}</text>
        </view>
        <view class="floor-row">底价 ¥{{ fmtPrice(active.floorPrice) }}</view>

        <view class="prog-bar">
          <view class="prog-fill" :style="{ width: progressPct + '%' }" />
        </view>
        <view class="prog-text">已砍 {{ progressPct }}% · 已助力 {{ record.helpCount || 0 }} 次</view>

        <view v-if="record.status === 'ongoing' && !reachedFloor" class="help-tip">
          邀请好友助力，每刀砍掉 5%~15% 剩余空间
        </view>
        <view v-if="reachedFloor && record.status === 'ongoing'" class="help-tip done">
          已砍到底价，快去下单吧
        </view>
      </view>

      <view class="bottom-spacer" />

      <!-- 操作栏 -->
      <view class="bar">
        <template v-if="!record">
          <button class="bar-btn" :disabled="starting" @click="onStart">
            {{ starting ? '发起中…' : '发起砍价' }}
          </button>
        </template>
        <template v-else>
          <button
            v-if="record.status === 'ongoing' && !reachedFloor"
            class="bar-btn help"
            :disabled="helping"
            @click="onHelp"
          >
            {{ helping ? '砍价中…' : '请好友砍一刀' }}
          </button>
          <button class="bar-btn" @click="goCheckout">
            用当前价下单
          </button>
        </template>
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
.floor-price { color: #d4380d; font-size: 32rpx; font-weight: 800; }
.origin { color: #b5b3ad; font-size: 24rpx; }
.badges { display: flex; gap: 12rpx; }
.badge { font-size: 22rpx; color: #722ed1; background: #f4ecff; padding: 4rpx 14rpx; border-radius: 8rpx; }

.card { background: #fff; border-radius: 18rpx; padding: 24rpx; margin-bottom: 18rpx; }
.card-title { font-size: 28rpx; font-weight: 600; margin-bottom: 18rpx; }

.sku-list { display: flex; flex-wrap: wrap; gap: 16rpx; }
.sku-item {
  display: flex; flex-direction: column; gap: 6rpx;
  border: 2rpx solid rgba(11,11,11,0.1); border-radius: 12rpx; padding: 16rpx 22rpx;
  min-width: 180rpx;
}
.sku-item.active { border-color: #722ed1; background: #f4ecff; }
.sku-price { color: #d4380d; font-size: 28rpx; font-weight: 700; }
.sku-stock { font-size: 20rpx; color: #898781; }

.bargain-card { background: linear-gradient(135deg, #f4ecff, #fff); }
.prog-hd { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 18rpx; }
.prog-title { font-size: 30rpx; font-weight: 700; color: #722ed1; }
.prog-cd { font-size: 26rpx; color: #d4380d; font-variant-numeric: tabular-nums; }
.price-big { display: flex; align-items: baseline; gap: 12rpx; justify-content: center; margin: 10rpx 0; }
.cur-label { font-size: 26rpx; color: #898781; }
.cur-price { color: #d4380d; font-size: 64rpx; font-weight: 800; }
.floor-row { text-align: center; font-size: 24rpx; color: #898781; margin-bottom: 18rpx; }
.prog-bar { height: 20rpx; border-radius: 10rpx; background: #f0efec; overflow: hidden; }
.prog-fill { height: 100%; background: linear-gradient(90deg, #9254de, #722ed1); }
.prog-text { font-size: 24rpx; color: #4a4844; margin-top: 12rpx; text-align: center; }
.help-tip { font-size: 24rpx; color: #722ed1; margin-top: 14rpx; text-align: center; }
.help-tip.done { color: #d4380d; font-weight: 600; }

.bottom-spacer { height: 40rpx; }
.bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center; gap: 18rpx;
  background: #fff; border-top: 2rpx solid rgba(11,11,11,0.08);
  padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.bar-btn {
  background: #722ed1; color: #fff; border-radius: 38rpx;
  height: 76rpx; line-height: 76rpx; font-size: 28rpx; padding: 0 56rpx; margin: 0;
  flex: 1; max-width: 460rpx;
}
.bar-btn.help { background: #ff8a3d; }
.bar-btn[disabled] { opacity: 0.6; }
</style>
