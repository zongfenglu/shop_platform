<script setup>
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { listOfflineStores } from '@/api'

/**
 * 门店列表。对应原型 docs/prototype/h5/store-locator.html。
 * - 浏览：从「我的」进入，可拨打电话 / 打开地图导航
 * - 选择：结算页带 select=1，确认后写回 checkout_picked_store
 */
const loading = ref(true)
const stores = ref([])
const selectedId = ref('')
const selectMode = ref(false)

const selectedStore = computed(() =>
  stores.value.find((s) => String(s.id) === String(selectedId.value)) || null
)

onLoad((query) => {
  selectMode.value = query?.select === '1' || query?.select === 'true'
  if (query?.storeId) selectedId.value = String(query.storeId)
  if (selectMode.value) {
    uni.setNavigationBarTitle({ title: '选择自提门店' })
  }
  load()
})

async function load() {
  loading.value = true
  try {
    stores.value = (await listOfflineStores()) || []
    if (selectMode.value && !selectedId.value && stores.value.length === 1) {
      selectedId.value = String(stores.value[0].id)
    }
  } catch (e) {
    stores.value = []
  } finally {
    loading.value = false
  }
}

function addr(store) {
  return [store.region, store.detail].filter(Boolean).join(' ') || '地址待完善'
}

function onSelect(store) {
  if (!selectMode.value) return
  selectedId.value = String(store.id)
}

function onConfirm() {
  if (!selectedStore.value) {
    uni.showToast({ title: '请选择自提门店', icon: 'none' })
    return
  }
  uni.setStorageSync('checkout_picked_store', JSON.stringify(selectedStore.value))
  uni.navigateBack()
}

function onCall(store) {
  if (!store.phone) {
    uni.showToast({ title: '该门店暂无电话', icon: 'none' })
    return
  }
  uni.makePhoneCall({ phoneNumber: String(store.phone) })
}

function onNavigate(store) {
  const lat = Number(store.latitude)
  const lng = Number(store.longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
    uni.showToast({ title: '该门店暂无定位', icon: 'none' })
    return
  }
  uni.openLocation({
    latitude: lat,
    longitude: lng,
    name: store.name || '自提门店',
    address: addr(store),
  })
}
</script>

<template>
  <view class="page" :class="{ 'has-bar': selectMode }">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else-if="!stores.length" class="empty">暂无可用自提门店</view>
    <view
      v-for="store in stores"
      :key="store.id"
      class="store-item"
      :class="{ selected: selectMode && String(store.id) === String(selectedId) }"
      @click="onSelect(store)"
    >
      <image v-if="store.logo" class="store-logo" :src="store.logo" mode="aspectFill" />
      <view v-else class="store-ico">🏬</view>
      <view class="store-body">
        <view class="store-name">{{ store.name }}</view>
        <view class="store-addr">{{ addr(store) }}</view>
        <view class="store-meta">
          <text v-if="store.businessHours">营业时间 {{ store.businessHours }}</text>
          <text v-if="store.phone">{{ store.phone }}</text>
        </view>
        <view v-if="!selectMode" class="store-actions">
          <text class="act" @click.stop="onCall(store)">拨打电话</text>
          <text class="act" @click.stop="onNavigate(store)">导航</text>
        </view>
      </view>
      <text v-if="selectMode && String(store.id) === String(selectedId)" class="check">✓</text>
    </view>

    <view v-if="selectMode && stores.length" class="bottom-bar">
      <button class="btn" @click="onConfirm">确认选择该门店</button>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 16rpx 0 40rpx;
  min-height: 100vh;
}
.page.has-bar {
  padding-bottom: 160rpx;
}
.store-item {
  display: flex;
  gap: 20rpx;
  background: #fff;
  margin: 16rpx 28rpx;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(11, 11, 11, 0.04);
}
.store-item.selected {
  box-shadow: 0 0 0 4rpx #2a78d6;
}
.store-ico,
.store-logo {
  width: 88rpx;
  height: 88rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
  background: #eef4fc;
}
.store-ico {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
}
.store-body {
  flex: 1;
  min-width: 0;
}
.store-name {
  font-size: 28rpx;
  font-weight: 600;
}
.store-addr {
  font-size: 24rpx;
  color: #898781;
  margin-top: 8rpx;
  line-height: 1.5;
}
.store-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  font-size: 22rpx;
  color: #898781;
  margin-top: 10rpx;
}
.store-actions {
  display: flex;
  gap: 28rpx;
  margin-top: 16rpx;
}
.act {
  font-size: 24rpx;
  color: #2a78d6;
}
.check {
  align-self: center;
  color: #2a78d6;
  font-size: 32rpx;
  font-weight: 700;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 120rpx 0;
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(11, 11, 11, 0.06);
}
.btn {
  background: #e34948;
  color: #fff;
  border-radius: 12rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}
</style>
