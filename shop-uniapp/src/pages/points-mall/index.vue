<script setup>
import { onShow } from '@dcloudio/uni-app'
import { ref, computed } from 'vue'
import { listPointsGoods, redeemPointsGoods, payPointsExchange, getMyProfile } from '@/api/index'
import { getToken } from '@/utils/request'

/**
 * 积分商城。对应原型 docs/prototype/h5/points-mall.html。
 * GET /api/points-mall/goods 公开（未登录也可浏览），兑换需登录。
 */
const goodsList = ref([])
const profile = ref(null)
const loading = ref(false)

onShow(async () => {
  await loadGoods()
  if (getToken()) {
    try {
      profile.value = await getMyProfile()
    } catch (e) { /* silent */ }
  }
})

async function loadGoods() {
  try {
    goodsList.value = await listPointsGoods()
  } catch (e) { /* silent */ }
}

const myPoints = computed(() => profile.value?.member?.points || 0)

function priceLabel(item) {
  let label = `${item.points}积分`
  if (item.cash && Number(item.cash) > 0) {
    label += ` +¥${Number(item.cash).toFixed(2)}`
  }
  return label
}

function canRedeem(item) {
  if (!getToken()) return true // 未登录可点击，但会跳登录
  return myPoints.value >= item.points && (item.stock === 0 || item.stock > 0)
}

function btnLabel(item) {
  if (!getToken()) return '兑换'
  if (myPoints.value < item.points) return '积分不足'
  return '兑换'
}

async function onRedeem(item) {
  if (!getToken()) {
    uni.navigateTo({ url: '/pages/my/login' })
    return
  }
  if (myPoints.value < item.points) return

  uni.showModal({
    title: '确认兑换',
    content: `确认使用 ${item.points} 积分${item.cash && Number(item.cash) > 0 ? ' +¥' + Number(item.cash).toFixed(2) : ''} 兑换「${item.name}」？`,
    success: async (res) => {
      if (!res.confirm) return
      loading.value = true
      try {
        const exchange = await redeemPointsGoods(Number(item.id))
        // 如果有现金部分，需要二次确认支付
        if (exchange.cashPrice && Number(exchange.cashPrice) > 0) {
          uni.showModal({
            title: '支付现金',
            content: `需支付 ¥${Number(exchange.cashPrice).toFixed(2)}，确认支付？`,
            success: async (res2) => {
              if (!res2.confirm) return
              await payPointsExchange(Number(exchange.id))
              uni.showToast({ title: '兑换成功', icon: 'success' })
              profile.value = await getMyProfile()
              await loadGoods()
            },
          })
        } else {
          await payPointsExchange(Number(exchange.id))
          uni.showToast({ title: '兑换成功', icon: 'success' })
          profile.value = await getMyProfile()
          await loadGoods()
        }
      } catch (e) {
        // 拦截器已提示
      } finally {
        loading.value = false
      }
    },
  })
}

function goPointsLog() {
  uni.navigateTo({ url: '/pages/my/points-log' })
}

function goSignIn() {
  uni.navigateTo({ url: '/pages/my/sign-in' })
}
</script>

<template>
  <view class="page">
    <!-- 积分头部 -->
    <view class="hero">
      <view class="points-row">
        <view class="points-label">我的积分</view>
        <view class="points-value" v-if="getToken()">{{ myPoints }}</view>
        <view class="points-value" v-else>--</view>
      </view>
      <view class="hero-links">
        <text class="link" @click="goPointsLog">积分明细 ›</text>
        <text class="link" @click="goSignIn">签到得积分 ›</text>
      </view>
    </view>

    <!-- 商品网格 -->
    <view class="goods-grid">
      <view
        class="goods-card"
        v-for="item in goodsList"
        :key="item.id"
        @click="onRedeem(item)"
      >
        <image
          class="goods-img"
          :src="item.image || '/static/placeholder.png'"
          mode="aspectFill"
        />
        <view class="goods-info">
          <view class="goods-name">{{ item.name }}</view>
          <view class="goods-bottom">
            <text class="goods-price">{{ priceLabel(item) }}</text>
            <text
              class="goods-btn"
              :class="{ disabled: !canRedeem(item) }"
            >{{ btnLabel(item) }}</text>
          </view>
        </view>
      </view>

      <view v-if="goodsList.length === 0" class="empty">暂无兑换商品</view>
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
  background: linear-gradient(135deg, #2b2b2b, #3d3d3d);
  padding: 32rpx 32rpx 28rpx;
}
.points-row {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.points-label { color: rgba(255,255,255,.8); font-size: 24rpx; }
.points-value { color: #fff; font-size: 56rpx; font-weight: 700; margin-top: 6rpx; }
.hero-links {
  display: flex;
  justify-content: center;
  gap: 48rpx;
  margin-top: 16rpx;
}
.link { color: rgba(255,255,255,.8); font-size: 22rpx; }

.goods-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx 20rpx;
  gap: 16rpx;
}
.goods-card {
  width: calc(50% - 8rpx);
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}
.goods-img {
  width: 100%;
  height: 280rpx;
  background: #f0f0ee;
}
.goods-info {
  padding: 16rpx 16rpx 20rpx;
}
.goods-name {
  font-size: 24rpx;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 68rpx;
}
.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
}
.goods-price {
  font-size: 24rpx;
  color: #e34948;
  font-weight: 700;
}
.goods-btn {
  font-size: 20rpx;
  background: linear-gradient(135deg, #ff9143, #e34948);
  color: #fff;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
}
.goods-btn.disabled {
  background: #e0ded7;
  color: #a8a59b;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 80rpx 0;
  width: 100%;
}
</style>
