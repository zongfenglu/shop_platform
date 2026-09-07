<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getMyCoupons } from '@/api/index'

const tabs = [
  { key: 'unused', label: '未使用' },
  { key: 'used', label: '已使用' },
  { key: 'expired', label: '已过期' },
]
const active = ref('unused')
const list = ref([])

onShow(() => load())

async function load() {
  try {
    list.value = (await getMyCoupons(active.value)) || []
  } catch (e) {
    // 拦截器已提示
  }
}

function switchTab(key) {
  active.value = key
  load()
}

function face(s) {
  if (!s) return '券'
  if (s.type === 'discount') return (Number(s.discountRatio) * 10).toFixed(1).replace(/\.0$/, '') + '折'
  return '¥' + s.reducePrice
}
function condition(s) {
  if (!s) return ''
  return Number(s.minPrice) > 0 ? `满${s.minPrice}可用` : '无门槛'
}
function validity(uc) {
  if (!uc.endTime) return ''
  return `${uc.startTime || ''} ~ ${uc.endTime}`
}
</script>

<template>
  <view class="page">
    <view class="tabs">
      <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: active === t.key }" @click="switchTab(t.key)">{{ t.label }}</view>
    </view>

    <view v-if="!list.length" class="empty">暂无优惠券</view>
    <view v-for="uc in list" :key="uc.id" class="coupon" :class="{ dim: uc.status !== 'unused' }">
      <view class="left">
        <view class="amt">{{ face(uc.snapshot) }}</view>
        <view class="cond">{{ condition(uc.snapshot) }}</view>
      </view>
      <view class="right">
        <view class="name">{{ uc.snapshot?.name || '优惠券' }}</view>
        <view class="meta">{{ validity(uc) }}</view>
        <view class="status">{{ { unused: '可使用', used: '已使用', expired: '已过期' }[uc.status] }}</view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f9f9f7; padding-bottom: 40rpx; }
.tabs { display: flex; background: #fff; position: sticky; top: 0; z-index: 2; }
.tab { flex: 1; text-align: center; padding: 24rpx 0; font-size: 26rpx; color: #52514e; }
.tab.active { color: #e34948; font-weight: 600; border-bottom: 4rpx solid #e34948; }
.empty { text-align: center; color: #898781; padding: 120rpx 0; }
.coupon { display: flex; background: #fff; border-radius: 20rpx; overflow: hidden; margin: 20rpx 24rpx; }
.coupon.dim { opacity: 0.5; }
.left { width: 200rpx; flex-shrink: 0; background: linear-gradient(135deg, #fff3ec, #ffe4d6); color: #e34948; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.amt { font-size: 44rpx; font-weight: 800; }
.cond { font-size: 22rpx; margin-top: 6rpx; }
.right { flex: 1; padding: 24rpx; position: relative; }
.name { font-size: 28rpx; font-weight: 600; }
.meta { font-size: 22rpx; color: #898781; margin-top: 8rpx; }
.status { position: absolute; right: 24rpx; top: 24rpx; font-size: 22rpx; color: #898781; }
</style>
