<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getMapConfig, loadAmapJs, lookupMapLocation, reverseGeocode, searchMapPlaces, searchPlacesByKeyword, waitForElementSize } from '@/utils/amap'

const emit = defineEmits(['confirm', 'close'])
const mapEl = ref(null)
const keyword = ref('')
const locating = ref(false)
const resolving = ref(false)
const selected = ref(null)
const pois = ref([])
let map = null
let marker = null
let lookupTimer = 0
let searchTimer = 0
let lookupSeq = 0

function nativeNode(el, selector) {
  if (!el) return null
  const root = el.$el || el
  if (selector && root.querySelector) return root.querySelector(selector) || root
  return root
}

function setPoint(lat, lng, extra = {}) {
  const keepName = Boolean(extra.name)
  selected.value = {
    latitude: Number(Number(lat).toFixed(6)),
    longitude: Number(Number(lng).toFixed(6)),
    name: extra.name || '',
    address: extra.address || '',
    detail: extra.detail || extra.name || '',
    province: extra.province || '',
    city: extra.city || '',
    region: extra.region || extra.district || '',
  }
  const position = [selected.value.longitude, selected.value.latitude]
  if (!marker) {
    marker = new window.AMap.Marker({ position })
    map.add(marker)
  } else {
    marker.setPosition(position)
  }
  map.setZoomAndCenter(Math.max(map.getZoom(), 16), position)
  window.clearTimeout(lookupTimer)
  lookupTimer = window.setTimeout(() => resolveAddress(keepName), 220)
}

async function resolveAddress(keepName = false) {
  if (!selected.value) return
  const seq = ++lookupSeq
  resolving.value = true
  try {
    let data = await reverseGeocode(selected.value.longitude, selected.value.latitude)
    if (!data?.address) {
      try {
        data = await lookupMapLocation(selected.value.longitude, selected.value.latitude)
      } catch (e) {
        data = data || null
      }
    }
    if (seq !== lookupSeq) return
    const nearby = Array.isArray(data?.pois) ? data.pois : []
    if (nearby.length) pois.value = nearby
    selected.value = {
      ...selected.value,
      name: keepName && selected.value.name ? selected.value.name : (data?.name || selected.value.name),
      address: data?.address || selected.value.address,
      detail: keepName && selected.value.detail ? selected.value.detail : (data?.detail || selected.value.detail),
      province: data?.province || selected.value.province,
      city: data?.city || selected.value.city,
      region: data?.district || data?.region || selected.value.region,
    }
  } catch (e) {
    // request.js 已提示
  } finally {
    if (seq === lookupSeq) resolving.value = false
  }
}

function onSearchInput(event) {
  keyword.value = event?.detail?.value ?? event?.target?.value ?? keyword.value
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => searchPlaces(keyword.value), 280)
}

async function searchPlaces(text) {
  const query = String(text || keyword.value || '').trim()
  if (!query) return
  resolving.value = true
  try {
    let list = await searchPlacesByKeyword(query)
    if (!list.length) {
      try {
        list = await searchMapPlaces(query, selected.value?.longitude, selected.value?.latitude)
      } catch (e) {
        list = []
      }
    }
    pois.value = Array.isArray(list) ? list : []
    if (!pois.value.length) uni.showToast({ title: '没有搜到地点', icon: 'none' })
  } catch (e) {
    // request.js 已提示
  } finally {
    resolving.value = false
  }
}

function choosePoi(poi) {
  setPoint(poi.latitude, poi.longitude, {
    name: poi.name,
    address: poi.address,
    detail: poi.name,
    province: poi.province,
    city: poi.city,
    region: poi.district,
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
      uni.showToast({ title: '定位失败，请搜索或在地图上选点', icon: 'none' })
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
    await waitForElementSize(container)
    map = new AMap.Map(container, {
      zoom: 12,
      center: [120.3119, 31.4912],
      viewMode: '2D',
      resizeEnable: true,
      layers: [new AMap.TileLayer()],
    })
    map.on('click', (event) => setPoint(event.lnglat.getLat(), event.lnglat.getLng()))
    setTimeout(() => map && map.resize(), 200)
  } catch (e) {
    uni.showToast({ title: e?.message || '高德地图加载失败', icon: 'none' })
  }
})

onBeforeUnmount(() => {
  window.clearTimeout(lookupTimer)
  window.clearTimeout(searchTimer)
  if (map) map.destroy()
})
</script>

<template>
  <view class="amap-mask" @click.self="emit('close')">
    <view class="amap-dialog">
      <view class="amap-hd">
        <text class="amap-title">选择收货地址</text>
        <text class="amap-close" @click="emit('close')">关闭</text>
      </view>
      <input
        class="amap-search"
        :value="keyword"
        placeholder="搜索小区、写字楼或街道"
        confirm-type="search"
        @input="onSearchInput"
        @confirm="searchPlaces(keyword)"
      />
      <view ref="mapEl" class="amap-map" />
      <view class="amap-selected">
        <view class="amap-name">{{ selected?.name || (resolving ? '正在解析地址…' : '点击地图或搜索地点') }}</view>
        <view class="amap-addr">{{ selected?.address || (selected && resolving ? '正在获取详细地址…' : '') }}</view>
      </view>
      <scroll-view scroll-y class="amap-pois">
        <view
          v-for="poi in pois"
          :key="poi.name + String(poi.distance)"
          class="amap-poi"
          :class="{ active: poi.name === selected?.name }"
          @click="choosePoi(poi)"
        >
          <view class="amap-poi-name">{{ poi.name }}</view>
          <view class="amap-poi-addr">{{ poi.address }}<text v-if="poi.distance"> · {{ poi.distance }}m</text></view>
        </view>
      </scroll-view>
      <view class="amap-ft">
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
.amap-dialog { width: 100%; max-height: 92vh; background: #fff; border-radius: 24rpx 24rpx 0 0; overflow: hidden; }
.amap-hd { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 28rpx 12rpx; }
.amap-title { font-size: 30rpx; font-weight: 700; }
.amap-close { color: #2a78d6; font-size: 26rpx; }
.amap-search { margin: 0 24rpx 16rpx; height: 72rpx; padding: 0 20rpx; border: 2rpx solid #e6e4de; border-radius: 12rpx; font-size: 26rpx; }
.amap-map { width: 100%; height: 280px; background: #eef1ee; }
.amap-map :deep(.amap-container),
.amap-map :deep(.amap-layers) { width: 100% !important; height: 100% !important; }
.amap-selected { padding: 16rpx 28rpx; background: #fff8f6; }
.amap-name { font-size: 28rpx; font-weight: 700; }
.amap-addr { margin-top: 6rpx; color: #52514e; font-size: 24rpx; line-height: 1.5; }
.amap-pois { max-height: 240rpx; }
.amap-poi { padding: 18rpx 28rpx; border-top: 2rpx solid #f1efe8; }
.amap-poi.active { background: #fff4f0; }
.amap-poi-name { font-size: 26rpx; font-weight: 600; }
.amap-poi-addr { margin-top: 6rpx; color: #898781; font-size: 22rpx; }
.amap-ft { padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); }
.amap-actions { display: flex; gap: 16rpx; }
.amap-btn { flex: 1; height: 76rpx; margin: 0; border: 0; border-radius: 38rpx; background: #e34948; color: #fff; font-size: 26rpx; line-height: 76rpx; }
.amap-btn::after { border: 0; }
.amap-btn.ghost { background: #f3f1ec; color: #20262e; }
</style>
