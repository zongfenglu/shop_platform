<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { clearToken, getLoginUser, getToken, setLoginUser, setToken } from '@/utils/request'
import { bindWechatPhone, getMyProfile } from '@/api/index'
import { ensureWechatLogin } from '@/utils/wechatAuth'

const loggedIn = ref(false)
const user = ref(null)
const profile = ref(null)
const bindingPhone = ref(false)

onShow(async () => {
  // #ifdef MP-WEIXIN
  await ensureWechatLogin().catch(() => null)
  // #endif
  loggedIn.value = !!getToken()
  user.value = getLoginUser()
  if (loggedIn.value) await loadProfile()
  else profile.value = null
})

async function loadProfile() {
  try {
    const data = await getMyProfile()
    profile.value = data
    if (data?.member?.nickname) {
      const cached = {
        ...(user.value || {}),
        nickname: data.member.nickname,
        userId: data.member.id,
        mobile: data.member.mobile || '',
        avatar: data.member.avatar || '',
      }
      setLoginUser(cached)
      user.value = cached
    }
  } catch (e) {
    // 请求层已提示
  }
}

async function onGetPhoneNumber(event) {
  const code = event?.detail?.code
  if (!code) {
    if (!String(event?.detail?.errMsg || '').includes('deny')) {
      uni.showToast({ title: '未取得手机号授权', icon: 'none' })
    }
    return
  }
  if (bindingPhone.value) return
  bindingPhone.value = true
  try {
    const data = await bindWechatPhone(code)
    setToken(data.token)
    const cached = { ...(user.value || {}), userId: data.userId, nickname: data.nickname, mobile: data.mobile || '' }
    setLoginUser(cached)
    user.value = cached
    await loadProfile()
    uni.showToast({ title: '手机号已绑定', icon: 'success' })
  } catch (e) {
    // 请求层已提示
  } finally {
    bindingPhone.value = false
  }
}

function maskMobile(mobile) {
  return String(mobile || '').replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}

function onLogin() { uni.navigateTo({ url: '/pages/my/login' }) }
function go(url) {
  if (!loggedIn.value) return onLogin()
  uni.navigateTo({ url })
}
function openNearbyStores() {
  uni.navigateTo({
    url: '/pages/store/locator',
    fail: () => uni.showToast({ title: '门店页面打开失败，请重试', icon: 'none' }),
  })
}
function onLogout() {
  uni.showModal({
    title: '退出登录', content: '确认退出当前账号？',
    success: (res) => {
      if (!res.confirm) return
      clearToken()
      loggedIn.value = false
      user.value = null
      profile.value = null
    },
  })
}
function money(v) { return Number(v || 0).toFixed(2) }
</script>

<template>
  <view class="page">
    <view class="hero">
      <view class="user-row" @click="loggedIn ? go('/pages/my/profile') : onLogin()">
        <image v-if="loggedIn && profile?.member?.avatar" class="avatar avatar-image" :src="profile.member.avatar" mode="aspectFill" />
        <view v-else class="avatar"><AppIcon name="user-round-white" :size="30" /></view>
        <view class="info">
          <view class="name">{{ loggedIn ? user?.nickname || '用户' : '登录账号' }}</view>
          <view v-if="loggedIn && profile?.grade?.name" class="member-line">
            <AppIcon name="crown-light" :size="15" /><text>{{ profile.grade.name }}</text>
            <text class="growth">成长值 {{ profile.member.growthValue || 0 }}</text>
          </view>
          <view v-else class="member-line">{{ loggedIn ? `用户ID ${user?.userId}` : '登录后查看账户与订单' }}</view>
        </view>
        <AppIcon v-if="!loggedIn" name="chevron-right-light" :size="22" />
      </view>
      <view class="asset-row">
        <view class="asset" @click="go('/pages/my/balance-log')"><view class="asset-value">¥{{ money(profile?.member?.balance) }}</view><view class="asset-label">余额</view></view>
        <view class="asset" @click="go('/pages/my/points-log')"><view class="asset-value">{{ profile?.member?.points || 0 }}</view><view class="asset-label">积分</view></view>
        <view class="asset" @click="go('/pages/coupon/mine')"><view class="asset-value">查看</view><view class="asset-label">优惠券</view></view>
      </view>
    </view>

    <view class="quick-panel">
      <view class="quick-item" @click="go('/pages/order/list')"><view class="quick-icon blue"><AppIcon name="package-blue" :size="23" /></view><text>我的订单</text></view>
      <view class="quick-item" @click="go('/pages/afterSale/index')"><view class="quick-icon coral"><AppIcon name="rotate-ccw-coral" :size="23" /></view><text>退款/售后</text></view>
      <view class="quick-item" @click="go('/pages/address/list')"><view class="quick-icon green"><AppIcon name="map-pin-green" :size="23" /></view><text>收货地址</text></view>
      <view class="quick-item" @click="go('/pages/coupon/center')"><view class="quick-icon amber"><AppIcon name="ticket-amber" :size="23" /></view><text>领券中心</text></view>
    </view>

    <view class="section">
      <view class="section-title">账户服务</view>
      <view class="service-grid">
        <view class="service-item" @click="go('/pages/my/balance-log')"><AppIcon name="wallet-cards-service" :size="22" /><text>余额明细</text></view>
        <view class="service-item" @click="go('/pages/my/points-log')"><AppIcon name="coins-service" :size="22" /><text>积分明细</text></view>
        <view class="service-item" @click="go('/pages/my/recharge')"><AppIcon name="circle-dollar-sign-service" :size="22" /><text>余额充值</text></view>
        <view class="service-item" @click="go('/pages/my/grade')"><AppIcon name="crown-service" :size="22" /><text>会员等级</text></view>
      </view>
    </view>

    <view class="section list-section">
      <view v-if="loggedIn" class="cell" @click="go('/pages/my/profile')">
        <view class="cell-main"><view class="cell-icon violet"><AppIcon name="user-round-white" :size="20" /></view><text>个人资料</text></view>
        <view class="cell-side"><text>头像与昵称</text><AppIcon name="chevron-right-muted" :size="18" /></view>
      </view>
      <!-- #ifdef MP-WEIXIN -->
      <button
        v-if="loggedIn && !profile?.member?.mobile"
        class="cell phone-cell"
        open-type="getPhoneNumber"
        :disabled="bindingPhone"
        @getphonenumber="onGetPhoneNumber"
      >
        <view class="cell-main"><view class="cell-icon green"><AppIcon name="phone-green" :size="20" /></view><text>绑定手机号</text></view>
        <view class="cell-side"><text>{{ bindingPhone ? '绑定中...' : '微信授权获取' }}</text><AppIcon name="chevron-right-muted" :size="18" /></view>
      </button>
      <view v-else-if="loggedIn && profile?.member?.mobile" class="cell">
        <view class="cell-main"><view class="cell-icon green"><AppIcon name="phone-green" :size="20" /></view><text>手机号</text></view>
        <view class="cell-side"><text>{{ maskMobile(profile.member.mobile) }}</text></view>
      </view>
      <!-- #endif -->
      <view class="cell" @tap="openNearbyStores"><view class="cell-main"><view class="cell-icon blue"><AppIcon name="store-blue" :size="20" /></view><text>附近门店</text></view><view class="cell-side"><text>查看自提点</text><AppIcon name="chevron-right-muted" :size="18" /></view></view>
      <view class="cell" @click="go('/pages/my/sign-in')"><view class="cell-main"><view class="cell-icon green"><AppIcon name="calendar-check-2-green" :size="20" /></view><text>每日签到</text></view><view class="cell-side"><text>签到领积分</text><AppIcon name="chevron-right-muted" :size="18" /></view></view>
      <view class="cell" @click="go('/pages/points-mall/index')"><view class="cell-main"><view class="cell-icon amber"><AppIcon name="gift-amber" :size="20" /></view><text>积分商城</text></view><view class="cell-side"><text>兑换好物</text><AppIcon name="chevron-right-muted" :size="18" /></view></view>
      <view class="cell" @click="go('/pages/my/dealer')"><view class="cell-main"><view class="cell-icon coral"><AppIcon name="share-2-coral" :size="20" /></view><text>我的分销</text></view><view class="cell-side"><text>分销中心</text><AppIcon name="chevron-right-muted" :size="18" /></view></view>
      <view class="cell" @click="go('/pages/coupon/mine')"><view class="cell-main"><view class="cell-icon violet"><AppIcon name="badge-percent-violet" :size="20" /></view><text>我的优惠券</text></view><view class="cell-side"><AppIcon name="chevron-right-muted" :size="18" /></view></view>
    </view>
    <!-- #ifndef MP-WEIXIN -->
    <button v-if="loggedIn" class="logout" @click="onLogout"><AppIcon name="log-out-red" :size="19" /><text>退出登录</text></button>
    <!-- #endif -->
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f4f5f3; padding-bottom: 40rpx; color: #1e2429; }
.hero { background: #20262e; padding: 36rpx 32rpx 42rpx; }
.user-row { min-height: 104rpx; display: flex; align-items: center; gap: 20rpx; }
.avatar { width: 92rpx; height: 92rpx; border-radius: 50%; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: #ed6a5a; color: #fff; }
.avatar-image { display: block; }
.info { flex: 1; min-width: 0; }
.name { color: #fff; font-size: 32rpx; font-weight: 700; }
.member-line { margin-top: 8rpx; display: flex; align-items: center; gap: 8rpx; color: #cfd5d9; font-size: 21rpx; }
.growth { margin-left: 8rpx; color: #9fa8ae; }
.asset-row { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 30rpx; padding-top: 26rpx; border-top: 1rpx solid rgba(255,255,255,.13); }
.asset { text-align: center; min-width: 0; }
.asset + .asset { border-left: 1rpx solid rgba(255,255,255,.13); }
.asset-value { color: #fff; font-size: 28rpx; font-weight: 700; }
.asset-label { color: #aeb7bd; font-size: 20rpx; margin-top: 5rpx; }
.quick-panel { margin: -18rpx 24rpx 20rpx; padding: 26rpx 12rpx 22rpx; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); background: #fff; border: 1rpx solid #e4e6e3; border-radius: 16rpx; position: relative; }
.quick-item { min-width: 0; display: flex; flex-direction: column; align-items: center; gap: 10rpx; color: #454b50; font-size: 21rpx; }
.quick-icon { width: 64rpx; height: 64rpx; border-radius: 14rpx; display: flex; align-items: center; justify-content: center; }
.blue { color: #2e70b8; background: #edf5fc; }.coral { color: #c94d45; background: #fff0ed; }.green { color: #287e62; background: #eaf6f1; }.amber { color: #9a6817; background: #fff6df; }.violet { color: #6854a5; background: #f2effb; }
.section { margin: 20rpx 24rpx; background: #fff; border: 1rpx solid #e4e6e3; border-radius: 16rpx; }
.section-title { padding: 24rpx 26rpx 8rpx; color: #767d82; font-size: 21rpx; }
.service-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); padding: 18rpx 10rpx 26rpx; }
.service-item { min-width: 0; display: flex; flex-direction: column; align-items: center; gap: 12rpx; color: #454b50; font-size: 21rpx; }
.list-section { padding: 0 26rpx; }
.cell { min-height: 94rpx; display: flex; justify-content: space-between; align-items: center; border-bottom: 1rpx solid #eceeeb; }
.phone-cell { width: 100%; padding: 0; border-radius: 0; background: transparent; color: inherit; font-size: inherit; text-align: left; }
.phone-cell::after { border: 0; }
.cell:last-child { border-bottom: 0; }
.cell-main, .cell-side { display: flex; align-items: center; }
.cell-main { gap: 18rpx; font-size: 25rpx; font-weight: 500; }.cell-side { gap: 5rpx; color: #969ca0; font-size: 21rpx; }
.cell-icon { width: 52rpx; height: 52rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.logout { margin: 20rpx 24rpx 0; width: calc(100% - 48rpx); height: 82rpx; display: flex; gap: 10rpx; align-items: center; justify-content: center; color: #b8403a; background: #fff; border: 1rpx solid #e4e6e3; border-radius: 16rpx; font-size: 24rpx; }
.logout::after { border: 0; }
</style>
