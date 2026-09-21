<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { getGoodsDetail } from '@/api'
import { encodeRouteId } from '@/utils/routeId'
const goods = ref(null); const goodsId = ref(''); const saved = ref(false)
const image = computed(() => goods.value?.images?.[0] || '')
const price = computed(() => Number(goods.value?.minPrice || goods.value?.skus?.[0]?.price || 0).toFixed(2))
onLoad(async (q) => { goodsId.value = q?.id || ''; if (goodsId.value) { try { goods.value = await getGoodsDetail(goodsId.value) } catch (e) {} } })
function save() { if (image.value) uni.previewImage({ urls: [image.value] }); saved.value = true; uni.showToast({ title: '已打开图片，可保存发送' }) }
function send() { uni.showToast({ title: '请点击右上角转发给好友', icon: 'none' }) }
onShareAppMessage(() => ({ title: goods.value?.name || '分享好物', path: `/pages/goods/detail?id=${encodeRouteId(goodsId.value)}`, ...(image.value ? { imageUrl: image.value } : {}) }))
</script>
<template>
  <view class="page"><view class="poster"><view class="brand">● 商城精选</view><view class="title">{{ goods?.name || '精选好物' }}</view><view class="sub">真实好物 · 现在分享更优惠</view><image v-if="image" :src="image" mode="aspectFill" /><view class="price">¥{{ price }} <text>长按识别购买</text></view></view><view class="state-card"><view class="state-icon">图</view><view class="state-title">{{ saved ? '已保存图片' : '分享商品海报' }}</view><view class="state-sub">可发送给微信好友或朋友圈</view></view><view class="actions"><button class="outline" @click="save">再分享一条</button><button class="green" @click="send">去微信发送</button></view></view>
</template>
<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 32rpx; background: linear-gradient(180deg,#edf7ff,#fff); }.poster { overflow: hidden; min-height: 650rpx; padding: 34rpx; border-radius: 24rpx; background: linear-gradient(145deg,#dbedff,#f6fbff); }.brand { color: #159264; font-size: 22rpx; font-weight: 700; }.title { margin-top: 58rpx; color: #17345c; font-size: 40rpx; font-weight: 800; }.sub { margin-top: 14rpx; color: #7189a4; font-size: 22rpx; }.poster image { display: block; width: 100%; height: 360rpx; margin-top: 45rpx; border-radius: 20rpx; }.price { display: flex; align-items: center; justify-content: space-between; margin-top: 26rpx; color: #f04432; font-size: 44rpx; font-weight: 800; }.price text { padding: 12rpx 20rpx; border-radius: 26rpx; color: #8d531a; background: #ffc56f; font-size: 20rpx; }.state-card { margin-top: 30rpx; padding: 34rpx; text-align: center; border-radius: 18rpx; background: #fff; }.state-icon { margin: auto; width: 68rpx; height: 68rpx; border-radius: 50%; color: #2677d8; background: #e8f1ff; line-height: 68rpx; }.state-title { margin-top: 14rpx; font-size: 26rpx; font-weight: 700; }.state-sub { margin-top: 10rpx; color: #8a9aaf; font-size: 21rpx; }.actions { display: flex; gap: 18rpx; margin-top: 40rpx; }.actions button { flex: 1; height: 76rpx; border-radius: 38rpx; font-size: 24rpx; line-height: 76rpx; }.actions button::after { border: 0; }.outline { border: 1rpx solid #c8d8ea; color: #3e638b; background: #fff; }.green { border: 0; color: #fff; background: #12ad68; }
</style>
