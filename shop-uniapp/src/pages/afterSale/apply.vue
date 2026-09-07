<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import { applyAfterSale, getOrder } from '@/api'

/**
 * 申请售后。对应 ConsumerAfterSaleController#apply。
 *
 * 后端 apply 需要 orderGoodsId（针对订单里的哪一件商品申请，不是针对整个订单），
 * 原型 order-list.html 的"申请售后"入口是从订单卡片进来的（一个订单可能有多个商品行），
 * 所以这里先加载订单详情，列出 goodsList 让用户选具体哪一件商品，而不是假设订单只有一件商品。
 */
const orderId = ref(null)
const order = ref(null)
const loading = ref(false)
const submitting = ref(false)

onLoad((query) => {
  orderId.value = query.orderId
  load()
})

async function load() {
  loading.value = true
  try {
    order.value = await getOrder(orderId.value)
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
}

const form = reactive({
  orderGoodsId: null,
  type: 'refund_only',
  applyReason: '',
  applyDesc: '',
  refundNum: 1,
})

const REASON_OPTIONS = ['拍错/多拍/不想要', '商品破损/质量问题', '与描述不符', '商家发错货', '其他原因']

function onSelectGoods(g) {
  form.orderGoodsId = g.id
  form.refundNum = g.totalNum
}

async function onSubmit() {
  if (!form.orderGoodsId) {
    uni.showToast({ title: '请选择要申请售后的商品', icon: 'none' })
    return
  }
  if (!form.applyReason) {
    uni.showToast({ title: '请选择申请原因', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const created = await applyAfterSale({
      // orderId 原样传字符串，绝不能 Number() 转换——见 comment.vue 同类注释
      orderId: orderId.value,
      orderGoodsId: form.orderGoodsId,
      type: form.type,
      applyReason: form.applyReason,
      applyDesc: form.applyDesc || undefined,
      images: [],
      refundNum: form.refundNum,
    })
    uni.showToast({ title: '申请已提交' })
    setTimeout(() => uni.redirectTo({ url: `/pages/afterSale/detail?id=${created.id}` }), 800)
  } catch (e) {
    // 已由 request.js 提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <view v-if="order" class="card">
      <view class="card-title">选择商品</view>
      <view
        v-for="g in order.goodsList"
        :key="g.id"
        class="goods-row"
        :class="{ selected: form.orderGoodsId === g.id }"
        @click="onSelectGoods(g)"
      >
        <view class="ph">📦</view>
        <view class="mid">
          <view class="g-name">{{ g.goodsName }}</view>
          <view class="g-spec">{{ g.specText || '默认规格' }} ×{{ g.totalNum }}</view>
        </view>
        <view class="check" v-if="form.orderGoodsId === g.id">✓</view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">售后类型</view>
      <view class="type-tabs">
        <view class="type-tab" :class="{ active: form.type === 'refund_only' }" @click="form.type = 'refund_only'">仅退款</view>
        <view class="type-tab" :class="{ active: form.type === 'return_refund' }" @click="form.type = 'return_refund'">退货退款</view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">申请原因</view>
      <view class="reason-list">
        <view
          v-for="r in REASON_OPTIONS"
          :key="r"
          class="reason-item"
          :class="{ active: form.applyReason === r }"
          @click="form.applyReason = r"
        >
          {{ r }}
        </view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">补充说明（选填）</view>
      <textarea v-model="form.applyDesc" class="textarea" placeholder="详细描述问题，帮助商家更快处理" />
    </view>

    <view class="bottom-bar">
      <button class="m-btn m-btn-warm" :disabled="submitting" @click="onSubmit">
        {{ submitting ? '提交中…' : '提交申请' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
  padding-bottom: 140rpx;
}
.card {
  background: #fff;
  margin: 20rpx 28rpx;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}
.card-title {
  font-size: 26rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
.goods-row {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 12rpx;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
}
.goods-row.selected {
  border-color: #2a78d6;
  background: #eaf2fc;
}
.ph {
  width: 80rpx;
  height: 80rpx;
  border-radius: 14rpx;
  background: linear-gradient(135deg, #f3e7e0, #eadad0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  flex-shrink: 0;
}
.mid {
  flex: 1;
}
.g-name {
  font-size: 26rpx;
}
.g-spec {
  font-size: 22rpx;
  color: #898781;
  margin-top: 4rpx;
}
.check {
  color: #2a78d6;
  font-weight: 700;
}
.type-tabs {
  display: flex;
  gap: 16rpx;
}
.type-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  border: 2rpx solid rgba(11, 11, 11, 0.16);
  font-size: 26rpx;
  color: #52514e;
}
.type-tab.active {
  border-color: #2a78d6;
  background: #eaf2fc;
  color: #1c5cab;
  font-weight: 600;
}
.reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.reason-item {
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
  border: 2rpx solid rgba(11, 11, 11, 0.16);
  font-size: 24rpx;
  color: #52514e;
}
.reason-item.active {
  border-color: #2a78d6;
  background: #eaf2fc;
  color: #1c5cab;
  font-weight: 600;
}
.textarea {
  width: 100%;
  min-height: 140rpx;
  font-size: 26rpx;
}
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 16rpx 28rpx;
  box-shadow: 0 -2rpx 8rpx rgba(11, 11, 11, 0.06);
}
.m-btn {
  width: 100%;
  margin: 0;
  font-size: 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
}
.m-btn-warm {
  background: #e34948;
  color: #fff;
  border: none;
}
</style>
