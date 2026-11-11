<script setup>
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { listOfflineStores } from '@/api'
import { formatDistance, haversineKm, lookupMapLocation, openMapNav } from '@/utils/amap'

/**
 * 门店列表。对应原型 docs/prototype/h5/store-locator.html。
 * - 浏览：从「我的」进入，可拨打电话 / 打开地图导航
 * - 选择：结算页带 select=1，确认后写回 checkout_picked_store
 */
const loading = ref(true)
const locating = ref(false)
const locateFailed = ref(false)
const stores = ref([])
const selectedId = ref('')
const selectMode = ref(false)
const myLocation = ref(null)
const locationLabel = ref('')

const selectedStore = computed(() =>
  stores.value.find((s) => String(s.id) === String(selectedId.value)) || null
)

const nearestId = computed(() => {
  if (!myLocation.value) return ''
  let bestId = ''
  let bestKm = Infinity
  stores.value.forEach((store) => {
    const km = distanceKm(store)
    if (km != null && km < bestKm) {
      bestKm = km
      bestId = String(store.id)
    }
  })
  return bestId
})

const viewStores = computed(() =>
  stores.value.map((store) => {
    const hours = hoursInfo(store)
    return {
      raw: store,
      id: store.id,
      name: store.name,
      logo: store.logo,
      phone: store.phone,
      addr: addr(store),
      hoursText: hours.text,
      hoursOpen: hours.open,
      distance: distanceText(store),
      nearest: String(store.id) === nearestId.value,
      canNav: canNavigate(store),
    }
  })
)

const locateHint = computed(() => {
  if (locating.value) return '正在定位…'
  if (locationLabel.value) return locationLabel.value
  if (myLocation.value) return '已获取当前位置'
  if (locateFailed.value) return '未开启定位，点击后可查看距离'
  return '点击获取当前位置，按距离排序'
})

onLoad((query) => {
  selectMode.value = query?.select === '1' || query?.select === 'true'
  if (query?.storeId) selectedId.value = String(query.storeId)
  if (selectMode.value) {
    uni.setNavigationBarTitle({ title: '选择自提门店' })
  }
  load()
  locateMe(false)
})

onShow(() => {
  if (locateFailed.value) locateMe(false)
})

async function load() {
  loading.value = true
  try {
    stores.value = (await listOfflineStores()) || []
    if (selectMode.value && !selectedId.value && stores.value.length === 1) {
      selectedId.value = String(stores.value[0].id)
    }
    sortStores()
  } catch (e) {
    stores.value = []
  } finally {
    loading.value = false
  }
}

function sortStores() {
  stores.value = [...stores.value].sort((a, b) => (distanceKm(a) ?? 9999) - (distanceKm(b) ?? 9999))
}

function fmtRegion(region) {
  return String(region || '').replace(/[\\/|]+/g, '').replace(/\s+/g, '')
}

function addr(store) {
  const region = fmtRegion(store.region)
  const detail = String(store.detail || '').trim()
  if (region && detail && detail.includes(region)) return detail
  return [region, detail].filter(Boolean).join('') || '地址待完善'
}

function hoursInfo(store) {
  const raw = String(store.businessHours || '').trim()
  if (!raw) return { text: '', open: null }
  const match = raw.match(/(\d{1,2}):(\d{2})\s*[-~—～至到]+\s*(\d{1,2}):(\d{2})/)
  if (!match) return { text: raw, open: null }
  const start = Number(match[1]) * 60 + Number(match[2])
  const end = Number(match[3]) * 60 + Number(match[4])
  const now = new Date()
  const current = now.getHours() * 60 + now.getMinutes()
  const open = end <= start ? current >= start || current < end : current >= start && current < end
  const pad = (n) => String(n).padStart(2, '0')
  return { text: `${pad(match[1])}:${match[2]}-${pad(match[3])}:${match[4]}`, open }
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

function locateMe(prompt = true) {
  locating.value = true
  uni.getLocation({
    type: 'gcj02',
    isHighAccuracy: true,
    success(res) {
      myLocation.value = { latitude: res.latitude, longitude: res.longitude }
      locateFailed.value = false
      sortStores()
      lookupMapLocation(res.longitude, res.latitude)
        .then((data) => {
          locationLabel.value = [data?.city, data?.district].filter(Boolean).join('')
            || data?.name
            || data?.address
            || ''
        })
        .catch(() => {})
    },
    fail() {
      locateFailed.value = true
      if (prompt) askLocationPermission()
    },
    complete() {
      locating.value = false
    },
  })
}

function askLocationPermission() {
  // #ifdef MP-WEIXIN
  uni.getSetting({
    success(setting) {
      if (setting.authSetting['scope.userLocation']) {
        uni.showToast({ title: '定位失败，请稍后重试', icon: 'none' })
        return
      }
      uni.showModal({
        title: '开启定位',
        content: '开启定位后可按距离排序，并显示距你多远',
        confirmText: '去开启',
        success(res) {
          if (res.confirm) uni.openSetting()
        },
      })
    },
  })
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '定位失败，请检查定位权限', icon: 'none' })
  // #endif
}

function distanceKm(store) {
  if (!myLocation.value) return null
  const lat = Number(store.latitude)
  const lng = Number(store.longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null
  return haversineKm(myLocation.value.latitude, myLocation.value.longitude, lat, lng)
}

function distanceText(store) {
  return formatDistance(distanceKm(store))
}

function canNavigate(store) {
  const lat = Number(store.latitude)
  const lng = Number(store.longitude)
  return Number.isFinite(lat) && Number.isFinite(lng) && !(lat === 0 && lng === 0)
}

function onNavigate(store) {
  openMapNav({
    latitude: store.latitude,
    longitude: store.longitude,
    name: store.name || '自提门店',
    address: addr(store),
  })
}
</script>

<template>
  <view class="page" :class="{ 'has-bar': selectMode }">
    <view class="locate-bar" @click="locateMe(true)">
      <view class="locate-ico">
        <AppIcon name="map-pin-green" :size="16" />
      </view>
      <view class="locate-copy">
        <text class="locate-title">{{ locateHint }}</text>
        <text class="locate-sub">{{ locating ? '请稍候' : '点击刷新定位' }}</text>
      </view>
      <text class="locate-count">共 {{ stores.length }} 家</text>
    </view>

    <view v-if="loading" class="empty">
      <view class="empty-ico"><AppIcon name="store-blue" :size="28" /></view>
      <text>正在加载门店…</text>
    </view>
    <view v-else-if="!stores.length" class="empty">
      <view class="empty-ico"><AppIcon name="store-blue" :size="28" /></view>
      <text>暂无可用门店</text>
      <text class="empty-sub">商家开通线下门店后会显示在这里</text>
    </view>

    <view
      v-for="store in viewStores"
      :key="store.id"
      class="store-item"
      :class="{ selected: selectMode && String(store.id) === String(selectedId) }"
      @click="onSelect(store.raw)"
    >
      <view class="store-top">
        <image v-if="store.logo" class="store-logo" :src="store.logo" mode="aspectFill" />
        <view v-else class="store-ico"><AppIcon name="store-blue" :size="28" /></view>
        <view class="store-body">
          <view class="name-row">
            <text class="store-name">{{ store.name }}</text>
            <text v-if="store.distance" class="distance">{{ store.distance }}</text>
          </view>
          <view class="tag-row">
            <text v-if="store.nearest" class="tag nearest">最近</text>
            <text v-if="store.hoursOpen === true" class="tag open">营业中</text>
            <text v-else-if="store.hoursOpen === false" class="tag closed">已打烊</text>
            <text v-if="selectMode && String(store.id) === String(selectedId)" class="tag picked">已选</text>
          </view>
          <view class="meta-line">
            <AppIcon name="map-pin-green" :size="13" />
            <text class="meta-text">{{ store.addr }}</text>
          </view>
          <view v-if="store.hoursText" class="meta-line">
            <AppIcon name="clock-3-green" :size="13" />
            <text class="meta-text">{{ store.hoursText }}</text>
          </view>
        </view>
        <view v-if="selectMode" class="radio" :class="{ on: String(store.id) === String(selectedId) }" />
      </view>

      <view class="store-actions">
        <view v-if="store.phone" class="act call" @click.stop="onCall(store.raw)">
          <AppIcon name="phone-green" :size="15" />
          <text>打电话</text>
        </view>
        <view class="act nav" :class="{ disabled: !store.canNav }" @click.stop="onNavigate(store.raw)">
          <AppIcon name="map-pin-green" :size="15" />
          <text>去导航</text>
        </view>
      </view>
    </view>

    <view v-if="selectMode && stores.length" class="bottom-bar">
      <button class="btn" @click="onConfirm">确认选择该门店</button>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 24rpx 40rpx;
  background: #f4f5f3;
  color: #1e2429;
}
.page.has-bar {
  padding-bottom: 160rpx;
}
.locate-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #fff;
  border: 1rpx solid #e4e6e3;
  border-radius: 16rpx;
  padding: 22rpx 24rpx;
  margin-bottom: 16rpx;
}
.locate-ico {
  width: 56rpx;
  height: 56rpx;
  border-radius: 14rpx;
  background: #eaf6f1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.locate-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.locate-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1e2429;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.locate-sub {
  font-size: 20rpx;
  color: #969ca0;
}
.locate-count {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #767d82;
}
.store-item {
  background: #fff;
  border: 1rpx solid #e4e6e3;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.store-item.selected {
  border-color: #2a78d6;
  box-shadow: 0 0 0 2rpx rgba(42, 120, 214, 0.18);
}
.store-top {
  display: flex;
  gap: 18rpx;
  align-items: flex-start;
}
.store-ico,
.store-logo {
  width: 96rpx;
  height: 96rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
  background: #edf5fc;
}
.store-ico {
  display: flex;
  align-items: center;
  justify-content: center;
}
.store-body {
  flex: 1;
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}
.store-name {
  flex: 1;
  min-width: 0;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.35;
}
.distance {
  flex-shrink: 0;
  font-size: 26rpx;
  font-weight: 700;
  color: #e34948;
  padding-top: 4rpx;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 10rpx;
}
.tag {
  font-size: 20rpx;
  line-height: 1;
  padding: 6rpx 10rpx;
  border-radius: 8rpx;
}
.tag.nearest {
  color: #e34948;
  background: #fff0ed;
}
.tag.open {
  color: #287e62;
  background: #eaf6f1;
}
.tag.closed {
  color: #898781;
  background: #f4f5f3;
}
.tag.picked {
  color: #2e70b8;
  background: #edf5fc;
}
.meta-line {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 10rpx;
}
.meta-text {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: #767d82;
  line-height: 1.45;
}
.radio {
  width: 36rpx;
  height: 36rpx;
  margin-top: 8rpx;
  border-radius: 50%;
  border: 3rpx solid #cfd5d9;
  flex-shrink: 0;
  box-sizing: border-box;
}
.radio.on {
  border-color: #2a78d6;
  background: radial-gradient(circle, #2a78d6 0 10rpx, transparent 11rpx);
}
.store-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eceeeb;
}
.act {
  flex: 1;
  height: 72rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  font-size: 26rpx;
  font-weight: 600;
}
.act.call {
  color: #287e62;
  background: #eaf6f1;
}
.act.nav {
  color: #2e70b8;
  background: #edf5fc;
}
.act.disabled {
  opacity: 0.45;
}
.empty {
  text-align: center;
  color: #767d82;
  font-size: 26rpx;
  padding: 120rpx 0 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.empty-ico {
  width: 88rpx;
  height: 88rpx;
  border-radius: 20rpx;
  background: #edf5fc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8rpx;
}
.empty-sub {
  font-size: 22rpx;
  color: #969ca0;
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
.btn::after {
  border: 0;
}
</style>
