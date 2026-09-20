<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { getShareConfig } from '@/api'
import ShareButton from '@/components/ShareButton.vue'

const config = ref({
  title: '分享好物',
  subtitle: '把商品、活动分享给朋友，一起享受优惠',
  brand: '商城精选',
  imageUrl: '',
})
const shareImage = computed(() => config.value.imageUrl || '')

onLoad(async () => {
  try {
    const data = await getShareConfig()
    if (data) config.value = { ...config.value, ...data }
  } catch (e) {
    // 使用内置默认海报，不阻断分享页展示。
  }
})

onShareAppMessage(() => ({
  title: config.value.title || '分享好物',
  path: '/pages/index/index?from=share',
  ...(shareImage.value ? { imageUrl: shareImage.value } : {}),
}))
</script>

<template>
  <view class="page">
    <view class="hero">
      <view class="hero-copy">
        <view class="eyebrow">GOOD THINGS TO SHARE</view>
        <view class="hero-title">{{ config.title || '分享好物' }}</view>
        <view class="hero-sub">{{ config.subtitle || '把商品、活动分享给朋友，一起享受优惠' }}</view>
      </view>
      <image v-if="shareImage" class="hero-image" :src="shareImage" mode="aspectFill" />
      <view v-else class="default-art" aria-hidden="true">
        <view class="art-card art-card-back" />
        <view class="art-card art-card-front"><view class="art-dot" /><view class="art-line short" /><view class="art-line" /><view class="art-price">¥</view></view>
      </view>
    </view>
    <view class="poster-card">
      <view class="poster-label">本店推荐</view>
      <view class="poster-brand">{{ config.brand || '商城精选' }}</view>
      <view class="poster-divider" />
      <view class="poster-copy">发现好商品<text>邀请朋友一起逛商城</text></view>
      <view class="poster-hint"><text class="hint-dot" />点击下方分享按钮，发送给微信好友</view>
    </view>
    <view class="actions">
      <ShareButton :title="config.title || '分享好物'" path="/pages/index/index?from=share" />
      <view class="action-note">分享链接会打开商城首页 · 好物一起挑</view>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; background: #f5f6f4; padding-bottom: 56rpx; }
.hero { position: relative; min-height: 300rpx; overflow: hidden; padding: 52rpx 36rpx 62rpx; color: #fff; background: #e85b49; }
.hero::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 14rpx; background: #f6b56c; opacity: .8; }
.hero-copy { position: relative; z-index: 2; max-width: 55%; }
.eyebrow { margin-bottom: 20rpx; color: rgba(255, 255, 255, .75); font-size: 18rpx; letter-spacing: 3rpx; }
.hero-title { font-size: 48rpx; font-weight: 800; line-height: 1.25; }
.hero-sub { margin-top: 18rpx; font-size: 24rpx; line-height: 1.6; opacity: .92; }
.hero-image { position: absolute; z-index: 1; top: 34rpx; right: 28rpx; width: 250rpx; height: 250rpx; border: 8rpx solid rgba(255, 255, 255, .8); border-radius: 18rpx; box-shadow: 0 16rpx 32rpx rgba(110, 39, 27, .2); transform: rotate(4deg); }
.default-art { position: absolute; z-index: 1; top: 30rpx; right: 34rpx; width: 240rpx; height: 242rpx; transform: rotate(4deg); }
.art-card { position: absolute; width: 172rpx; height: 214rpx; border-radius: 14rpx; box-shadow: 0 16rpx 30rpx rgba(110, 39, 27, .2); }
.art-card-back { top: 4rpx; right: 0; background: #f8bd73; transform: rotate(9deg); }
.art-card-front { top: 20rpx; left: 18rpx; box-sizing: border-box; padding: 28rpx 22rpx; background: #fff9ee; transform: rotate(-7deg); }
.art-dot { width: 62rpx; height: 62rpx; margin-bottom: 24rpx; border-radius: 50%; background: #ef7651; }
.art-line { width: 100%; height: 9rpx; margin-top: 12rpx; border-radius: 6rpx; background: #f0b58c; }
.art-line.short { width: 64%; background: #e85b49; }
.art-price { margin-top: 20rpx; color: #e85b49; font-size: 40rpx; font-weight: 800; }
.poster-card { position: relative; margin: -28rpx 28rpx 28rpx; padding: 40rpx 34rpx 42rpx; border: 1rpx solid rgba(220, 205, 193, .7); border-radius: 24rpx; background: #fff; box-shadow: 0 14rpx 36rpx rgba(55, 45, 35, .08); }
.poster-label { display: inline-flex; align-items: center; height: 40rpx; padding: 0 14rpx; border-radius: 20rpx; color: #bf503b; background: #fff0e8; font-size: 20rpx; }
.poster-brand { margin-top: 20rpx; color: #20262e; font-size: 42rpx; font-weight: 800; letter-spacing: 3rpx; }
.poster-divider { width: 100%; height: 3rpx; margin: 26rpx 0 30rpx; background: #f1ddd2; }
.poster-copy { display: flex; flex-direction: column; color: #20262e; font-size: 38rpx; font-weight: 800; line-height: 1.4; }
.poster-copy text { margin-top: 10rpx; color: #8d8f8c; font-size: 25rpx; font-weight: 400; }
.poster-hint { display: flex; align-items: center; margin-top: 40rpx; color: #8d8f8c; font-size: 22rpx; }
.hint-dot { width: 10rpx; height: 10rpx; margin-right: 10rpx; border-radius: 50%; background: #ef7651; }
.actions { display: flex; flex-direction: column; align-items: center; gap: 16rpx; }
.actions :deep(.share-button) { min-width: 220rpx; height: 76rpx; border-radius: 38rpx; background: #e85b49; color: #fff; box-shadow: 0 10rpx 22rpx rgba(232, 91, 73, .22); font-size: 26rpx; }
.action-note { color: #999b98; font-size: 22rpx; }
</style>
