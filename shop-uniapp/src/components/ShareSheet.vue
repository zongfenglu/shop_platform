<script setup>
import { computed } from 'vue'
import { encodeRouteId } from '@/utils/routeId'
import ShareButton from '@/components/ShareButton.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  goods: { type: Object, default: null },
})
const emit = defineEmits(['close'])
const goodsPath = computed(() => `/pages/goods/detail?id=${encodeRouteId(props.goods?.id || '')}`)

function copyLink() {
  uni.setClipboardData({
    data: goodsPath.value,
    success: () => uni.showToast({ title: '分享链接已复制' }),
  })
}

function openPage(path) {
  emit('close')
  uni.navigateTo({ url: `${path}?id=${encodeRouteId(props.goods?.id || '')}` })
}
</script>

<template>
  <view v-if="visible" class="share-layer">
    <view class="share-mask" @click="emit('close')" />
    <view class="share-panel">
      <view class="panel-title">分享商品</view>
      <view class="goods-preview">
        <image v-if="goods?.images?.[0]" :src="goods.images[0]" mode="aspectFill" />
        <view class="preview-copy"><text>{{ goods?.name || '精选好物' }}</text><text>分享给好友，一起享优惠</text></view>
      </view>
      <view class="share-grid">
        <view class="share-item">
          <ShareButton class="native-share" :title="goods?.name || '发现一个好商品'" :path="goodsPath" />
          <text>微信好友</text>
        </view>
        <view class="share-item" @click="copyLink"><view class="share-icon blue">链</view><text>复制链接</text></view>
        <view class="share-item" @click="openPage('/pages/share/image')"><view class="share-icon orange">图</view><text>分享图片</text></view>
        <view class="share-item" @click="openPage('/pages/share/link')"><view class="share-icon green">享</view><text>分享链接</text></view>
      </view>
      <button class="cancel" @click="emit('close')">取消</button>
    </view>
  </view>
</template>

<style scoped>
.share-layer { position: fixed; inset: 0; z-index: 30; }
.share-mask { position: absolute; inset: 0; background: rgba(0,0,0,.45); }
.share-panel { position: absolute; left: 0; right: 0; bottom: 0; padding: 28rpx 24rpx calc(22rpx + env(safe-area-inset-bottom)); border-radius: 28rpx 28rpx 0 0; background: #fff; }
.panel-title { text-align: center; color: #20262e; font-size: 30rpx; font-weight: 700; }
.goods-preview { display: flex; align-items: center; gap: 18rpx; margin: 26rpx 4rpx; padding: 16rpx; border-radius: 16rpx; background: #f6f9fc; }
.goods-preview image { width: 110rpx; height: 110rpx; border-radius: 12rpx; background: #e8eef4; }
.preview-copy { display: flex; flex-direction: column; gap: 8rpx; min-width: 0; }
.preview-copy text:first-child { overflow: hidden; color: #20262e; font-size: 25rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.preview-copy text:last-child { color: #92989c; font-size: 20rpx; }
.share-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12rpx; }
.share-item { display: flex; flex-direction: column; align-items: center; gap: 10rpx; color: #4a5157; font-size: 20rpx; }
.share-icon, .native-share { width: 76rpx; height: 76rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; }
.share-icon.blue { color: #2677d8; background: #e7f1ff; }.share-icon.orange { color: #e1781d; background: #fff1dc; }.share-icon.green { color: #159264; background: #e4f7ee; }
.native-share { margin: 0; padding: 0; border: 0; background: #e8f5ee; color: #159264; font-size: 22rpx; line-height: 76rpx; }
.native-share::after { border: 0; }
.cancel { height: 76rpx; margin-top: 26rpx; border: 0; border-radius: 14rpx; background: #f6f7f6; color: #4a5157; font-size: 25rpx; line-height: 76rpx; }
.cancel::after { border: 0; }
</style>
