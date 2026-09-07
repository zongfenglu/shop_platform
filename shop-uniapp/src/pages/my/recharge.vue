<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { createRechargeOrder, getRechargePlans, payRechargeOrder } from '@/api/index'

const plans = ref([])
const loading = ref(false)
const paying = ref(null)

async function load() {
  loading.value = true
  try {
    plans.value = (await getRechargePlans()) || []
  } catch (e) {
    plans.value = []
  } finally {
    loading.value = false
  }
}

onShow(load)

async function onPay(plan) {
  paying.value = plan.id
  try {
    const order = await createRechargeOrder(plan.id)
    await payRechargeOrder(order.id)
    uni.showToast({ title: '充值成功', icon: 'success' })
    // 充值后回到"我的"会重新拉取资产
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e) {
    // 拦截器已提示
  } finally {
    paying.value = null
  }
}

function money(v) {
  return Number(v || 0).toFixed(2)
}
</script>

<template>
  <view class="page">
    <view class="tip">选择充值方案，确认后即时到账（当前为模拟支付，接入微信支付后走真实收银台）。</view>
    <view class="list">
      <view v-for="plan in plans" :key="plan.id" class="plan">
        <view class="left">
          <view class="pay">¥{{ money(plan.money) }}</view>
          <view class="gift">
            到账 ¥{{ money(Number(plan.money) + Number(plan.giftMoney || 0)) }}
            <text v-if="plan.giftPoints > 0" class="pts">· 送{{ plan.giftPoints }}积分</text>
          </view>
        </view>
        <button class="btn" :disabled="paying === plan.id" @click="onPay(plan)">
          {{ paying === plan.id ? '处理中…' : '充值' }}
        </button>
      </view>
      <view v-if="!loading && plans.length === 0" class="empty">暂无充值方案</view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
  padding: 24rpx 28rpx;
}
.tip {
  font-size: 22rpx;
  color: #898781;
  line-height: 1.6;
  margin-bottom: 20rpx;
}
.list {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}
.plan {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 28rpx;
  border-bottom: 1rpx solid #f0f0ee;
}
.plan:last-child {
  border-bottom: 0;
}
.pay {
  font-size: 34rpx;
  font-weight: 700;
  color: #2a78d6;
}
.gift {
  font-size: 22rpx;
  color: #898781;
  margin-top: 8rpx;
}
.pts {
  color: #e34948;
}
.btn {
  background: #2a78d6;
  color: #fff;
  font-size: 26rpx;
  border-radius: 40rpx;
  padding: 0 36rpx;
  height: 64rpx;
  line-height: 64rpx;
}
.btn[disabled] {
  opacity: 0.6;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 24rpx;
  padding: 60rpx 0;
}
</style>
