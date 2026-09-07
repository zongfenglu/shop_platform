<script setup>
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getDiyPage } from '@/api'
import DiyPage from '@/components/diy/DiyPage.vue'

/**
 * 自定义装修页。装修组件里的跳转可以填 /pages/diy/custom?id={pageId}，或直接填页面数字 ID。
 */
const loading = ref(true)
const page = ref(null)
const pageId = ref('')

onLoad((query) => {
  pageId.value = String(query?.id || '').trim()
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function load() {
  if (!pageId.value) {
    page.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    const data = await getDiyPage(pageId.value)
    page.value = data && data.exists ? data : null
    if (data?.name) {
      uni.setNavigationBarTitle({ title: data.name })
    }
  } catch (e) {
    page.value = null
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!page" class="empty">页面不存在或尚未发布</view>
    <DiyPage v-else :items="page.items" :page="page.page" />
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 120rpx 0;
}
</style>
