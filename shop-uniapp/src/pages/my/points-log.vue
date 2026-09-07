<script setup>
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getMyPointsLogs } from '@/api/index'

const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await getMyPointsLogs({ pageNum: pageNum.value, pageSize })
    const list = page.records || []
    rows.value = pageNum.value === 1 ? list : rows.value.concat(list)
    total.value = page.total || 0
  } catch (e) {
    if (pageNum.value === 1) rows.value = []
  } finally {
    loading.value = false
  }
}

onShow(() => {
  pageNum.value = 1
  load()
})

onReachBottom(() => {
  if (loading.value) return
  if (rows.value.length >= total.value) return
  pageNum.value += 1
  load()
})

function sceneText(s) {
  return ({ recharge: '充值', consume: '消费', refund: '退款', admin: '后台调整', sign: '签到', order: '下单' })[s] || s || '—'
}
function val(v) {
  const n = Number(v || 0)
  return (n >= 0 ? '+' : '') + n
}
function time(t) {
  return String(t || '').replace('T', ' ').slice(0, 16) || '—'
}
</script>

<template>
  <view class="page">
    <view class="list">
      <view v-for="item in rows" :key="item.id" class="row">
        <view class="info">
          <view class="scene">{{ sceneText(item.scene) }}</view>
          <view class="time">{{ time(item.createTime) }}</view>
        </view>
        <view class="val" :class="Number(item.value || 0) >= 0 ? 'pos' : 'neg'">{{ val(item.value) }}</view>
      </view>
      <view v-if="!loading && rows.length === 0" class="empty">暂无积分明细</view>
      <view v-if="loading" class="empty">加载中…</view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
  padding: 24rpx 28rpx;
}
.list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
  border-bottom: 1rpx solid #f0f0ee;
}
.row:last-child {
  border-bottom: 0;
}
.scene {
  font-size: 28rpx;
  font-weight: 600;
}
.time {
  font-size: 22rpx;
  color: #898781;
  margin-top: 8rpx;
}
.val {
  font-size: 30rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.pos {
  color: #16a34a;
}
.neg {
  color: #dc2626;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 24rpx;
  padding: 60rpx 0;
}
</style>
