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
const flows = computed(() => orders.value.filter((o) => o.payStatus === 'paid').map((o) => ({ time: formatDate(o.payTime || o.createTime), type: '交易收入', cls: 'tag-good', desc: `订单 ${o.orderNo}`, amount: `+¥${Number(o.payPrice || 0).toFixed(2)}`, amountCls: 'income', balance: '—' })))
const monthIncome = computed(() => orders.value.filter((o) => o.payStatus === 'paid').reduce((sum, o) => sum + Number(o.payPrice || 0), 0))
const refundTotal = computed(() => afterSales.value.filter((a) => ['refunded', 'approved'].includes(a.status)).reduce((sum, a) => sum + Number(a.refundAmount || a.amount || 0), 0))
const reconciles = []
function formatDate(v) { return v ? String(v).replace('T', ' ').slice(0, 19) : '—' }
async function load() {
  loading.value = true
  try {
    const [dash, orderPage, salePage, withdrawList] = await Promise.all([getStoreDashboard(), pageOrders({ pageNum: 1, pageSize: 100 }), pageAfterSales({ pageNum: 1, pageSize: 100 }), listDealerWithdraws('pending')])
    overview.value = dash
    orders.value = orderPage.records || []
    afterSales.value = salePage.records || []
    withdrawals.value = withdrawList || []
  } catch (e) { orders.value = []; afterSales.value = []; withdrawals.value = [] } finally { loading.value = false }
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
      <div style="display: flex; gap: 12px; margin-bottom: 14px">
        <select class="form-select" style="width: 160px">
          <option>全部类型</option>
          <option>交易收入</option>
          <option>退款支出</option>
          <option>佣金支出</option>
          <option>提现</option>
        </select>
        <input class="form-input" style="width: 200px" placeholder="时间范围" />
        <button class="btn">查询</button>
      </div>
      <table class="table">
        <thead><tr><th>时间</th><th>类型</th><th>说明</th><th class="num">金额</th><th class="num">余额</th></tr></thead>
        <tbody>
          <tr v-for="item in flows" :key="item.time + item.desc">
            <td>{{ item.time }}</td>
            <td><span class="tag" :class="item.cls">{{ item.type }}</span></td>
            <td>{{ item.desc }}</td>
            <td class="num" :class="item.amountCls">{{ item.amount }}</td>
            <td class="num">{{ item.balance }}</td>
          </tr>
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
