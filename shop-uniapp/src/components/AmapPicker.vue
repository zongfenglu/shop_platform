<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getMapConfig, loadAmapJs } from '@/utils/amap'

const emit = defineEmits(['confirm', 'close'])
const mapEl = ref(null)
const searchEl = ref(null)
const locating = ref(false)
const selected = ref(null)
let map = null
let marker = null
let geocoder = null

function nativeNode(el, selector) {
  if (!el) return null
  const root = el.$el || el
  if (selector && root.querySelector) return root.querySelector(selector) || root
  return root
}

function setPoint(lat, lng, extra = {}) {
  selected.value = {
    latitude: Number(Number(lat).toFixed(6)),
    longitude: Number(Number(lng).toFixed(6)),
    name: extra.name || selected.value?.name || '',
    address: extra.address || selected.value?.address || '',
    province: extra.province || selected.value?.province || '',
    city: extra.city || selected.value?.city || '',
    region: extra.region || selected.value?.region || '',
  }
  const position = [selected.value.longitude, selected.value.latitude]
  if (!marker) {
    marker = new window.AMap.Marker({ position })
    map.add(marker)
  } else {
    marker.setPosition(position)
  }
  map.setZoomAndCenter(Math.max(map.getZoom(), 16), position)
  if (!geocoder) return
  geocoder.getAddress(position, (status, result) => {
    if (status !== 'complete' || !result?.regeocode) return
    const ac = result.regeocode.addressComponent || {}
    selected.value = {
      ...selected.value,
      address: extra.address || result.regeocode.formattedAddress || selected.value.address,
      province: ac.province || selected.value.province,
      city: ac.city || ac.province || selected.value.city,
      region: ac.district || selected.value.region,
      name: extra.name || ac.building || ac.streetNumber || ac.street || selected.value.name,
    }
  })
}

function locateCurrent() {
  if (!map) return
  locating.value = true
  map.plugin('AMap.Geolocation', () => {
    const geo = new window.AMap.Geolocation({ enableHighAccuracy: true, timeout: 10000 })
    geo.getCurrentPosition((status, result) => {
      locating.value = false
      if (status === 'complete' && result?.position) {
        setPoint(result.position.lat, result.position.lng)
        return
      }
      uni.showToast({ title: '定位失败，请在地图上选点', icon: 'none' })
    })
  })
}

function confirm() {
  if (!selected.value) {
    uni.showToast({ title: '请先选择位置', icon: 'none' })
    return
  }
  emit('confirm', selected.value)
}

onMounted(async () => {
  try {
    const cfg = await getMapConfig()
    if (!cfg?.apiKey) {
      uni.showToast({ title: '尚未配置高德地图', icon: 'none' })
      emit('close')
      return
    }
    const AMap = await loadAmapJs(cfg.apiKey, cfg.securityJsCode)
    await nextTick()
    const container = nativeNode(mapEl.value)
    const searchInput = nativeNode(searchEl.value, 'input')
    map = new AMap.Map(container, { zoom: 12, center: [120.3119, 31.4912] })
    AMap.plugin(['AMap.Geocoder', 'AMap.AutoComplete', 'AMap.Geolocation'], () => {
      geocoder = new AMap.Geocoder({ radius: 1000, extensions: 'base' })
      if (searchInput) {
        const auto = new AMap.AutoComplete({ input: searchInput, city: '全国' })
        auto.on('select', (event) => {
          const poi = event?.poi
          if (!poi?.location) return
          setPoint(poi.location.lat, poi.location.lng, {
            name: poi.name,
            address: [poi.district, poi.address, poi.name].filter(Boolean).join(''),
          })
        })
      }
    })
    map.on('click', (event) => setPoint(event.lnglat.getLat(), event.lnglat.getLng()))
    setTimeout(() => map && map.resize(), 200)
  } catch (e) {
    uni.showToast({ title: e?.message || '高德地图加载失败', icon: 'none' })
  }
})

onBeforeUnmount(() => {
  if (map) map.destroy()
})
</script>

<template>
  <view class="amap-mask" @click.self="emit('close')">
    <view class="amap-dialog">
      <view class="amap-hd">
        <text class="amap-title">地图选点</text>
        <text class="amap-close" @click="emit('close')">关闭</text>
      </view>
      <input ref="searchEl" class="amap-search" placeholder="搜索小区、写字楼或街道" confirm-type="search" />
      <view ref="mapEl" class="amap-map" />
      <view class="amap-ft">
        <view class="amap-addr">{{ selected?.address || '点击地图或搜索地点' }}</view>
        <view class="amap-actions">
          <button class="amap-btn ghost" @click="locateCurrent">{{ locating ? '定位中…' : '定位当前' }}</button>
          <button class="amap-btn" @click="confirm">确认位置</button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.amap-mask { position: fixed; inset: 0; z-index: 80; background: rgba(20,24,28,.45); display: flex; align-items: flex-end; }
.amap-dialog { width: 100%; background: #fff; border-radius: 24rpx 24rpx 0 0; overflow: hidden; }
.amap-hd { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 28rpx 12rpx; }
.amap-title { font-size: 30rpx; font-weight: 700; }
.amap-close { color: #2a78d6; font-size: 26rpx; }
.amap-search { margin: 0 24rpx 16rpx; height: 72rpx; padding: 0 20rpx; border: 2rpx solid #e6e4de; border-radius: 12rpx; font-size: 26rpx; }
.amap-map { width: 100%; height: 52vh; background: #eef1ee; }
.amap-ft { padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom)); }
.amap-addr { min-height: 40rpx; color: #52514e; font-size: 24rpx; line-height: 1.5; }
.amap-actions { display: flex; gap: 16rpx; margin-top: 16rpx; }
.amap-btn { flex: 1; height: 76rpx; margin: 0; border: 0; border-radius: 38rpx; background: #e34948; color: #fff; font-size: 26rpx; line-height: 76rpx; }
.amap-btn::after { border: 0; }
.amap-btn.ghost { background: #f3f1ec; color: #20262e; }
</style>
