<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { getGoodsDetail, getShareConfig } from '@/api'
import AppIcon from '@/components/AppIcon.vue'
import { decodeRouteId, encodeRouteId } from '@/utils/routeId'

const goods = ref(null)
const goodsId = ref('')
const config = ref({ title: '发现一家好店', subtitle: '把好物分享给朋友', brand: '商城精选', imageUrl: '' })

const isShopPoster = computed(() => !goodsId.value)
const image = computed(() => goods.value?.images?.[0] || config.value.imageUrl || '')
const price = computed(() => Number(goods.value?.minPrice || goods.value?.skus?.[0]?.price || 0).toFixed(2))
const posterTitle = computed(() => goods.value?.name || config.value.title || '精选好物')
const posterSub = computed(() => goods.value ? '真实好物 · 发给好友即可购买' : (config.value.subtitle || '打开小程序逛整店'))
const sharePath = computed(() => goodsId.value
  ? `/pages/goods/detail?id=${encodeRouteId(goodsId.value)}`
  : '/pages/index/index')

onLoad(async (q) => {
  goodsId.value = decodeRouteId(q?.id || '')
  try {
    const data = await getShareConfig()
    if (data) config.value = { ...config.value, ...data }
  } catch (e) { /* 默认文案 */ }
  if (goodsId.value) {
    try { goods.value = await getGoodsDetail(goodsId.value) } catch (e) { goods.value = null }
  }
})

function savePoster() {
  if (image.value) {
    uni.previewImage({ urls: [image.value] })
    uni.showToast({ title: '长按图片可保存', icon: 'none' })
    return
  }
  uni.showToast({ title: '请截屏保存当前海报', icon: 'none' })
}

function shareH5() {
  if (typeof window === 'undefined') return
  const url = window.location.href
  if (navigator.share) {
    navigator.share({ title: posterTitle.value, url }).catch(() => {})
    return
  }
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '链接已复制' }),
  })
}

onShareAppMessage(() => ({
  title: posterTitle.value,
  path: sharePath.value,
  ...(image.value ? { imageUrl: image.value } : {}),
}))
</script>

<template>
  <view class="page">
    <view class="poster">
      <view class="brand">{{ config.brand || '商城精选' }}</view>
      <view class="title">{{ posterTitle }}</view>
      <view class="sub">{{ posterSub }}</view>
      <image v-if="image" class="poster-cover" :src="image" mode="aspectFill" />
      <view v-else class="poster-default">
        <view class="poster-default-orb poster-default-orb-a" />
        <view class="poster-default-orb poster-default-orb-b" />
        <view class="poster-default-badge">精选好店</view>
        <view class="poster-default-icon"><AppIcon name="store-blue" :size="48" /></view>
        <view class="poster-default-name">{{ config.brand || '商城精选' }}</view>
        <view class="poster-default-tag">进店选购 · 正品保障</view>
      </view>
      <view class="price-row">
        <text v-if="!isShopPoster" class="price">¥{{ price }}</text>
        <text v-else class="price-tip">进店选购</text>
        <view class="qr-hint"><AppIcon name="qr-code-coral" :size="16" /><text>发给好友即可打开</text></view>
      </view>
    </view>

    <view class="state-card">
      <view class="state-title">{{ isShopPoster ? '店铺海报已备好' : '商品海报已备好' }}</view>
      <view class="state-sub">微信里直接转发给好友或群，好友点开就能买</view>
    </view>

    <view class="actions">
      <button class="outline" @click="savePoster">保存图片</button>
      <!-- #ifdef MP-WEIXIN -->
      <button class="share-main" open-type="share">发给好友</button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <button class="share-main" @click="shareH5">发给好友</button>
      <!-- #endif -->
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 32rpx; background: linear-gradient(180deg, #edf7ff, #fff); }
.poster { overflow: hidden; min-height: 620rpx; padding: 34rpx; border-radius: 24rpx; background: linear-gradient(145deg, #fff6e8, #fff); box-shadow: 0 16rpx 36rpx rgba(160, 90, 40, .08); }
.brand { color: #9a6817; font-size: 22rpx; font-weight: 700; letter-spacing: 1rpx; }
.title { margin-top: 28rpx; color: #20262e; font-size: 40rpx; font-weight: 800; line-height: 1.35; }
.sub { margin-top: 14rpx; color: #8b9398; font-size: 24rpx; }
.poster-cover { display: block; width: 100%; height: 360rpx; margin-top: 32rpx; border-radius: 20rpx; background: #f3f1ec; }
.poster-default {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 420rpx;
  margin-top: 32rpx;
  border-radius: 20rpx;
  background: linear-gradient(160deg, #ffe7c2 0%, #f6c57a 52%, #e8a24a 100%);
}
.poster-default-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
}
.poster-default-orb-a { width: 220rpx; height: 220rpx; top: -60rpx; right: -40rpx; }
.poster-default-orb-b { width: 160rpx; height: 160rpx; bottom: -50rpx; left: -30rpx; }
.poster-default-badge {
  position: relative;
  padding: 6rpx 18rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.55);
  color: #7a4e12;
  font-size: 22rpx;
  font-weight: 700;
}
.poster-default-icon {
  position: relative;
  margin-top: 22rpx;
  width: 112rpx;
  height: 112rpx;
  border-radius: 56rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.poster-default-name {
  position: relative;
  margin-top: 18rpx;
  color: #5c3a10;
  font-size: 40rpx;
  font-weight: 800;
}
.poster-default-tag {
  position: relative;
  margin-top: 8rpx;
  color: #7a5220;
  font-size: 24rpx;
}
.price-row { display: flex; align-items: center; justify-content: space-between; margin-top: 26rpx; gap: 12rpx; }
.price { color: #e34948; font-size: 44rpx; font-weight: 800; }
.price-tip { color: #c94d45; font-size: 28rpx; font-weight: 700; }
.qr-hint { display: flex; align-items: center; gap: 8rpx; padding: 10rpx 16rpx; border-radius: 28rpx; background: #fff0ed; color: #c94d45; font-size: 20rpx; }
.state-card { margin-top: 28rpx; padding: 28rpx; text-align: center; border-radius: 18rpx; background: #fff; }
.state-title { font-size: 28rpx; font-weight: 700; }
.state-sub { margin-top: 10rpx; color: #8a9aaf; font-size: 22rpx; }
.actions { display: flex; gap: 18rpx; margin-top: 36rpx; align-items: stretch; }
.outline, .share-main {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  margin: 0;
  padding: 0 12rpx;
  border-radius: 38rpx;
  font-size: 28rpx;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.actions button::after { border: 0; }
.outline { border: 1rpx solid #e4d3b8; color: #9a6817; background: #fff; }
.share-main { border: 0; background: #e34948; color: #fff; }
</style>
