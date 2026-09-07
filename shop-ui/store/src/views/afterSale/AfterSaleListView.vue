<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { approveAfterSale, pageAfterSales, refundAfterSale, rejectAfterSale } from '@/api/afterSale'

/**
 * 售后管理。对应原型 docs/prototype/store/after-sale-list.html。
 *
 * 状态机见 StoreAfterSaleController 类注释：
 *   applying(审核中) -[同意/拒绝]-> approved/rejected
 *   approved -[仅退款可直接退款；退货退款需买家先寄回]-> return_shipped -[退款]-> refunding -> refunded
 */
const TABS = [
  { key: 'all', label: '全部', status: '' },
  { key: 'applying', label: '待处理', status: 'applying' },
  { key: 'return_shipped', label: '待收货', status: 'return_shipped' },
  { key: 'approved', label: '已同意', status: 'approved' },
  { key: 'rejected', label: '已拒绝', status: 'rejected' },
  { key: 'closed', label: '已关闭', status: 'closed' },
]

const TYPE_TEXT = { refund_only: '仅退款', return_refund: '退货退款' }
const STATUS_META = {
  applying: { text: '待处理', cls: 'tag-warning' },
  approved: { text: '已同意', cls: 'tag-good' },
  rejected: { text: '已拒绝', cls: 'tag-critical' },
  return_shipped: { text: '买家已退货', cls: 'tag-warning' },
  refunding: { text: '退款中', cls: 'tag-warning' },
  refunded: { text: '已退款', cls: 'tag-muted' },
  closed: { text: '已关闭', cls: 'tag-muted' },
}

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const activeTab = ref('applying')
const query = reactive({ pageNum: 1, pageSize: 20 })

async function load() {
  loading.value = true
  try {
    const tab = TABS.find((t) => t.key === activeTab.value)
    const page = await pageAfterSales({
      status: tab.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
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

onMounted(load)

function onTabChange(key) {
  activeTab.value = key
  query.pageNum = 1
  load()
}

function onPageChange(p) {
  query.pageNum = p
  load()
}

function statusOf(row) {
  return STATUS_META[row.status] || { text: row.status, cls: 'tag-muted' }
}

function fmtPrice(v) {
  if (v === null || v === undefined) return '—'
  return `¥${Number(v).toFixed(2)}`
}

function fmtDateTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '—'
}

function canReview(row) {
  return row.status === 'applying'
}

function canRefund(row) {
  // 仅退款：approved 之后可直接退款；退货退款：要等买家寄回（return_shipped）才能退款
  return (row.type === 'refund_only' && row.status === 'approved') || row.status === 'return_shipped'
}

// ---------- 审核弹窗 ----------
const modalOpen = ref(false)
const modalTarget = ref(null)
const auditRemark = ref('')
const submitting = ref(false)

function openReview(row) {
  modalTarget.value = row
  auditRemark.value = ''
  modalOpen.value = true
}

async function onApprove() {
  submitting.value = true
  try {
    await approveAfterSale(modalTarget.value.id, auditRemark.value.trim() || undefined)
    message.success('已同意')
    modalOpen.value = false
    await load()
  } catch (e) {
    // 已由拦截器提示
  } finally {
    submitting.value = false
  }
}

async function onReject() {
  if (!auditRemark.value.trim()) {
    message.error('拒绝时请填写拒绝原因，将展示给买家')
    return
  }
  submitting.value = true
  try {
    await rejectAfterSale(modalTarget.value.id, auditRemark.value.trim())
    message.success('已拒绝')
    modalOpen.value = false
    await load()
  } catch (e) {
    // 已由拦截器提示
  } finally {
    submitting.value = false
  }
}

async function onRefund(row) {
  try {
    await refundAfterSale(row.id)
    message.success('退款已发起')
    await load()
  } catch (e) {
    // 已由拦截器提示
  }
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">售后管理</div>
      <div class="page-desc">共 {{ total }} 条售后申请</div>
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

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead>
          <tr>
            <th>申请类型</th>
            <th>申请原因</th>
            <th class="num">退款金额</th>
            <th>状态</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>{{ TYPE_TEXT[row.type] || row.type }}</td>
            <td>{{ row.applyReason || '—' }}</td>
            <td class="num">{{ fmtPrice(row.refundAmount) }}</td>
            <td><span class="tag tag-dot" :class="statusOf(row).cls">{{ statusOf(row).text }}</span></td>
            <td>{{ fmtDateTime(row.createTime) }}</td>
            <td>
              <button v-if="canReview(row)" class="btn btn-sm" @click="openReview(row)">审核</button>
              <button v-if="canRefund(row)" class="btn btn-sm" @click="onRefund(row)">执行退款</button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 40px 0">暂无售后申请</td>
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

  <a-modal v-model:open="modalOpen" title="售后审核" :footer="null">
    <template v-if="modalTarget">
      <div class="kv-row"><div class="k">申请类型</div><div class="v">{{ TYPE_TEXT[modalTarget.type] || modalTarget.type }}</div></div>
      <div class="kv-row"><div class="k">退款金额</div><div class="v">{{ fmtPrice(modalTarget.refundAmount) }}</div></div>
      <div class="kv-row"><div class="k">申请原因</div><div class="v">{{ modalTarget.applyReason || '—' }}</div></div>
      <div class="kv-row"><div class="k">买家说明</div><div class="v">{{ modalTarget.applyDesc || '—' }}</div></div>

      <div class="form-item" style="margin-top: 16px">
        <label class="form-label">审核备注（选填，拒绝时必填，将展示给买家）</label>
        <textarea v-model="auditRemark" class="form-textarea" rows="2"></textarea>
      </div>

      <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px">
        <button class="btn" :disabled="submitting" @click="onReject">拒绝</button>
        <button class="btn btn-primary" :disabled="submitting" @click="onApprove">同意</button>
      </div>
    </template>
  </a-modal>
</template>

<style scoped>
.kv-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px dashed var(--gridline);
  font-size: 13px;
}
.kv-row:last-child {
  border-bottom: none;
}
.kv-row .k {
  width: 90px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.kv-row .v {
  font-weight: 500;
  flex: 1;
}
</style>
