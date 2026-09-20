<script setup>
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

const props = defineProps({
  title: { type: String, default: '发现一个好商品' },
  path: { type: String, default: '/pages/index/index' },
  compact: { type: Boolean, default: false },
})

const copied = ref(false)

function shareH5() {
  if (typeof window === 'undefined') return
  const url = window.location.href
  if (navigator.share) {
    navigator.share({ title: props.title, url }).catch(() => {})
    return
  }
  const copyTask = navigator.clipboard?.writeText(url)
  if (!copyTask) return
  copyTask.then(() => {
    copied.value = true
    setTimeout(() => { copied.value = false }, 1600)
  }).catch(() => {})
}
</script>

<template>
  <!-- #ifdef MP-WEIXIN -->
  <button class="share-button" :class="{ compact }" open-type="share">
    <AppIcon name="share-2-coral" :size="compact ? 17 : 19" /><text>{{ compact ? '' : '分享' }}</text>
  </button>
  <!-- #endif -->
  <!-- #ifndef MP-WEIXIN -->
  <button class="share-button" :class="{ compact }" @click="shareH5">
    <AppIcon name="share-2-coral" :size="compact ? 17 : 19" /><text>{{ copied ? '已复制' : (compact ? '' : '分享') }}</text>
  </button>
  <!-- #endif -->
</template>

<style scoped>
.share-button { display: inline-flex; align-items: center; justify-content: center; gap: 8rpx; width: auto; min-width: 128rpx; height: 64rpx; margin: 0; padding: 0 22rpx; border: 0; border-radius: 32rpx; background: #fff0ed; color: #c94d45; font-size: 23rpx; line-height: 64rpx; }
.share-button::after { border: 0; }
.share-button.compact { min-width: 64rpx; width: 64rpx; padding: 0; }
</style>
