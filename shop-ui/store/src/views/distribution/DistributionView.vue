<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getDealerSettings, saveDealerSettings,
  listDealerUsers, approveDealer, rejectDealer, disableDealer,
  listDealerOrders,
  listDealerWithdraws, approveDealerWithdraw, rejectDealerWithdraw,
} from '@/api/dealer'

const activeTab = ref('partners')

function asList(res) {
  return Array.isArray(res) ? res : []
}

// ---------- 设置 ----------
const settings = ref({ isEnable: 0, commissionRate: 10, commissionType: 'order', minWithdraw: 10, autoApprove: 0 })
const settingForm = reactive({ isEnable: 0, commissionRate: 10, commissionType: 'order', minWithdraw: 10, autoApprove: 0 })

async function loadSettings() {
  try {
    const d = await getDealerSettings()
    if (d) {
      settings.value = d
      Object.assign(settingForm, {
        isEnable: d.isEnable ?? 0, commissionRate: d.commissionRate ?? 10,
        commissionType: d.commissionType ?? 'order', minWithdraw: d.minWithdraw ?? 10,
        autoApprove: d.autoApprove ?? 0,
      })
    }
  } catch (e) { /* */ }
}

async function submitSettings() {
  await saveDealerSettings({
    isEnable: Number(settingForm.isEnable), commissionRate: Number(settingForm.commissionRate),
    commissionType: settingForm.commissionType, minWithdraw: Number(settingForm.minWithdraw),
    autoApprove: Number(settingForm.autoApprove),
  })
  alert('已保存')
  await loadSettings()
}

// ---------- 分销商 ----------
const dealerUsers = ref([])
const dealerKeyword = ref('')
const dealerStatus = ref('')

async function loadDealerUsers() {
  try {
    dealerUsers.value = asList(await listDealerUsers({ status: dealerStatus.value || undefined, keyword: dealerKeyword.value || undefined }))
  } catch (e) { /* */ }
}

async function onApprove(id) { await approveDealer(id); await loadDealerUsers() }
async function onReject(id) { await rejectDealer(id); await loadDealerUsers() }
async function onDisable(id) { if (confirm('确认禁用？')) { await disableDealer(id); await loadDealerUsers() } }

// ---------- 订单 ----------
const dealerOrders = ref([])
const orderStatus = ref('')

async function loadDealerOrders() {
  try {
    dealerOrders.value = asList(await listDealerOrders(orderStatus.value || undefined))
  } catch (e) { /* */ }
}

const statusLabel = (s) => ({ pending: '待结算', settled: '已结算', refunded: '已退款' }[s] || s)
const statusClass = (s) => ({ pending: 'tag-warning', settled: 'tag-good', refunded: 'tag-muted' }[s] || '')

const summary = computed(() => ({
  userCount: dealerUsers.value.length,
  orderCount: dealerOrders.value.length,
  pendingAmount: dealerOrders.value.filter(o => o.status === 'pending')
    .reduce((s, o) => s + Number(o.commissionAmount || 0), 0).toFixed(2),
}))

// ---------- 提现 ----------
const withdraws = ref([])
const withdrawStatus = ref('')

async function loadWithdraws() {
  try {
    withdraws.value = asList(await listDealerWithdraws(withdrawStatus.value || undefined))
  } catch (e) { /* */ }
}

async function onApproveWithdraw(id) {
  const remark = prompt('审批备注（可选）') || ''
  await approveDealerWithdraw(id, remark)
  await loadWithdraws()
}

async function onRejectWithdraw(id) {
  const remark = prompt('拒绝原因') || ''
  if (!remark) return
  await rejectDealerWithdraw(id, remark)
  await loadWithdraws()
}

const wStatusLabel = (s) => ({ applying: '待审核', approved: '已通过', rejected: '已拒绝', paid: '已打款' }[s] || s)
const wStatusClass = (s) => ({ applying: 'tag-warning', approved: 'tag-good', rejected: 'tag-critical', paid: 'tag-good' }[s] || '')

onMounted(() => { loadSettings(); loadDealerUsers(); loadDealerOrders(); loadWithdraws() })
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">分销中心</div>
      <div class="page-desc">分销商 {{ summary.userCount }} 人 · 分销订单 {{ summary.orderCount }} 笔 · 待结算佣金 ¥{{ summary.pendingAmount }}</div>
    </div>
    <button class="btn" @click="activeTab = 'settings'">分销设置</button>
  </div>

  <div class="grid grid-4" style="margin-bottom:16px">
    <div class="card kpi"><div class="kpi-label">分销商总数</div><div class="kpi-value">{{ summary.userCount }}</div></div>
    <div class="card kpi"><div class="kpi-label">分销订单</div><div class="kpi-value">{{ summary.orderCount }}</div></div>
    <div class="card kpi"><div class="kpi-label">待结算佣金</div><div class="kpi-value">¥{{ summary.pendingAmount }}</div></div>
  </div>

  <div class="tabs" style="margin-bottom:16px">
    <div class="tab" :class="{ active: activeTab === 'partners' }" @click="activeTab = 'partners'">分销商</div>
    <div class="tab" :class="{ active: activeTab === 'orders' }" @click="activeTab = 'orders'">分销订单</div>
    <div class="tab" :class="{ active: activeTab === 'withdrawals' }" @click="activeTab = 'withdrawals'">提现审核</div>
    <div class="tab" :class="{ active: activeTab === 'settings' }" @click="activeTab = 'settings'">分销设置</div>
  </div>

  <!-- 分销商 Tab -->
  <div v-if="activeTab === 'partners'" class="card card-pad">
    <div style="display:flex;gap:12px;margin-bottom:14px">
      <input v-model="dealerKeyword" class="form-input" style="width:220px" placeholder="真实姓名 / 手机号" />
      <select v-model="dealerStatus" class="form-select" style="width:140px">
        <option value="">全部状态</option><option value="applying">申请中</option><option value="active">已通过</option><option value="disabled">已禁用</option><option value="rejected">已拒绝</option>
      </select>
      <button class="btn" @click="loadDealerUsers">查询</button>
    </div>
    <table class="table">
      <thead><tr><th>用户ID</th><th>姓名</th><th>手机号</th><th>上级ID</th><th>累计佣金</th><th>可提现</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="u in dealerUsers" :key="u.id">
          <td>{{ u.userId }}</td>
          <td>{{ u.realName || '-' }}</td>
          <td>{{ u.mobile || '-' }}</td>
          <td>{{ u.parentId || '一级' }}</td>
          <td style="color:var(--price);font-weight:600">¥{{ Number(u.totalCommission || 0).toFixed(2) }}</td>
          <td>¥{{ Number(u.availableCommission || 0).toFixed(2) }}</td>
          <td><span class="tag" :class="u.status === 'active' ? 'tag-good' : u.status === 'applying' ? 'tag-warning' : u.status === 'disabled' ? 'tag-critical' : 'tag-muted'">{{ { applying: '申请中', active: '已通过', disabled: '已禁用', rejected: '已拒绝' }[u.status] || u.status }}</span></td>
          <td>
            <button v-if="u.status === 'applying'" class="btn btn-sm" @click="onApprove(u.id)">通过</button>
            <button v-if="u.status === 'applying'" class="btn btn-sm" @click="onReject(u.id)" style="margin-left:4px">拒绝</button>
            <button v-if="u.status === 'active'" class="btn btn-sm" @click="onDisable(u.id)">禁用</button>
          </td>
        </tr>
        <tr v-if="!dealerUsers.length"><td colspan="8" style="text-align:center;color:var(--text-muted)">暂无分销商</td></tr>
      </tbody>
    </table>
  </div>

  <!-- 分销订单 Tab -->
  <div v-if="activeTab === 'orders'" class="card card-pad">
    <div style="display:flex;gap:12px;margin-bottom:14px">
      <select v-model="orderStatus" class="form-select" style="width:140px">
        <option value="">全部</option><option value="pending">待结算</option><option value="settled">已结算</option><option value="refunded">已退款</option>
      </select>
      <button class="btn" @click="loadDealerOrders">查询</button>
    </div>
    <table class="table">
      <thead><tr><th>订单ID</th><th>分销商ID</th><th>订单金额</th><th>佣金比例</th><th>佣金</th><th>状态</th></tr></thead>
      <tbody>
        <tr v-for="o in dealerOrders" :key="o.id">
          <td>{{ o.orderId }}</td>
          <td>{{ o.dealerUserId }}</td>
          <td>¥{{ Number(o.orderTotal || 0).toFixed(2) }}</td>
          <td>{{ o.commissionRate }}%</td>
          <td style="color:var(--price);font-weight:600">¥{{ Number(o.commissionAmount || 0).toFixed(2) }}</td>
          <td><span class="tag" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span></td>
        </tr>
        <tr v-if="!dealerOrders.length"><td colspan="6" style="text-align:center;color:var(--text-muted)">暂无分销订单</td></tr>
      </tbody>
    </table>
  </div>

  <!-- 提现审核 Tab -->
  <div v-if="activeTab === 'withdrawals'" class="card card-pad">
    <div style="display:flex;gap:12px;margin-bottom:14px">
      <select v-model="withdrawStatus" class="form-select" style="width:140px">
        <option value="">全部</option><option value="applying">待审核</option><option value="approved">已通过</option><option value="rejected">已拒绝</option>
      </select>
      <button class="btn" @click="loadWithdraws">查询</button>
    </div>
    <table class="table">
      <thead><tr><th>分销商ID</th><th>用户ID</th><th>金额</th><th>方式</th><th>申请时间</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="w in withdraws" :key="w.id">
          <td>{{ w.dealerUserId }}</td>
          <td>{{ w.userId }}</td>
          <td style="color:var(--price);font-weight:600">¥{{ Number(w.amount || 0).toFixed(2) }}</td>
          <td>{{ w.method }}</td>
          <td>{{ w.applyTime }}</td>
          <td><span class="tag" :class="wStatusClass(w.status)">{{ wStatusLabel(w.status) }}</span></td>
          <td v-if="w.status === 'applying'">
            <button class="btn btn-sm" @click="onApproveWithdraw(w.id)">通过</button>
            <button class="btn btn-sm" @click="onRejectWithdraw(w.id)" style="margin-left:4px">拒绝</button>
          </td>
          <td v-else>{{ w.remark || '-' }}</td>
        </tr>
        <tr v-if="!withdraws.length"><td colspan="7" style="text-align:center;color:var(--text-muted)">暂无提现申请</td></tr>
      </tbody>
    </table>
  </div>

  <!-- 分销设置 Tab -->
  <div v-if="activeTab === 'settings'" class="card card-pad">
    <div class="form-item"><label class="form-label">分销开关</label><select v-model="settingForm.isEnable" class="form-select" style="width:200px"><option :value="1">开启</option><option :value="0">关闭</option></select></div>
    <div class="form-item" style="margin-top:12px"><label class="form-label">佣金比例（%）</label><input v-model.number="settingForm.commissionRate" type="number" min="0" max="100" step="0.01" class="form-input" style="width:200px" /></div>
    <div class="form-item" style="margin-top:12px">
      <label class="form-label">佣金类型</label>
      <select v-model="settingForm.commissionType" class="form-select" style="width:200px"><option value="order">整单佣金</option><option value="goods">按商品佣金</option></select>
      <div class="form-hint" style="max-width:520px">
        整单佣金：按订单实付金额（含运费）× 上方统一比例计佣。<br />
        按商品佣金：按每个商品行的实付金额（不含运费）× 该商品的「单品佣金比例」逐行计佣求和；未单独设置比例的商品使用上方统一比例，设为 0 的商品不参与分佣。单品比例在「商品 → 编辑商品 → 分销」里设置。
      </div>
    </div>
    <div class="form-item" style="margin-top:12px"><label class="form-label">最低提现金额</label><input v-model.number="settingForm.minWithdraw" type="number" min="0" step="0.01" class="form-input" style="width:200px" /></div>
    <div class="form-item" style="margin-top:12px"><label class="form-label">自动审核</label><select v-model="settingForm.autoApprove" class="form-select" style="width:200px"><option :value="0">人工审核</option><option :value="1">自动通过</option></select></div>
    <button class="btn btn-primary" style="margin-top:16px" @click="submitSettings">保存设置</button>
  </div>
</template>
