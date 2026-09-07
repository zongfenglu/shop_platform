<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  adjustMemberBalance,
  adjustMemberPoints,
  getMember,
  getMemberBalanceLogs,
  getMemberPointsLogs,
  pageMembers,
} from '@/api/member'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const drawerOpen = ref(false)
const detailLoading = ref(false)
const selected = ref(null)
const query = reactive({ keyword: '', status: '', pageNum: 1, pageSize: 20 })

const logTab = ref('balance')
const logs = ref([])
const logTotal = ref(0)
const logPageNum = ref(1)
const logPageSize = 10

const adjustOpen = ref(false)
const adjustType = ref('balance') // balance | points
const adjustForm = reactive({ amount: null, remark: '' })

async function load() {
  loading.value = true
  try {
    const page = await pageMembers({
      keyword: query.keyword || undefined,
      status: query.status === '' ? undefined : Number(query.status),
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

function onSearch() {
  query.pageNum = 1
  load()
}

function onPageChange(page) {
  query.pageNum = page
  load()
}

function displayName(row) {
  return row.nickname || `用户${String(row.mobile || '').slice(-4)}` || '未命名会员'
}

function maskedMobile(mobile) {
  if (!mobile) return '—'
  return mobile.length >= 7 ? `${mobile.slice(0, 3)}****${mobile.slice(-4)}` : mobile
}

function statusOf(row) {
  return row.status === 1 ? { text: '正常', cls: 'tag-good' } : { text: '已禁用', cls: 'tag-critical' }
}

function createdAt(row) {
  return String(row.createTime || '').replace('T', ' ').slice(0, 16) || '—'
}

function money(v) {
  return Number(v || 0).toFixed(2)
}

async function openDetail(row) {
  drawerOpen.value = true
  detailLoading.value = true
  selected.value = row
  logTab.value = 'balance'
  logs.value = []
  logTotal.value = 0
  logPageNum.value = 1
  try {
    selected.value = await getMember(row.id)
    await loadLogs()
  } catch (e) {
    message.error('会员详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function loadLogs() {
  if (!selected.value) return
  try {
    const params = { pageNum: logPageNum.value, pageSize: logPageSize }
    const page = logTab.value === 'balance'
      ? await getMemberBalanceLogs(selected.value.id, params)
      : await getMemberPointsLogs(selected.value.id, params)
    logs.value = page.records || []
    logTotal.value = page.total || 0
  } catch (e) {
    logs.value = []
    logTotal.value = 0
  }
}

async function onLogTabChange() {
  logPageNum.value = 1
  await loadLogs()
}

async function onLogPageChange(p) {
  logPageNum.value = p
  await loadLogs()
}

function openAdjust(type) {
  adjustType.value = type
  Object.assign(adjustForm, { amount: null, remark: '' })
  adjustOpen.value = true
}

function closeAdjust() {
  adjustOpen.value = false
}

async function submitAdjust() {
  const v = Number(adjustForm.amount)
  if (!v || v === 0) return message.error('请输入非零金额')
  try {
    if (adjustType.value === 'balance') {
      await adjustMemberBalance(selected.value.id, { userId: selected.value.id, amount: v, remark: adjustForm.remark })
    } else {
      await adjustMemberPoints(selected.value.id, { userId: selected.value.id, value: Math.round(v), remark: adjustForm.remark })
    }
    message.success('已调整')
    adjustOpen.value = false
    selected.value = await getMember(selected.value.id)
    await loadLogs()
  } catch (e) {
    // http 拦截器已统一提示
  }
}

function logSceneText(scene) {
  const map = { recharge: '充值', consume: '消费', refund: '退款', admin: '后台调整', commission: '佣金', sign: '签到', order: '下单' }
  return map[scene] || scene || '—'
}

function logMoney(item) {
  const v = logTab.value === 'balance' ? Number(item.money || 0) : Number(item.value || 0)
  const sign = v > 0 ? '+' : ''
  return logTab.value === 'balance' ? `${sign}${v.toFixed(2)}` : `${sign}${v}`
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">会员管理</div>
      <div class="page-desc">共 {{ total }} 位会员 · 含余额/积分/等级资产</div>
    </div>
  </div>

  <div class="card card-pad" style="margin-bottom: 14px">
    <div class="filter-row">
      <input v-model="query.keyword" class="form-input keyword" placeholder="昵称 / 手机号" @keyup.enter="onSearch" />
      <select v-model="query.status" class="form-select status-select">
        <option value="">全部状态</option>
        <option value="1">正常</option>
        <option value="0">已禁用</option>
      </select>
      <button class="btn btn-primary" @click="onSearch">查询</button>
    </div>
  </div>

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead><tr><th>会员</th><th>状态</th><th>手机号</th><th>余额</th><th>积分</th><th>注册时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td><div class="user-cell"><div class="user-avatar">{{ displayName(row).slice(0, 1) }}</div><div><div style="font-weight: 600">{{ displayName(row) }}</div><div class="muted">ID {{ row.id }}</div></div></div></td>
            <td><span class="tag" :class="statusOf(row).cls">{{ statusOf(row).text }}</span></td>
            <td>{{ maskedMobile(row.mobile) }}</td>
            <td class="num">¥{{ money(row.balance) }}</td>
            <td class="num">{{ row.points || 0 }}</td>
            <td>{{ createdAt(row) }}</td>
            <td><button class="btn btn-sm" @click="openDetail(row)">详情</button></td>
          </tr>
          <tr v-if="!loading && rows.length === 0"><td colspan="7"><div class="empty-state"><div class="icon">◎</div><div>暂无会员数据</div></div></td></tr>
        </tbody>
      </table>
    </a-spin>
    <div v-if="total > query.pageSize" class="pagination-row"><a-pagination :current="query.pageNum" :page-size="query.pageSize" :total="total" :show-size-changer="false" @change="onPageChange" /></div>
  </div>

  <div v-if="drawerOpen" class="drawer-mask" @click.self="drawerOpen = false">
    <aside class="drawer">
      <div class="drawer-header"><div><div class="card-title">会员详情</div><div class="muted">资产与明细</div></div><button class="modal-close" aria-label="关闭" @click="drawerOpen = false">×</button></div>
      <a-spin :spinning="detailLoading">
        <div v-if="selected" class="drawer-body">
          <div class="profile"><div class="user-avatar large">{{ displayName(selected).slice(0, 1) }}</div><div><div class="profile-name">{{ displayName(selected) }}</div><div class="muted">{{ maskedMobile(selected.mobile) }}</div></div></div>

          <div class="asset-grid">
            <div class="asset"><div class="k">余额</div><div class="v">¥{{ money(selected.balance) }}</div></div>
            <div class="asset"><div class="k">积分</div><div class="v">{{ selected.points || 0 }}</div></div>
            <div class="asset"><div class="k">成长值</div><div class="v">{{ selected.growthValue || 0 }}</div></div>
            <div class="asset"><div class="k">累计消费</div><div class="v">¥{{ money(selected.payMoney) }}</div></div>
          </div>

          <div class="kv-row"><div class="k">会员状态</div><div><span class="tag" :class="statusOf(selected).cls">{{ statusOf(selected).text }}</span></div></div>
          <div class="kv-row"><div class="k">会员 ID</div><div>{{ selected.id }}</div></div>
          <div class="kv-row"><div class="k">注册时间</div><div>{{ createdAt(selected) }}</div></div>

          <div class="adjust-row">
            <button class="btn btn-sm btn-primary" @click="openAdjust('balance')">调整余额</button>
            <button class="btn btn-sm" @click="openAdjust('points')">调整积分</button>
          </div>

          <div class="log-section">
            <div class="tabs">
              <button class="tab" :class="{ active: logTab === 'balance' }" @click="logTab = 'balance'; onLogTabChange()">余额明细</button>
              <button class="tab" :class="{ active: logTab === 'points' }" @click="logTab = 'points'; onLogTabChange()">积分明细</button>
            </div>
            <table class="table compact">
              <thead><tr><th>场景</th><th>变动</th><th>{{ logTab === 'balance' ? '变动后余额' : '变动后积分' }}</th><th>时间</th></tr></thead>
              <tbody>
                <tr v-for="item in logs" :key="item.id">
                  <td>{{ logSceneText(item.scene) }}</td>
                  <td class="num" :class="Number(item.money || item.value || 0) >= 0 ? 'pos' : 'neg'">{{ logMoney(item) }}</td>
                  <td class="num">{{ logTab === 'balance' ? money(item.after) : (item.after || 0) }}</td>
                  <td class="muted">{{ createdAt(item) }}</td>
                </tr>
                <tr v-if="logs.length === 0"><td colspan="4"><div class="empty-state small">暂无明细</div></td></tr>
              </tbody>
            </table>
            <div v-if="logTotal > logPageSize" class="pagination-row"><a-pagination :current="logPageNum" :page-size="logPageSize" :total="logTotal" :show-size-changer="false" size="small" @change="onLogPageChange" /></div>
          </div>
        </div>
      </a-spin>
    </aside>
  </div>

  <div v-if="adjustOpen" class="modal-mask" @click.self="closeAdjust">
    <div class="modal" style="width: 460px">
      <div class="modal-header"><span>{{ adjustType === 'balance' ? '调整余额' : '调整积分' }}</span><button class="modal-close" aria-label="关闭" @click="closeAdjust">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>{{ adjustType === 'balance' ? '金额（正数增加/负数扣减）' : '积分（正数增加/负数扣减）' }}</label><input v-model.number="adjustForm.amount" type="number" step="0.01" class="form-input" /></div>
        <div class="form-item"><label class="form-label">备注</label><input v-model="adjustForm.remark" class="form-input" /></div>
        <div class="form-hint">扣减后不可透支，失败会提示"余额不足/积分不足"。</div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeAdjust">取消</button><button class="btn btn-primary" @click="submitAdjust">确认调整</button></div>
    </div>
  </div>
</template>

<style scoped>
.filter-row { display: flex; gap: 12px; align-items: center; }
.keyword { width: 240px; }
.status-select { width: 140px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #ffb199, #e34948); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; flex-shrink: 0; }
.user-avatar.large { width: 44px; height: 44px; font-size: 16px; }
.muted { color: var(--text-muted); font-size: 12px; }
.num { font-variant-numeric: tabular-nums; }
.pos { color: var(--status-good, #16a34a); }
.neg { color: var(--status-critical, #dc2626); }
.pagination-row { display: flex; justify-content: flex-end; padding: 14px; }
.drawer-mask { position: fixed; inset: 0; z-index: 60; background: rgba(11, 11, 11, .35); display: flex; justify-content: flex-end; }
.drawer { width: 480px; max-width: 92vw; height: 100%; overflow-y: auto; background: var(--surface); box-shadow: var(--shadow-modal); }
.drawer-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 18px 22px; border-bottom: 1px solid var(--gridline); }
.drawer-body { padding: 20px 22px; }
.profile { display: flex; align-items: center; gap: 12px; padding-bottom: 18px; margin-bottom: 4px; border-bottom: 1px solid var(--gridline); }
.profile-name { font-weight: 700; font-size: 15px; margin-bottom: 3px; }
.asset-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 16px 0; }
.asset { background: var(--surface-alt, #f7f7f5); border-radius: 10px; padding: 12px; text-align: center; }
.asset .k { color: var(--text-muted); font-size: 12px; margin-bottom: 6px; }
.asset .v { font-weight: 700; font-size: 16px; font-variant-numeric: tabular-nums; }
.kv-row { display: flex; gap: 12px; padding: 10px 0; border-bottom: 1px dashed var(--gridline); font-size: 13px; }
.kv-row .k { width: 84px; flex-shrink: 0; color: var(--text-muted); }
.adjust-row { display: flex; gap: 10px; margin: 18px 0; }
.log-section { margin-top: 8px; }
.tabs { display: flex; gap: 6px; margin-bottom: 10px; }
.tab { border: 0; background: transparent; cursor: pointer; padding: 6px 12px; border-radius: 8px; font-size: 13px; }
.tab.active { background: var(--surface-alt, #f0f0ee); font-weight: 600; }
.table.compact th, .table.compact td { padding: 8px 10px; font-size: 12.5px; }
.empty-state.small { padding: 18px 0; font-size: 13px; color: var(--text-muted); }
.form-hint { color: var(--text-muted); font-size: 12px; line-height: 1.5; }
</style>
