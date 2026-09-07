<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import { getOrder, publishComment } from '@/api'

/**
 * 发布商品评价。对应 GoodsCommentController#publish。
 *
 * 原型 h5/goods-comment.html 展示的是"某商品的评价列表"页（含综合评分统计），
 * 不是发布表单——原型里没有对应的发布页可以照抄，这里按站内既有的卡片/表单视觉语言
 * （圆角卡片 + 橙红品牌色）自己设计，字段严格对应后端 PublishCommentRequest：
 * orderId/orderGoodsId/score(1-5)/content/images。
 *
 * 同商品列表逻辑一样：一个订单可能有多个商品行，要先选具体针对哪一件评价。
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
  score: 5,
  content: '',
})

function onSelectGoods(g) {
  form.orderGoodsId = g.id
}

function onPickScore(n) {
  form.score = n
}

async function onSubmit() {
  if (!form.orderGoodsId) {
    uni.showToast({ title: '请选择要评价的商品', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await publishComment({
      // orderId 是字符串（19位雪花ID，超过 JS Number.MAX_SAFE_INTEGER 会被静默舍入精度，
      // 见 shop-framework 的 JacksonConfig 注释与 CONTRIBUTING.md 的 Code Review 关注点），
      // 从 URL query 拿到什么就原样传什么，绝不能 Number() 转换。
      orderId: orderId.value,
      orderGoodsId: form.orderGoodsId,
      score: form.score,
      content: form.content || undefined,
      images: [],
    })
    uni.showToast({ title: '评价发布成功' })
    setTimeout(() => uni.navigateBack({ delta: 1 }), 800)
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
          <view class="g-spec">{{ g.specText || '默认规格' }}</view>
        </view>
        <view class="check" v-if="form.orderGoodsId === g.id">✓</view>
      </view>
    </view>

    <view class="card">
      <view class="card-title">评分</view>
      <view class="stars">
        <text
          v-for="n in 5"
          :key="n"
          class="star"
          :class="{ filled: n <= form.score }"
          @click="onPickScore(n)"
        >★</text>
      </view>
      <view class="score-hint">{{ ['', '很不满意', '不满意', '一般', '满意', '非常满意'][form.score] }}</view>
    </view>

    <view class="card">
      <view class="card-title">评价内容（选填）</view>
      <textarea v-model="form.content" class="textarea" placeholder="说说商品使用感受，帮助其他买家参考" />
    </view>

    <view class="bottom-bar">
      <button class="m-btn m-btn-warm" :disabled="submitting" @click="onSubmit">
        {{ submitting ? '发布中…' : '发布评价' }}
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
  border-color: #e34948;
  background: #fbe7e6;
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
  color: #e34948;
  font-weight: 700;
}
.stars {
  display: flex;
  gap: 12rpx;
  justify-content: center;
  padding: 12rpx 0;
}
.star {
  font-size: 60rpx;
  color: #e1e0d9;
}
.star.filled {
  color: #fab219;
}
.score-hint {
  text-align: center;
  font-size: 24rpx;
  color: #898781;
}
.textarea {
  width: 100%;
  min-height: 160rpx;
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
