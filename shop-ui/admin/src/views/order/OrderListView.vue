<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { confirmShopOrderPaid, pageShopOrders } from '@/api/order'
import { issueInvoice, listInvoices, rejectInvoice } from '@/api/invoice'

const activeTab = ref('all')
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const summary = ref({
  total: 0,
  pending: 0,
  paid: 0,
  refunded: 0,
  newCount: 0,
  renewCount: 0,
  upgradeCount: 0,
  addonCount: 0,
})
const selectedRow = ref(null)
const invoices = ref([])
const invoiceLoading = ref(false)

const query = reactive({
  keyword: '',
  type: 'all',
  pageNum: 1,
  pageSize: 10,
})

const tabs = computed(() => [
  { key: 'all', label: '全部', count: summary.value.total },
  { key: 'new', label: '新开通', count: summary.value.newCount },
  { key: 'renew', label: '续费', count: summary.value.renewCount },
  { key: 'upgrade', label: '升级', count: summary.value.upgradeCount },
  { key: 'service', label: '增值服务', count: summary.value.addonCount },
  { key: 'refund', label: '退款', count: summary.value.refunded },
])

onMounted(() => {
  load()
  loadInvoices()
})

async function load() {
  loading.value = true
  try {
    const res = await pageShopOrders({
      keyword: query.keyword || undefined,
      type: query.type,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    rows.value = res?.records || []
    total.value = res?.total || 0
    summary.value = res?.summary || summary.value
    selectedRow.value = rows.value[0] || null
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  load()
}

function onTabChange(key) {
  activeTab.value = key
  query.type = key
  query.pageNum = 1
  load()
}

function onPageChange(page) {
  query.pageNum = page
  load()
}

function selectRow(row) {
  selectedRow.value = row
}

function typeText(row) {
  return {
    new: '新开通',
    renew: '续费',
    upgrade: '升级',
    addon: '增值服务',
  }[row.type] || row.type || '—'
}

function planText(row) {
  const name = row.packageName || '—'
  const duration = row.durationMonth ? `${row.durationMonth}个月` : '—'
  return row.type === 'upgrade' ? `${name} · ${duration}` : `${name} · ${duration}`
}

function payMethodText(row) {
  if (row.payStatus === 'refunded') return '原路退回'
  return {
    wechat: '微信支付',
    alipay: '支付宝',
    offline: '线下转账',
  }[row.payMethod] || row.payMethod || '—'
}

function statusText(row) {
  if (row.payStatus === 'pending') return row.payMethod === 'offline' ? '待审核' : '待确认到账'
  if (row.payStatus === 'paid') return '已完成'
  if (row.payStatus === 'refunded') return '已退款'
  if (row.payStatus === 'closed') return '已关闭'
  return row.payStatus || '—'
}

function statusClass(row) {
  if (row.payStatus === 'pending') return 'tag-warning'
  if (row.payStatus === 'paid') return 'tag-good'
  if (row.payStatus === 'refunded') return 'tag-critical'
  if (row.payStatus === 'closed') return 'tag-muted'
  return 'tag-muted'
}

function actionText(row) {
  if (row.payStatus === 'pending') return '确认到账'
  return '查看'
}

async function onAction(row) {
  if (row.payStatus !== 'pending') {
    selectRow(row)
    return
  }
  Modal.confirm({
    title: '确认线下已到账？',
    content: `${row.orderNo} · ${row.shopName} · ¥${Number(row.amount || 0).toFixed(2)}。确认后将延长该店套餐有效期。`,
    okText: '确认到账',
    cancelText: '取消',
    async onOk() {
      await confirmShopOrderPaid(row.id)
      message.success('已确认到账')
      await load()
      await loadInvoices()
    },
  })
}

function formatAmount(row) {
  const amount = Number(row.amount ?? 0)
  const negative = row.payStatus === 'refunded' || amount < 0
  const value = Math.abs(amount).toFixed(2)
  return `${negative ? '-' : ''}¥${value}`
}

function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadInvoices() {
  invoiceLoading.value = true
  try {
    invoices.value = (await listInvoices()) || []
  } catch (e) {
    invoices.value = []
  } finally {
    invoiceLoading.value = false
  }
}

function invoiceStatusText(status) {
  return { applying: '待开票', issued: '已开票', rejected: '已驳回' }[status] || status || '—'
}

function invoiceStatusClass(status) {
  if (status === 'issued') return 'tag-good'
  if (status === 'rejected') return 'tag-critical'
  return 'tag-warning'
}

async function onIssue(row) {
  Modal.confirm({
    title: '确认开票？',
    content: `${row.shopName} · ${row.title} · ¥${Number(row.amount || 0).toFixed(2)}`,
    okText: '开票',
    cancelText: '取消',
    async onOk() {
      await issueInvoice(row.id)
      message.success('已开票')
      await loadInvoices()
    },
  })
}

async function onReject(row) {
  Modal.confirm({
    title: '驳回该发票申请？',
    content: `${row.shopName} · ${row.title}`,
    okText: '驳回',
    cancelText: '取消',
    async onOk() {
      await rejectInvoice(row.id, { reason: '不符合开票要求' })
      message.success('已驳回')
      await loadInvoices()
    },
  })
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">订单管理</div>
      <div class="page-desc">平台侧商家订购单，支持新开通、续费、升级和退款记录查看</div>
    </div>
    <button class="btn">导出订单</button>
  </div>

  <div class="grid grid-4" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">订单总数</div><div class="kpi-value">{{ summary.total }}</div></div>
    <div class="card kpi"><div class="kpi-label">待处理</div><div class="kpi-value" style="color: var(--status-warning-text)">{{ summary.pending }}</div></div>
    <div class="card kpi"><div class="kpi-label">已完成</div><div class="kpi-value" style="color: var(--status-good-text)">{{ summary.paid }}</div></div>
    <div class="card kpi"><div class="kpi-label">已退款</div><div class="kpi-value" style="color: var(--status-critical)">{{ summary.refunded }}</div></div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="border-bottom: none; margin-bottom: 14px">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        @click="onTabChange(tab.key)"
      >
        {{ tab.label }} <span class="count">({{ tab.count }})</span>
      </div>
    </div>

    <div style="display: flex; gap: 12px; margin-bottom: 14px">
      <input
        v-model="query.keyword"
        class="form-input"
        style="width: 240px"
        placeholder="订单号 / 商家名称 / 域名前缀"
        @keyup.enter="onSearch"
      />
      <button class="btn btn-primary" @click="onSearch">查询</button>
    </div>

    <a-spin :spinning="loading">
      <table class="table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>商家</th>
            <th>类型</th>
            <th>套餐</th>
            <th class="num">金额</th>
            <th>支付方式</th>
            <th>时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!rows.length && !loading">
            <td colspan="9" style="text-align: center; color: var(--text-muted)">暂无订购单</td>
          </tr>
          <tr
            v-for="row in rows"
            :key="row.id"
            :class="{ active: selectedRow && selectedRow.id === row.id }"
            @click="selectRow(row)"
          >
            <td>{{ row.orderNo }}</td>
            <td>
              <div style="font-weight: 600">{{ row.shopName }}</div>
              <div style="font-size: 12px; color: var(--text-muted)">#{{ row.shopId }}</div>
            </td>
            <td>{{ typeText(row) }}</td>
            <td>{{ planText(row) }}</td>
            <td class="num">{{ formatAmount(row) }}</td>
            <td>{{ payMethodText(row) }}</td>
            <td>{{ fmtTime(row.createTime) }}</td>
            <td><span class="tag" :class="statusClass(row)">{{ statusText(row) }}</span></td>
            <td><button class="btn btn-sm" @click.stop="onAction(row)">{{ actionText(row) }}</button></td>
          </tr>
        </tbody>
      </table>
    </a-spin>

    <div style="padding-top: 14px; display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: var(--text-muted)">
        <span>共 {{ total }} 条</span>
      <a-pagination
        v-if="total > query.pageSize"
        :current="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        :show-size-changer="false"
        @change="onPageChange"
      />
    </div>
  </div>

  <div v-if="selectedRow" class="card card-pad" style="margin-top: 14px">
    <div class="page-header" style="margin-bottom: 12px">
      <div>
        <div class="card-title" style="margin: 0">订单详情预览</div>
        <div class="card-sub" style="margin-top: 4px">{{ selectedRow.orderNo }} · {{ selectedRow.shopName }}</div>
      </div>
      <span class="tag" :class="statusClass(selectedRow)">{{ statusText(selectedRow) }}</span>
    </div>
    <div v-if="selectedRow.payStatus === 'pending'" style="margin-bottom: 12px">
      <button class="btn btn-primary" @click="onAction(selectedRow)">确认到账并延长套餐</button>
    </div>
    <div class="detail-grid">
      <div class="kv"><span class="k">订单类型</span><span class="v">{{ typeText(selectedRow) }}</span></div>
      <div class="kv"><span class="k">套餐</span><span class="v">{{ planText(selectedRow) }}</span></div>
      <div class="kv"><span class="k">金额</span><span class="v">{{ formatAmount(selectedRow) }}</span></div>
      <div class="kv"><span class="k">支付方式</span><span class="v">{{ payMethodText(selectedRow) }}</span></div>
      <div class="kv"><span class="k">下单时间</span><span class="v">{{ fmtTime(selectedRow.createTime) }}</span></div>
      <div class="kv"><span class="k">支付时间</span><span class="v">{{ fmtTime(selectedRow.payTime) }}</span></div>
    </div>
  </div>

  <div class="card card-pad" style="margin-top: 14px">
    <div class="card-title">发票申请</div>
    <div class="form-hint" style="margin-bottom: 12px">商户对已支付订购单申请发票，平台开票或驳回。</div>
    <a-spin :spinning="invoiceLoading">
      <table class="table">
        <thead>
          <tr>
            <th>商家</th>
            <th>订购单</th>
            <th>抬头 / 税号</th>
            <th class="num">金额</th>
            <th>状态</th>
            <th>发票号</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!invoices.length && !invoiceLoading">
            <td colspan="7" style="text-align: center; color: var(--text-muted)">暂无发票申请</td>
          </tr>
          <tr v-for="row in invoices" :key="row.id">
            <td>
              <div style="font-weight: 600">{{ row.shopName }}</div>
              <div style="font-size: 12px; color: var(--text-muted)">#{{ row.shopId }}</div>
            </td>
            <td>{{ row.orderNo || row.shopOrderId }}</td>
            <td>
              <div>{{ row.title }}</div>
              <div style="font-size: 12px; color: var(--text-muted)">{{ row.taxNo }}</div>
            </td>
            <td class="num">¥{{ Number(row.amount || 0).toFixed(2) }}</td>
            <td><span class="tag" :class="invoiceStatusClass(row.status)">{{ invoiceStatusText(row.status) }}</span></td>
            <td>{{ row.invoiceNo || '—' }}</td>
            <td>
              <template v-if="row.status === 'applying'">
                <button class="btn btn-sm btn-primary" @click="onIssue(row)">开票</button>
                <button class="btn btn-sm" style="margin-left: 6px" @click="onReject(row)">驳回</button>
              </template>
              <span v-else class="form-hint" style="margin: 0">{{ row.rejectReason || '—' }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </a-spin>
  </div>
</template>

<style scoped>
tr.active {
  background: var(--primary-bg);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 24px;
}

.kv {
  display: flex;
  gap: 12px;
  font-size: 13px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--gridline);
}

.kv .k {
  width: 84px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.kv .v {
  font-weight: 500;
}
</style>
