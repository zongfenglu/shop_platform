<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  createOfflineStore,
  listOfflineStores,
  listVerifyLogs,
  updateOfflineStore,
  updateOfflineStoreStatus,
  verifyPickupCode,
} from '@/api/offlineStore'

const loading = ref(false)
const activeTab = ref('stores')
const stores = ref([])
const logs = ref([])
const modalOpen = ref(false)
const editing = ref(null)
const saving = ref(false)
const verifyCode = ref('')
const verifying = ref(false)

const form = reactive({ name: '', phone: '', region: '', detail: '', businessHours: '' })

const storeNameById = computed(() => new Map(stores.value.map((s) => [s.id, s.name])))

async function load() {
  loading.value = true
  try {
    const [storeRows, logRows] = await Promise.all([listOfflineStores(), listVerifyLogs()])
    stores.value = storeRows || []
    logs.value = logRows || []
  } catch (e) {
    stores.value = []
    logs.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function resetForm() {
  Object.assign(form, { name: '', phone: '', region: '', detail: '', businessHours: '' })
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
    businessHours: store.businessHours || '',
  })
  modalOpen.value = true
}

function closeModal() {
  if (!saving.value) modalOpen.value = false
}

async function save() {
  if (!form.name.trim()) return message.error('请输入门店名称')
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
    <div class="modal">
      <div class="modal-header"><span>{{ editing ? '编辑门店' : '新建门店' }}</span><button class="modal-close" aria-label="关闭" @click="closeModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>门店名称</label><input v-model="form.name" class="form-input" /></div>
        <div class="form-item"><label class="form-label">联系电话</label><input v-model="form.phone" class="form-input" /></div>
        <div class="form-item"><label class="form-label">所在地区</label><input v-model="form.region" class="form-input" placeholder="省/市/区" /></div>
        <div class="form-item"><label class="form-label">详细地址</label><input v-model="form.detail" class="form-input" /></div>
        <div class="form-item"><label class="form-label">营业时间</label><input v-model="form.businessHours" class="form-input" placeholder="例如 10:00 - 22:00" /></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeModal">取消</button><button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button></div>
    </div>
  </div>
</template>

<style scoped>
.tab { border: 0; background: transparent; cursor: pointer; }
.verify-box { display: flex; gap: 10px; margin-bottom: 16px; max-width: 480px; }
.verify-box .form-input { flex: 1; }
</style>
