<script setup>
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { getArticle } from '@/api'

const loading = ref(true)
const article = ref(null)
const articleId = ref('')
const views = computed(() => (article.value?.virtualViews || 0) + (article.value?.actualViews || 0))

onLoad((query) => {
  articleId.value = String(query?.id || '').trim()
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function load() {
  if (!articleId.value) { loading.value = false; return }
  loading.value = true
  try {
    article.value = await getArticle(articleId.value)
    if (article.value?.title) uni.setNavigationBarTitle({ title: article.value.title })
  } catch {
    article.value = null
  } finally { loading.value = false }
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!article" class="empty">文章不存在或已隐藏</view>
    <view v-else class="article">
      <view class="title">{{ article.title }}</view>
      <view class="meta">{{ views }} 次阅读</view>
      <image class="cover" :src="article.coverUrl" mode="widthFix" />
      <rich-text class="body" :nodes="article.content" />
    </view>
  </view>
</template>

<style scoped>
.page{min-height:100vh;background:#fff}.article{padding:36rpx 30rpx 80rpx}.title{font-size:42rpx;line-height:1.35;margin:0 0 18rpx;font-weight:700;letter-spacing:0}.meta{font-size:24rpx;color:#898781;margin-bottom:28rpx}.cover{width:100%;display:block;margin-bottom:30rpx}.body{font-size:29rpx;line-height:1.8;color:#242424;word-break:break-word}.body :deep(img){max-width:100%;height:auto;display:block}.body :deep(p){margin:0 0 20rpx}.empty{text-align:center;color:#898781;font-size:26rpx;padding:140rpx 0}
</style>
