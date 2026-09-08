<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getReceivableCoupons, receiveCoupon } from '@/api/index'

const list = ref([])
const loading = ref(false)

onShow(() => load())

async function load() {
  loading.value = true
  try {
    list.value = (await getReceivableCoupons()) || []
  } finally {
    loading.value = false
  }
}

function face(c) {
  if (c.type === 'discount') return (Number(c.discountRatio) * 10).toFixed(1).replace(/\.0$/, '') + '折'
  return '¥' + c.reducePrice
}
function condition(c) {
  return Number(c.minPrice) > 0 ? `满${c.minPrice}可用` : '无门槛'
}
function validity(c) {
  if (c.expireType === 'receive') return `领取后 ${c.expireDays || 7} 天内有效`
  return `${c.startTime || ''} ~ ${c.endTime || ''}`
}

async function receive(c) {
  try {
    await receiveCoupon(c.id)
    uni.showToast({ title: '领取成功' })
    await load()
  } catch (e) {
    // 拦截器已提示
  }
}
</script>

<template>
  <view class="page">
    <view v-if="!loading && !list.length" class="empty">暂无可领取的优惠券</view>
    <view v-for="c in list" :key="c.id" class="coupon">
      <view class="left">
        <view class="amt">{{ face(c) }}</view>
        <view class="cond">{{ condition(c) }}</view>
      </view>
      <view class="right">
        <view class="name">{{ c.name }}</view>
        <view class="meta">{{ validity(c) }}</view>
        <view class="meta">每人限领 {{ c.limitPerUser > 0 ? c.limitPerUser : '不限' }} 张 · 已领 {{ c.receivedNum || 0 }}/{{ c.totalNum || 0 }}</view>
        <button class="btn" size="mini" @click="receive(c)">立即领取</button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f9f9f7; padding: 24rpx; }
.empty { text-align: center; color: #898781; padding: 120rpx 0; }
.coupon { display: flex; background: #fff; border-radius: 20rpx; overflow: hidden; margin-bottom: 20rpx; }
.left { width: 200rpx; flex-shrink: 0; background: linear-gradient(135deg, #fff3ec, #ffe4d6); color: #e34948; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.amt { font-size: 44rpx; font-weight: 800; }
.cond { font-size: 22rpx; margin-top: 6rpx; }
.right { flex: 1; padding: 24rpx; }
.name { font-size: 28rpx; font-weight: 600; }
.meta { font-size: 22rpx; color: #898781; margin-top: 8rpx; }
.btn { margin-top: 16rpx; background: #e34948; color: #fff; border: 0; font-size: 24rpx; }
</style>
