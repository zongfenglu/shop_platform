<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref, computed } from 'vue'
import { getDealerSetting, applyDealer, getMyDealer, listDealerOrders, listMyDealerTeam, applyWithdraw, listMyWithdraws } from '@/api/index'
import { getToken } from '@/utils/request'

/**
 * 我的分销中心。对应原型 docs/prototype/h5/distribution-center.html。
 */
const dealer = ref(null)
const orders = ref([])
const team = ref([])
const setting = ref(null)
const withdraws = ref([])

onShow(async () => {
  if (!getToken()) return
  await loadAll()
})

async function loadAll() {
  try { dealer.value = await getMyDealer() } catch (e) { /* */ }
  try { orders.value = await listDealerOrders() } catch (e) { /* */ }
  try { team.value = await listMyDealerTeam() } catch (e) { /* */ }
  try { setting.value = await getDealerSetting() } catch (e) { /* */ }
  try { withdraws.value = await listMyWithdraws() } catch (e) { /* */ }
}

const statusText = computed(() => {
  if (!dealer.value) return 'none'
  const m = { none: '未开通', applying: '审核中', active: '已开通', disabled: '已禁用', rejected: '已拒绝' }
  return m[dealer.value.status] || dealer.value.status
})

const totalCommission = computed(() => Number(dealer.value?.totalCommission || 0).toFixed(2))
const availableCommission = computed(() => Number(dealer.value?.availableCommission || 0).toFixed(2))

async function onWithdraw() {
  const avail = Number(dealer.value?.availableCommission || 0)
  if (avail <= 0) {
    uni.showToast({ title: '暂无可提现金额', icon: 'none' })
    return
  }
  uni.showModal({
    title: '申请提现',
    content: `可提现 ¥${avail.toFixed(2)}，确认提现？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await applyWithdraw(avail, 'wechat', '')
        uni.showToast({ title: '提现申请已提交', icon: 'success' })
        await loadAll()
      } catch (e) { /* */ }
    },
  })
}

async function onApply() {
  if (!setting.value?.isEnable) {
    uni.showToast({ title: '分销功能未开放', icon: 'none' })
    return
  }
  uni.showModal({
    title: '申请成为分销商',
    content: '确认申请成为分销商？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await applyDealer()
        uni.showToast({ title: '申请已提交', icon: 'success' })
        await loadAll()
      } catch (e) { /* */ }
    },
  })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/my/login' })
}

const orderStatusLabel = (s) => ({ pending: '待结算', settled: '可提现', refunded: '已退款' }[s] || s)
</script>

<template>
  <view class="page">
    <view v-if="!getToken()" class="hero">
      <view class="user-row" @click="goLogin">
        <view class="avatar">?</view>
        <view class="info"><view class="name">点击登录</view><view class="sub">登录后可查看分销中心</view></view>
      </view>
    </view>

    <template v-else>
      <!-- 申请中 / 未开通 -->
      <view v-if="!dealer || dealer.status === 'none' || dealer.status === 'applying' || dealer.status === 'rejected'" class="hero">
        <view class="status-badge">{{ statusText }}</view>
        <view class="hero-desc" v-if="!dealer || dealer.status === 'none'">成为分销商，推广商品赚取佣金</view>
        <view class="hero-desc" v-if="dealer && dealer.status === 'applying'">审核中，请耐心等待</view>
        <view class="hero-desc" v-if="dealer && dealer.status === 'rejected'">申请未通过，可重新申请</view>
        <button v-if="!dealer || dealer.status === 'none' || dealer.status === 'rejected'"
          class="apply-btn" @click="onApply">申请成为分销商</button>
      </view>

      <!-- 已开通 -->
      <template v-if="dealer && dealer.status === 'active'">
        <view class="hero">
          <view class="asset-row">
            <view class="asset"><view class="av">¥{{ totalCommission }}</view><view class="al">累计佣金</view></view>
            <view class="asset"><view class="av">¥{{ availableCommission }}</view><view class="al">可提现</view></view>
            <view class="asset"><view class="av">{{ team.length }}</view><view class="al">团队成员</view></view>
          </view>
        </view>

        <!-- 提现按钮 -->
        <view class="card" style="text-align:center">
          <button class="withdraw-btn" @click="onWithdraw">申请提现</button>
        </view>

        <!-- 提现记录 -->
        <view class="card" v-if="withdraws.length">
          <view class="card-title">提现记录</view>
          <view class="order-item" v-for="w in withdraws" :key="w.id">
            <view class="order-left">
              <view class="order-no">¥{{ Number(w.amount || 0).toFixed(2) }}</view>
              <view class="order-status">{{ { applying: '待审核', approved: '已通过', rejected: '已拒绝', paid: '已打款' }[w.status] || w.status }}</view>
            </view>
            <view class="order-amount" style="font-size:22rpx;color:#898781">{{ w.applyTime?.slice(0,10) }}</view>
          </view>
        </view>

        <!-- 佣金明细 -->
        <view class="card" v-if="orders.length">
          <view class="card-title">佣金明细</view>
          <view class="order-item" v-for="o in orders" :key="o.id">
            <view class="order-left">
              <view class="order-no">订单 {{ o.orderId }}</view>
              <view class="order-status" :class="o.status === 'settled' ? 'settled' : ''">{{ orderStatusLabel(o.status) }}</view>
            </view>
            <view class="order-amount">¥{{ Number(o.commissionAmount || 0).toFixed(2) }}</view>
          </view>
        </view>

        <!-- 我的团队 -->
        <view class="card" v-if="team.length">
          <view class="card-title">我的团队（{{ team.length }}人）</view>
          <view class="team-item" v-for="t in team" :key="t.id">
            <view class="team-avatar">{{ (t.realName || '用户')[0] }}</view>
            <view class="team-info">
              <view class="team-name">{{ t.realName || t.mobile || '用户' }}</view>
              <view class="team-sub">累计佣金 ¥{{ Number(t.totalCommission || 0).toFixed(2) }}</view>
            </view>
          </view>
        </view>
      </template>
    </template>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f9f9f7; padding-bottom: 40rpx; }
.hero { background: linear-gradient(135deg, #6750a4, #4a3aa7); padding: 40rpx 32rpx; }
.user-row { display: flex; align-items: center; gap: 20rpx; justify-content: center; }
.avatar { width: 80rpx; height: 80rpx; border-radius: 50%; background: rgba(255,255,255,.25); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 32rpx; font-weight: 700; }
.name { color: #fff; font-size: 30rpx; font-weight: 700; }
.sub { color: rgba(255,255,255,.85); font-size: 22rpx; margin-top: 6rpx; }
.status-badge { color: #fff; font-size: 36rpx; font-weight: 700; text-align: center; }
.hero-desc { color: rgba(255,255,255,.85); font-size: 24rpx; text-align: center; margin-top: 12rpx; }
.apply-btn { margin-top: 24rpx; background: #ff9143; color: #fff; border: none; border-radius: 48rpx; font-size: 28rpx; padding: 20rpx 0; width: 80%; }
.asset-row { display: flex; gap: 16rpx; }
.asset { flex: 1; text-align: center; }
.av { color: #fff; font-size: 34rpx; font-weight: 700; }
.al { color: rgba(255,255,255,.85); font-size: 22rpx; margin-top: 6rpx; }
.card { background: #fff; margin: 16rpx 28rpx; border-radius: 20rpx; padding: 24rpx 28rpx; }
.card-title { font-size: 28rpx; font-weight: 700; margin-bottom: 16rpx; }
.order-item { display: flex; justify-content: space-between; align-items: center; padding: 14rpx 0; border-bottom: 1rpx solid #f0f0ee; }
.order-item:last-child { border-bottom: 0; }
.order-no { font-size: 24rpx; }
.order-status { font-size: 20rpx; color: #f0a830; }
.order-status.settled { color: #1baf7a; }
.order-amount { font-size: 28rpx; color: #e34948; font-weight: 700; }
.team-item { display: flex; align-items: center; gap: 16rpx; padding: 14rpx 0; border-bottom: 1rpx solid #f0f0ee; }
.team-item:last-child { border-bottom: 0; }
.team-avatar { width: 64rpx; height: 64rpx; border-radius: 50%; background: linear-gradient(135deg,#ffb199,#e34948); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 24rpx; flex-shrink: 0; }
.team-name { font-size: 26rpx; }
.team-sub { font-size: 20rpx; color: #898781; margin-top: 4rpx; }
</style>
