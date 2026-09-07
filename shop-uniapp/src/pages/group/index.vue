<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { listGroupActives } from '@/api/index'

/**
 * 拼团专场。对应原型 h5/group-buy.html。
 * 列表展示上架拼团活动，点击进入详情页选 SKU 开团/参团。
 */
const actives = ref([])
const loading = ref(true)

onShow(() => load())

async function load() {
  loading.value = true
  try {
    actives.value = (await listGroupActives()) || []
  } finally {
    loading.value = false
  }
}

function fmtPrice(v) { return Number(v ?? 0).toFixed(2) }

function goDetail(a) {
  uni.navigateTo({ url: `/pages/group/detail?id=${a.id}` })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else>
      <view class="hero">
        <view class="hero-title">拼团专场</view>
        <view class="hero-sub">邀请好友一起拼，人满成团享拼团价</view>
      </view>

      <view v-if="!actives.length" class="empty">暂无进行中的拼团活动</view>
      <view v-for="a in actives" :key="a.id" class="active" @click="goDetail(a)">
        <image v-if="a.goodsImage" class="goods-img" :src="a.goodsImage" mode="aspectFill" />
        <view v-else class="goods-img goods-img-ph">无图</view>
        <view class="info">
          <view class="name">{{ a.goodsName || ('商品' + a.goodsId) }}</view>
          <view class="meta">
            <text class="group-num">{{ a.groupNum }}人团</text>
            <text class="valid">{{ a.validHours }}小时成团</text>
          </view>
          <view class="price-row">
            <text class="group-price">¥{{ fmtPrice(a.minGroupPrice) }}</text>
            <text class="tag">拼团价</text>
          </view>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f3f1ec; padding-bottom: 40rpx; }
.empty { text-align: center; color: #898781; padding: 120rpx 0; font-size: 26rpx; }

.hero {
  background: linear-gradient(135deg, #2a78d6, #4ea0e8);
  color: #fff; padding: 36rpx 28rpx 30rpx;
}
.hero-title { font-size: 40rpx; font-weight: 800; letter-spacing: 2rpx; }
.hero-sub { margin-top: 14rpx; font-size: 26rpx; opacity: 0.92; }

.active {
  display: flex; align-items: center; gap: 18rpx;
  background: #fff; margin: 18rpx 24rpx; border-radius: 18rpx; padding: 22rpx;
  position: relative;
}
.goods-img { width: 160rpx; height: 160rpx; border-radius: 12rpx; background: #f0efec; flex-shrink: 0; }
.goods-img-ph { display: flex; align-items: center; justify-content: center; color: #b5b3ad; font-size: 22rpx; }
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 10rpx; }
.name { font-size: 28rpx; line-height: 1.4; }
.meta { display: flex; gap: 16rpx; font-size: 22rpx; }
.group-num { color: #2a78d6; background: #eaf2fb; padding: 4rpx 12rpx; border-radius: 8rpx; }
.valid { color: #898781; }
.price-row { display: flex; align-items: baseline; gap: 12rpx; }
.group-price { color: #d4380d; font-size: 36rpx; font-weight: 800; }
.tag { font-size: 20rpx; color: #d4380d; border: 2rpx solid #d4380d; border-radius: 6rpx; padding: 2rpx 10rpx; }
.arrow { position: absolute; right: 22rpx; top: 50%; transform: translateY(-50%); color: #b5b3ad; font-size: 34rpx; }
</style>
