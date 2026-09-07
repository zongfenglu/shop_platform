<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getMyGrades } from '@/api/index'

const grades = ref([])
const currentGradeId = ref(0)
const growthValue = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getMyGrades()
    grades.value = data.grades || []
    currentGradeId.value = data.currentGradeId || 0
    growthValue.value = data.growthValue || 0
  } catch (e) {
    grades.value = []
  } finally {
    loading.value = false
  }
}

onShow(load)

function isCurrent(g) {
  return g.id === currentGradeId.value
}

function progressTo(g) {
  if (growthValue.value >= g.growthValue) return 100
  if (g.growthValue <= 0) return 100
  return Math.min(100, Math.round((growthValue.value / g.growthValue) * 100))
}
</script>

<template>
  <view class="page">
    <view class="summary">
      <view class="lbl">我的成长值</view>
      <view class="num">{{ growthValue }}</view>
    </view>

    <view class="list">
      <view v-for="g in grades" :key="g.id" class="grade" :class="{ current: isCurrent(g) }">
        <view class="top">
          <view class="name">
            {{ g.name }}
            <text v-if="isCurrent(g)" class="now">当前</text>
          </view>
          <view class="need">需 {{ g.growthValue }} 成长值</view>
        </view>
        <view class="bar">
          <view class="fill" :style="{ width: progressTo(g) + '%' }"></view>
        </view>
        <view class="discount">会员折扣 {{ Number(g.discountRatio) >= 1 ? '无' : Math.round(Number(g.discountRatio) * 100) + '折' }}</view>
      </view>
      <view v-if="!loading && grades.length === 0" class="empty">暂无会员等级</view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
  padding: 24rpx 28rpx;
}
.summary {
  background: linear-gradient(135deg, #2a78d6, #4f9dff);
  color: #fff;
  border-radius: 20rpx;
  padding: 40rpx 32rpx;
  margin-bottom: 24rpx;
}
.lbl {
  font-size: 24rpx;
  opacity: 0.9;
}
.num {
  font-size: 56rpx;
  font-weight: 700;
  margin-top: 8rpx;
}
.list {
  background: #fff;
  border-radius: 20rpx;
  padding: 12rpx 28rpx;
}
.grade {
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f0f0ee;
}
.grade:last-child {
  border-bottom: 0;
}
.grade.current .name {
  color: #2a78d6;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.name {
  font-size: 30rpx;
  font-weight: 700;
}
.now {
  font-size: 20rpx;
  background: #2a78d6;
  color: #fff;
  padding: 2rpx 12rpx;
  border-radius: 20rpx;
  margin-left: 12rpx;
}
.need {
  font-size: 22rpx;
  color: #898781;
}
.bar {
  height: 12rpx;
  background: #f0f0ee;
  border-radius: 8rpx;
  overflow: hidden;
  margin: 16rpx 0 10rpx;
}
.fill {
  height: 100%;
  background: linear-gradient(90deg, #2a78d6, #4f9dff);
}
.discount {
  font-size: 22rpx;
  color: #898781;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 24rpx;
  padding: 60rpx 0;
}
</style>
