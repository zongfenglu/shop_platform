<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listMyBargains } from '@/api/index'
import { encodeRouteId } from '@/utils/routeId'

const rows = ref([])
const loading = ref(true)

onShow(load)

async function load() {
  loading.value = true
  try {
    rows.value = (await listMyBargains()) || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function fmtPrice(v) {
  return Number(v ?? 0).toFixed(2)
}

function statusText(status) {
  return { ongoing: '砍价中', done: '已到底价' }[status] || status || ''
}

function goDetail(row) {
  uni.navigateTo({
    url: `/pages/bargain/detail?id=${encodeRouteId(row.activeId)}&recordId=${encodeRouteId(row.recordId)}`,
  })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!rows.length" class="empty">暂无进行中的砍价</view>
    <view v-for="row in rows" :key="row.recordId" class="card" @click="goDetail(row)">
      <image v-if="row.goodsImage" class="img" :src="row.goodsImage" mode="aspectFill" />
      <view v-else class="img ph">无图</view>
      <view class="info">
        <view class="name">{{ row.goodsName || '砍价商品' }}</view>
        <view class="price">当前价 ¥{{ fmtPrice(row.currentPrice) }}</view>
        <view class="meta">底价 ¥{{ fmtPrice(row.floorPrice) }} · 已助力 {{ row.helpCount || 0 }} 次</view>
      </view>
      <text class="st">{{ statusText(row.status) }}</text>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f3f1ec; padding: 20rpx 0 40rpx; }
.empty { text-align: center; color: #898781; padding: 120rpx 0; font-size: 26rpx; }
.card {
  display: flex; align-items: center; gap: 18rpx;
  margin: 0 24rpx 18rpx; padding: 22rpx; background: #fff; border-radius: 18rpx;
}
.img { width: 140rpx; height: 140rpx; border-radius: 12rpx; background: #f0efec; flex-shrink: 0; }
.ph { display: flex; align-items: center; justify-content: center; color: #b5b3ad; font-size: 22rpx; }
.info { flex: 1; min-width: 0; }
.name { font-size: 28rpx; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price { margin-top: 8rpx; color: #d4380d; font-size: 28rpx; font-weight: 700; }
.meta { margin-top: 6rpx; color: #898781; font-size: 22rpx; }
.st { flex-shrink: 0; color: #722ed1; font-size: 24rpx; font-weight: 600; }
</style>
