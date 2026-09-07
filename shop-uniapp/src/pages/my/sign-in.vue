<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref, computed } from 'vue'
import { signStatus, dailySign, makeupSign } from '@/api/index'
import { getToken } from '@/utils/request'

/**
 * 每日签到。对应原型 docs/prototype/h5/sign-in.html。
 * 状态接口 /api/sign/status 返回连续天数、周状态、奖励规则、是否可补签。
 */
const statusData = ref(null)
const loading = ref(false)

onShow(() => {
  if (getToken()) {
    loadStatus()
  }
})

async function loadStatus() {
  try {
    statusData.value = await signStatus()
  } catch (e) {
    // 拦截器已提示
  }
}

const streakDays = computed(() => statusData.value?.streakDays || 0)
const signedToday = computed(() => statusData.value?.signedToday || false)
const dailyPoints = computed(() => statusData.value?.dailyPoints || 2)
const weekDays = computed(() => statusData.value?.week || [])
const rules = computed(() => statusData.value?.continuousRules || [])
const canMakeup = computed(() => statusData.value?.canMakeupYesterday || false)

const weekDayLabels = ['一', '二', '三', '四', '五', '六', '日']

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr + 'T00:00:00')
  return d.getDate()
}

async function onDailySign() {
  if (signedToday.value || loading.value) return
  loading.value = true
  try {
    const result = await dailySign()
    uni.showToast({ title: `+${result.earnedPoints}积分`, icon: 'success' })
    await loadStatus()
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function onMakeup() {
  if (loading.value) return
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  const dateStr = yesterday.toISOString().slice(0, 10)

  uni.showModal({
    title: '补签昨日',
    content: `补签 ${dateStr} 可恢复连续签到记录，但不获得积分。确认补签？`,
    success: async (res) => {
      if (!res.confirm) return
      loading.value = true
      try {
        await makeupSign(dateStr)
        uni.showToast({ title: '补签成功', icon: 'success' })
        await loadStatus()
      } catch (e) {
        // 拦截器已提示
      } finally {
        loading.value = false
      }
    },
  })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/my/login' })
}

function ruleLabel(rule) {
  if (rule.type === 'points') return `+${rule.value}积分`
  if (rule.type === 'coupon') return '优惠券一张'
  return '未知奖励'
}
</script>

<template>
  <view class="page">
    <!-- 未登录 -->
    <view v-if="!getToken()" class="hero">
      <view class="user-row" @click="goLogin">
        <view class="avatar">?</view>
        <view class="info">
          <view class="name">点击登录</view>
          <view class="sub">登录后开启每日签到</view>
        </view>
      </view>
    </view>

    <!-- 已登录 -->
    <template v-else>
      <view class="hero">
        <view class="streak-label">已连续签到</view>
        <view class="streak-num">{{ streakDays }}<text class="unit">天</text></view>
      </view>

      <!-- 本周签到条 -->
      <view class="week-strip">
        <view
          v-for="(day, i) in weekDays"
          :key="i"
          class="day-dot"
          :class="{ signed: day.signed, today: day.isToday, makeup: day.isMakeup }"
        >
          <view class="dot-label">{{ weekDayLabels[i] }}</view>
          <view class="dot-circle">
            <text v-if="day.signed">✓</text>
            <text v-else-if="day.isToday" class="today-points">+{{ day.previewPoints || dailyPoints }}</text>
            <text v-else class="future-points">+{{ day.previewPoints || dailyPoints }}</text>
          </view>
          <view class="dot-date">{{ formatDate(day.date) }}</view>
        </view>
      </view>

      <!-- 签到按钮 -->
      <view class="btn-area">
        <button
          class="sign-btn"
          :class="{ disabled: signedToday }"
          :disabled="signedToday || loading"
          @click="onDailySign"
        >
          {{ signedToday ? '今日已签到' : `立即签到 +${dailyPoints}积分` }}
        </button>
      </view>

      <!-- 连续签到奖励 -->
      <view class="card" v-if="rules.length">
        <view class="card-title">连续签到奖励</view>
        <view class="rule-row" v-for="(rule, i) in rules" :key="i">
          <view class="rule-left">
            <text class="rule-days">连续{{ rule.days }}天</text>
          </view>
          <view class="rule-reward" :class="{ reached: streakDays >= rule.days }">
            {{ ruleLabel(rule) }}
            <text v-if="streakDays >= rule.days" class="reached-tag">已获得</text>
          </view>
        </view>
      </view>

      <!-- 补签入口 -->
      <view class="card" v-if="canMakeup">
        <view class="cell" @click="onMakeup">
          <text class="l">📅 补签昨日</text>
          <text class="r">恢复连续记录 ›</text>
        </view>
      </view>

      <!-- 规则说明 -->
      <view class="card">
        <view class="card-title">签到规则</view>
        <view class="rule-text">
          每日签到可获得积分奖励，连续签到奖励更丰厚；断签后连续天数将重新计算。当日可使用补签恢复连续记录。
        </view>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
  padding-bottom: 40rpx;
}
.hero {
  background: linear-gradient(135deg, #ff9143, #e34948);
  padding: 50rpx 32rpx 30rpx;
  text-align: center;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  justify-content: center;
}
.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(255,255,255,.25);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 700;
}
.name { color: #fff; font-size: 30rpx; font-weight: 700; }
.sub { color: rgba(255,255,255,.85); font-size: 22rpx; margin-top: 6rpx; }
.streak-label { color: rgba(255,255,255,.9); font-size: 24rpx; }
.streak-num { color: #fff; font-size: 80rpx; font-weight: 700; line-height: 1.2; }
.streak-num .unit { font-size: 28rpx; font-weight: 400; margin-left: 6rpx; }

.week-strip {
  background: #fff;
  margin: -24rpx 28rpx 0;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(11,11,11,.08);
  padding: 24rpx 12rpx;
  display: flex;
  position: relative;
  z-index: 2;
}
.day-dot {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.dot-label { font-size: 20rpx; color: #898781; }
.dot-circle {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #f0f0ee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #898781;
}
.day-dot.signed .dot-circle { background: #1baf7a; color: #fff; font-size: 26rpx; }
.day-dot.today .dot-circle { background: #e34948; }
.day-dot.today .today-points { color: #fff; font-size: 22rpx; font-weight: 700; }
.day-dot .future-points { color: #c2c0ba; font-size: 20rpx; }
.dot-date { font-size: 20rpx; color: #52514e; }
.day-dot.today .dot-date { color: #e34948; font-weight: 700; }

.btn-area {
  padding: 32rpx 32rpx 16rpx;
}
.sign-btn {
  width: 100%;
  background: linear-gradient(135deg, #ff9143, #e34948);
  color: #fff;
  border: none;
  border-radius: 48rpx;
  font-size: 30rpx;
  font-weight: 700;
  padding: 24rpx 0;
}
.sign-btn.disabled {
  background: #e0ded7;
  color: #a8a59b;
}

.card {
  background: #fff;
  margin: 16rpx 28rpx;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}
.card-title {
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.rule-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f0f0ee;
}
.rule-row:last-child { border-bottom: 0; }
.rule-days { font-size: 26rpx; }
.rule-reward { font-size: 26rpx; color: #898781; }
.rule-reward.reached { color: #1baf7a; font-weight: 600; }
.reached-tag { font-size: 20rpx; background: #e8f5e9; color: #1baf7a; padding: 2rpx 12rpx; border-radius: 10rpx; margin-left: 10rpx; }
.rule-text { font-size: 24rpx; color: #898781; line-height: 1.8; }

.cell {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 26rpx;
  padding: 8rpx 0;
}
.cell .r { color: #898781; }
</style>
