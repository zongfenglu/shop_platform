<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageShops } from '@/api/shop'
import { pageDomains } from '@/api/domain'
import { disableAdminMpTemplate, getAdminMpExtJson, getAdminMpOverview, listAdminMpAuthorizers, listAdminMpTemplates, saveAdminMpTemplate } from '@/api/mp'
import { message } from 'ant-design-vue'

/**
 * 客户端管理。对照原型 admin/mp-client.html。
 * H5/域名、第三方授权列表、代码模板库接真实接口；批量发布与 APP 打包仍未对接。
 */
const router = useRouter()
const activeTab = ref(1)
const loading = ref(false)
const shops = ref([])
const domains = ref([])
const mpOverview = ref({ componentConfigured: false, ticketReady: false, componentAppId: '', callbackUrl: '' })
const authorizers = ref([])
const templates = ref([])
const tplForm = reactive({ templateId: '', userVersion: '', userDesc: '' })

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [shopPage, domainPage, ov, auth, tpls] = await Promise.all([
      pageShops({ pageNum: 1, pageSize: 100 }),
      pageDomains({ pageNum: 1, pageSize: 200 }),
      getAdminMpOverview(),
      listAdminMpAuthorizers(),
      listAdminMpTemplates(),
    ])
    shops.value = shopPage?.records || []
    domains.value = domainPage?.records || []
    mpOverview.value = ov || mpOverview.value
    authorizers.value = auth || []
    templates.value = tpls || []
  } catch (e) {
    shops.value = []
    domains.value = []
  } finally {
    loading.value = false
  }
}

const rows = computed(() => shops.value.map((shop) => {
  const mine = domains.value.filter((d) => String(d.shopId) === String(shop.id))
  const sub = mine.find((d) => d.type === 'sub')
  const custom = mine.filter((d) => d.type === 'custom')
  const verified = mine.filter((d) => d.verifyStatus === 'verified').length
  const pending = mine.filter((d) => d.verifyStatus === 'pending').length
  return {
    ...shop,
    host: sub?.domain || custom[0]?.domain || `${shop.code}.shop.com`,
    domainTotal: mine.length,
    verified,
    pending,
  }
}))

const summary = computed(() => ({
  shopTotal: shops.value.length,
  domainTotal: domains.value.length,
  verified: domains.value.filter((d) => d.verifyStatus === 'verified').length,
  pending: domains.value.filter((d) => d.verifyStatus === 'pending').length,
}))

function shopStatusClass(status) {
  if (status === 'disabled' || status === 'expired') return 'tag-critical'
  if (status === 'trial' || status === 'normal') return 'tag-good'
  return 'tag-muted'
}

function shopStatusText(status) {
  return { trial: '试用', normal: '正常', expired: '过期', disabled: '停用', archived: '归档' }[status] || status || '—'
}

function goDomains() {
  router.push({ name: 'domains' })
}

function authText(row) {
  if (row.authMode === 'self') return '自填'
  return '托管'
}

async function onSaveTpl() {
  if (!tplForm.templateId.trim() || !tplForm.userVersion.trim()) {
    message.warning('请填写模板 ID 与版本号')
    return
  }
  await saveAdminMpTemplate({ ...tplForm })
  message.success('已保存模板')
  tplForm.templateId = ''
  tplForm.userVersion = ''
  tplForm.userDesc = ''
  templates.value = (await listAdminMpTemplates()) || []
}

async function onDisableTpl(id) {
  await disableAdminMpTemplate(id)
  message.success('已停用')
  templates.value = (await listAdminMpTemplates()) || []
}

async function onPreviewExt(shopId) {
  const data = await getAdminMpExtJson(shopId, 'mini')
  message.info(JSON.stringify(data?.ext || data))
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">客户端管理</div>
      <div class="page-desc">H5 / 域名、小程序授权与代码模板已接入；批量发布与 APP 云打包尚未对接</div>
    </div>
  </div>

  <div class="grid grid-4" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">已开通商城</div><div class="kpi-value">{{ summary.shopTotal }}</div></div>
    <div class="card kpi"><div class="kpi-label">绑定域名</div><div class="kpi-value">{{ summary.domainTotal }}</div></div>
    <div class="card kpi"><div class="kpi-label">已生效</div><div class="kpi-value">{{ summary.verified }}</div></div>
    <div class="card kpi"><div class="kpi-label">待审核</div><div class="kpi-value" style="color: var(--status-warning-text)">{{ summary.pending }}</div></div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <div class="tab" :class="{ active: activeTab === 1 }" @click="activeTab = 1">H5 / 域名</div>
      <div class="tab" :class="{ active: activeTab === 2 }" @click="activeTab = 2">小程序托管</div>
      <div class="tab" :class="{ active: activeTab === 3 }" @click="activeTab = 3">代码模板库</div>
      <div class="tab" :class="{ active: activeTab === 4 }" @click="activeTab = 4">APP 打包</div>
    </div>

    <template v-if="activeTab === 1">
      <a-spin :spinning="loading">
        <table class="table">
          <thead><tr><th>商城</th><th>默认访问</th><th class="num">域名数</th><th>审核</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-if="!rows.length && !loading">
              <td colspan="6" style="text-align: center; color: var(--text-muted)">暂无商城</td>
            </tr>
            <tr v-for="row in rows" :key="row.id">
              <td>
                <div style="font-weight: 600">{{ row.name }}</div>
                <div style="font-size: 12px; color: var(--text-muted)">{{ row.code }} · #{{ row.id }}</div>
              </td>
              <td>{{ row.host }}</td>
              <td class="num">{{ row.domainTotal }}</td>
              <td>
                <span v-if="row.pending" class="tag tag-warning">{{ row.pending }} 待审</span>
                <span v-else-if="row.verified" class="tag tag-good">{{ row.verified }} 已生效</span>
                <span v-else class="tag tag-muted">未绑定</span>
              </td>
              <td><span class="tag" :class="shopStatusClass(row.status)">{{ shopStatusText(row.status) }}</span></td>
              <td><button class="btn btn-sm" @click="goDomains">去域名管理</button></td>
            </tr>
          </tbody>
        </table>
      </a-spin>
    </template>

    <template v-else-if="activeTab === 2">
      <div class="form-hint" style="margin-bottom: 12px">
        第三方平台：{{ mpOverview.componentConfigured ? '已配置 ' + (mpOverview.componentAppId || '') : '尚未配置' }}
        · 票据：{{ mpOverview.ticketReady ? '已收到' : '尚未收到' }}
        · 回调 {{ mpOverview.callbackUrl || '—' }}
      </div>
      <table class="table">
        <thead><tr><th>商城</th><th>类型</th><th>模式</th><th>AppID</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-if="!authorizers.length && !loading">
            <td colspan="6" style="text-align: center; color: var(--text-muted)">暂无授权记录</td>
          </tr>
          <tr v-for="row in authorizers" :key="row.id">
            <td>{{ row.shopName }} · {{ row.shopCode }}</td>
            <td>{{ row.appType === 'official' ? '公众号' : '小程序' }}</td>
            <td>{{ authText(row) }}</td>
            <td>{{ row.appId }}</td>
            <td><span class="tag" :class="row.authStatus === 'authorized' ? 'tag-good' : 'tag-muted'">{{ row.authStatus === 'authorized' ? '已授权' : '已取消' }}</span></td>
            <td><button class="btn btn-sm" @click="onPreviewExt(row.shopId)">查看 ext.json</button></td>
          </tr>
        </tbody>
      </table>
    </template>

    <template v-else-if="activeTab === 3">
      <div class="form-hint">从微信开放平台草稿箱拿到 template_id 后登记到这里。上传/提审走批量发布队列，尚未接线。</div>
      <div class="form-row form-item" style="margin-top: 12px">
        <div><label class="form-label">模板 ID</label><input v-model="tplForm.templateId" class="form-input" /></div>
        <div><label class="form-label">版本号</label><input v-model="tplForm.userVersion" class="form-input" placeholder="如 2.4.0" /></div>
      </div>
      <div class="form-item">
        <label class="form-label">说明</label>
        <input v-model="tplForm.userDesc" class="form-input" />
      </div>
      <button class="btn btn-primary" @click="onSaveTpl">登记模板</button>
      <table class="table" style="margin-top: 16px">
        <thead><tr><th>模板 ID</th><th>版本</th><th>说明</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-if="!templates.length"><td colspan="5" style="text-align: center; color: var(--text-muted)">尚未登记</td></tr>
          <tr v-for="row in templates" :key="row.id">
            <td>{{ row.templateId }}</td>
            <td>{{ row.userVersion }}</td>
            <td>{{ row.userDesc || '—' }}</td>
            <td>{{ row.status }}</td>
            <td><button v-if="row.status === 'online'" class="btn btn-sm" @click="onDisableTpl(row.id)">停用</button></td>
          </tr>
        </tbody>
      </table>
    </template>

    <template v-else>
      <div class="form-hint">APP 云打包队列尚未对接 uni-app 云打包。</div>
    </template>
  </div>
</template>
