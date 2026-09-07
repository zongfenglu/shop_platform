<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listCart, removeCartItems, updateCartQuantity } from '@/api'
import { getToken } from '@/utils/request'

/**
 * 购物车。对应原型 docs/prototype/h5/cart.html。
 *
 * 后端 cart 表不存价格快照，列表返回的价格/库存/商品名都是**实时**读 goods_sku/goods 的
 * （见 V11 迁移注释），所以商家改价、下架会立刻反映在这里。失效行（下架/无库存/商品被删）
 * 后端标 invalid=true，这里置灰且不可勾选、不计入合计 —— 不静默移除，让用户知道东西没了。
 *
 * 勾选状态是纯前端的：cart 表没有 selected 列，也不需要有（选中态是一次结算会话内的临时状态，
 * 存进库反而会出现"换个设备打开还记得上次勾了什么"的怪异体验）。
 */
const rows = ref([])
const loading = ref(true)
const loggedIn = ref(false)
/** 选中的购物车行 id 集合（字符串 id，不做数值转换） */
const checked = ref([])
const editing = ref(false)

const validRows = computed(() => rows.value.filter((r) => !r.invalid))
const checkedRows = computed(() => validRows.value.filter((r) => checked.value.includes(r.id)))
const allChecked = computed(
  () => validRows.value.length > 0 && checkedRows.value.length === validRows.value.length
)
const totalPrice = computed(() =>
  checkedRows.value.reduce((sum, r) => sum + Number(r.price ?? 0) * (r.quantity || 0), 0)
)
const totalCount = computed(() => checkedRows.value.reduce((sum, r) => sum + (r.quantity || 0), 0))

onShow(load)

async function load() {
  loggedIn.value = !!getToken()
  if (!loggedIn.value) {
    loading.value = false
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value = (await listCart()) || []
    // 保留原有勾选，同时丢掉已经不存在/已失效的行
    const validIds = validRows.value.map((r) => r.id)
    checked.value = checked.value.filter((id) => validIds.includes(id))
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function toggleCheck(row) {
  if (row.invalid) return
  checked.value = checked.value.includes(row.id)
    ? checked.value.filter((id) => id !== row.id)
    : checked.value.concat(row.id)
}

function toggleAll() {
  checked.value = allChecked.value ? [] : validRows.value.map((r) => r.id)
}

async function onQuantityChange(row, delta) {
  const next = (row.quantity || 0) + delta
  if (next < 1) return
  if (row.stock != null && next > row.stock) {
    uni.showToast({ title: `库存仅剩 ${row.stock} 件`, icon: 'none' })
    return
  }
  const before = row.quantity
  row.quantity = next // 乐观更新，失败再回滚
  try {
    await updateCartQuantity(row.id, next)
  } catch (e) {
    row.quantity = before
  }
}

async function onRemoveChecked() {
  if (!checked.value.length) return
  uni.showModal({
    title: '删除商品',
    content: `确认从购物车删除选中的 ${checked.value.length} 件商品？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await removeCartItems(checked.value)
        checked.value = []
        await load()
      } catch (e) {
        // request.js 已统一提示
      }
    },
  })
}

async function onRemoveInvalid() {
  const invalidIds = rows.value.filter((r) => r.invalid).map((r) => r.id)
  if (!invalidIds.length) return
  try {
    await removeCartItems(invalidIds)
    await load()
  } catch (e) {
    // request.js 已统一提示
  }
}

function onCheckout() {
  if (!checkedRows.value.length) {
    uni.showToast({ title: '请先选择商品', icon: 'none' })
    return
  }
  // 只把 cartId 传给结算页，由它重新拉一次购物车拿最新价格 —— 不把价格从这里带过去，
  // 免得出现"购物车显示的价和结算页算的价对不上"的经典问题（算价一律以服务端为准）。
  const ids = checkedRows.value.map((r) => r.id).join(',')
  uni.navigateTo({ url: `/pages/order/checkout?cartIds=${ids}` })
}

function goLogin() {
  uni.navigateTo({ url: '/pages/my/login' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function goDetail(row) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${row.goodsId}` })
}

function fmtPrice(v) {
  return Number(v ?? 0).toFixed(2)
}
</script>

<template>
  <view class="page">
    <view v-if="!loggedIn" class="empty">
      <text class="empty-txt">登录后查看购物车</text>
      <button class="empty-btn" @click="goLogin">去登录</button>
    </view>

    <template v-else>
      <view v-if="loading" class="empty"><text class="empty-txt">加载中…</text></view>
      <view v-else-if="!rows.length" class="empty">
        <text class="empty-txt">购物车还是空的</text>
        <button class="empty-btn" @click="goHome">去逛逛</button>
      </view>

      <template v-else>
        <view class="hd">
          <text class="hd-count">共 {{ rows.length }} 件商品</text>
          <text class="hd-edit" @click="editing = !editing">{{ editing ? '完成' : '管理' }}</text>
        </view>

        <view class="list">
          <view v-for="row in rows" :key="row.id" class="row" :class="{ invalid: row.invalid }">
            <view class="check" :class="{ on: checked.includes(row.id), off: row.invalid }" @click="toggleCheck(row)">
              <text v-if="checked.includes(row.id) && !row.invalid">✓</text>
            </view>
            <image v-if="row.image" class="img" :src="row.image" mode="aspectFill" @click="goDetail(row)" />
            <view v-else class="img img-ph">无图</view>
            <view class="info">
              <view class="name" @click="goDetail(row)">{{ row.goodsName }}</view>
              <view v-if="row.specText" class="spec">{{ row.specText }}</view>
              <view v-if="row.invalid" class="invalid-tag">{{ row.invalidReason }}</view>
              <view class="row-foot">
                <text class="price">¥{{ fmtPrice(row.price) }}</text>
                <view v-if="!row.invalid" class="stepper">
                  <view class="step-btn" @click="onQuantityChange(row, -1)">−</view>
                  <text class="step-num">{{ row.quantity }}</text>
                  <view class="step-btn" @click="onQuantityChange(row, 1)">＋</view>
                </view>
                <text v-else class="qty-txt">×{{ row.quantity }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="rows.some((r) => r.invalid)" class="clear-invalid" @click="onRemoveInvalid">
          清空失效商品
        </view>

        <view class="bottom-spacer" />

        <view class="bar">
          <view class="bar-check" @click="toggleAll">
            <view class="check" :class="{ on: allChecked }"><text v-if="allChecked">✓</text></view>
            <text class="bar-all">全选</text>
          </view>
          <template v-if="editing">
            <view class="bar-spacer" />
            <button class="bar-btn del" @click="onRemoveChecked">删除({{ checkedRows.length }})</button>
          </template>
          <template v-else>
            <view class="bar-total">
              <text class="bar-total-label">合计</text>
              <text class="bar-total-price">¥{{ fmtPrice(totalPrice) }}</text>
            </view>
            <button class="bar-btn buy" @click="onCheckout">结算({{ totalCount }})</button>
          </template>
        </view>
      </template>
    </template>
  </view>
</template>

<style scoped>
.page {
  padding: 20rpx;
  min-height: 100vh;
}
.hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6rpx 8rpx 18rpx;
}
.hd-count {
  font-size: 24rpx;
  color: #898781;
}
.hd-edit {
  font-size: 26rpx;
  color: #2a78d6;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}
.row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.08);
  border-radius: 18rpx;
  padding: 20rpx;
}
.row.invalid {
  opacity: 0.55;
}
.check {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(11, 11, 11, 0.24);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24rpx;
  flex-shrink: 0;
}
.check.on {
  background: #d4380d;
  border-color: #d4380d;
}
.check.off {
  background: #eceae6;
  border-color: #eceae6;
}
.img {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  background: #f0efec;
  flex-shrink: 0;
}
.img-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b5b3ad;
  font-size: 22rpx;
}
.info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.name {
  font-size: 27rpx;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.spec {
  font-size: 22rpx;
  color: #898781;
  background: #f5f4f1;
  border-radius: 6rpx;
  padding: 4rpx 12rpx;
  align-self: flex-start;
}
.invalid-tag {
  font-size: 22rpx;
  color: #d4380d;
}
.row-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4rpx;
}
.price {
  color: #d4380d;
  font-size: 30rpx;
  font-weight: 700;
}
.qty-txt {
  font-size: 24rpx;
  color: #898781;
}
.stepper {
  display: flex;
  align-items: center;
  border: 2rpx solid rgba(11, 11, 11, 0.14);
  border-radius: 8rpx;
  overflow: hidden;
}
.step-btn {
  width: 56rpx;
  height: 50rpx;
  line-height: 50rpx;
  text-align: center;
  font-size: 28rpx;
  background: #f5f4f1;
}
.step-num {
  width: 72rpx;
  text-align: center;
  font-size: 25rpx;
}
.clear-invalid {
  text-align: center;
  color: #898781;
  font-size: 24rpx;
  padding: 28rpx 0 8rpx;
}
.bottom-spacer {
  /* 结算栏 + tabBar 两层的高度，否则最后一行商品会被压住 */
  height: 240rpx;
}
.bar {
  position: fixed;
  left: 0;
  right: 0;
  /* 购物车是 tabBar 页面，bottom:0 会被 uni 的 tabBar 盖住（结算/全选点不到），
     所以要抬到 tabBar 之上。H5 端 tabBar 高度为 50px，各端一致由 uni 保证；
     再叠加安全区，避免全面屏机型上被下巴挡住。 */
  bottom: calc(50px + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  border-top: 2rpx solid rgba(11, 11, 11, 0.08);
  padding: 16rpx 24rpx;
}
.bar-check {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.bar-all {
  font-size: 26rpx;
}
.bar-spacer {
  flex: 1;
}
.bar-total {
  flex: 1;
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 8rpx;
}
.bar-total-label {
  font-size: 24rpx;
  color: #898781;
}
.bar-total-price {
  color: #d4380d;
  font-size: 34rpx;
  font-weight: 700;
}
.bar-btn {
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 38rpx;
  font-size: 27rpx;
  color: #fff;
  margin: 0;
  padding: 0 44rpx;
}
.bar-btn.buy {
  background: #d4380d;
}
.bar-btn.del {
  background: #898781;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28rpx;
  padding: 160rpx 0;
}
.empty-txt {
  color: #898781;
  font-size: 27rpx;
}
.empty-btn {
  background: #2a78d6;
  color: #fff;
  border-radius: 36rpx;
  font-size: 27rpx;
  padding: 0 60rpx;
  height: 72rpx;
  line-height: 72rpx;
}
</style>
