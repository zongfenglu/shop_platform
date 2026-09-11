<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { closeAfterSale, getAfterSale, returnShippedAfterSale } from '@/api'

/**
 * 售后详情。对应 ConsumerAfterSaleController#detail/return-shipped/close。
 * 状态机见 StoreAfterSaleController 类注释：
 *   applying(审核中) -[商户同意/拒绝]-> approved/rejected
 *   approved -[退货退款需买家先寄回]-> return_shipped -[商户退款]-> refunding -> refunded
 * 买家侧能做的操作：applying 时可撤销（close）；approved 且是退货退款类型时需要填写寄回物流（return-shipped）。
 */
const id = ref(null)
const data = ref(null)
const loading = ref(false)

onLoad((query) => {
  id.value = query.id
  load()
})

async function load() {
  loading.value = true
  try {
    data.value = await getAfterSale(id.value)
  } catch (e) {
    data.value = null
  } finally {
    loading.value = false
  }
}

const TYPE_TEXT = { refund_only: '仅退款', return_refund: '退货退款' }
const STATUS_TEXT = {
  applying: '审核中',
  approved: '已同意，等待处理',
  rejected: '已拒绝',
  return_shipped: '已寄回，等待商家退款',
  refunding: '退款中',
  refunded: '已退款',
  closed: '已关闭',
}

function fmtPrice(v) {
  return `¥${Number(v ?? 0).toFixed(2)}`
}

function fmtDateTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '—'
}

function returnAddress() {
  try {
    return JSON.parse(data.value?.returnAddressSnapshot || 'null')
  } catch (e) {
    return null
  }
}

function canClose() {
  return data.value?.status === 'applying'
}

function needsReturnShip() {
  return data.value?.type === 'return_refund' && data.value?.status === 'approved'
}

function onClose() {
  uni.showModal({
    title: '撤销申请',
    content: '确认撤销本次售后申请？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await closeAfterSale(id.value)
        uni.showToast({ title: '已撤销' })
        await load()
      } catch (e) {
        // 已由 request.js 提示
      }
    },
  })
}

// ---------- 寄回物流 ----------
const shipModalOpen = ref(false)
const expressCompany = ref('')
const expressNo = ref('')
const submitting = ref(false)

function openShipModal() {
  expressCompany.value = ''
  expressNo.value = ''
  shipModalOpen.value = true
}

async function onSubmitShip() {
  if (!expressCompany.value.trim() || !expressNo.value.trim()) {
    uni.showToast({ title: '请填写完整的快递信息', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await returnShippedAfterSale(id.value, {
      expressCompany: expressCompany.value.trim(),
      expressNo: expressNo.value.trim(),
    })
    uni.showToast({ title: '已提交' })
    shipModalOpen.value = false
    await load()
  } catch (e) {
    // 已由 request.js 提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <view v-if="data" class="hero">
      <view class="hero-title">{{ STATUS_TEXT[data.status] || data.status }}</view>
    </view>

    <view v-if="data" class="card">
      <view class="kv-row"><text>申请类型</text><text>{{ TYPE_TEXT[data.type] || data.type }}</text></view>
      <view class="kv-row"><text>退款金额</text><text>{{ fmtPrice(data.refundAmount) }}</text></view>
      <view class="kv-row"><text>申请原因</text><text>{{ data.applyReason || '—' }}</text></view>
      <view class="kv-row" v-if="data.applyDesc"><text>补充说明</text><text>{{ data.applyDesc }}</text></view>
      <view class="kv-row" v-if="data.auditRemark"><text>商家备注</text><text>{{ data.auditRemark }}</text></view>
      <view class="kv-row"><text>申请时间</text><text>{{ fmtDateTime(data.createTime) }}</text></view>
    </view>

    <view v-if="data && data.returnExpressNo" class="card">
      <view class="card-title">寄回物流</view>
      <view class="kv-row"><text>快递公司</text><text>{{ data.returnExpressCompany }}</text></view>
      <view class="kv-row"><text>快递单号</text><text>{{ data.returnExpressNo }}</text></view>
    </view>

    <view v-if="data && returnAddress() && needsReturnShip()" class="card">
      <view class="card-title">寄回地址</view>
      <view class="address-name">{{ returnAddress().contactName }} {{ returnAddress().phone }}</view>
      <view class="address-detail">{{ returnAddress().province }}{{ returnAddress().city }}{{ returnAddress().district }}{{ returnAddress().detail }}</view>
    </view>

    <view v-if="data" class="bottom-bar">
      <button v-if="canClose()" class="m-btn" @click="onClose">撤销申请</button>
      <button v-if="needsReturnShip()" class="m-btn m-btn-warm" @click="openShipModal">填写寄回物流</button>
    </view>

    <view v-if="shipModalOpen" class="modal-mask" @click="shipModalOpen = false">
      <view class="modal" @click.stop>
        <view class="modal-title">填写寄回物流</view>
        <view class="form-item">
          <view class="form-label">快递公司</view>
          <input v-model="expressCompany" class="input" placeholder="如 顺丰速运" />
        </view>
        <view class="form-item">
          <view class="form-label">快递单号</view>
          <input v-model="expressNo" class="input" />
        </view>
        <view class="modal-actions">
          <button class="m-btn" @click="shipModalOpen = false">取消</button>
          <button class="m-btn m-btn-warm" :disabled="submitting" @click="onSubmitShip">提交</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f9f9f7;
  padding-bottom: 140rpx;
}
.hero {
  background: linear-gradient(135deg, #ff9143, #e34948);
  padding: 40rpx 32rpx;
}
.hero-title {
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
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
  margin-bottom: 12rpx;
}
.address-name { font-size: 26rpx; font-weight: 600; margin-bottom: 8rpx; }
.address-detail { font-size: 24rpx; color: #52514e; line-height: 1.55; }
.kv-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  font-size: 24rpx;
  color: #52514e;
}
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 16rpx 28rpx;
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  box-shadow: 0 -2rpx 8rpx rgba(11, 11, 11, 0.06);
}
.m-btn {
  font-size: 26rpx;
  margin: 0;
  padding: 0 32rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  background: #fff;
  border: 2rpx solid rgba(11, 11, 11, 0.16);
}
.m-btn-warm {
  background: #e34948;
  color: #fff;
  border: none;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  width: 600rpx;
}
.modal-title {
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
}
.form-item {
  margin-bottom: 20rpx;
}
.form-label {
  font-size: 24rpx;
  color: #52514e;
  margin-bottom: 8rpx;
}
.input {
  border: 2rpx solid rgba(11, 11, 11, 0.16);
  border-radius: 12rpx;
  height: 72rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
}
.modal-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}
.modal-actions .m-btn {
  flex: 1;
}
</style>
