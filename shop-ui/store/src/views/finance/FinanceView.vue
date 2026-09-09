<script setup>
import { computed, onMounted, ref } from 'vue'
import { getStoreDashboard } from '@/api/dashboard'
import { pageOrders } from '@/api/order'
import { pageAfterSales } from '@/api/afterSale'
import { listDealerWithdraws, approveDealerWithdraw, rejectDealerWithdraw } from '@/api/dealer'
import { message } from 'ant-design-vue'

const activeTab = ref('flows')

const loading = ref(false)
const orders = ref([])
const afterSales = ref([])
const withdrawals = ref([])
const overview = ref(null)

// 流水筛选。流水本身是订单/售后/提现三个来源在前端合成的（没有独立的资金流水表），
// 所以筛选也在前端做；数据源各拉 500 条按时间倒序，覆盖常规店铺数月的量。
const flowType = ref('')
const flowStart = ref('')
const flowEnd = ref('')

function formatDate(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '—' }

const allFlows = computed(() => {
  const rows = []
  for (const o of orders.value) {
    if (o.payStatus !== 'paid') continue
    rows.push({ time: o.payTime || o.createTime, type: '交易收入', cls: 'tag-good', desc: `订单 ${o.orderNo}`, amount: Number(o.payPrice || 0), sign: 1 })
  }
  for (const a of afterSales.value) {
    if (a.status !== 'refunded') continue
    rows.push({ time: a.updateTime || a.createTime, type: '退款支出', cls: 'tag-critical', desc: `售后单 ${a.id}`, amount: Number(a.refundAmount || 0), sign: -1 })
  }
  for (const w of allWithdraws.value) {
    if (w.status !== 'paid' && w.status !== 'approved') continue
    rows.push({ time: w.updateTime || w.createTime, type: '提现', cls: 'tag-warning', desc: `分销提现 ${w.id}`, amount: Number(w.amount || 0), sign: -1 })
  }
  return rows.sort((x, y) => String(y.time).localeCompare(String(x.time)))
})

const flows = computed(() => allFlows.value.filter((row) => {
  if (flowType.value && row.type !== flowType.value) return false
  const t = String(row.time || '')
  // date input 给的是 YYYY-MM-DD；流水时间是 YYYY-MM-DDTHH:mm:ss，按字符串前缀比较即可
  if (flowStart.value && t.slice(0, 10) < flowStart.value) return false
  if (flowEnd.value && t.slice(0, 10) > flowEnd.value) return false
  return true
}))

function resetFlowFilter() {
  flowType.value = ''
  flowStart.value = ''
  flowEnd.value = ''
}

const allWithdraws = ref([])
const monthIncome = computed(() => orders.value.filter((o) => o.payStatus === 'paid').reduce((sum, o) => sum + Number(o.payPrice || 0), 0))
const refundTotal = computed(() => afterSales.value.filter((a) => ['refunded', 'approved'].includes(a.status)).reduce((sum, a) => sum + Number(a.refundAmount || a.amount || 0), 0))
const reconciles = []

async function load() {
  loading.value = true
  try {
    const [dash, orderPage, salePage, pendingList, allList] = await Promise.all([
      getStoreDashboard(),
      pageOrders({ pageNum: 1, pageSize: 500 }),
      pageAfterSales({ pageNum: 1, pageSize: 500 }),
      listDealerWithdraws('pending'),
      listDealerWithdraws(),
    ])
    overview.value = dash
    orders.value = orderPage.records || []
    afterSales.value = salePage.records || []
    withdrawals.value = pendingList || []
    allWithdraws.value = allList || []
  } catch (e) { orders.value = []; afterSales.value = []; withdrawals.value = []; allWithdraws.value = [] } finally { loading.value = false }
}
onMounted(load)
async function audit(item, action) { try { if (action === 'approve') await approveDealerWithdraw(item.id); else await rejectDealerWithdraw(item.id); message.success('操作成功'); await load() } catch (e) {} }
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">财务管理</div>
      <div class="page-desc">资金流水、提现审核与经营报表</div>
    </div>
  </div>

  <div class="grid grid-4" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">账户余额</div><div class="kpi-value">暂无数据</div></div>
    <div class="card kpi"><div class="kpi-label">已支付订单收入</div><div class="kpi-value">¥{{ monthIncome.toFixed(2) }}</div></div>
    <div class="card kpi"><div class="kpi-label">已处理退款</div><div class="kpi-value" style="color: var(--status-critical)">¥{{ refundTotal.toFixed(2) }}</div></div>
    <div class="card kpi"><div class="kpi-label">待处理提现</div><div class="kpi-value">{{ withdrawals.length }} 笔</div></div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <div class="tab" :class="{ active: activeTab === 'flows' }" @click="activeTab = 'flows'">资金流水</div>
      <div class="tab" :class="{ active: activeTab === 'withdrawals' }" @click="activeTab = 'withdrawals'">提现管理</div>
      <div class="tab" :class="{ active: activeTab === 'reconcile' }" @click="activeTab = 'reconcile'">交易对账</div>
    </div>

    <a-spin :spinning="loading">
    <template v-if="activeTab === 'flows'">
      <div style="display: flex; gap: 12px; margin-bottom: 14px; align-items: center; flex-wrap: wrap">
        <select v-model="flowType" class="form-select" style="width: 160px">
          <option value="">全部类型</option>
          <option value="交易收入">交易收入</option>
          <option value="退款支出">退款支出</option>
          <option value="提现">提现</option>
        </select>
        <input v-model="flowStart" type="date" class="form-input" style="width: 160px" />
        <span style="color: var(--text-muted)">至</span>
        <input v-model="flowEnd" type="date" class="form-input" style="width: 160px" />
        <button class="btn" @click="resetFlowFilter">重置</button>
        <span style="font-size: 12px; color: var(--text-muted)">共 {{ flows.length }} 条</span>
      </div>
      <table class="table">
        <thead><tr><th>时间</th><th>类型</th><th>说明</th><th class="num">金额</th></tr></thead>
        <tbody>
          <tr v-for="item in flows" :key="item.type + item.time + item.desc">
            <td>{{ formatDate(item.time) }}</td>
            <td><span class="tag" :class="item.cls">{{ item.type }}</span></td>
            <td>{{ item.desc }}</td>
            <td class="num" :class="item.sign > 0 ? 'income' : 'expense'">{{ item.sign > 0 ? '+' : '-' }}¥{{ item.amount.toFixed(2) }}</td>
          </tr>
          <tr v-if="!loading && !flows.length"><td colspan="4"><div class="empty-state"><div class="icon">◎</div><div>{{ allFlows.length ? '筛选条件下没有流水' : '暂无资金流水' }}</div></div></td></tr>
        </tbody>
      </table>
    </template>

    <template v-else-if="activeTab === 'withdrawals'">
      <table class="table">
        <thead><tr><th>申请人</th><th class="num">金额</th><th>类型</th><th>申请时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in withdrawals" :key="item.id">
            <td>{{ item.userId || '—' }}</td>
            <td class="num">¥{{ Number(item.amount || 0).toFixed(2) }}</td>
            <td>{{ item.method || '—' }}</td>
            <td>{{ formatDate(item.createTime) }}</td>
            <td><span class="tag tag-warning">{{ item.status || 'pending' }}</span></td>
            <td><button class="btn btn-sm btn-primary" @click="audit(item, 'approve')">通过</button> <button class="btn btn-sm" @click="audit(item, 'reject')">拒绝</button></td>
          </tr>
        </tbody>
      </table>
    </template>

    <template v-else>
      <div class="form-hint" style="margin-bottom: 14px">与微信支付账单自动核对，标记差异订单。</div>
      <table class="table">
        <thead><tr><th>日期</th><th class="num">系统订单总额</th><th class="num">微信账单总额</th><th class="num">差异</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="item in reconciles" :key="item.date">
            <td>{{ item.date }}</td>
            <td class="num">{{ item.system }}</td>
            <td class="num">{{ item.wechat }}</td>
            <td class="num" :class="{ expense: item.diff.startsWith('-') }">{{ item.diff }}</td>
            <td><span class="tag" :class="item.cls">{{ item.status }}</span></td>
          </tr>
          <tr v-if="!reconciles.length"><td colspan="5"><div class="empty-state"><div class="icon">◎</div><div>对账功能依赖微信支付账单接口，暂未开通</div></div></td></tr>
        </tbody>
      </table>
    </template>
    </a-spin>
  </div>
</template>

<style scoped>
.income { color: var(--status-good-text); }
.expense { color: var(--status-critical); }
</style>
