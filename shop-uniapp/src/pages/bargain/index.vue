<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { listBargainActives } from '@/api/index'

/**
 * 砍价专场。对应原型 h5/bargain.html。
 * 列表展示上架砍价活动，点击进入详情页发起砍价/邀请好友助力。
 */
const actives = ref([])
const loading = ref(true)

onShow(() => load())

async function load() {
  loading.value = true
  try {
    actives.value = (await listBargainActives()) || []
  } finally {
    loading.value = false
  }
}

function fmtPrice(v) { return Number(v ?? 0).toFixed(2) }

function goDetail(a) {
  uni.navigateTo({ url: `/pages/bargain/detail?id=${a.id}` })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else>
      <view class="hero">
        <view class="hero-title">砍价专场</view>
        <view class="hero-sub">邀请好友助力，砍到最低价再下单</view>
      </view>

      <view v-if="!actives.length" class="empty">暂无进行中的砍价活动</view>
      <view v-for="a in actives" :key="a.id" class="active" @click="goDetail(a)">
        <image v-if="a.goodsImage" class="goods-img" :src="a.goodsImage" mode="aspectFill" />
        <view v-else class="goods-img goods-img-ph">无图</view>
        <view class="info">
          <view class="name">{{ a.goodsName || ('商品' + a.goodsId) }}</view>
          <view class="meta">
            <text class="floor">底价 ¥{{ fmtPrice(a.floorPrice) }}</text>
            <text v-if="a.helpLimit > 0" class="limit">最多砍 {{ a.helpLimit }} 刀</text>
            <text v-else class="limit">砍到最低价</text>
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
  background: linear-gradient(135deg, #722ed1, #9254de);
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
.floor { color: #d4380d; font-weight: 600; }
.limit { color: #898781; }
.arrow { position: absolute; right: 22rpx; top: 50%; transform: translateY(-50%); color: #b5b3ad; font-size: 34rpx; }
</style>
