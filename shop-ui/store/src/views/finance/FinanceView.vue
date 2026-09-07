<script setup>
import { ref } from 'vue'

const activeTab = ref('flows')

const flows = [
  { time: '2026-08-10 09:24', type: '交易收入', cls: 'tag-good', desc: '订单 20260810049231', amount: '+¥189.00', amountCls: 'income', balance: '¥42,860.00' },
  { time: '2026-08-09 21:03', type: '退款支出', cls: 'tag-critical', desc: '售后单 AS20260809998', amount: '-¥599.00', amountCls: 'expense', balance: '¥42,671.00' },
  { time: '2026-08-09 16:45', type: '佣金支出', cls: 'tag-serious', desc: '分销佣金结算 · 陈晓', amount: '-¥19.90', amountCls: 'expense', balance: '¥43,270.00' },
  { time: '2026-08-08 09:12', type: '提现', cls: 'tag-muted', desc: '分销商提现 · 孙悦', amount: '-¥86.00', amountCls: 'expense', balance: '¥43,289.90' },
]

const withdrawals = [
  { user: '陈晓（分销佣金）', amount: '¥1,860.00', method: '微信零钱', time: '2026-08-10 10:20', status: '待审核', cls: 'tag-warning' },
  { user: '刘思思（分销佣金）', amount: '¥420.00', method: '银行卡', time: '2026-08-09 16:45', status: '待审核', cls: 'tag-warning' },
]

const reconciles = [
  { date: '2026-08-09', system: '¥28,940.00', wechat: '¥28,940.00', diff: '¥0.00', status: '一致', cls: 'tag-good' },
  { date: '2026-08-08', system: '¥31,200.00', wechat: '¥31,180.00', diff: '-¥20.00', status: '存在差异', cls: 'tag-serious' },
]
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">财务管理</div>
      <div class="page-desc">资金流水、提现审核与经营报表</div>
    </div>
    <button class="btn">导出报表</button>
  </div>

  <div class="grid grid-4" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">账户余额</div><div class="kpi-value">¥42,860.00</div></div>
    <div class="card kpi"><div class="kpi-label">本月交易收入</div><div class="kpi-value">¥286,420.00</div></div>
    <div class="card kpi"><div class="kpi-label">本月退款支出</div><div class="kpi-value" style="color: var(--status-critical)">¥8,940.00</div></div>
    <div class="card kpi"><div class="kpi-label">待处理提现</div><div class="kpi-value">3 笔</div></div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <div class="tab" :class="{ active: activeTab === 'flows' }" @click="activeTab = 'flows'">资金流水</div>
      <div class="tab" :class="{ active: activeTab === 'withdrawals' }" @click="activeTab = 'withdrawals'">提现管理</div>
      <div class="tab" :class="{ active: activeTab === 'reconcile' }" @click="activeTab = 'reconcile'">交易对账</div>
    </div>

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
          <tr v-for="item in withdrawals" :key="item.user">
            <td>{{ item.user }}</td>
            <td class="num">{{ item.amount }}</td>
            <td>{{ item.method }}</td>
            <td>{{ item.time }}</td>
            <td><span class="tag" :class="item.cls">{{ item.status }}</span></td>
            <td><button class="btn btn-sm btn-primary">通过</button> <button class="btn btn-sm">拒绝</button></td>
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
  </div>
</template>

<style scoped>
.income { color: var(--status-good-text); }
.expense { color: var(--status-critical); }
</style>
