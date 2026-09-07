<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import {
  clearToken,
  getLoginUser,
  getToken,
  setLoginUser,
} from '@/utils/request'
import { getMyProfile } from '@/api/index'

/**
 * 我的。对应原型 docs/prototype/h5/my.html。
 * 会员资产（余额/积分/会员等级）由 Sprint 7 会员体系 /api/member/me 提供，
 * 登录后拉取一次并缓存到本地 user，避免每次 onShow 都打接口；下拉或主动刷新时重新拉取。
 */
const loggedIn = ref(false)
const user = ref(null)
const profile = ref(null)

onShow(async () => {
  loggedIn.value = !!getToken()
  user.value = getLoginUser()
  if (loggedIn.value) {
    await loadProfile()
  } else {
    profile.value = null
  }
})

async function loadProfile() {
  try {
    const data = await getMyProfile()
    profile.value = data
    // 同步更新本地缓存的昵称，登录页之外也能用到
    if (data?.member?.nickname) {
      const u = { ...(user.value || {}), nickname: data.member.nickname, userId: data.member.id }
      setLoginUser(u)
      user.value = u
    }
  } catch (e) {
    // 拦截器已提示，资产区保持空态
  }
}

function onLogin() {
  uni.navigateTo({ url: '/pages/my/login' })
}

function onLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出登录？',
    success: (res) => {
      if (!res.confirm) return
      clearToken()
      loggedIn.value = false
      user.value = null
      profile.value = null
    },
  })
}

function goOrders() {
  if (!loggedIn.value) return onLogin()
  uni.navigateTo({ url: '/pages/order/list' })
}

function goAddresses() {
  if (!loggedIn.value) return onLogin()
  uni.navigateTo({ url: '/pages/address/list' })
}

function go(url) {
  if (!loggedIn.value) return onLogin()
  uni.navigateTo({ url })
}

function money(v) {
  return Number(v || 0).toFixed(2)
}
</script>

<template>
  <view class="page">
    <view class="hero">
      <view class="user-row" @click="loggedIn ? null : onLogin()">
        <view class="avatar">{{ loggedIn ? (user?.nickname || '用户')[0] : '?' }}</view>
        <view class="info">
          <view class="name">{{ loggedIn ? user?.nickname || '用户' : '点击登录' }}</view>
          <view class="sub" v-if="loggedIn && profile?.grade?.name">
            <text class="grade-tag">{{ profile.grade.name }}</text>
            <text class="growth">成长值 {{ profile.member.growthValue || 0 }}</text>
          </view>
          <view class="sub" v-else-if="loggedIn">用户ID {{ user?.userId }}</view>
          <view class="sub" v-else>登录后可查看订单、申请售后</view>
        </view>
      </view>

      <view v-if="loggedIn" class="asset-row">
        <view class="asset" @click="go('/pages/my/balance-log')">
          <view class="av">¥{{ money(profile?.member?.balance) }}</view>
          <view class="al">余额</view>
        </view>
        <view class="asset" @click="go('/pages/my/points-log')">
          <view class="av">{{ profile?.member?.points || 0 }}</view>
          <view class="al">积分</view>
        </view>
        <view class="asset" @click="go('/pages/my/recharge')">
          <view class="av">充值</view>
          <view class="al">立即充值</view>
        </view>
      </view>
    </view>

    <view class="shortcut">
      <view class="item" @click="goOrders">
        <text class="ico">📦</text>
        <text class="label">我的订单</text>
      </view>
      <view class="item" @click="goOrders">
        <text class="ico">↺</text>
        <text class="label">退款/售后</text>
      </view>
      <view class="item" @click="goAddresses">
        <text class="ico">📍</text>
        <text class="label">收货地址</text>
      </view>
      <view class="item" @click="go('/pages/my/grade')">
        <text class="ico">★</text>
        <text class="label">会员等级</text>
      </view>
      <view class="item" @click="go('/pages/coupon/center')">
        <text class="ico">🎟</text>
        <text class="label">领券中心</text>
      </view>
    </view>

    <view class="card">
      <view class="cell" @click="uni.navigateTo({ url: '/pages/store/locator' })">
        <text class="l">🏬 附近门店</text>
        <text class="r">查看自提点 ›</text>
      </view>
    </view>

    <view v-if="loggedIn" class="card">
      <view class="cell" @click="go('/pages/my/balance-log')">
        <text class="l">余额明细</text>
        <text class="r">›</text>
      </view>
      <view class="cell" @click="go('/pages/my/points-log')">
        <text class="l">积分明细</text>
        <text class="r">›</text>
      </view>
      <view class="cell" @click="go('/pages/my/sign-in')">
        <text class="l">📅 每日签到</text>
        <text class="r">签到领积分 ›</text>
      </view>
      <view class="cell" @click="go('/pages/points-mall/index')">
        <text class="l">🏬 积分商城</text>
        <text class="r">兑换好物 ›</text>
      </view>
      <view class="cell" @click="go('/pages/my/recharge')">
        <text class="l">余额充值</text>
        <text class="r">›</text>
      </view>
      <view class="cell" @click="go('/pages/my/grade')">
        <text class="l">会员等级</text>
        <text class="r">›</text>
      </view>
      <view class="cell" @click="go('/pages/my/dealer')">
        <text class="l">🔗 我的分销</text>
        <text class="r">推广赚钱 ›</text>
      </view>
      <view class="cell" @click="go('/pages/coupon/mine')">
        <text class="l">我的优惠券</text>
        <text class="r">›</text>
      </view>
    </view>

    <view v-if="loggedIn" class="card">
      <view class="cell" @click="onLogout">
        <text class="l">退出登录</text>
        <text class="r">›</text>
      </view>
    </view>
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
  padding: 60rpx 32rpx 40rpx;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  font-weight: 700;
  flex-shrink: 0;
}
.name {
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
}
.sub {
  color: rgba(255, 255, 255, 0.9);
  font-size: 22rpx;
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}
.grade-tag {
  background: rgba(255, 255, 255, 0.25);
  padding: 2rpx 14rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
}
.asset-row {
  display: flex;
  margin-top: 32rpx;
  gap: 16rpx;
}
.asset {
  flex: 1;
  text-align: center;
}
.av {
  color: #fff;
  font-size: 34rpx;
  font-weight: 700;
}
.al {
  color: rgba(255, 255, 255, 0.85);
  font-size: 22rpx;
  margin-top: 6rpx;
}
.shortcut {
  background: #fff;
  margin: -28rpx 28rpx 0;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(11, 11, 11, 0.08);
  padding: 32rpx 8rpx;
  display: flex;
  position: relative;
  z-index: 2;
}
.shortcut .item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}
.shortcut .ico {
  font-size: 40rpx;
}
.shortcut .label {
  font-size: 22rpx;
  color: #52514e;
}
.card {
  background: #fff;
  margin: 20rpx 28rpx;
  border-radius: 20rpx;
  padding: 8rpx 28rpx;
}
.cell {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 26rpx;
  padding: 26rpx 0;
  border-bottom: 1rpx solid #f0f0ee;
}
.cell:last-child {
  border-bottom: 0;
}
.cell .r {
  color: #898781;
}
</style>
