<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { lookupMapLocation } from '@/api/operationSettings'
import { loadAmapJs, PLATFORM_AMAP_JS_KEY, PLATFORM_AMAP_JS_SECRET, reverseGeocode, waitForElementSize } from '@/utils/amap'

const props = defineProps({
  latitude: { type: [Number, String], default: null },
  longitude: { type: [Number, String], default: null },
  title: { type: String, default: '选择地图位置' },
  description: { type: String, default: '搜索或点击地图选点，会解析出地点名称、完整地址和附近地点' },
  provider: { type: String, default: 'amap' },
  apiKey: { type: String, default: '' },
  securityJsCode: { type: String, default: '' },
})
const emit = defineEmits(['confirm', 'close'])

const mapEl = ref(null)
const searchEl = ref(null)
const locating = ref(false)
const resolving = ref(false)
const selected = ref(emptySelected())
const pois = ref([])
let map = null
let marker = null
let resizeObserver = null
let lookupTimer = 0
let lookupSeq = 0

function emptySelected() {
  return {
    latitude: toCoordinate(props.latitude, -90, 90),
    longitude: toCoordinate(props.longitude, -180, 180),
    name: '',
    address: '',
    detail: '',
    province: '',
    city: '',
    district: '',
  }
}

function toCoordinate(value, min, max) {
  if (value == null || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= min && parsed <= max ? parsed : null
}

function setMapPoint(latitude, longitude, extra = {}, zoom) {
  const keepName = Boolean(extra.name)
  selected.value = {
    latitude: Number(Number(latitude).toFixed(6)),
    longitude: Number(Number(longitude).toFixed(6)),
    name: extra.name || '',
    address: extra.address || '',
    detail: extra.detail || extra.name || '',
    province: extra.province || '',
    city: extra.city || '',
    district: extra.district || '',
  }
  const position = [selected.value.longitude, selected.value.latitude]
  if (!marker) {
    marker = new window.AMap.Marker({ position, draggable: true })
    marker.on('dragend', (event) => {
      const lnglat = event.lnglat
      setMapPoint(lnglat.getLat(), lnglat.getLng(), {}, map.getZoom())
    })
    map.add(marker)
  } else {
    marker.setPosition(position)
  }
  map.setZoomAndCenter(zoom || Math.max(map.getZoom(), 16), position)
  scheduleLookup(keepName)
}

function scheduleLookup(keepName = false) {
  window.clearTimeout(lookupTimer)
  lookupTimer = window.setTimeout(() => resolveAddress(keepName), 220)
}

async function resolveAddress(keepName = false) {
  const lat = selected.value.latitude
  const lng = selected.value.longitude
  if (lat == null || lng == null) return
  const seq = ++lookupSeq
  resolving.value = true
  try {
    let data = await reverseGeocode(lng, lat)
    if (!data?.address) {
      try {
        data = await lookupMapLocation(lng, lat)
      } catch (e) {
        data = data || null
      }
    }
    if (seq !== lookupSeq) return
    applyLookup(data || {}, keepName)
    if (!selected.value.address) message.warning('未能解析详细地址，可搜索地点或确认后手工补全')
  } catch (e) {
    if (seq === lookupSeq) message.warning('地址解析失败，可搜索地点或确认后手工补全')
  } finally {
    if (seq === lookupSeq) resolving.value = false
  }
}

function applyLookup(data, keepName = false) {
  const nearby = Array.isArray(data.pois) ? data.pois : []
  pois.value = nearby
  selected.value = {
    ...selected.value,
    name: keepName && selected.value.name ? selected.value.name : (data.name || selected.value.name || ''),
    address: data.address || selected.value.address || '',
    detail: keepName && selected.value.detail ? selected.value.detail : (data.detail || selected.value.detail || ''),
    province: data.province || selected.value.province || '',
    city: data.city || selected.value.city || '',
    district: data.district || data.region || selected.value.district || '',
  }
}

function choosePoi(poi) {
  setMapPoint(poi.latitude, poi.longitude, {
    name: poi.name,
    address: poi.address,
    detail: poi.name,
  }, 17)
}

function locateCurrent() {
  locating.value = true
  map.plugin('AMap.Geolocation', () => {
    const geo = new window.AMap.Geolocation({ enableHighAccuracy: true, timeout: 10000 })
    geo.getCurrentPosition((status, result) => {
      locating.value = false
      if (status === 'complete' && result?.position) {
        setMapPoint(result.position.lat, result.position.lng, {}, 17)
        return
      }
      message.warning('无法获取当前位置，请搜索或直接点击地图选点')
    })
  })
}

function confirm() {
  if (selected.value.latitude == null || selected.value.longitude == null) {
    message.warning('请先在地图上选择位置')
    return
  }
  emit('confirm', { ...selected.value })
}

onMounted(async () => {
  try {
    const AMap = await loadAmapJs(props.apiKey || PLATFORM_AMAP_JS_KEY, props.securityJsCode || PLATFORM_AMAP_JS_SECRET)
    await nextTick()
    await waitForElementSize(mapEl.value)
    const hasPoint = selected.value.latitude != null && selected.value.longitude != null
    map = new AMap.Map(mapEl.value, {
      zoom: hasPoint ? 16 : 12,
      center: hasPoint ? [selected.value.longitude, selected.value.latitude] : [120.3119, 31.4912],
      viewMode: '2D',
      resizeEnable: true,
      layers: [new AMap.TileLayer()],
    })
    map.on('click', (event) => setMapPoint(event.lnglat.getLat(), event.lnglat.getLng(), {}))
    map.on('complete', () => { if (map) map.resize() })
    AMap.plugin(['AMap.AutoComplete', 'AMap.Geolocation', 'AMap.Geocoder', 'AMap.PlaceSearch'], () => {
      if (!searchEl.value) return
      const auto = new AMap.AutoComplete({ input: searchEl.value, city: '全国' })
      auto.on('select', (event) => {
        const poi = event?.poi
        if (!poi?.location) return
        setMapPoint(poi.location.lat, poi.location.lng, {
          name: poi.name,
          address: [poi.district, poi.address, poi.name].filter(Boolean).join(''),
          detail: poi.address || poi.name,
        }, 17)
      })
    })
    if (hasPoint) setMapPoint(selected.value.latitude, selected.value.longitude, {}, 16)
    if (typeof ResizeObserver !== 'undefined' && mapEl.value) {
      resizeObserver = new ResizeObserver(() => { if (map) map.resize() })
      resizeObserver.observe(mapEl.value)
    }
    setTimeout(() => map && map.resize(), 80)
  } catch (error) {
    message.error(error?.message || '高德地图加载失败')
  }
})

onBeforeUnmount(() => {
  window.clearTimeout(lookupTimer)
  if (resizeObserver) resizeObserver.disconnect()
  if (map) map.destroy()
})
</script>

<template>
  <Teleport to="body">
    <div class="map-picker-mask" @click.self="emit('close')">
      <div class="map-picker-dialog">
        <div class="map-picker-header">
          <div>
            <div class="map-picker-title">{{ title }}</div>
            <div class="map-picker-sub">{{ description }}</div>
          </div>
          <button class="map-picker-close" type="button" title="关闭" @click="emit('close')">×</button>
        </div>
        <div class="map-search">
          <input ref="searchEl" class="map-search-input" placeholder="搜索小区、写字楼、道路，例如：无锡观湖花园" />
        </div>
        <div class="map-body">
          <div ref="mapEl" class="location-map" />
          <div class="poi-panel">
            <div class="poi-title">附近地点</div>
            <div v-if="resolving" class="poi-empty">正在解析地址…</div>
            <button
              v-for="poi in pois"
              :key="poi.name + String(poi.distance)"
              class="poi-item"
              :class="{ active: poi.name === selected.name }"
              type="button"
              @click="choosePoi(poi)"
            >
              <div class="poi-name">{{ poi.name }}</div>
              <div class="poi-addr">{{ poi.address }}<template v-if="poi.distance"> · {{ poi.distance }}m</template></div>
            </button>
            <div v-if="!resolving && !pois.length" class="poi-empty">点击地图或搜索后，这里会列出附近地点</div>
          </div>
        </div>
        <div class="selected-card">
          <div class="selected-name">{{ selected.name || (selected.latitude == null ? '尚未选点' : (resolving ? '正在解析地址…' : '已选位置')) }}</div>
          <div class="selected-addr">{{ selected.address || (selected.latitude == null ? '搜索或点击地图，选点后会显示完整地址' : (resolving ? '正在获取附近地点和详细地址…' : '未解析到地址，可搜索地点后再确认')) }}</div>
          <div v-if="selected.latitude != null" class="selected-coord">纬度 {{ selected.latitude }} · 经度 {{ selected.longitude }}</div>
        </div>
        <div class="map-picker-footer">
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
.map-picker-dialog { width: min(920px, 96vw); border-radius: 8px; background: #fff; box-shadow: 0 18px 60px rgba(0, 0, 0, .22); }
.map-picker-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid var(--border); }
.map-picker-title { color: var(--text); font-size: 18px; font-weight: 650; }
.map-picker-sub { margin-top: 4px; color: var(--text-muted); font-size: 12px; }
.map-picker-close { width: 34px; height: 34px; border: 0; background: transparent; color: var(--text-muted); font-size: 25px; cursor: pointer; }
.map-search { padding: 12px 20px 0; }
.map-search-input { box-sizing: border-box; width: 100%; height: 40px; padding: 0 12px; border: 1px solid var(--border); border-radius: 8px; }
.map-body { display: grid; grid-template-columns: minmax(0, 1.4fr) 280px; gap: 0; margin-top: 12px; border-top: 1px solid var(--border); }
.location-map { width: 100%; height: 360px; background: #eef1ee; }
.location-map :deep(.amap-container),
.location-map :deep(.amap-layers),
.location-map :deep(.amap-e) { width: 100% !important; height: 100% !important; }
.location-map :deep(canvas),
.location-map :deep(img) { max-width: none !important; }
.poi-panel { height: 360px; overflow: auto; border-left: 1px solid var(--border); background: #fafafa; }
.poi-title { padding: 12px 14px 6px; color: var(--text-muted); font-size: 12px; }
.poi-item { display: block; width: 100%; padding: 10px 14px; border: 0; border-bottom: 1px solid var(--border); background: transparent; text-align: left; cursor: pointer; }
.poi-item:hover, .poi-item.active { background: #fff; }
.poi-name { color: var(--text); font-size: 14px; font-weight: 600; }
.poi-addr { margin-top: 4px; color: var(--text-muted); font-size: 12px; line-height: 1.4; }
.poi-empty { padding: 18px 14px; color: var(--text-muted); font-size: 12px; }
.selected-card { padding: 12px 20px; border-top: 1px solid var(--border); background: #fff8f6; }
.selected-name { color: var(--text); font-size: 15px; font-weight: 650; }
.selected-addr { margin-top: 4px; color: var(--text-muted); font-size: 13px; line-height: 1.5; }
.selected-coord { margin-top: 4px; color: #9aa0a6; font-size: 12px; }
.map-picker-footer { display: flex; justify-content: flex-end; padding: 12px 20px 16px; }
.map-picker-actions { display: flex; gap: 8px; }
@media (max-width: 760px) {
  .map-picker-mask { padding: 8px; }
  .map-body { grid-template-columns: 1fr; }
  .location-map, .poi-panel { height: 240px; }
  .poi-panel { border-left: 0; border-top: 1px solid var(--border); }
}
</style>
