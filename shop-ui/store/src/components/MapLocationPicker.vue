<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  latitude: { type: [Number, String], default: null },
  longitude: { type: [Number, String], default: null },
  title: { type: String, default: '选择地图位置' },
  description: { type: String, default: '点击地图放置定位点，保存后可直接发起导航' },
  provider: { type: String, default: 'amap' },
  apiKey: { type: String, default: '' },
})
const emit = defineEmits(['confirm', 'close'])

const mapEl = ref(null)
const locating = ref(false)
const selectedLatitude = ref(toCoordinate(props.latitude, -90, 90))
const selectedLongitude = ref(toCoordinate(props.longitude, -180, 180))
let map = null
let marker = null

function outsideChina(latitude, longitude) {
  return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271
}

function transformLatitude(x, y) {
  let result = -100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  result += (20 * Math.sin(6 * x * Math.PI) + 20 * Math.sin(2 * x * Math.PI)) * 2 / 3
  result += (20 * Math.sin(y * Math.PI) + 40 * Math.sin(y / 3 * Math.PI)) * 2 / 3
  result += (160 * Math.sin(y / 12 * Math.PI) + 320 * Math.sin(y * Math.PI / 30)) * 2 / 3
  return result
}

function transformLongitude(x, y) {
  let result = 300 + x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  result += (20 * Math.sin(6 * x * Math.PI) + 20 * Math.sin(2 * x * Math.PI)) * 2 / 3
  result += (20 * Math.sin(x * Math.PI) + 40 * Math.sin(x / 3 * Math.PI)) * 2 / 3
  result += (150 * Math.sin(x / 12 * Math.PI) + 300 * Math.sin(x / 30 * Math.PI)) * 2 / 3
  return result
}

function wgs84ToGcj02(latitude, longitude) {
  if (outsideChina(latitude, longitude)) return [latitude, longitude]
  const radLat = latitude / 180 * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - 0.006693421622965943 * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  const deltaLat = transformLatitude(longitude - 105, latitude - 35) * 180
    / ((6378245 * (1 - 0.006693421622965943)) / (magic * sqrtMagic) * Math.PI)
  const deltaLng = transformLongitude(longitude - 105, latitude - 35) * 180
    / (6378245 / sqrtMagic * Math.cos(radLat) * Math.PI)
  return [latitude + deltaLat, longitude + deltaLng]
}

function gcj02ToWgs84(latitude, longitude) {
  if (outsideChina(latitude, longitude)) return [latitude, longitude]
  const converted = wgs84ToGcj02(latitude, longitude)
  return [latitude * 2 - converted[0], longitude * 2 - converted[1]]
}

function toCoordinate(value, min, max) {
  if (value == null || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= min && parsed <= max ? parsed : null
}

function setMapPoint(latitude, longitude, zoom = 16) {
  const [navigationLatitude, navigationLongitude] = wgs84ToGcj02(latitude, longitude)
  selectedLatitude.value = Number(navigationLatitude.toFixed(6))
  selectedLongitude.value = Number(navigationLongitude.toFixed(6))
  if (!marker) {
    marker = L.marker([latitude, longitude], {
      icon: L.divIcon({ className: 'map-pin-marker', html: '<span></span>', iconSize: [24, 34], iconAnchor: [12, 34] }),
    }).addTo(map)
  } else {
    marker.setLatLng([latitude, longitude])
  }
  map.setView([latitude, longitude], zoom)
}

function locateCurrent() {
  if (!navigator.geolocation) {
    message.warning('当前浏览器不支持定位，请直接点击地图选点')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (position) => {
      locating.value = false
      setMapPoint(position.coords.latitude, position.coords.longitude)
    },
    () => {
      locating.value = false
      message.warning('无法获取当前位置，请检查浏览器定位权限或直接点击地图选点')
    },
    { enableHighAccuracy: true, timeout: 10000 },
  )
}

function confirm() {
  if (selectedLatitude.value == null || selectedLongitude.value == null) {
    message.warning('请先在地图上选择位置')
    return
  }
  emit('confirm', { latitude: selectedLatitude.value, longitude: selectedLongitude.value })
}

onMounted(async () => {
  const hasPoint = selectedLatitude.value != null && selectedLongitude.value != null
  const center = hasPoint
    ? gcj02ToWgs84(selectedLatitude.value, selectedLongitude.value)
    : [35.8617, 104.1954]
  map = L.map(mapEl.value, { zoomControl: true }).setView(center, hasPoint ? 16 : 4)
  // 高德底图采用 GCJ-02，选点后转换为 GCJ-02 坐标保存，供微信地图直接导航。
  const tileKey = props.apiKey ? `&key=${encodeURIComponent(props.apiKey)}` : ''
  const tileUrl = props.provider === 'baidu'
    ? 'https://maponline0{s}.bdimg.com/tile/?qt=tile&x={x}&y={y}&z={z}&styles=pl'
    : `https://webrd0{s}.is.autonavi.com/appmaptile?style=7&x={x}&y={y}&z={z}${tileKey}`
  L.tileLayer(tileUrl, {
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 19,
    attribution: '&copy; 高德地图',
  }).addTo(map)
  map.on('click', (event) => setMapPoint(event.latlng.lat, event.latlng.lng, Math.max(map.getZoom(), 15)))
  if (hasPoint) setMapPoint(center[0], center[1])
  await nextTick()
  map.invalidateSize()
})

onBeforeUnmount(() => {
  if (map) map.remove()
})
</script>

<template>
  <Teleport to="body">
    <div class="map-picker-mask" @click.self="emit('close')">
      <div class="map-picker-dialog">
        <div class="map-picker-header">
          <div><div class="map-picker-title">{{ title }}</div><div class="map-picker-sub">{{ description }}</div></div>
          <button class="map-picker-close" type="button" title="关闭" @click="emit('close')">×</button>
        </div>
        <div ref="mapEl" class="location-map" />
        <div class="map-picker-footer">
          <div class="coordinate-text">
            {{ selectedLatitude == null ? '尚未选点' : `纬度 ${selectedLatitude} · 经度 ${selectedLongitude}` }}
          </div>
          <div class="map-picker-actions">
            <button class="btn" type="button" :disabled="locating" @click="locateCurrent">{{ locating ? '定位中…' : '定位到当前' }}</button>
            <button class="btn" type="button" @click="emit('close')">取消</button>
            <button class="btn btn-primary" type="button" @click="confirm">确认位置</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.map-picker-mask { position: fixed; inset: 0; z-index: 2100; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(20, 24, 28, .55); }
.map-picker-dialog { width: min(820px, 96vw); overflow: hidden; border-radius: 8px; background: #fff; box-shadow: 0 18px 60px rgba(0, 0, 0, .22); }
.map-picker-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid var(--border); }
.map-picker-title { color: var(--text); font-size: 18px; font-weight: 650; }
.map-picker-sub { margin-top: 4px; color: var(--text-muted); font-size: 12px; }
.map-picker-close { width: 34px; height: 34px; border: 0; background: transparent; color: var(--text-muted); font-size: 25px; cursor: pointer; }
.location-map { width: 100%; height: min(56vh, 500px); min-height: 340px; background: #eef1ee; }
.map-picker-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 20px; border-top: 1px solid var(--border); }
.coordinate-text { min-width: 0; color: var(--text-muted); font-size: 13px; }
.map-picker-actions { display: flex; flex-shrink: 0; gap: 8px; }
:global(.map-pin-marker) { background: transparent; border: 0; }
:global(.map-pin-marker span) { display: block; width: 22px; height: 22px; border: 3px solid #fff; border-radius: 50% 50% 50% 0; background: #e34948; box-shadow: 0 2px 8px rgba(0, 0, 0, .3); transform: rotate(-45deg); }
@media (max-width: 640px) {
  .map-picker-mask { padding: 10px; }
  .map-picker-footer { align-items: stretch; flex-direction: column; }
  .map-picker-actions { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
</style>
