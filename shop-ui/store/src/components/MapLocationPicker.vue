<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { loadAmapJs, PLATFORM_AMAP_JS_KEY } from '@/utils/amap'

const props = defineProps({
  latitude: { type: [Number, String], default: null },
  longitude: { type: [Number, String], default: null },
  title: { type: String, default: '选择地图位置' },
  description: { type: String, default: '搜索或点击地图放置定位点，保存后可直接发起导航' },
  provider: { type: String, default: 'amap' },
  apiKey: { type: String, default: '' },
})
const emit = defineEmits(['confirm', 'close'])

const mapEl = ref(null)
const searchEl = ref(null)
const locating = ref(false)
const selectedLatitude = ref(toCoordinate(props.latitude, -90, 90))
const selectedLongitude = ref(toCoordinate(props.longitude, -180, 180))
const selectedAddress = ref('')
let map = null
let marker = null
let geocoder = null

function toCoordinate(value, min, max) {
  if (value == null || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= min && parsed <= max ? parsed : null
}

function setMapPoint(latitude, longitude, zoom) {
  selectedLatitude.value = Number(Number(latitude).toFixed(6))
  selectedLongitude.value = Number(Number(longitude).toFixed(6))
  const position = [selectedLongitude.value, selectedLatitude.value]
  if (!marker) {
    marker = new window.AMap.Marker({ position, draggable: true })
    marker.on('dragend', (event) => {
      const lnglat = event.lnglat
      setMapPoint(lnglat.getLat(), lnglat.getLng())
    })
    map.add(marker)
  } else {
    marker.setPosition(position)
  }
  map.setZoomAndCenter(zoom || Math.max(map.getZoom(), 16), position)
  reverseGeocode(selectedLongitude.value, selectedLatitude.value)
}

function reverseGeocode(lng, lat) {
  if (!geocoder) return
  geocoder.getAddress([lng, lat], (status, result) => {
    if (status === 'complete' && result?.regeocode?.formattedAddress) {
      selectedAddress.value = result.regeocode.formattedAddress
    }
  })
}

function locateCurrent() {
  locating.value = true
  map.plugin('AMap.Geolocation', () => {
    const geo = new window.AMap.Geolocation({ enableHighAccuracy: true, timeout: 10000 })
    geo.getCurrentPosition((status, result) => {
      locating.value = false
      if (status === 'complete' && result?.position) {
        setMapPoint(result.position.lat, result.position.lng, 17)
        return
      }
      message.warning('无法获取当前位置，请检查定位权限或直接点击地图选点')
    })
  })
}

function confirm() {
  if (selectedLatitude.value == null || selectedLongitude.value == null) {
    message.warning('请先在地图上选择位置')
    return
  }
  emit('confirm', {
    latitude: selectedLatitude.value,
    longitude: selectedLongitude.value,
    address: selectedAddress.value,
  })
}

onMounted(async () => {
  try {
    const AMap = await loadAmapJs(props.apiKey || PLATFORM_AMAP_JS_KEY)
    await nextTick()
    const hasPoint = selectedLatitude.value != null && selectedLongitude.value != null
    map = new AMap.Map(mapEl.value, {
      zoom: hasPoint ? 16 : 5,
      center: hasPoint ? [selectedLongitude.value, selectedLatitude.value] : [104.1954, 35.8617],
    })
    map.on('click', (event) => setMapPoint(event.lnglat.getLat(), event.lnglat.getLng()))
    AMap.plugin(['AMap.Geocoder', 'AMap.AutoComplete', 'AMap.Geolocation'], () => {
      geocoder = new AMap.Geocoder({ radius: 1000, extensions: 'base' })
      if (hasPoint) setMapPoint(selectedLatitude.value, selectedLongitude.value, 16)
      if (searchEl.value) {
        const auto = new AMap.AutoComplete({ input: searchEl.value, city: '全国' })
        auto.on('select', (event) => {
          const poi = event?.poi
          if (!poi?.location) return
          setMapPoint(poi.location.lat, poi.location.lng, 17)
          selectedAddress.value = [poi.district, poi.address, poi.name].filter(Boolean).join('')
        })
      }
    })
    setTimeout(() => map && map.resize(), 200)
  } catch (error) {
    message.error(error?.message || '高德地图加载失败')
  }
})

onBeforeUnmount(() => {
  if (map) map.destroy()
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
        <div class="map-search">
          <input ref="searchEl" class="map-search-input" placeholder="搜索地点，例如：无锡观湖花园" />
        </div>
        <div ref="mapEl" class="location-map" />
        <div class="map-picker-footer">
          <div class="coordinate-text">
            <template v-if="selectedLatitude == null">尚未选点</template>
            <template v-else>
              <div>{{ selectedAddress || '已选点' }}</div>
              <div>纬度 {{ selectedLatitude }} · 经度 {{ selectedLongitude }}</div>
            </template>
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
.map-search { padding: 12px 20px 0; }
.map-search-input { box-sizing: border-box; width: 100%; height: 40px; padding: 0 12px; border: 1px solid var(--border); border-radius: 8px; }
.location-map { width: 100%; height: min(52vh, 460px); min-height: 320px; margin-top: 12px; background: #eef1ee; }
.map-picker-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 20px; border-top: 1px solid var(--border); }
.coordinate-text { min-width: 0; color: var(--text-muted); font-size: 13px; line-height: 1.5; }
.map-picker-actions { display: flex; flex-shrink: 0; gap: 8px; }
@media (max-width: 640px) {
  .map-picker-mask { padding: 10px; }
  .map-picker-footer { align-items: stretch; flex-direction: column; }
  .map-picker-actions { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
</style>
