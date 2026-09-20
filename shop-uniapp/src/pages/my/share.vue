<script setup>
import { onShareAppMessage } from '@dcloudio/uni-app'
import ShareButton from '@/components/ShareButton.vue'

onShareAppMessage(() => ({
  title: '欢迎进入我的商城，分享好物一起买',
  path: '/pages/index/index?from=share',
}))
</script>

<template>
  <view class="page">
    <view class="hero">
      <view class="hero-title">分享好物</view>
      <view class="hero-sub">把商品、活动分享给朋友，一起享受优惠</view>
    </view>
    <view class="poster-card">
      <view class="poster-brand">商城精选</view>
      <view class="poster-divider" />
      <view class="poster-copy">发现好商品<br /><text>邀请朋友一起逛商城</text></view>
      <view class="poster-hint">点击下方分享按钮发送给微信好友</view>
    </view>
    <view class="actions">
      <ShareButton title="欢迎进入我的商城，分享好物一起买" path="/pages/index/index?from=share" />
      <view class="action-note">分享链接会打开商城首页</view>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f4f5f3; padding-bottom: 40rpx; }
.hero { padding: 42rpx 32rpx 52rpx; color: #fff; background: linear-gradient(135deg, #e34948, #ff6b35); }
.hero-title { font-size: 44rpx; font-weight: 800; }
.hero-sub { margin-top: 12rpx; font-size: 24rpx; opacity: .9; }
.poster-card { margin: -24rpx 28rpx 24rpx; padding: 44rpx 34rpx 52rpx; border-radius: 20rpx; background: #fff; box-shadow: 0 8rpx 30rpx rgba(54, 34, 22, .08); }
.poster-brand { color: #d4380d; font-size: 46rpx; font-weight: 800; letter-spacing: 4rpx; }
.poster-divider { width: 100%; height: 4rpx; margin: 30rpx 0; background: #f3d5c8; }
.poster-copy { color: #20262e; font-size: 38rpx; font-weight: 700; line-height: 1.7; }
.poster-copy text { color: #898781; font-size: 25rpx; font-weight: 400; }
.poster-hint { margin-top: 44rpx; color: #898781; font-size: 23rpx; }
.actions { display: flex; flex-direction: column; align-items: center; gap: 14rpx; }
.action-note { color: #898781; font-size: 22rpx; }
</style>
