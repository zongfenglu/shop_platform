<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { createBackup, downloadBackup, flushOpsCache, getOpsCache, getOpsOverview, getOpsQueues, listBackups, listDailyStats, listOpsJobs, runOpsJob } from '@/api/ops'

/**
 * 运维中心。对照 docs/prototype/admin/ops-center.html。
 * 定时任务、队列积压、备份、缓存、用量、日报来自真实接口。
 */
const activeTab = ref(1)
const loading = ref(false)
const runningCode = ref('')
const summary = ref({
  shopTotal: 0,
  activeShopTotal: 0,
  pendingDomainTotal: 0,
  orderTotal: 0,
  platformUserTotal: 0,
  totalStorageBytes: 0,
  totalSmsUsed: 0,
  generatedAt: '',
})
const usageRows = ref([])
const jobs = ref([])
const dailyRows = ref([])
const dailyDate = ref('')
const dailyLoading = ref(false)
const backups = ref([])
const backupLoading = ref(false)
const backingUp = ref(false)
const queues = ref({ reachable: false, nameServer: '', message: '', totalBacklog: 0, queues: [] })
const queueLoading = ref(false)
const cacheInfo = ref({ tenantKeyCount: 0, usedMemory: '0B', maxMemory: '未限制', hitRate: '—', shops: [] })
const cacheShopId = ref('')
const cacheLoading = ref(false)
const flushing = ref('')

onMounted(() => {
  loadOverview()
  loadJobs()
})

async function loadDaily() {
  dailyLoading.value = true
  try {
    dailyRows.value = (await listDailyStats()) || []
    dailyDate.value = dailyRows.value[0]?.statDate || ''
  } catch (e) {
    dailyRows.value = []
  } finally {
    dailyLoading.value = false
  }
}

async function loadOverview() {
  loading.value = true
  try {
    const res = await getOpsOverview()
    const { usageRows: rows = [], ...rest } = res || {}
    summary.value = { ...summary.value, ...rest }
    usageRows.value = rows
  } catch (e) {
    usageRows.value = []
  } finally {
    loading.value = false
  }
}

async function loadJobs() {
  try {
    jobs.value = (await listOpsJobs()) || []
  } catch (e) {
    jobs.value = []
  }
}

async function onRun(row) {
  if (!row.runnable || runningCode.value) return
  runningCode.value = row.code
  try {
    const result = await runOpsJob(row.code)
    message.success(result?.message || '已执行')
    await Promise.all([loadJobs(), loadOverview()])
    if (row.code === 'daily-report') await loadDaily()
  } catch (e) {
    // 拦截器已提示
  } finally {
    runningCode.value = ''
  }
}

async function loadQueues() {
  queueLoading.value = true
  try {
    queues.value = (await getOpsQueues()) || queues.value
  } catch (e) {
    queues.value = { ...queues.value, queues: [] }
  } finally {
    queueLoading.value = false
  }
}

async function loadCache() {
  cacheLoading.value = true
  try {
    const res = await getOpsCache()
    cacheInfo.value = { ...cacheInfo.value, ...res }
    if (!cacheShopId.value && res?.shops?.length) {
      cacheShopId.value = String(res.shops[0].id)
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    cacheLoading.value = false
  }
}

async function onFlush(scope) {
  if (flushing.value) return
  if (scope === 'tenant' && !cacheShopId.value) {
    message.error('请选择商城')
    return
  }
  flushing.value = scope
  try {
    const res = await flushOpsCache({
      scope,
      shopId: scope === 'tenant' ? Number(cacheShopId.value) : undefined,
    })
    message.success(`已删除 ${res?.deleted ?? 0} 个 key`)
    await loadCache()
  } catch (e) {
    // 拦截器已提示
  } finally {
    flushing.value = ''
  }
}

async function loadBackups() {
  backupLoading.value = true
  try {
    backups.value = (await listBackups()) || []
  } catch (e) {
    backups.value = []
  } finally {
    backupLoading.value = false
  }
}

async function onCreateBackup() {
  if (backingUp.value) return
  backingUp.value = true
  try {
    const row = await createBackup()
    if (row?.status === 'success') {
      message.success(row.message || '备份完成')
    } else {
      message.error(row?.message || '备份失败')
    }
    await loadBackups()
  } catch (e) {
    // 拦截器已提示
  } finally {
    backingUp.value = false
  }
}

async function onDownloadBackup(row) {
  if (row.status !== 'success') return
  try {
    await downloadBackup(row.id, row.filename)
  } catch (e) {
    // 拦截器已提示
  }
}

function formatBackupSize(bytes) {
  if (!bytes) return '0 KB'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function onRunDaily() {
  await onRun({ code: 'daily-report', runnable: true })
}

function statusClass(row) {
  if (!row.implemented) return 'tag-muted'
  if (row.lastStatus === '失败') return 'tag-critical'
  if (row.lastStatus === '成功') return 'tag-good'
  return 'tag-warning'
}

function shopStatusClass(status) {
  if (status === 'disabled' || status === 'expired') return 'tag-critical'
  if (status === 'trial' || status === 'normal') return 'tag-good'
  return 'tag-muted'
}

function shopStatusText(status) {
  if (status === 'disabled') return '停用'
  if (status === 'expired') return '过期'
  if (status === 'trial') return '试用'
  if (status === 'normal') return '正常'
  return status || '—'
}

function formatBytes(bytes) {
  if (!bytes) return '0 MB'
  const gb = bytes / 1024 / 1024 / 1024
  if (gb >= 1) return `${gb.toFixed(1)} GB`
  const mb = bytes / 1024 / 1024
  return `${mb.toFixed(0)} MB`
}

function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

function durationText(ms) {
  if (ms == null) return '—'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">运维中心</div>
      <div class="page-desc">定时任务、队列、备份、缓存。统计时间：{{ fmtTime(summary.generatedAt) }}</div>
    </div>
  </div>

  <div class="grid grid-4" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">店铺总数</div><div class="kpi-value">{{ summary.shopTotal }}</div></div>
    <div class="card kpi"><div class="kpi-label">正常/试用店铺</div><div class="kpi-value">{{ summary.activeShopTotal }}</div></div>
    <div class="card kpi"><div class="kpi-label">待审域名</div><div class="kpi-value" style="color: var(--status-warning-text)">{{ summary.pendingDomainTotal }}</div></div>
    <div class="card kpi"><div class="kpi-label">平台账号</div><div class="kpi-value">{{ summary.platformUserTotal }}</div></div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <div class="tab" :class="{ active: activeTab === 1 }" @click="activeTab = 1">定时任务</div>
      <div class="tab" :class="{ active: activeTab === 2 }" @click="activeTab = 2; loadQueues()">消息队列</div>
      <div class="tab" :class="{ active: activeTab === 3 }" @click="activeTab = 3; loadBackups()">数据库备份</div>
      <div class="tab" :class="{ active: activeTab === 4 }" @click="activeTab = 4; loadCache()">缓存管理</div>
      <div class="tab" :class="{ active: activeTab === 5 }" @click="activeTab = 5">店铺用量</div>
      <div class="tab" :class="{ active: activeTab === 6 }" @click="activeTab = 6; loadDaily()">交易日报</div>
    </div>

    <template v-if="activeTab === 1">
      <div class="form-hint" style="margin-bottom: 12px">
        已实现任务由 shop-job 按 cron 调度。配额、到期、关单、确认收货、售后超时、日报可立即执行。
      </div>
      <table class="table">
        <thead><tr><th>任务名称</th><th>频率</th><th>上次执行</th><th>耗时</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in jobs" :key="row.code">
            <td>{{ row.name }}</td>
            <td>{{ row.cron }}</td>
            <td>{{ fmtTime(row.lastRunAt) }}</td>
            <td>{{ durationText(row.lastDurationMs) }}</td>
            <td><span class="tag" :class="statusClass(row)">{{ row.lastStatus }}</span></td>
            <td>
              <button
                v-if="row.runnable"
                class="btn btn-sm btn-primary"
                :disabled="!!runningCode"
                @click="onRun(row)"
              >
                {{ runningCode === row.code ? '执行中…' : '立即执行' }}
              </button>
              <span v-else class="form-hint" style="margin: 0">{{ row.lastMessage }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <template v-else-if="activeTab === 2">
      <a-spin :spinning="queueLoading">
        <div class="form-hint" style="margin-bottom: 12px">
          {{ queues.message || '—' }}
          <span v-if="queues.nameServer"> · {{ queues.nameServer }}</span>
          <span v-if="queues.reachable"> · 已接入队列积压 {{ queues.totalBacklog }}</span>
        </div>
        <table class="table">
          <thead>
            <tr>
              <th>队列</th>
              <th>Topic / 消费组</th>
              <th class="num">待消费</th>
              <th class="num">死信</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in queues.queues" :key="row.name">
              <td>{{ row.name }}</td>
              <td>
                <div>{{ row.topic }}</div>
                <div style="font-size: 12px; color: var(--text-muted)">{{ row.consumerGroup }}</div>
              </td>
              <td class="num">{{ row.implemented ? (row.backlog ?? 0) : '—' }}</td>
              <td class="num">{{ row.implemented ? (row.deadLetter ?? 0) : '—' }}</td>
              <td>
                <span class="tag" :class="row.implemented ? (queues.reachable ? 'tag-good' : 'tag-warning') : 'tag-muted'">
                  {{ row.implemented ? (queues.reachable ? '已接入' : '不可达') : '未接入' }}
                </span>
                <span class="form-hint" style="margin-left: 8px">{{ row.message }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </a-spin>
    </template>

    <template v-else-if="activeTab === 3">
      <a-spin :spinning="backupLoading">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
          <div class="form-hint" style="margin: 0">逻辑导出当前库全部业务表，每天 02:00 自动执行，也可立即备份。</div>
          <button class="btn btn-sm btn-primary" :disabled="backingUp" @click="onCreateBackup">
            {{ backingUp ? '备份中…' : '立即备份' }}
          </button>
        </div>
        <table class="table">
          <thead>
            <tr>
              <th>文件</th>
              <th>时间</th>
              <th class="num">大小</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!backups.length && !backupLoading">
              <td colspan="5" style="text-align: center; color: var(--text-muted)">暂无备份，可点击「立即备份」</td>
            </tr>
            <tr v-for="row in backups" :key="row.id">
              <td>
                <div style="font-weight: 600">{{ row.filename }}</div>
                <div style="font-size: 12px; color: var(--text-muted)">{{ row.message || '—' }}</div>
              </td>
              <td>{{ fmtTime(row.createTime) }}</td>
              <td class="num">{{ formatBackupSize(row.sizeBytes) }}</td>
              <td><span class="tag" :class="row.status === 'success' ? 'tag-good' : 'tag-critical'">{{ row.status === 'success' ? '成功' : '失败' }}</span></td>
              <td>
                <button v-if="row.status === 'success'" class="btn btn-sm" @click="onDownloadBackup(row)">下载</button>
                <span v-else class="form-hint" style="margin: 0">不可下载</span>
              </td>
            </tr>
          </tbody>
        </table>
      </a-spin>
    </template>

    <template v-else-if="activeTab === 4">
      <a-spin :spinning="cacheLoading">
        <div class="form-hint" style="margin-bottom: 14px">
          Redis {{ cacheInfo.usedMemory }} / {{ cacheInfo.maxMemory }} · 命中率 {{ cacheInfo.hitRate }} · 租户前缀 key {{ cacheInfo.tenantKeyCount }}
          （只清 <code>t:&#123;shopId&#125;:</code>，不免密登录票据）
        </div>
        <div class="form-item" style="max-width: 520px">
          <label class="form-label">按租户清除</label>
          <div style="display: flex; gap: 10px">
            <select v-model="cacheShopId" class="form-select" style="width: 240px">
              <option v-for="shop in cacheInfo.shops" :key="shop.id" :value="String(shop.id)">{{ shop.name }}</option>
            </select>
            <button class="btn btn-sm" :disabled="!!flushing || !cacheShopId" @click="onFlush('tenant')">
              {{ flushing === 'tenant' ? '清除中…' : '清除该租户缓存' }}
            </button>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">按业务前缀</label>
          <div style="display: flex; gap: 10px; flex-wrap: wrap">
            <button class="btn btn-sm" :disabled="!!flushing" @click="onFlush('goods')">{{ flushing === 'goods' ? '清除中…' : '清除商品缓存' }}</button>
            <button class="btn btn-sm" :disabled="!!flushing" @click="onFlush('diy')">{{ flushing === 'diy' ? '清除中…' : '清除装修缓存' }}</button>
            <button class="btn btn-sm" :disabled="!!flushing" @click="onFlush('seckill')">{{ flushing === 'seckill' ? '清除中…' : '清除秒杀库存缓存' }}</button>
            <button class="btn btn-sm btn-primary" :disabled="!!flushing" @click="onFlush('all')">{{ flushing === 'all' ? '清除中…' : '清除全部租户缓存' }}</button>
          </div>
        </div>
      </a-spin>
    </template>

    <template v-else-if="activeTab === 6">
      <a-spin :spinning="dailyLoading">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
          <div class="form-hint" style="margin: 0">
            T+1 汇总{{ dailyDate ? '（' + dailyDate + '）' : '' }}。浏览 uv/pv 暂无埋点，记为 0。
          </div>
          <button class="btn btn-sm btn-primary" :disabled="!!runningCode" @click="onRunDaily">
            {{ runningCode === 'daily-report' ? '汇总中…' : '立即汇总昨日' }}
          </button>
        </div>
        <table class="table">
          <thead>
            <tr>
              <th>店铺</th>
              <th class="num">下单</th>
              <th class="num">支付</th>
              <th class="num">成交额</th>
              <th class="num">退款</th>
              <th class="num">新会员</th>
              <th class="num">成交用户</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!dailyRows.length && !dailyLoading">
              <td colspan="7" style="text-align: center; color: var(--text-muted)">暂无昨日日报，可点击「立即汇总昨日」</td>
            </tr>
            <tr v-for="row in dailyRows" :key="row.shopId + '-' + row.statDate">
              <td>
                <div style="font-weight: 600">{{ row.shopName }}</div>
                <div style="font-size: 12px; color: var(--text-muted)">#{{ row.shopId }}</div>
              </td>
              <td class="num">{{ row.orderCount }}</td>
              <td class="num">{{ row.payCount }}</td>
              <td class="num">¥{{ Number(row.payAmount || 0).toFixed(2) }}</td>
              <td class="num">¥{{ Number(row.refundAmount || 0).toFixed(2) }}</td>
              <td class="num">{{ row.newUser }}</td>
              <td class="num">{{ row.activeUser }}</td>
            </tr>
          </tbody>
        </table>
      </a-spin>
    </template>

    <template v-else>
      <a-spin :spinning="loading">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
          <div class="form-hint" style="margin: 0">
            当前总存储 {{ formatBytes(summary.totalStorageBytes) }}，本月短信消耗 {{ summary.totalSmsUsed }} 条（短信计费尚未接入，现为 0）
          </div>
          <button class="btn btn-sm btn-primary" :disabled="!!runningCode" @click="onRun({ code: 'quota-snapshot', runnable: true })">
            立即刷新快照
          </button>
        </div>
        <table class="table">
          <thead><tr><th>店铺</th><th>统计日期</th><th class="num">商品数</th><th class="num">员工数</th><th>存储</th><th>短信</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-if="!usageRows.length">
              <td colspan="7" style="text-align: center; color: var(--text-muted)">暂无用量快照，可点击「立即刷新快照」</td>
            </tr>
            <tr v-for="row in usageRows" :key="row.shopId + '-' + row.statDate">
              <td>
                <div style="font-weight: 600">{{ row.shopName }}</div>
                <div style="font-size: 12px; color: var(--text-muted)">#{{ row.shopId }}</div>
              </td>
              <td>{{ fmtTime(row.statDate) }}</td>
              <td class="num">{{ row.goodsCount }}</td>
              <td class="num">{{ row.staffCount }}</td>
              <td>{{ formatBytes(row.storageBytes) }}</td>
              <td>{{ row.smsMonthUsed }}</td>
              <td><span class="tag" :class="shopStatusClass(row.shopStatus)">{{ shopStatusText(row.shopStatus) }}</span></td>
            </tr>
          </tbody>
        </table>
      </a-spin>
    </template>
  </div>
</template>
