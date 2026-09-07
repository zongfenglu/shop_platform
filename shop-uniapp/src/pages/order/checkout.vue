<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import {
  getDefaultAddress,
  getGoodsDetail,
  getMyCoupons,
  listCart,
  listOfflineStores,
  previewCheckout,
  submitCheckout,
} from '@/api'

/**
 * 确认订单。对应原型 docs/prototype/h5/checkout.html。
 *
 * 两个入口：
 *   ① 购物车结算 —— URL 带 cartIds=id1,id2，重新拉一次购物车取最新价（不信任上个页面带过来的价格）
 *   ② 立即购买  —— URL 带 skuId + quantity，不经过购物车
 *
 * 金额一律以 /api/checkout/preview 的返回为准，前端自己算的小计只用于列表展示。
 * preview 和 submit 共用同一个 CheckoutRequest 结构（后端刻意这么设计的），
 * 所以这里只维护一份 payload，避免"预览一个价、提交又是另一个价"。
 */
const items = ref([]) // [{ skuId, quantity, goodsName, specText, image, price }]
const cartIds = ref([])
const address = ref(null)
const buyerRemark = ref('')
const priceResult = ref(null)
const loading = ref(true)
const submitting = ref(false)

// 门店自提：express快递配送（默认）/ pickup门店自提，见 OrderServiceImpl#createOrder 的校验规则
const deliveryType = ref('express')
const pickupStores = ref([])
const pickupStoreId = ref(null)

// 秒杀/限时折扣：从秒杀专场"立即抢购"进入时携带，preview 与 submit 透传给价格引擎
const activityType = ref(null)
const activityId = ref(null)
// 拼团：参团时携带要加入的 group_record.id；开团时为 null（由后端 CheckoutAppService 创建团）
const groupRecordId = ref(null)

// 优惠券
const myCoupons = ref([]) // 未使用券列表（user_coupon）
const selectedCoupon = ref(null) // 选中的 user_coupon
const couponPickerOpen = ref(false)

/** 商品小计（展示用；真正的应付金额看 priceResult.payPrice） */
const goodsTotal = computed(() =>
  items.value.reduce((sum, i) => sum + Number(i.price ?? 0) * (i.quantity || 0), 0)
)

onLoad(async (query) => {
  try {
    // 秒杀/限时折扣进入时携带活动信息（单 sku 行）
    if (query?.activityType) activityType.value = query.activityType
    if (query?.activityId) activityId.value = Number(query.activityId)
    // 拼团参团：携带要加入的 group_record.id
    if (query?.groupRecordId) groupRecordId.value = Number(query.groupRecordId)
    if (query?.cartIds) {
      // id 全程按字符串处理，split 出来就是字符串，不要 Number()
      cartIds.value = String(query.cartIds).split(',').filter(Boolean)
      const rows = (await listCart()) || []
      items.value = rows
        .filter((r) => cartIds.value.includes(r.id) && !r.invalid)
        .map((r) => ({
          skuId: r.skuId,
          quantity: r.quantity,
          goodsName: r.goodsName,
          specText: r.specText,
          image: r.image,
          price: r.price,
        }))
    } else if (query?.skuId) {
      const quantity = Number(query.quantity || 1) // 数量是小整数，转 Number 安全；skuId 绝不转
      const detail = await getGoodsDetail(query.goodsId || '')
        .catch(() => null)
      const sku = detail?.skus?.find((s) => s.id === query.skuId)
      items.value = [
        {
          skuId: query.skuId,
          quantity,
          goodsName: detail?.name || '商品',
          specText: sku?.specText || '',
          image: sku?.image || detail?.images?.[0] || '',
          price: sku?.price,
        },
      ]
    }
    address.value = await getDefaultAddress().catch(() => null)
    pickupStores.value = await listOfflineStores().catch(() => [])
    await loadMyCoupons()
    await refreshPrice()
  } finally {
    loading.value = false
  }
})

async function loadMyCoupons() {
  try {
    myCoupons.value = (await getMyCoupons('unused')) || []
  } catch (e) {
    myCoupons.value = []
  }
}

const selectedPickupStore = computed(() =>
  pickupStores.value.find((s) => String(s.id) === String(pickupStoreId.value)) || null
)

// 从地址列表页 / 门店列表页选完后把结果接回来
onShow(() => {
  const picked = uni.getStorageSync('checkout_picked_address')
  if (picked) {
    try {
      address.value = JSON.parse(picked)
    } catch (e) {
      // 忽略脏数据
    }
    uni.removeStorageSync('checkout_picked_address')
    refreshPrice()
  }
  const pickedStore = uni.getStorageSync('checkout_picked_store')
  if (pickedStore) {
    try {
      const store = typeof pickedStore === 'string' ? JSON.parse(pickedStore) : pickedStore
      pickupStoreId.value = store.id
      deliveryType.value = 'pickup'
      if (store.id && !pickupStores.value.some((s) => String(s.id) === String(store.id))) {
        pickupStores.value = [...pickupStores.value, store]
      }
    } catch (e) {
      // 忽略脏数据
    }
    uni.removeStorageSync('checkout_picked_store')
    refreshPrice()
  }
})

function buildPayload() {
  return {
    items: items.value.map((i) => ({ skuId: i.skuId, quantity: i.quantity })),
    deliveryType: deliveryType.value,
    pickupStoreId: deliveryType.value === 'pickup' ? pickupStoreId.value : undefined,
    buyerRemark: buyerRemark.value || undefined,
    couponId: selectedCoupon.value?.id || undefined,
    activityType: activityType.value || undefined,
    activityId: activityId.value || undefined,
    groupRecordId: groupRecordId.value || undefined,
    address: deliveryType.value === 'express' && address.value
      ? {
          name: address.value.name,
          phone: address.value.phone,
          province: address.value.province,
          city: address.value.city,
          region: address.value.region,
          detail: address.value.detail,
        }
      : undefined,
    cartIds: cartIds.value.length ? cartIds.value : undefined,
  }
}

function chooseDeliveryType(type) {
  if (deliveryType.value === type) return
  deliveryType.value = type
  refreshPrice()
}

async function refreshPrice() {
  if (!items.value.length) return
  try {
    priceResult.value = await previewCheckout(buildPayload())
  } catch (e) {
    priceResult.value = null
  }
}

async function onSubmit() {
  if (!items.value.length) {
    uni.showToast({ title: '没有可结算的商品', icon: 'none' })
    return
  }
  if (deliveryType.value === 'express' && !address.value) {
    uni.showToast({ title: '请先选择收货地址', icon: 'none' })
    return
  }
  if (deliveryType.value === 'pickup' && !pickupStoreId.value) {
    uni.showToast({ title: '请先选择自提门店', icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await submitCheckout(buildPayload())
    // 下单成功后跳订单详情。redirectTo 而不是 navigateTo：不能让用户返回到结算页重复提交。
    uni.redirectTo({ url: `/pages/order/detail?id=${res.orderId}` })
  } catch (e) {
    // request.js 已统一提示（库存不足/商品下架等业务错误都会走到这里）
  } finally {
    submitting.value = false
  }
}

function goAddressList() {
  uni.navigateTo({ url: '/pages/address/list?pick=1' })
}

function goPickStore() {
  const id = pickupStoreId.value || ''
  uni.navigateTo({ url: `/pages/store/locator?select=1&storeId=${encodeURIComponent(id)}` })
}

function openCouponPicker() {
  couponPickerOpen.value = true
}

function pickCoupon(uc) {
  selectedCoupon.value = uc || null
  couponPickerOpen.value = false
  refreshPrice()
}

function couponFace(s) {
  if (!s) return ''
  if (s.type === 'discount') return (Number(s.discountRatio) * 10).toFixed(1).replace(/\.0$/, '') + '折'
  return '¥' + s.reducePrice
}
function couponLabel(uc) {
  if (!uc?.snapshot) return '优惠券'
  return `${couponFace(uc.snapshot)} · ${uc.snapshot.name}`
}

function fmtPrice(v) {
  return Number(v ?? 0).toFixed(2)
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <template v-else>
      <!-- 配送方式：仅当门店有可用自提点时才展示切换，否则只有快递配送一种，行为与之前一致 -->
      <view v-if="pickupStores.length" class="card delivery-switch">
        <view class="delivery-tab" :class="{ active: deliveryType === 'express' }" @click="chooseDeliveryType('express')">快递配送</view>
        <view class="delivery-tab" :class="{ active: deliveryType === 'pickup' }" @click="chooseDeliveryType('pickup')">门店自提</view>
      </view>

      <!-- 收货地址 -->
      <view v-if="deliveryType === 'express'" class="card addr" @click="goAddressList">
        <template v-if="address">
          <view class="addr-hd">
            <text class="addr-name">{{ address.name }}</text>
            <text class="addr-phone">{{ address.phone }}</text>
            <text v-if="address.isDefault" class="addr-default">默认</text>
          </view>
          <view class="addr-detail">
            {{ address.province }}{{ address.city }}{{ address.region }}{{ address.detail }}
          </view>
        </template>
        <view v-else class="addr-empty">＋ 请选择收货地址</view>
        <text class="arrow">›</text>
      </view>

      <!-- 自提门店选择：进入独立门店页，对应原型 h5/store-locator.html -->
      <view v-else class="card addr" @click="goPickStore">
        <template v-if="selectedPickupStore">
          <view class="addr-hd">
            <text class="addr-name">{{ selectedPickupStore.name }}</text>
          </view>
          <view class="addr-detail">
            {{ [selectedPickupStore.region, selectedPickupStore.detail].filter(Boolean).join(' ') || '地址待完善' }}
          </view>
          <view v-if="selectedPickupStore.businessHours" class="pickup-hours">
            营业时间 {{ selectedPickupStore.businessHours }}
          </view>
        </template>
        <view v-else class="addr-empty">＋ 请选择自提门店</view>
        <text class="arrow">›</text>
      </view>

      <!-- 商品 -->
      <view class="card">
        <view v-for="(item, i) in items" :key="i" class="goods-row">
          <image v-if="item.image" class="goods-img" :src="item.image" mode="aspectFill" />
          <view v-else class="goods-img goods-img-ph">无图</view>
          <view class="goods-info">
            <view class="goods-name">{{ item.goodsName }}</view>
            <view v-if="item.specText" class="goods-spec">{{ item.specText }}</view>
            <view class="goods-foot">
              <text class="price">¥{{ fmtPrice(item.price) }}</text>
              <text class="qty">×{{ item.quantity }}</text>
            </view>
          </view>
        </view>

        <view class="remark-row">
          <text class="remark-label">买家留言</text>
          <input v-model="buyerRemark" class="remark-input" placeholder="选填，给商家留言" maxlength="200" />
        </view>
      </view>

      <!-- 金额明细：全部取自 /api/checkout/preview 的返回，前端不自己算应付金额 -->
      <view class="card">
        <view class="fee-row">
          <text class="fee-label">商品金额</text>
          <text class="fee-value">¥{{ fmtPrice(priceResult?.totalPrice ?? goodsTotal) }}</text>
        </view>
        <view v-if="priceResult?.discountPrice > 0" class="fee-row">
          <text class="fee-label">优惠</text>
          <text class="fee-value discount">-¥{{ fmtPrice(priceResult.discountPrice) }}</text>
        </view>
        <view class="fee-row" @click="openCouponPicker">
          <text class="fee-label">优惠券</text>
          <view class="coupon-pick">
            <text v-if="priceResult?.couponPrice > 0" class="fee-value discount">-¥{{ fmtPrice(priceResult.couponPrice) }}</text>
            <text v-else-if="selectedCoupon" class="fee-value">{{ couponLabel(selectedCoupon) }}</text>
            <text v-else-if="myCoupons.length" class="fee-value muted">{{ myCoupons.length }} 张可用 ›</text>
            <text v-else class="fee-value muted">无可用 ›</text>
          </view>
        </view>
        <view class="fee-row">
          <text class="fee-label">运费</text>
          <text class="fee-value">¥{{ fmtPrice(priceResult?.expressPrice) }}</text>
        </view>
      </view>

      <view class="bottom-spacer" />

      <!-- 优惠券选择弹层 -->
      <view v-if="couponPickerOpen" class="picker-mask" @click.self="couponPickerOpen = false">
        <view class="picker">
          <view class="picker-hd">选择优惠券</view>
          <scroll-view scroll-y class="picker-body">
            <view class="pick-item" :class="{ active: !selectedCoupon }" @click="pickCoupon(null)">
              <text>不使用优惠券</text>
            </view>
            <view
              v-for="uc in myCoupons"
              :key="uc.id"
              class="pick-item"
              :class="{ active: selectedCoupon?.id === uc.id }"
              @click="pickCoupon(uc)"
            >
              <view class="pick-left">
                <text class="pick-amt">{{ couponFace(uc.snapshot) }}</text>
              </view>
              <view class="pick-right">
                <text class="pick-name">{{ uc.snapshot?.name }}</text>
                <text class="pick-meta">{{ uc.snapshot?.minPrice > 0 ? '满' + uc.snapshot.minPrice + '可用' : '无门槛' }} · 至 {{ uc.endTime }}</text>
              </view>
            </view>
            <view v-if="!myCoupons.length" class="pick-empty">暂无可用优惠券</view>
          </scroll-view>
        </view>
      </view>

      <view class="bar">
        <view class="bar-total">
          <text class="bar-label">应付</text>
          <text class="bar-price">¥{{ fmtPrice(priceResult?.payPrice ?? goodsTotal) }}</text>
        </view>
        <button class="bar-btn" :disabled="submitting" @click="onSubmit">
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page {
  padding: 20rpx;
  min-height: 100vh;
}
.card {
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.08);
  border-radius: 18rpx;
  padding: 24rpx;
  margin-bottom: 18rpx;
}
.addr {
  position: relative;
  padding-right: 50rpx;
}
.addr-hd {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 10rpx;
}
.addr-name {
  font-size: 29rpx;
  font-weight: 600;
}
.addr-phone {
  font-size: 26rpx;
  color: #4a4844;
}
.addr-default {
  font-size: 20rpx;
  color: #d4380d;
  border: 2rpx solid #d4380d;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
}
.addr-detail {
  font-size: 25rpx;
  color: #4a4844;
  line-height: 1.5;
}
.addr-empty {
  font-size: 27rpx;
  color: #898781;
  padding: 12rpx 0;
}
.delivery-switch {
  display: flex;
  gap: 16rpx;
  padding: 12rpx;
}
.delivery-tab {
  flex: 1;
  text-align: center;
  font-size: 26rpx;
  color: #4a4844;
  padding: 16rpx 0;
  border-radius: 12rpx;
  background: #f5f4f1;
}
.delivery-tab.active {
  background: #d4380d;
  color: #fff;
  font-weight: 600;
}
.pickup-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 2rpx solid rgba(11, 11, 11, 0.06);
}
.pickup-row:last-child {
  border-bottom: none;
}
.pickup-row.active {
  color: #d4380d;
}
.pickup-name {
  font-size: 28rpx;
  font-weight: 600;
}
.pickup-detail {
  font-size: 24rpx;
  color: #898781;
  margin-top: 6rpx;
}
.pickup-hours {
  font-size: 22rpx;
  color: #898781;
  margin-top: 4rpx;
}
.pickup-check {
  color: #d4380d;
  font-size: 32rpx;
  font-weight: 700;
}
.arrow {
  position: absolute;
  right: 22rpx;
  top: 50%;
  transform: translateY(-50%);
  color: #b5b3ad;
  font-size: 34rpx;
}
.goods-row {
  display: flex;
  gap: 18rpx;
  padding-bottom: 20rpx;
  margin-bottom: 20rpx;
  border-bottom: 2rpx solid rgba(11, 11, 11, 0.06);
}
.goods-img {
  width: 150rpx;
  height: 150rpx;
  border-radius: 12rpx;
  background: #f0efec;
  flex-shrink: 0;
}
.goods-img-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b5b3ad;
  font-size: 22rpx;
}
.goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.goods-name {
  font-size: 27rpx;
  line-height: 1.4;
}
.goods-spec {
  font-size: 22rpx;
  color: #898781;
  background: #f5f4f1;
  border-radius: 6rpx;
  padding: 4rpx 12rpx;
  align-self: flex-start;
}
.goods-foot {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: auto;
}
.price {
  color: #d4380d;
  font-size: 29rpx;
  font-weight: 700;
}
.qty {
  font-size: 24rpx;
  color: #898781;
}
.remark-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.remark-label {
  font-size: 26rpx;
  color: #898781;
  width: 130rpx;
}
.remark-input {
  flex: 1;
  font-size: 26rpx;
  height: 64rpx;
}
.fee-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
}
.fee-label {
  font-size: 26rpx;
  color: #4a4844;
}
.fee-value {
  font-size: 26rpx;
}
.fee-value.discount {
  color: #d4380d;
}
.coupon-pick { display: flex; align-items: center; }
.fee-value.muted { color: #b5b3ad; }
.picker-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 50;
  display: flex; align-items: flex-end;
}
.picker {
  width: 100%; background: #fff; border-radius: 24rpx 24rpx 0 0; max-height: 70vh; display: flex; flex-direction: column;
}
.picker-hd { text-align: center; font-size: 28rpx; font-weight: 600; padding: 24rpx; border-bottom: 2rpx solid #f0f0ee; }
.picker-body { max-height: 60vh; }
.pick-item { display: flex; align-items: center; gap: 18rpx; padding: 24rpx; border-bottom: 2rpx solid #f5f4f1; }
.pick-item.active { background: #fff5f0; }
.pick-left { color: #d4380d; font-weight: 700; width: 130rpx; }
.pick-amt { font-size: 30rpx; }
.pick-right { display: flex; flex-direction: column; gap: 6rpx; }
.pick-name { font-size: 27rpx; }
.pick-meta { font-size: 22rpx; color: #898781; }
.pick-empty { text-align: center; color: #898781; padding: 60rpx 0; }
.bottom-spacer {
  height: 140rpx;
}
.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-top: 2rpx solid rgba(11, 11, 11, 0.08);
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.bar-total {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
}
.bar-label {
  font-size: 25rpx;
  color: #898781;
}
.bar-price {
  color: #d4380d;
  font-size: 36rpx;
  font-weight: 700;
}
.bar-btn {
  background: #d4380d;
  color: #fff;
  border-radius: 38rpx;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 28rpx;
  padding: 0 56rpx;
  margin: 0;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 120rpx 0;
}
</style>
