<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  latitude: { type: [Number, String], default: null },
  longitude: { type: [Number, String], default: null },
})
const emit = defineEmits(['confirm', 'close'])

const mapEl = ref(null)
const locating = ref(false)
const selectedLatitude = ref(toCoordinate(props.latitude, -90, 90))
const selectedLongitude = ref(toCoordinate(props.longitude, -180, 180))
let map = null
let marker = null

function toCoordinate(value, min, max) {
  if (value == null || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= min && parsed <= max ? parsed : null
}

function setPoint(latitude, longitude, zoom = 16) {
  selectedLatitude.value = Number(latitude.toFixed(6))
  selectedLongitude.value = Number(longitude.toFixed(6))
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
      setPoint(position.coords.latitude, position.coords.longitude)
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
    message.warning('请先在地图上选择门店位置')
    return
  }
  emit('confirm', { latitude: selectedLatitude.value, longitude: selectedLongitude.value })
}

onMounted(async () => {
  const hasPoint = selectedLatitude.value != null && selectedLongitude.value != null
  const center = hasPoint ? [selectedLatitude.value, selectedLongitude.value] : [35.8617, 104.1954]
  map = L.map(mapEl.value, { zoomControl: true }).setView(center, hasPoint ? 16 : 4)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)
  map.on('click', (event) => setPoint(event.latlng.lat, event.latlng.lng, Math.max(map.getZoom(), 15)))
  if (hasPoint) setPoint(selectedLatitude.value, selectedLongitude.value)
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
          <div><div class="map-picker-title">选择门店位置</div><div class="map-picker-sub">点击地图放置定位点，小程序导航将使用此坐标</div></div>
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
