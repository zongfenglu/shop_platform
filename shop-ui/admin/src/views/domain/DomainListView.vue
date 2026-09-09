<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { approveDomain, checkDomainCname, issueDomainCert, pageDomains, rejectDomain, unbindDomain, updateDomainProtocol, uploadDomainCertificate } from '@/api/domain'

const activeTab = ref('review')
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const summary = ref({
  total: 0,
  customCount: 0,
  subCount: 0,
  pendingCount: 0,
  validCount: 0,
})

const query = reactive({
  keyword: '',
  type: 'custom',
  verifyStatus: 'pending',
  pageNum: 1,
  pageSize: 10,
})
const certOpen = ref(false)
const certRow = ref(null)
const certFile = ref(null)
const keyFile = ref(null)
const certSubmitting = ref(false)

const tabs = computed(() => [
  { key: 'review', label: '自定义域名审核', count: summary.value.pendingCount },
  { key: 'all', label: '全部域名', count: summary.value.total },
  { key: 'pool', label: '泛域名池', count: summary.value.subCount },
])

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await pageDomains({
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      verifyStatus: query.verifyStatus || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    rows.value = res?.records || []
    total.value = res?.total || 0
    summary.value = res?.summary || summary.value
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.pageNum = 1
  syncTabFilters()
  load()
}

function onTabChange(key) {
  activeTab.value = key
  query.pageNum = 1
  query.keyword = ''
  syncTabFilters()
  if (key !== 'pool') load()
}

function syncTabFilters() {
  query.type = activeTab.value === 'review' ? 'custom' : ''
  query.verifyStatus = activeTab.value === 'review' ? 'pending' : ''
}

function onPageChange(page) {
  query.pageNum = page
  load()
}

function typeText(row) {
  return row.type === 'custom' ? '自定义域名' : '平台泛域名'
}

function certText(row) {
  if (row.certStatus === 'valid') {
    if (!row.certExpireTime) return '已签发'
    return `已签发 · ${String(row.certExpireTime).slice(0, 10)} 到期`
  }
  if (row.certStatus === 'issuing') return '签发中'
  if (row.certStatus === 'expired') return '已过期'
  if (row.certStatus === 'failed') return row.certError ? `签发失败` : '签发失败'
  if (row.certStatus === 'pending') return row.verifyStatus === 'verified' ? '待签发' : '待审核'
  return row.certStatus || '—'
}

function certClass(row) {
  if (row.certStatus === 'valid') return 'tag-good'
  if (row.certStatus === 'expired' || row.certStatus === 'failed') return 'tag-critical'
  return 'tag-muted'
}

function activeText(row) {
  if (row.verifyStatus === 'verified') return '已生效'
  if (row.verifyStatus === 'resolving') return '解析中'
  if (row.verifyStatus === 'rejected') return '已驳回'
  return '待审核'
}

function activeClass(row) {
  if (row.verifyStatus === 'verified') return 'tag-good'
  if (row.verifyStatus === 'resolving') return 'tag-warning'
  if (row.verifyStatus === 'rejected') return 'tag-critical'
  return 'tag-warning'
}

function cnameText(row) {
  if (row.cnameStatus === 'ok') return '已解析'
  if (row.cnameStatus === 'skipped') return '已跳过'
  if (row.cnameStatus === 'fail') return '解析失败'
  return '待解析'
}

function cnameClass(row) {
  if (row.cnameStatus === 'ok' || row.cnameStatus === 'skipped') return 'tag-good'
  if (row.cnameStatus === 'fail') return 'tag-critical'
  return 'tag-warning'
}

function cnameReady(row) {
  return row.cnameStatus === 'ok' || row.cnameStatus === 'skipped'
}

function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function onCheck(row) {
  try {
    await checkDomainCname(row.id)
    message.success('已重新检测解析')
    await load()
  } catch (e) {
    // 拦截器已提示
  }
}

async function onApprove(row) {
  try {
    await approveDomain(row.id)
    message.success('已通过。未启用 ACME 时证书保持待签发，不会假装已签发')
    await load()
  } catch (e) {
    // 拦截器已提示
  }
}

function onReject(row) {
  Modal.confirm({
    title: '确认驳回该域名申请？',
    content: row.domain,
    okText: '驳回',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await rejectDomain(row.id, '平台驳回')
      message.success('已驳回')
      await load()
    },
  })
}

async function onIssueCert(row) {
  try {
    await issueDomainCert(row.id)
    message.success('证书已签发')
    await load()
  } catch (e) {
    // 未启用或 HTTP-01 失败时由接口返回明确错误
    await load()
  }
}

function openCert(row) { certRow.value = row; certFile.value = null; keyFile.value = null; certOpen.value = true }
async function submitCert() {
  if (!certFile.value || !keyFile.value) { message.error('请选择证书和私钥文件'); return }
  certSubmitting.value = true
  try { await uploadDomainCertificate(certRow.value.id, certFile.value, keyFile.value); message.success('证书上传成功'); certOpen.value = false; await load() } finally { certSubmitting.value = false }
}
async function changeProtocol(row, event) {
  try { await updateDomainProtocol(row.id, event.target.value); row.protocol = event.target.value; message.success('协议配置已更新') } catch (e) { event.target.value = row.protocol || 'http' }
}

function onUnbind(row) {
  Modal.confirm({
    title: '确认解绑？',
    content: row.domain,
    okText: '解绑',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await unbindDomain(row.id)
      message.success('已解绑')
      await load()
    },
  })
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">域名管理</div>
      <div class="page-desc">平台泛域名与自定义域名审核，当前共 {{ summary.total }} 条记录</div>
    </div>
  </div>

  <div class="grid grid-4" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">域名总数</div><div class="kpi-value">{{ summary.total }}</div></div>
    <div class="card kpi"><div class="kpi-label">自定义域名</div><div class="kpi-value">{{ summary.customCount }}</div></div>
    <div class="card kpi"><div class="kpi-label">泛域名</div><div class="kpi-value">{{ summary.subCount }}</div></div>
    <div class="card kpi"><div class="kpi-label">待审核</div><div class="kpi-value" style="color: var(--status-warning-text)">{{ summary.pendingCount }}</div></div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <div v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }" @click="onTabChange(tab.key)">
        {{ tab.label }} <span class="count">({{ tab.count }})</span>
      </div>
    </div>

    <div v-if="activeTab !== 'pool'" style="display: flex; gap: 12px; margin-bottom: 14px">
      <input v-model="query.keyword" class="form-input" style="width: 240px" placeholder="域名 / 商家名称" @keyup.enter="onSearch" />
      <button class="btn btn-primary" @click="onSearch">查询</button>
    </div>

    <template v-if="activeTab === 'pool'">
      <div class="card" style="background: var(--surface-2); border: none; padding: 14px 18px; margin-bottom: 14px">
        <div style="font-weight: 600; margin-bottom: 6px">*.shop.com（可由 PLATFORM_BASE_DOMAIN 覆盖）</div>
        <div class="form-hint">新建商家时自动分配子域名，无需审核。平台泛域名证书请在网关单独安装；自定义域名走 Let's Encrypt HTTP-01，未启用 ACME 时审核通过也不会假装已签发。</div>
      </div>
      <table class="table">
        <thead><tr><th>保留词</th><th>说明</th></tr></thead>
        <tbody>
          <tr v-for="word in ['admin', 'api', 'www', 'store', 'mp', 'cdn', 'static', 'mail', 'ftp']" :key="word">
            <td>{{ word }}</td>
            <td>不可作为商家二级域名前缀</td>
          </tr>
        </tbody>
      </table>
    </template>

    <template v-else>
      <a-spin :spinning="loading">
        <table class="table">
          <thead>
            <tr>
              <th>域名</th>
              <th>协议</th>
              <th>商家</th>
              <th>类型</th>
              <th v-if="activeTab === 'review'">CNAME 目标</th>
              <th v-if="activeTab === 'review'">解析校验</th>
              <th>证书状态</th>
              <th>生效状态</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!rows.length">
              <td :colspan="activeTab === 'review' ? 10 : 8" style="text-align: center; color: var(--text-muted)">暂无记录</td>
            </tr>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.domain }}</td>
              <td>
                <select v-if="row.type === 'custom'" class="form-select protocol-select" :value="row.protocol || 'http'" @change="changeProtocol(row, $event)">
                  <option value="http">HTTP</option><option value="https">HTTPS</option>
                </select>
                <span v-else>{{ (row.protocol || 'http').toUpperCase() }}</span>
              </td>
              <td>
                <div style="font-weight: 600">{{ row.shopName }}</div>
                <div style="font-size: 12px; color: var(--text-muted)">#{{ row.shopId }}</div>
              </td>
              <td>{{ typeText(row) }}</td>
              <td v-if="activeTab === 'review'">{{ row.cnameTarget || '—' }}</td>
              <td v-if="activeTab === 'review'"><span class="tag" :class="cnameClass(row)">{{ cnameText(row) }}</span></td>
              <td>
                <span class="tag" :class="certClass(row)">{{ certText(row) }}</span>
                <div v-if="row.certError" class="form-hint">{{ row.certError }}</div>
              </td>
              <td><span class="tag" :class="activeClass(row)">{{ activeText(row) }}</span></td>
              <td>{{ fmtTime(row.createTime) }}</td>
              <td>
                <template v-if="row.type === 'custom' && row.verifyStatus === 'pending'">
                  <button class="btn btn-sm" @click="onCheck(row)">检测解析</button>
                  <button class="btn btn-sm btn-primary" :disabled="!cnameReady(row)" @click="onApprove(row)">
                    {{ cnameReady(row) ? '通过' : '需先解析' }}
                  </button>
                  <button class="btn btn-sm" @click="onReject(row)">驳回</button>
                </template>
                <template v-else-if="row.type === 'custom'">
                  <button v-if="row.verifyStatus === 'verified' && row.certStatus !== 'valid'" class="btn btn-sm btn-primary" @click="onIssueCert(row)">签发证书</button>
                  <button v-if="row.verifyStatus === 'verified'" class="btn btn-sm" @click="openCert(row)">上传证书</button>
                  <button class="btn btn-sm" @click="onUnbind(row)">解绑</button>
                </template>
                <button v-else class="btn btn-sm" disabled>默认</button>
              </td>
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
    </template>
  </div>

  <div v-if="certOpen" class="modal-mask" @click.self="certOpen = false">
    <div class="modal">
      <div class="modal-header"><span>上传域名证书 · {{ certRow?.domain }}</span><button class="modal-close" @click="certOpen = false">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label">证书 PEM</label><input type="file" accept=".pem,.crt,.cer" @change="certFile = $event.target.files[0]" /></div>
        <div class="form-item"><label class="form-label">私钥 PEM</label><input type="file" accept=".pem,.key" @change="keyFile = $event.target.files[0]" /></div>
        <div class="form-hint">仅支持 PEM 格式；证书上传后将加密保存，并按 HTTPS 配置使用。</div>
      </div>
      <div class="modal-footer"><button class="btn" @click="certOpen = false">取消</button><button class="btn btn-primary" :disabled="certSubmitting" @click="submitCert">{{ certSubmitting ? '上传中…' : '确认上传' }}</button></div>
    </div>
  </div>
</template>

<style scoped>
.protocol-select { width: 86px; min-width: 86px; padding: 4px 6px; }
</style>
