<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { pageOrders, shipOrder } from '@/api/order'
import { listExpressCompanies } from '@/api/operationSettings'

/**
 * 订单列表。对应原型 docs/prototype/store/order-list.html。
 *
 * Tab 与查询条件的映射照后端真实的状态机字段拼（见 OrderServiceImpl 的 ship/confirmReceipt/cancel）：
 * - 待付款 = payStatus=unpaid
 * - 待发货 = payStatus=paid 且 deliveryStatus=pending
 * - 待收货 = deliveryStatus=shipped
 * - 已完成 = orderStatus=finished（confirmReceipt 时一并置位）
 * - 已取消 = orderStatus=cancelled（仅 unpaid 订单能被商户关单，见 cancel() 的乐观锁条件）
 */
const TABS = [
  { key: 'all', label: '全部', query: {} },
  { key: 'unpaid', label: '待付款', query: { payStatus: 'unpaid' } },
  { key: 'to-ship', label: '待发货', query: { payStatus: 'paid', deliveryStatus: 'pending' } },
  { key: 'shipped', label: '待收货', query: { deliveryStatus: 'shipped' } },
  { key: 'finished', label: '已完成', query: { orderStatus: 'finished' } },
  { key: 'cancelled', label: '已取消', query: { orderStatus: 'cancelled' } },
]

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const expressCompanies = ref([])
const activeTab = ref('all')
const query = reactive({ orderNo: '', pageNum: 1, pageSize: 20 })

async function load() {
  loading.value = true
  try {
    const tab = TABS.find((t) => t.key === activeTab.value)
    const page = await pageOrders({
      orderNo: query.orderNo || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      ...tab.query,
    })
    rows.value = page.records || []
    total.value = page.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    load(),
    listExpressCompanies(true).then((items) => { expressCompanies.value = items || [] }).catch(() => {}),
  ])
})

function onTabChange(key) {
  activeTab.value = key
  query.pageNum = 1
  selectedIds.value = []
  load()
}

function onSearch() {
  query.pageNum = 1
  load()
}

function onPageChange(p) {
  query.pageNum = p
  load()
}

const PAY_STATUS_META = {
  unpaid: { text: '待付款', cls: 'tag-warning' },
  paid: { text: '已支付', cls: 'tag-good' },
  refunding: { text: '退款中', cls: 'tag-warning' },
  refunded: { text: '已退款', cls: 'tag-muted' },
}

function statusOf(row) {
  if (row.orderStatus === 'cancelled') return { text: '已取消', cls: 'tag-muted' }
  if (row.orderStatus === 'finished') return { text: '已完成', cls: 'tag-good' }
  if (row.payStatus === 'unpaid') return { text: '待付款', cls: 'tag-warning' }
  if (row.deliveryStatus === 'pending') return { text: '待发货', cls: 'tag-warning' }
  if (row.deliveryStatus === 'shipped') return { text: '待收货', cls: 'tag-warning' }
  return PAY_STATUS_META[row.payStatus] || { text: row.payStatus, cls: 'tag-muted' }
}

function fmtPrice(v) {
  if (v === null || v === undefined) return '—'
  return `¥${Number(v).toFixed(2)}`
}

function fmtDateTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '—'
}

function canBatchShip(row) {
  return row.payStatus === 'paid'
    && row.deliveryStatus === 'pending'
    && row.deliveryType !== 'pickup'
    && row.orderStatus !== 'cancelled'
}

const selectedIds = ref([])
const batchOpen = ref(false)
const batching = ref(false)
const batchForm = reactive({ expressCompany: '', expressNo: '' })

function selectableRows() {
  return rows.value.filter(canBatchShip)
}

function toggleAll(ev) {
  selectedIds.value = ev.target.checked ? selectableRows().map((r) => String(r.id)) : []
}

function openBatch() {
  if (!selectedIds.value.length) {
    message.warning('请先勾选待发货订单')
    return
  }
  batchForm.expressCompany = ''
  batchForm.expressNo = ''
  batchOpen.value = true
}

async function onBatchShip() {
  if (!batchForm.expressCompany.trim() || !batchForm.expressNo.trim()) {
    message.warning('请填写快递公司和单号')
    return
  }
  batching.value = true
  let ok = 0
  try {
    for (const id of selectedIds.value) {
      await shipOrder(id, {
        expressCompany: batchForm.expressCompany.trim(),
        expressNo: batchForm.expressNo.trim(),
        orderGoodsIds: null,
      })
      ok += 1
    }
    message.success(`已发货 ${ok} 笔`)
    batchOpen.value = false
    selectedIds.value = []
    await load()
  } catch (e) {
    if (ok) {
      message.warning(`已发货 ${ok} 笔，后续失败已停止`)
      await load()
    }
  } finally {
    batching.value = false
  }
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">订单</div>
      <div class="page-desc">共 {{ total }} 个订单</div>
    </div>
  </div>

  <div class="card" style="margin-bottom: 14px">
    <div class="tabs" style="padding: 0 16px">
      <div
        v-for="tab in TABS"
        :key="tab.key"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        @click="onTabChange(tab.key)"
      >
        {{ tab.label }}
      </div>
    </div>
  </div>

  <div class="card card-pad" style="margin-bottom: 14px">
    <div style="display: flex; gap: 12px; align-items: center">
      <input
        v-model="query.orderNo"
        class="form-input"
        style="width: 240px"
        placeholder="搜索订单号"
        @keyup.enter="onSearch"
      />
      <button class="btn btn-primary" @click="onSearch">查询</button>
      <button class="btn" :disabled="!selectedIds.length" @click="openBatch">批量发货</button>
    </div>
  </div>

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead>
          <tr>
            <th style="width: 36px">
              <input
                type="checkbox"
                :checked="selectableRows().length > 0 && selectedIds.length === selectableRows().length"
                :disabled="selectableRows().length === 0"
                @change="toggleAll"
              />
            </th>
            <th>订单号</th>
            <th class="num">实付金额</th>
            <th>状态</th>
            <th>下单时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>
              <input
                v-if="canBatchShip(row)"
                type="checkbox"
                :value="String(row.id)"
                v-model="selectedIds"
              />
            </td>
            <td>{{ row.orderNo }}</td>
            <td class="num">{{ fmtPrice(row.payPrice) }}</td>
            <td><span class="tag tag-dot" :class="statusOf(row).cls">{{ statusOf(row).text }}</span></td>
            <td>{{ fmtDateTime(row.createTime) }}</td>
            <td>
              <button class="btn btn-sm" @click="router.push({ name: 'order-detail', params: { id: row.id } })">查看</button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 40px 0">暂无订单</td>
          </tr>
        </tbody>
      </table>
    </a-spin>

    <div v-if="total > query.pageSize" style="padding: 14px; display: flex; justify-content: flex-end">
      <a-pagination
        :current="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        :show-size-changer="false"
        @change="onPageChange"
      />
    </div>
  </div>

  <a-modal
    v-model:open="batchOpen"
    title="批量发货"
    :confirm-loading="batching"
    ok-text="确认发货"
    cancel-text="取消"
    @ok="onBatchShip"
  >
    <div class="form-hint" style="margin-bottom: 12px">将对勾选的 {{ selectedIds.length }} 笔待发货订单整单发出（同一快递公司与单号）。部分发货请进入订单详情。</div>
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>快递公司</label>
      <select v-model="batchForm.expressCompany" class="form-select">
        <option value="">请选择快递公司</option>
        <option v-for="company in expressCompanies" :key="company.id" :value="company.name">{{ company.name }}</option>
      </select>
    </div>
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>快递单号</label>
      <input v-model="batchForm.expressNo" class="form-input" />
    </div>
  </a-modal>
</template>
