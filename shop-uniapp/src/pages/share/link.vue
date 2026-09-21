<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { getGoodsDetail } from '@/api'
import { encodeRouteId } from '@/utils/routeId'
const goods = ref(null)
const copied = ref(false)
const goodsId = ref('')
const image = computed(() => goods.value?.images?.[0] || '')
const price = computed(() => Number(goods.value?.minPrice || goods.value?.skus?.[0]?.price || 0).toFixed(2))
onLoad(async (query) => { goodsId.value = query?.id || ''; if (goodsId.value) { try { goods.value = await getGoodsDetail(goodsId.value) } catch (e) {} } })
function copy() { uni.setClipboardData({ data: `/pages/goods/detail?id=${encodeRouteId(goodsId.value)}`, success: () => { copied.value = true; uni.showToast({ title: '已复制分享链接' }) } }) }
function shareAgain() { copied.value = false; copy() }
function goPaste() { uni.showToast({ title: '请打开微信粘贴发送', icon: 'none' }) }
onShareAppMessage(() => ({ title: goods.value?.name || '分享好物', path: `/pages/goods/detail?id=${encodeRouteId(goodsId.value)}`, ...(image.value ? { imageUrl: image.value } : {}) }))
</script>
<template>
  <view class="page">
    <view class="hero"><view class="brand">● {{ goods?.shopName || '商城精选' }}</view><view class="hero-title">{{ goods?.name || '精选好物' }}</view><view class="hero-sub">品质好物 · 放心购买</view><image v-if="image" :src="image" mode="aspectFill" /><view v-else class="art" /></view>
    <view class="price-bar"><text>¥{{ price }}</text><text v-if="goods?.linePrice">¥{{ Number(goods.linePrice).toFixed(2) }}</text><button open-type="share">长按识别链接，立即购买</button></view>
    <view class="state-card"><view class="state-icon">链</view><view class="state-title">{{ copied ? '已复制分享链接' : '分享商品链接' }}</view><view class="state-sub">可发送给微信好友或朋友圈</view></view>
    <view class="actions"><button class="outline" @click="shareAgain">再分享一条</button><button class="green" @click="goPaste">前往微信粘贴</button></view>
  </view>
</template>
<style scoped>
.page { min-height: 100vh; padding: 32rpx; box-sizing: border-box; background: linear-gradient(180deg,#eaf5ff,#f8fbff 55%,#fff); color: #17345c; }
.hero { position: relative; overflow: hidden; min-height: 420rpx; padding: 34rpx; border-radius: 24rpx; background: linear-gradient(135deg,#d7ebff,#eff7ff); box-shadow: 0 16rpx 35rpx rgba(39,110,190,.12); }
.hero image { position: absolute; right: 18rpx; bottom: 12rpx; width: 300rpx; height: 260rpx; border-radius: 20rpx; }.brand { color: #1c9a62; font-size: 22rpx; font-weight: 700; }.hero-title { max-width: 55%; margin-top: 56rpx; font-size: 42rpx; font-weight: 800; }.hero-sub { margin-top: 14rpx; color: #67809d; font-size: 22rpx; }.art { position: absolute; right: 45rpx; bottom: 45rpx; width: 220rpx; height: 170rpx; border-radius: 24rpx; background: #fff; box-shadow: 0 10rpx 22rpx rgba(56,104,160,.16); }
.price-bar { display: flex; align-items: center; gap: 12rpx; padding: 20rpx 10rpx; }.price-bar>text:first-child { color: #f04432; font-size: 42rpx; font-weight: 800; }.price-bar>text:nth-child(2) { color: #8b9bb0; font-size: 20rpx; text-decoration: line-through; }.price-bar button { margin-left: auto; padding: 0 20rpx; border: 0; border-radius: 28rpx; background: #ffb347; color: #713d09; font-size: 20rpx; line-height: 56rpx; }.price-bar button::after { border: 0; }
.state-card { margin-top: 30rpx; padding: 36rpx; border-radius: 18rpx; text-align: center; background: rgba(255,255,255,.75); }.state-icon { margin: auto; width: 68rpx; height: 68rpx; border-radius: 50%; color: #2677d8; background: #e8f1ff; font-size: 25rpx; line-height: 68rpx; }.state-title { margin-top: 16rpx; font-size: 26rpx; font-weight: 700; }.state-sub { margin-top: 10rpx; color: #8a9aaf; font-size: 21rpx; }.actions { display: flex; gap: 18rpx; margin-top: 40rpx; }.actions button { flex: 1; height: 76rpx; border-radius: 38rpx; font-size: 24rpx; line-height: 76rpx; }.actions button::after { border: 0; }.outline { border: 1rpx solid #c8d8ea; color: #3e638b; background: #fff; }.green { border: 0; color: #fff; background: #12ad68; }
</style>
