<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addToCart, getGoodsComments, getGoodsDetail } from '@/api'
import { getToken } from '@/utils/request'

/**
 * 商品详情。对应原型 docs/prototype/h5/goods-detail.html。
 *
 * 规格选择器（Sprint 3 的验收项："规格选择器正确联动库存与价格"）：
 * 后端 /api/goods/{id} 返回 specs 规格树 + skus 列表，前端把用户选中的规格值 id
 * **按 specs 的顺序**拼成 "12_35"，与 sku.specValueIds 精确匹配得到当前 SKU。
 * 顺序由后端保证（它是从 SKU 串里各段的位置反推出来的），前端不能自己排序，
 * 否则"红色/M"永远匹配不到按 "M/红色" 存的那条 SKU。
 *
 * 单规格商品的 specs 为空数组、唯一 SKU 的 specValueIds 为空串——这里不为它写第二套分支，
 * 与后端"下单只认 SKU"的约定保持一致：selectedSku 直接取唯一那条即可。
 */
const goods = ref(null)
const comments = ref([])
const loading = ref(true)
/** specIndex -> 选中的规格值 id（字符串，绝不转 Number，见 CONTRIBUTING.md） */
const selected = ref({})
const quantity = ref(1)
const specPopupVisible = ref(false)
/** 'cart' 加购 / 'buy' 立即购买——弹层确认后走哪条路 */
const specAction = ref('cart')

const isMultiSpec = computed(() => (goods.value?.specs?.length || 0) > 0)

/** 当前选中的 SKU；多规格未选全时为 null */
const selectedSku = computed(() => {
  if (!goods.value) return null
  if (!isMultiSpec.value) return goods.value.skus?.[0] || null
  const key = goods.value.specs.map((_, i) => selected.value[i]).join('_')
  if (goods.value.specs.some((_, i) => !selected.value[i])) return null
  return goods.value.skus.find((s) => s.specValueIds === key) || null
})

const selectedSpecText = computed(() => {
  if (!isMultiSpec.value) return ''
  return goods.value.specs
    .map((g, i) => {
      const vid = selected.value[i]
      const v = g.values.find((x) => x.id === vid)
      return v ? v.value : null
    })
    .filter(Boolean)
    .join('/')
})

/** 展示价：选中 SKU 用其单价，否则用区间 */
const displayPrice = computed(() => {
  if (selectedSku.value) return fmtPrice(selectedSku.value.price)
  const g = goods.value
  if (!g) return '0.00'
  if (g.minPrice != null && g.maxPrice != null && String(g.minPrice) !== String(g.maxPrice)) {
    return `${fmtPrice(g.minPrice)} ~ ${fmtPrice(g.maxPrice)}`
  }
  return fmtPrice(g.minPrice)
})

const displayStock = computed(() =>
  selectedSku.value ? selectedSku.value.stock : goods.value?.stockTotal ?? 0
)

/**
 * 某个规格值是否可选：把它代入当前选择，看看还有没有**有库存**的 SKU 能匹配上。
 * 这就是"联动库存"——选了"红色"之后，红色没货的尺码会自动置灰，而不是让用户点进去才报库存不足。
 */
function isValueAvailable(specIndex, valueId) {
  if (!goods.value) return false
  const trial = { ...selected.value, [specIndex]: valueId }
  return goods.value.skus.some((sku) => {
    if (!sku.stock || sku.stock <= 0) return false
    const segs = sku.specValueIds.split('_')
    return goods.value.specs.every((_, i) => !trial[i] || segs[i] === trial[i])
  })
}

onLoad(async (query) => {
  // query.id 来自 URL，本来就是字符串，保持原样传给接口。
  const id = query?.id
  if (!id) {
    uni.showToast({ title: '缺少商品参数', icon: 'none' })
    loading.value = false
    return
  }
  try {
    goods.value = await getGoodsDetail(id)
    // 单规格：唯一 SKU 直接可用，无需选择
    comments.value = (await getGoodsComments(id).catch(() => [])) || []
  } catch (e) {
    goods.value = null
  } finally {
    loading.value = false
  }
})

function onSelectValue(specIndex, valueId) {
  if (!isValueAvailable(specIndex, valueId)) return
  selected.value =
    selected.value[specIndex] === valueId
      ? { ...selected.value, [specIndex]: undefined }
      : { ...selected.value, [specIndex]: valueId }
  // 换规格后数量夹回新 SKU 的库存范围内
  const stock = selectedSku.value?.stock
  if (stock != null && quantity.value > stock) quantity.value = Math.max(stock, 1)
}

function openSpec(action) {
  specAction.value = action
  if (!isMultiSpec.value) {
    // 单规格没什么可选的，直接执行
    confirmSpec()
    return
  }
  specPopupVisible.value = true
}

function onQuantityChange(delta) {
  const next = quantity.value + delta
  const max = selectedSku.value?.stock ?? 999
  if (next < 1 || next > max) return
  quantity.value = next
}

async function confirmSpec() {
  if (!requireLogin()) return
  const sku = selectedSku.value
  if (!sku) {
    uni.showToast({ title: '请选择完整规格', icon: 'none' })
    return
  }
  if (!sku.stock || sku.stock <= 0) {
    uni.showToast({ title: '该规格暂时无货', icon: 'none' })
    return
  }

  if (specAction.value === 'buy') {
    specPopupVisible.value = false
    // 立即购买不经过购物车，直接把 goodsId/skuId/数量带到结算页（结算页据此重新拉商品取最新价）
    uni.navigateTo({
      url: `/pages/order/checkout?goodsId=${goods.value.id}&skuId=${sku.id}&quantity=${quantity.value}`,
    })
    return
  }
  try {
    await addToCart(sku.id, quantity.value)
    specPopupVisible.value = false
    uni.showToast({ title: '已加入购物车' })
  } catch (e) {
    // request.js 已统一提示
  }
}

function requireLogin() {
  if (getToken()) return true
  uni.navigateTo({ url: '/pages/my/login' })
  return false
}

function goCart() {
  uni.switchTab({ url: '/pages/cart/index' })
}

function fmtPrice(v) {
  return Number(v ?? 0).toFixed(2)
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!goods" class="empty">商品不存在或已下架</view>
    <template v-else>
      <swiper v-if="goods.images?.length" class="banner" indicator-dots circular>
        <swiper-item v-for="(img, i) in goods.images" :key="i">
          <image class="banner-img" :src="img" mode="aspectFill" />
        </swiper-item>
      </swiper>
      <view v-else class="banner banner-ph">暂无图片</view>

      <view class="block">
        <view class="price-row">
          <text class="price">¥{{ displayPrice }}</text>
          <text v-if="goods.linePrice" class="price-old">¥{{ fmtPrice(goods.linePrice) }}</text>
        </view>
        <view class="name">{{ goods.name }}</view>
        <view v-if="goods.subName" class="sub">{{ goods.subName }}</view>
        <view class="meta-row">
          <text class="meta">已售 {{ goods.sales ?? 0 }}</text>
          <text class="meta">库存 {{ displayStock ?? 0 }}</text>
          <text v-if="goods.isVirtual" class="meta">虚拟商品 · 无需物流</text>
        </view>
      </view>

      <view v-if="isMultiSpec" class="block tap-row" @click="openSpec('cart')">
        <text class="tap-label">规格</text>
        <text class="tap-value">{{ selectedSpecText || '请选择规格' }}</text>
        <text class="tap-arrow">›</text>
      </view>

      <view class="block">
        <view class="block-title">商品详情</view>
        <view v-if="goods.content" class="goods-html">
          <rich-text :nodes="goods.content" />
        </view>
        <text v-else class="muted small">商家暂未填写详情</text>
      </view>

      <view class="block">
        <view class="block-title">商品评价（{{ comments.length }}）</view>
        <view v-if="!comments.length" class="muted small">暂无评价</view>
        <view v-for="c in comments" :key="c.id" class="comment">
          <view class="comment-hd">
            <text class="stars">{{ '★'.repeat(c.score || 0) }}{{ '☆'.repeat(5 - (c.score || 0)) }}</text>
            <text class="comment-date">{{ String(c.createTime || '').slice(0, 10) }}</text>
          </view>
          <view class="comment-body">{{ c.content || '此用户没有填写评价' }}</view>
          <view v-if="c.reply" class="comment-reply">商家回复：{{ c.reply }}</view>
        </view>
      </view>

      <view class="bottom-spacer" />

      <view class="action-bar">
        <view class="action-ico" @click="goCart">
          <text class="action-ico-txt">购物车</text>
        </view>
        <button class="action-btn cart" @click="openSpec('cart')">加入购物车</button>
        <button class="action-btn buy" @click="openSpec('buy')">立即购买</button>
      </view>

      <!-- 规格选择弹层 -->
      <view v-if="specPopupVisible" class="mask" @click="specPopupVisible = false" />
      <view v-if="specPopupVisible" class="popup">
        <view class="popup-hd">
          <view class="popup-price">
            <text class="price">¥{{ displayPrice }}</text>
            <text class="muted small">库存 {{ displayStock ?? 0 }} 件</text>
          </view>
          <text class="popup-close" @click="specPopupVisible = false">✕</text>
        </view>

        <scroll-view class="popup-body" scroll-y>
          <view v-for="(group, gi) in goods.specs" :key="group.specId || gi" class="spec-group">
            <view class="spec-name">{{ group.name }}</view>
            <view class="spec-values">
              <view
                v-for="v in group.values"
                :key="v.id"
                class="spec-chip"
                :class="{ active: selected[gi] === v.id, disabled: !isValueAvailable(gi, v.id) }"
                @click="onSelectValue(gi, v.id)"
              >
                {{ v.value }}
              </view>
            </view>
          </view>

          <view class="qty-row">
            <text class="spec-name">购买数量</text>
            <view class="stepper">
              <view class="step-btn" @click="onQuantityChange(-1)">−</view>
              <text class="step-num">{{ quantity }}</text>
              <view class="step-btn" @click="onQuantityChange(1)">＋</view>
            </view>
          </view>
        </scroll-view>

        <button class="popup-confirm" @click="confirmSpec">
          {{ specAction === 'buy' ? '立即购买' : '加入购物车' }}
        </button>
      </view>
    </template>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
}
.banner {
  width: 100%;
  height: 640rpx;
  background: #f0efec;
}
.banner-img {
  width: 100%;
  height: 640rpx;
}
.banner-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b5b3ad;
  font-size: 26rpx;
}
.block {
  background: #fcfcfb;
  padding: 28rpx 24rpx;
  margin-bottom: 18rpx;
}
.block-title {
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.goods-html {
  font-size: 27rpx;
  color: #333;
  line-height: 1.7;
  overflow: hidden;
  word-break: break-word;
}
.goods-html :deep(img) {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 12rpx 0;
}
.goods-html :deep(p) {
  margin: 0 0 12rpx;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}
.price {
  color: #d4380d;
  font-size: 44rpx;
  font-weight: 700;
}
.price-old {
  color: #b5b3ad;
  font-size: 24rpx;
  text-decoration: line-through;
}
.name {
  font-size: 32rpx;
  font-weight: 600;
  line-height: 1.45;
  margin-top: 12rpx;
}
.sub {
  font-size: 25rpx;
  color: #898781;
  margin-top: 6rpx;
}
.meta-row {
  display: flex;
  gap: 28rpx;
  margin-top: 14rpx;
}
.meta {
  font-size: 23rpx;
  color: #898781;
}
.tap-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.tap-label {
  font-size: 27rpx;
  color: #898781;
  width: 90rpx;
}
.tap-value {
  flex: 1;
  font-size: 27rpx;
}
.tap-arrow {
  color: #b5b3ad;
  font-size: 32rpx;
}
.muted {
  color: #898781;
}
.small {
  font-size: 24rpx;
  line-height: 1.7;
}
.comment {
  border-top: 2rpx solid rgba(11, 11, 11, 0.06);
  padding: 20rpx 0;
}
.comment-hd {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8rpx;
}
.stars {
  color: #f0a020;
  font-size: 24rpx;
}
.comment-date {
  color: #b5b3ad;
  font-size: 22rpx;
}
.comment-body {
  font-size: 26rpx;
  line-height: 1.6;
}
.comment-reply {
  margin-top: 10rpx;
  background: #f5f4f1;
  border-radius: 10rpx;
  padding: 14rpx;
  font-size: 24rpx;
  color: #4a4844;
}
.bottom-spacer {
  height: 140rpx;
}
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #fff;
  border-top: 2rpx solid rgba(11, 11, 11, 0.08);
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.action-ico {
  width: 110rpx;
  text-align: center;
}
.action-ico-txt {
  font-size: 22rpx;
  color: #4a4844;
}
.action-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  color: #fff;
  margin: 0;
}
.action-btn.cart {
  background: #f0a020;
}
.action-btn.buy {
  background: #d4380d;
}
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 10;
}
.popup {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-height: 76vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  z-index: 11;
  display: flex;
  flex-direction: column;
  padding: 28rpx 24rpx;
  padding-bottom: calc(28rpx + env(safe-area-inset-bottom));
}
.popup-hd {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20rpx;
}
.popup-price {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.popup-close {
  font-size: 32rpx;
  color: #898781;
  padding: 0 10rpx;
}
.popup-body {
  flex: 1;
  max-height: 46vh;
}
.spec-group {
  margin-bottom: 26rpx;
}
.spec-name {
  font-size: 26rpx;
  color: #4a4844;
  margin-bottom: 14rpx;
}
.spec-values {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.spec-chip {
  border: 2rpx solid rgba(11, 11, 11, 0.16);
  border-radius: 10rpx;
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  color: #0b0b0b;
  background: #fcfcfb;
}
.spec-chip.active {
  border-color: #d4380d;
  color: #d4380d;
  background: #fdf1ed;
}
.spec-chip.disabled {
  color: #c9c7c1;
  background: #f5f4f1;
  border-color: rgba(11, 11, 11, 0.06);
  text-decoration: line-through;
}
.qty-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0 24rpx;
}
.stepper {
  display: flex;
  align-items: center;
  border: 2rpx solid rgba(11, 11, 11, 0.14);
  border-radius: 10rpx;
  overflow: hidden;
}
.step-btn {
  width: 68rpx;
  height: 60rpx;
  line-height: 60rpx;
  text-align: center;
  font-size: 30rpx;
  background: #f5f4f1;
}
.step-num {
  width: 88rpx;
  text-align: center;
  font-size: 27rpx;
}
.popup-confirm {
  background: #d4380d;
  color: #fff;
  border-radius: 40rpx;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 29rpx;
  margin-top: 12rpx;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 120rpx 0;
}
</style>
