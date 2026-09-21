<script setup>
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { applyDealer, applyWithdraw, getDealerSetting, getMyDealer, listDealerOrders, listMyDealerTeam, listMyWithdraws, pageGoods } from '@/api/index'
import { getToken } from '@/utils/request'

const dealer = ref(null)
const orders = ref([])
const team = ref([])
const setting = ref(null)
const withdraws = ref([])
const activeSection = ref('commission')
const products = ref([])

onShow(async () => { if (getToken()) await loadAll() })

async function loadAll() {
  try { dealer.value = await getMyDealer() } catch (e) { dealer.value = null }
  try { orders.value = await listDealerOrders() } catch (e) { orders.value = [] }
  try { team.value = await listMyDealerTeam() } catch (e) { team.value = [] }
  try { setting.value = await getDealerSetting() } catch (e) { setting.value = null }
  try { withdraws.value = await listMyWithdraws() } catch (e) { withdraws.value = [] }
  try { const result = await pageGoods({ pageNum: 1, pageSize: 3 }); products.value = result?.records || result?.list || result?.items || [] } catch (e) { products.value = [] }
}

const statusText = computed(() => {
  const statuses = { none: '未开通', applying: '审核中', active: '已开通', disabled: '已停用', rejected: '未通过' }
  return statuses[dealer.value?.status || 'none'] || dealer.value?.status
})
const totalCommission = computed(() => money(dealer.value?.totalCommission))
const availableCommission = computed(() => money(dealer.value?.availableCommission))
const frozenCommission = computed(() => money(dealer.value?.frozenCommission))
const minWithdraw = computed(() => money(setting.value?.minWithdraw))

function money(value) { return Number(value || 0).toFixed(2) }

async function onWithdraw() {
  const available = Number(dealer.value?.availableCommission || 0)
  const minimum = Number(setting.value?.minWithdraw || 0)
  if (available <= 0 || available < minimum) {
    uni.showToast({ title: minimum > 0 ? `满 ¥${minimum.toFixed(2)} 可提现` : '暂无可提现金额', icon: 'none' })
    return
  }
  uni.showModal({
    title: '申请提现', content: `本次提现 ¥${available.toFixed(2)}`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await applyWithdraw(available, 'wechat', '')
        uni.showToast({ title: '提现申请已提交' })
        await loadAll()
      } catch (e) { /* 请求层已提示 */ }
    },
  })
}

async function onApply() {
  if (!setting.value?.isEnable) {
    uni.showToast({ title: '分销功能未开放', icon: 'none' })
    return
  }
  try {
    await applyDealer()
    uni.showToast({ title: '申请已提交' })
    await loadAll()
  } catch (e) { /* 请求层已提示 */ }
}

function goLogin() { uni.navigateTo({ url: '/pages/my/login' }) }
const orderStatusLabel = (status) => ({ pending: '待结算', settled: '已结算', refunded: '已退款' }[status] || status)
const withdrawStatusLabel = (status) => ({ applying: '待审核', approved: '已通过', rejected: '已拒绝', paid: '已打款' }[status] || status)
</script>

<template>
  <view class="page">
    <view v-if="!getToken()" class="login-state">
      <AppIcon name="wallet-cards-muted" :size="40" />
      <view class="login-title">登录后查看分销账户</view>
      <button class="primary-btn" @click="goLogin">去登录</button>
    </view>

    <template v-else-if="!dealer || ['none', 'applying', 'rejected'].includes(dealer.status)">
      <view class="apply-hero">
        <view class="status-mark"><AppIcon v-if="dealer?.status === 'applying'" name="clock-3-green" :size="30" /><AppIcon v-else name="check-circle-2-green" :size="30" /></view>
        <view class="apply-status">{{ statusText }}</view>
        <button v-if="!dealer || dealer.status === 'none' || dealer.status === 'rejected'" class="primary-btn" @click="onApply">申请成为分销商</button>
      </view>
    </template>

    <view v-else-if="dealer.status === 'disabled'" class="login-state">
      <AppIcon name="wallet-cards-muted" :size="40" />
      <view class="login-title">分销账户已停用</view>
      <view class="disabled-hint">如需恢复，请联系平台管理员</view>
    </view>

    <template v-else-if="dealer.status === 'active'">
      <view class="account-hero">
        <view class="hero-top"><text>佣金账户</text><text class="status-pill">{{ statusText }}</text></view>
        <view class="available-label">可提现佣金</view>
        <view class="available-value"><text>¥</text>{{ availableCommission }}</view>
        <view class="metric-row">
          <view class="metric"><text class="metric-value">¥{{ totalCommission }}</text><text class="metric-label">累计佣金</text></view>
          <view class="metric"><text class="metric-value">¥{{ frozenCommission }}</text><text class="metric-label">待结算</text></view>
          <view class="metric"><text class="metric-value">{{ dealer.teamCount ?? team.length }}</text><text class="metric-label">团队成员</text></view>
        </view>
      </view>

      <view class="dealer-shortcuts">
        <view class="shortcut" @click="uni.navigateTo({ url: '/pages/my/share' })"><AppIcon name="share-2-coral" :size="30" /><text>分享海报</text></view>
        <view class="shortcut" @click="uni.navigateTo({ url: '/pages/share/link' })"><AppIcon name="share-2-coral" :size="30" /><text>分享链接</text></view>
        <view class="shortcut" @click="uni.navigateTo({ url: '/pages/order/list' })"><AppIcon name="package-blue" :size="30" /><text>我的订单</text></view>
        <view class="shortcut" @click="activeSection = 'commission'"><AppIcon name="coins-green" :size="30" /><text>佣金明细</text></view>
      </view>

      <view class="product-block">
        <view class="product-heading"><text>推广商品</text><text @click="uni.switchTab({ url: '/pages/goods/list' })">查看全部 ›</text></view>
        <view v-if="products.length" class="product-grid"><view v-for="item in products" :key="item.id" class="product" @click="uni.navigateTo({ url: `/pages/goods/detail?id=${item.id}` })"><image v-if="item.images?.[0]" :src="item.images[0]" mode="aspectFill" /><view class="product-name">{{ item.name }}</view><view class="product-price">¥{{ Number(item.minPrice || item.price || 0).toFixed(2) }}</view></view></view>
        <view v-else class="product-empty">暂无可推广商品</view>
      </view>

      <view class="invite-card" @click="uni.navigateTo({ url: '/pages/my/share' })"><view><view class="invite-title">邀请好友一起赚钱</view><view class="invite-sub">好友下单，你也有佣金</view></view><text>立即邀请 ›</text></view>
      <view class="rules-link" @click="uni.navigateTo({ url: '/pages/my/rules' })">查看分销规则 ›</view>

      <view class="withdraw-row">
        <view><view class="withdraw-title">佣金提现</view><view class="withdraw-sub">最低提现 ¥{{ minWithdraw }}</view></view>
        <button class="withdraw-btn" @click="onWithdraw"><AppIcon name="arrow-down-to-line-white" :size="18" /><text>申请提现</text></button>
      </view>

      <view class="tabs">
        <button class="tab" :class="{ active: activeSection === 'commission' }" @click="activeSection = 'commission'"><AppIcon :name="activeSection === 'commission' ? 'coins-green' : 'coins-muted'" :size="18" /><text>佣金</text></button>
        <button class="tab" :class="{ active: activeSection === 'team' }" @click="activeSection = 'team'"><AppIcon :name="activeSection === 'team' ? 'users-green' : 'users-muted'" :size="18" /><text>团队</text></button>
        <button class="tab" :class="{ active: activeSection === 'withdraw' }" @click="activeSection = 'withdraw'"><AppIcon :name="activeSection === 'withdraw' ? 'receipt-text-green' : 'receipt-text-muted'" :size="18" /><text>提现</text></button>
      </view>

      <view class="content-section">
        <template v-if="activeSection === 'commission'">
          <view class="section-heading">佣金明细</view>
          <view v-if="!orders.length" class="empty"><AppIcon name="coins-soft" :size="34" /><text>暂无佣金记录</text></view>
          <view v-for="order in orders" :key="order.id" class="record-row">
            <view><view class="record-title">订单佣金</view><view class="record-meta">订单 {{ order.orderId }}</view></view>
            <view class="record-right"><view class="amount">+¥{{ money(order.commissionAmount) }}</view><view class="state" :class="order.status">{{ orderStatusLabel(order.status) }}</view></view>
          </view>
        </template>

        <template v-else-if="activeSection === 'team'">
          <view class="section-heading">团队成员 <text>{{ team.length }}</text></view>
          <view v-if="!team.length" class="empty"><AppIcon name="users-soft" :size="34" /><text>暂无团队成员</text></view>
          <view v-for="member in team" :key="member.id" class="record-row">
            <view class="member-main"><view class="member-avatar">{{ (member.realName || member.mobile || '用')[0] }}</view><view><view class="record-title">{{ member.realName || member.mobile || '用户' }}</view><view class="record-meta">团队成员</view></view></view>
            <view class="record-right"><view class="amount neutral">¥{{ money(member.totalCommission) }}</view><view class="record-meta">累计佣金</view></view>
          </view>
        </template>

        <template v-else>
          <view class="section-heading">提现记录</view>
          <view v-if="!withdraws.length" class="empty"><AppIcon name="receipt-text-soft" :size="34" /><text>暂无提现记录</text></view>
          <view v-for="item in withdraws" :key="item.id" class="record-row">
            <view><view class="record-title">佣金提现</view><view class="record-meta">{{ item.applyTime?.slice(0, 10) || '—' }}</view></view>
            <view class="record-right"><view class="amount neutral">-¥{{ money(item.amount) }}</view><view class="state" :class="item.status">{{ withdrawStatusLabel(item.status) }}</view></view>
          </view>
        </template>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f4f5f3; color: #20262e; padding-bottom: 36rpx; }
.account-hero { background: linear-gradient(135deg,#ff5d3d,#ef442e); color: #fff; padding: 34rpx 32rpx 36rpx; }
.hero-top { display: flex; align-items: center; justify-content: space-between; font-size: 24rpx; font-weight: 600; }
.status-pill { padding: 5rpx 14rpx; border-radius: 8rpx; background: #2f8f6f; font-size: 19rpx; }
.available-label { margin-top: 30rpx; color: #aeb7bd; font-size: 21rpx; }
.available-value { margin-top: 4rpx; font-size: 56rpx; font-weight: 750; }.available-value text { margin-right: 5rpx; font-size: 28rpx; }
.metric-row { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 34rpx; padding-top: 26rpx; border-top: 1rpx solid rgba(255,255,255,.13); }
.metric { display: flex; flex-direction: column; align-items: center; gap: 5rpx; }.metric + .metric { border-left: 1rpx solid rgba(255,255,255,.13); }
.metric-value { font-size: 24rpx; font-weight: 650; }.metric-label { color: #aeb7bd; font-size: 19rpx; }
.withdraw-row { margin: 20rpx 24rpx; padding: 24rpx 26rpx; border: 1rpx solid #e2e5e1; border-radius: 16rpx; background: #fff; display: flex; align-items: center; justify-content: space-between; }
.withdraw-title { font-size: 25rpx; font-weight: 650; }.withdraw-sub { margin-top: 5rpx; color: #8c9397; font-size: 20rpx; }
.withdraw-btn, .primary-btn { border: 0; background: #e35549; color: #fff; border-radius: 12rpx; }
.withdraw-btn { margin: 0; height: 64rpx; padding: 0 22rpx; display: flex; align-items: center; gap: 8rpx; font-size: 22rpx; }
.withdraw-btn::after, .primary-btn::after, .tab::after { border: 0; }
.tabs { height: 82rpx; padding: 0 24rpx; display: grid; grid-template-columns: repeat(3, 1fr); background: #fff; border-top: 1rpx solid #e2e5e1; border-bottom: 1rpx solid #e2e5e1; }
.tab { margin: 0; padding: 0; height: 82rpx; border: 0; border-radius: 0; background: transparent; color: #858c90; display: flex; align-items: center; justify-content: center; gap: 8rpx; font-size: 22rpx; }
.tab.active { color: #246d58; border-bottom: 4rpx solid #2f8f6f; font-weight: 650; }
.content-section { margin-top: 16rpx; padding: 0 28rpx; background: #fff; border-top: 1rpx solid #e2e5e1; border-bottom: 1rpx solid #e2e5e1; }
.section-heading { padding: 24rpx 0 16rpx; font-size: 25rpx; font-weight: 700; }.section-heading text { margin-left: 8rpx; color: #8c9397; font-size: 20rpx; font-weight: 500; }
.record-row { min-height: 112rpx; display: flex; align-items: center; justify-content: space-between; gap: 18rpx; border-top: 1rpx solid #eceeeb; }
.record-title { font-size: 24rpx; font-weight: 600; }.record-meta { margin-top: 4rpx; color: #92989c; font-size: 19rpx; }
.record-right { text-align: right; }.amount { color: #287e62; font-size: 25rpx; font-weight: 700; }.amount.neutral { color: #30373c; }
.state { margin-top: 4rpx; color: #aa751b; font-size: 19rpx; }.state.settled, .state.paid, .state.approved { color: #287e62; }.state.refunded, .state.rejected { color: #a64a45; }
.member-main { display: flex; align-items: center; gap: 14rpx; }.member-avatar { width: 58rpx; height: 58rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: #edf5fc; color: #2e70b8; font-size: 23rpx; font-weight: 700; }
.empty { min-height: 260rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16rpx; color: #a1a7aa; font-size: 22rpx; }
.login-state, .apply-hero { min-height: 520rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #687076; }
.login-title, .apply-status { margin-top: 18rpx; color: #30373c; font-size: 28rpx; font-weight: 650; }
.disabled-hint { margin-top: 10rpx; color: #8c9397; font-size: 21rpx; }
.status-mark { width: 80rpx; height: 80rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #287e62; background: #eaf6f1; }
.primary-btn { margin-top: 28rpx; width: 300rpx; height: 72rpx; line-height: 72rpx; font-size: 24rpx; }
.dealer-shortcuts { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8rpx; margin: 18rpx 24rpx; padding: 22rpx 10rpx; border-radius: 18rpx; background: #fff; }.shortcut { display: flex; flex-direction: column; align-items: center; gap: 8rpx; color: #4b5962; font-size: 19rpx; }.product-block { margin: 18rpx 24rpx; padding: 24rpx; border-radius: 18rpx; background: #fff; }.product-heading { display: flex; justify-content: space-between; font-size: 26rpx; font-weight: 700; }.product-heading text:last-child { color: #8f989e; font-size: 20rpx; font-weight: 400; }.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; margin-top: 18rpx; }.product image { width: 100%; height: 150rpx; border-radius: 10rpx; background: #edf1f4; }.product-name { overflow: hidden; margin-top: 8rpx; font-size: 19rpx; text-overflow: ellipsis; white-space: nowrap; }.product-price { margin-top: 5rpx; color: #ed4c31; font-size: 21rpx; font-weight: 700; }.product-empty { padding: 40rpx 0 18rpx; color: #9ca4a8; text-align: center; font-size: 21rpx; }.invite-card { display: flex; align-items: center; justify-content: space-between; margin: 18rpx 24rpx; padding: 24rpx; border-radius: 18rpx; color: #1769bd; background: #e6f3ff; }.invite-title { font-size: 24rpx; font-weight: 700; }.invite-sub { margin-top: 6rpx; color: #86a4bf; font-size: 19rpx; }.invite-card>text { padding: 12rpx 20rpx; border-radius: 24rpx; color: #fff; background: #2682e9; font-size: 19rpx; }.rules-link { margin: 14rpx 0 22rpx; color: #5688b7; text-align: center; font-size: 20rpx; }
</style>
