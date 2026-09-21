<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { pcaTextArr } from 'element-china-area-data'
import MapLocationPicker from '@/components/MapLocationPicker.vue'
import {
  createOfflineStore,
  listOfflineStores,
  listVerifyLogs,
  updateOfflineStore,
  updateOfflineStoreStatus,
  verifyPickupCode,
} from '@/api/offlineStore'
import { getOperationSettings } from '@/api/operationSettings'

const loading = ref(false)
const activeTab = ref('stores')
const stores = ref([])
const logs = ref([])
const modalOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
const verifyCode = ref('')
const verifying = ref(false)
const locationPickerOpen = ref(false)
const mapSetting = ref({ mapProvider: 'amap', mapApiKey: '' })

const form = reactive({ name: '', phone: '', region: '', detail: '', longitude: '', latitude: '', businessHours: '' })
// 省市区级联的选中值（文本三段，如 ['四川省','成都市','武侯区']）。后端 region 是单个字符串，
// 存取时用 '/' 拼接/拆分——与库里已有的自由文本兼容：拆不回三段的老数据保持原文显示，重选后覆盖。
const regionParts = ref([])

function regionToParts(text) {
  const parts = String(text || '').split('/').map((s) => s.trim()).filter(Boolean)
  if (parts.length < 2 || parts.length > 3) return []
  // 只有能在区划数据里找到对应省份的才回显到级联；否则视为老的自由文本
  return pcaTextArr.some((p) => p.value === parts[0]) ? parts : []
}

const storeNameById = computed(() => new Map(stores.value.map((s) => [s.id, s.name])))

async function load() {
  loading.value = true
  try {
    const [storeRows, logRows] = await Promise.all([listOfflineStores(), listVerifyLogs()])
    stores.value = storeRows || []
    logs.value = logRows || []
    try { mapSetting.value = (await getOperationSettings()) || mapSetting.value } catch (e) { /* 地图配置缺失不影响门店列表 */ }
  } catch (e) {
    stores.value = []
    logs.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function resetForm() {
  Object.assign(form, { name: '', phone: '', region: '', detail: '', longitude: '', latitude: '', businessHours: '' })
  regionParts.value = []
}

function openCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(store) {
  editing.value = store
  Object.assign(form, {
    name: store.name || '',
    phone: store.phone || '',
    region: store.region || '',
    detail: store.detail || '',
    longitude: store.longitude ?? '',
    latitude: store.latitude ?? '',
    businessHours: store.businessHours || '',
  })
  regionParts.value = regionToParts(store.region)
  modalOpen.value = true
}

function onRegionChange(parts) {
  form.region = (parts || []).join('/')
}

function closeModal() {
  if (!saving.value) modalOpen.value = false
}

function onLocationConfirm(location) {
  form.latitude = location.latitude
  form.longitude = location.longitude
  locationPickerOpen.value = false
}

function openMap(store) {
  const latitude = Number(store.latitude)
  const longitude = Number(store.longitude)
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return message.warning('该门店尚未设置地图位置')
  const name = encodeURIComponent(store.name || '门店')
  window.open(`https://uri.amap.com/marker?position=${longitude},${latitude}&name=${name}&coordinate=gaode&callnative=1`, '_blank', 'noopener')
}

async function save() {
  if (!form.name.trim()) return message.error('请输入门店名称')
  const hasLongitude = form.longitude !== '' && form.longitude != null
  const hasLatitude = form.latitude !== '' && form.latitude != null
  if (hasLongitude !== hasLatitude) return message.error('门店经纬度必须同时设置')
  saving.value = true
  try {
    if (editing.value) {
      await updateOfflineStore(editing.value.id, form)
      message.success('门店信息已更新')
    } else {
      await createOfflineStore(form)
      message.success('门店已创建')
    }
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function toggleStatus(store) {
  const next = store.status === 'enabled' ? 'disabled' : 'enabled'
  Modal.confirm({
    title: next === 'disabled' ? '停用该门店？' : '启用该门店？',
    content: next === 'disabled' ? '停用后消费者下单时将无法再选择该门店自提。' : '启用后消费者下单时可以选择该门店自提。',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      await updateOfflineStoreStatus(store.id, next)
      message.success('门店状态已更新')
      await load()
    },
  })
}

async function submitVerify() {
  if (!verifyCode.value.trim()) return message.error('请输入核销码')
  verifying.value = true
  try {
    await verifyPickupCode(verifyCode.value.trim())
    message.success('核销成功')
    verifyCode.value = ''
    await load()
  } finally {
    verifying.value = false
  }
}

function dateText(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">门店管理</div>
      <div class="page-desc">共 {{ stores.length }} 家门店</div>
    </div>
    <button v-if="activeTab === 'stores'" class="btn btn-primary" @click="openCreate">＋ 新建门店</button>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <button class="tab" :class="{ active: activeTab === 'stores' }" @click="activeTab = 'stores'">门店列表 <span class="count">({{ stores.length }})</span></button>
      <button class="tab" :class="{ active: activeTab === 'records' }" @click="activeTab = 'records'">核销记录 <span class="count">({{ logs.length }})</span></button>
    </div>

    <a-spin :spinning="loading">
      <template v-if="activeTab === 'stores'">
        <table class="table">
          <thead>
            <tr>
              <th>门店</th>
              <th>地址</th>
              <th>联系电话</th>
              <th>营业时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in stores" :key="item.id">
              <td style="font-weight: 600">{{ item.name }}</td>
              <td>{{ [item.region, item.detail].filter(Boolean).join(' ') || '—' }}</td>
              <td>{{ item.phone || '—' }}</td>
              <td>{{ item.businessHours || '—' }}</td>
              <td><span class="tag" :class="item.status === 'enabled' ? 'tag-good' : 'tag-muted'">{{ item.status === 'enabled' ? '营业中' : '已停用' }}</span></td>
              <td>
                <button class="btn btn-sm" @click="openEdit(item)">编辑</button>
                <button v-if="item.latitude != null && item.longitude != null" class="btn btn-sm" @click="openMap(item)">查看地图</button>
                <button class="btn btn-sm" @click="toggleStatus(item)">{{ item.status === 'enabled' ? '停用' : '启用' }}</button>
              </td>
            </tr>
            <tr v-if="!loading && stores.length === 0"><td colspan="6"><div class="empty-state"><div class="icon">◎</div><div>暂无门店，点击右上角新建</div></div></td></tr>
          </tbody>
        </table>
      </template>

      <template v-else>
        <div class="verify-box">
          <input v-model="verifyCode" class="form-input" placeholder="输入消费者出示的自提核销码" @keyup.enter="submitVerify" />
          <button class="btn btn-primary" :disabled="verifying" @click="submitVerify">{{ verifying ? '核销中…' : '核销' }}</button>
        </div>
        <table class="table">
          <thead>
            <tr>
              <th>核销码</th>
              <th>关联订单</th>
              <th>核销门店</th>
              <th>核销员工</th>
              <th>核销时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in logs" :key="item.id">
              <td>{{ item.verifyCode }}</td>
              <td>{{ item.orderId }}</td>
              <td>{{ item.storeName || storeNameById.get(item.storeId) || '—' }}</td>
              <td>{{ item.clerkName || '—' }}</td>
              <td>{{ dateText(item.verifyTime) }}</td>
            </tr>
            <tr v-if="!loading && logs.length === 0"><td colspan="5"><div class="empty-state"><div class="icon">◎</div><div>暂无核销记录</div></div></td></tr>
          </tbody>
        </table>
      </template>
    </a-spin>
  </div>

  <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
    <div class="modal store-modal">
      <div class="modal-header"><span>{{ editing ? '编辑门店' : '新建门店' }}</span><button class="modal-close" aria-label="关闭" @click="closeModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>门店名称</label><input v-model="form.name" class="form-input" /></div>
        <div class="form-item"><label class="form-label">联系电话</label><input v-model="form.phone" class="form-input" /></div>
        <div class="form-item">
          <label class="form-label">所在地区</label>
          <a-cascader
            v-model:value="regionParts"
            :options="pcaTextArr"
            placeholder="请选择省 / 市 / 区"
            style="width: 100%"
            @change="onRegionChange"
          />
          <div v-if="form.region && !regionParts.length" class="form-hint">当前保存值：{{ form.region }}（老数据，重新选择后覆盖）</div>
        </div>
        <div class="form-item"><label class="form-label">详细地址</label><input v-model="form.detail" class="form-input" /></div>
        <div class="form-item">
          <label class="form-label">地图位置</label>
          <div class="location-field">
            <div class="location-value">
              <template v-if="form.latitude !== '' && form.longitude !== ''">纬度 {{ form.latitude }} · 经度 {{ form.longitude }}</template>
              <template v-else>尚未选择门店位置</template>
            </div>
            <button class="btn" type="button" @click="locationPickerOpen = true">地图选点</button>
          </div>
          <div class="form-hint">配置后，消费者可在小程序“附近门店”中打开地图导航。</div>
        </div>
        <div class="form-item"><label class="form-label">营业时间</label><input v-model="form.businessHours" class="form-input" placeholder="例如 10:00 - 22:00" /></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeModal">取消</button><button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button></div>
    </div>
  </div>

  <MapLocationPicker
    v-if="locationPickerOpen"
    :latitude="form.latitude"
    :longitude="form.longitude"
    :provider="mapSetting.mapProvider || 'amap'"
    :api-key="mapSetting.mapApiKey || ''"
    @confirm="onLocationConfirm"
    @close="locationPickerOpen = false"
  />
</template>

<style scoped>
.tab { border: 0; background: transparent; cursor: pointer; }
.verify-box { display: flex; gap: 10px; margin-bottom: 16px; max-width: 480px; }
.verify-box .form-input { flex: 1; }
.store-modal { width: min(680px, calc(100vw - 32px)); }
.location-field { display: flex; align-items: center; gap: 10px; }
.location-value { flex: 1; min-width: 0; padding: 10px 12px; border: 1px solid var(--border); border-radius: 6px; color: var(--text-muted); background: var(--surface-2); font-size: 13px; }
</style>
