<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { deleteAddress, listAddresses, setDefaultAddress } from '@/api'

/**
 * 收货地址列表。两种用途：
 *   - 「我的」进来：纯管理（编辑/删除/设默认）
 *   - 结算页进来（URL 带 pick=1）：点一条即选中并返回结算页
 * 选中的地址通过 storage 回传给结算页 —— uni-app 没有 Vue Router 那样的返回值机制，
 * 事件总线在小程序端跨页面也不可靠，storage 是各端行为一致的做法。
 */
const rows = ref([])
const loading = ref(true)
const pickMode = ref(false)

onLoad((query) => {
  pickMode.value = query?.pick === '1'
})

onShow(load)

async function load() {
  loading.value = true
  try {
    rows.value = (await listAddresses()) || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function onPick(row) {
  if (!pickMode.value) return
  uni.setStorageSync('checkout_picked_address', JSON.stringify(row))
  uni.navigateBack()
}

function onEdit(row) {
  uni.navigateTo({ url: `/pages/address/edit?id=${row.id}` })
}

function onCreate() {
  uni.navigateTo({ url: '/pages/address/edit' })
}

async function onSetDefault(row) {
  if (row.isDefault) return
  try {
    await setDefaultAddress(row.id)
    await load()
  } catch (e) {
    // request.js 已统一提示
  }
}

function onDelete(row) {
  uni.showModal({
    title: '删除地址',
    content: `确认删除「${row.name} ${row.detail}」？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteAddress(row.id)
        await load()
      } catch (e) {
        // request.js 已统一提示
      }
    },
  })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!rows.length" class="empty">
      <text class="empty-txt">还没有收货地址</text>
    </view>

    <view v-else class="list">
      <view v-for="row in rows" :key="row.id" class="card">
        <view class="main" @click="onPick(row)">
          <view class="hd">
            <text class="name">{{ row.name }}</text>
            <text class="phone">{{ row.phone }}</text>
            <text v-if="row.isDefault" class="tag">默认</text>
          </view>
          <view class="detail">
            {{ row.province }}{{ row.city }}{{ row.region }}{{ row.detail }}
          </view>
        </view>
        <view class="ops">
          <text class="op" @click="onSetDefault(row)">{{ row.isDefault ? '已是默认' : '设为默认' }}</text>
          <text class="op" @click="onEdit(row)">编辑</text>
          <text class="op danger" @click="onDelete(row)">删除</text>
        </view>
      </view>
    </view>

    <view class="bottom-spacer" />
    <view class="bar">
      <button class="bar-btn" @click="onCreate">＋ 新增收货地址</button>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 20rpx;
  min-height: 100vh;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}
.card {
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.08);
  border-radius: 18rpx;
  padding: 24rpx;
}
.hd {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 10rpx;
}
.name {
  font-size: 29rpx;
  font-weight: 600;
}
.phone {
  font-size: 26rpx;
  color: #4a4844;
}
.tag {
  font-size: 20rpx;
  color: #d4380d;
  border: 2rpx solid #d4380d;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
}
.detail {
  font-size: 25rpx;
  color: #4a4844;
  line-height: 1.5;
}
.ops {
  display: flex;
  justify-content: flex-end;
  gap: 34rpx;
  border-top: 2rpx solid rgba(11, 11, 11, 0.06);
  margin-top: 18rpx;
  padding-top: 18rpx;
}
.op {
  font-size: 25rpx;
  color: #4a4844;
}
.op.danger {
  color: #d4380d;
}
.bottom-spacer {
  height: 140rpx;
}
.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-top: 2rpx solid rgba(11, 11, 11, 0.08);
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.bar-btn {
  background: #d4380d;
  color: #fff;
  border-radius: 38rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
  margin: 0;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
  padding: 140rpx 0;
}
.empty-txt {
  color: #898781;
  font-size: 27rpx;
}
</style>
