<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  applyStoreDomain,
  checkStoreDomainCname,
  getStoreDomains,
  updateStoreDomainProtocol,
  unbindStoreDomain,
} from '@/api/domain'
import { getStoreMp, getStoreMpAuthUrl, getStoreMpExtJson, saveStoreMpSelf, unbindStoreMp } from '@/api/mp'

/**
 * 客户端。对照 docs/prototype/store/client.html。
 * H5 域名、小程序/公众号自填与托管授权接真实接口；APP 云打包仍未对接。
 */
const activeTab = ref('h5')
const loading = ref(false)
const submitting = ref(false)
const overview = ref({
  platformBaseDomain: 'shop.com',
  h5PublicUrl: 'http://localhost:5175',
  skipCname: true,
  defaultDomain: '',
  domains: [],
})
const customInput = ref('')
const mpLoading = ref(false)
const mpSaving = ref(false)
const mpOverview = ref({ componentConfigured: false, ticketReady: false, authorizers: [] })
const miniMode = ref('self')
const officialMode = ref('self')
const miniForm = reactive({ appId: '', appSecret: '' })
const officialForm = reactive({ appId: '', appSecret: '' })

function authorizerOf(type) {
  return (mpOverview.value.authorizers || []).find((a) => a.appType === type) || null
}

function fillMpForms() {
  const mini = authorizerOf('mini')
  miniForm.appId = mini?.appId || ''
  miniForm.appSecret = mini?.secretSet ? '********' : ''
  miniMode.value = mini?.authMode === 'hosted' ? 'hosted' : 'self'
  const oa = authorizerOf('official')
  officialForm.appId = oa?.appId || ''
  officialForm.appSecret = oa?.secretSet ? '********' : ''
  officialMode.value = oa?.authMode === 'hosted' ? 'hosted' : 'self'
}

async function loadMp() {
  mpLoading.value = true
  try {
    mpOverview.value = (await getStoreMp()) || mpOverview.value
    fillMpForms()
  } catch (e) {
    mpOverview.value = { componentConfigured: false, ticketReady: false, authorizers: [] }
  } finally {
    mpLoading.value = false
  }
}

const customDomains = computed(() => (overview.value.domains || []).filter((d) => d.type === 'custom'))
const subDomain = computed(() => (overview.value.domains || []).find((d) => d.type === 'sub') || null)
const defaultHost = computed(() => subDomain.value?.domain || overview.value.defaultDomain || '')
const h5Hint = computed(() => {
  const host = defaultHost.value
  const base = overview.value.h5PublicUrl
  if (!host) return base
  return `${base}  （本地联调仍可带 X-Shop-Id；绑定生效后按 Host 识别）`
})

onMounted(() => {
  load()
  loadMp()
})

async function load() {
  loading.value = true
  try {
    overview.value = (await getStoreDomains()) || overview.value
  } catch (e) {
    overview.value = { ...overview.value, domains: [] }
  } finally {
    loading.value = false
  }
}

function verifyText(row) {
  if (row.verifyStatus === 'verified') return '已生效'
  if (row.verifyStatus === 'rejected') return '已驳回'
  return '待审核'
}

function verifyClass(row) {
  if (row.verifyStatus === 'verified') return 'tag-good'
  if (row.verifyStatus === 'rejected') return 'tag-critical'
  return 'tag-warning'
}

function cnameText(row) {
  if (row.cnameStatus === 'ok') return '已解析'
  if (row.cnameStatus === 'skipped') return '本地跳过'
  if (row.cnameStatus === 'fail') return '解析失败'
  return '待检测'
}

async function onApply() {
  const domain = customInput.value.trim()
  if (!domain) {
    message.error('请填写自定义域名')
    return
  }
  submitting.value = true
  try {
    await applyStoreDomain(domain)
    customInput.value = ''
    message.success('已提交，等待平台审核')
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function onCheck(row) {
  try {
    const data = await checkStoreDomainCname(row.id)
    if (data.cnameStatus === 'ok' || data.cnameStatus === 'skipped') {
      message.success('解析检测通过')
    } else {
      message.warning('尚未正确 CNAME 到 ' + (row.cnameTarget || defaultHost.value))
    }
    await load()
  } catch (e) {
    // 拦截器已提示
  }
}

async function onProtocolChange(row, event) {
  const protocol = event.target.value
  try {
    await updateStoreDomainProtocol(row.id, protocol)
    row.protocol = protocol
    message.success('访问协议已更新')
  } catch (e) {
    event.target.value = row.protocol || 'http'
  }
}

async function onSaveSelf(appType) {
  const form = appType === 'mini' ? miniForm : officialForm
  if (!form.appId.trim()) {
    message.error('请填写 AppID')
    return
  }
  mpSaving.value = true
  try {
    await saveStoreMpSelf({ appType, appId: form.appId.trim(), appSecret: form.appSecret })
    message.success('已保存')
    await loadMp()
  } catch (e) {
    // 拦截器已提示
  } finally {
    mpSaving.value = false
  }
}

async function onStartAuth(appType) {
  try {
    const data = await getStoreMpAuthUrl(appType)
    if (data?.url) {
      window.open(data.url, '_blank')
    }
  } catch (e) {
    // 套餐未开通或平台未配置时由接口返回明确错误
  }
}

async function onDownloadExt(appType) {
  try {
    const data = await getStoreMpExtJson(appType)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'ext.json'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    // 拦截器已提示
  }
}

function onUnbindMp(appType) {
  Modal.confirm({
    title: '确认解绑？',
    content: '解绑后需重新自填或扫码授权',
    okText: '解绑',
    cancelText: '取消',
    async onOk() {
      await unbindStoreMp(appType)
      message.success('已解绑')
      await loadMp()
    },
  })
}

function statusText(row) {
  if (!row) return '未绑定'
  return row.authStatus === 'authorized' ? '已授权' : '已取消授权'
}

function onUnbind(row) {
  Modal.confirm({
    title: '确认解绑该域名？',
    content: row.domain,
    okText: '解绑',
    cancelText: '取消',
    async onOk() {
      await unbindStoreDomain(row.id)
      message.success('已解绑')
      await load()
    },
  })
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">客户端</div>
      <div class="page-desc">管理小程序、公众号、H5 与 APP 的接入配置</div>
    </div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <div class="tab" :class="{ active: activeTab === 'h5' }" @click="activeTab = 'h5'">H5</div>
      <div class="tab" :class="{ active: activeTab === 'mini' }" @click="activeTab = 'mini'">小程序</div>
      <div class="tab" :class="{ active: activeTab === 'wechat' }" @click="activeTab = 'wechat'">公众号</div>
      <div class="tab" :class="{ active: activeTab === 'app' }" @click="activeTab = 'app'">APP</div>
    </div>

    <template v-if="activeTab === 'h5'">
      <a-spin :spinning="loading">
        <div class="form-item">
          <label class="form-label">默认访问地址（平台泛域名）</label>
          <input class="form-input" :value="defaultHost ? 'https://' + defaultHost : h5Hint" disabled style="max-width: 480px" />
          <div class="form-hint">本地开发消费者端：{{ overview.h5PublicUrl }} ，shopId 仍可用请求头注入。</div>
        </div>

        <div class="form-item">
          <label class="form-label">自定义域名</label>
          <div style="display: flex; gap: 8px; max-width: 560px">
            <input v-model="customInput" class="form-input" placeholder="如 www.your-shop.com" style="flex: 1" :disabled="customDomains.some((d) => d.verifyStatus === 'pending' || d.verifyStatus === 'verified')" />
            <button class="btn btn-primary" :disabled="submitting || customDomains.some((d) => d.verifyStatus === 'pending' || d.verifyStatus === 'verified')" @click="onApply">提交审核</button>
          </div>
          <div class="form-hint">
            请将域名 CNAME 到 <code>{{ defaultHost || ('{code}.' + overview.platformBaseDomain) }}</code>
            。审核通过后消费者端按 Host 识别本店。
            <template v-if="overview.skipCname"> 当前环境已跳过真实 DNS 检测，方便本地联调。</template>
          </div>
        </div>

        <table v-if="customDomains.length" class="table" style="margin-top: 16px">
          <thead>
            <tr>
              <th>域名</th>
              <th>协议</th>
              <th>CNAME 目标</th>
              <th>解析</th>
              <th>审核</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in customDomains" :key="row.id">
              <td>{{ row.domain }}</td>
              <td><select class="form-select" style="width: 84px" :value="row.protocol || 'http'" @change="onProtocolChange(row, $event)"><option value="http">HTTP</option><option value="https">HTTPS</option></select></td>
              <td>{{ row.cnameTarget || defaultHost }}</td>
              <td><span class="tag" :class="row.cnameStatus === 'ok' || row.cnameStatus === 'skipped' ? 'tag-good' : row.cnameStatus === 'fail' ? 'tag-critical' : 'tag-warning'">{{ cnameText(row) }}</span></td>
              <td>
                <span class="tag" :class="verifyClass(row)">{{ verifyText(row) }}</span>
                <span v-if="row.rejectReason" class="form-hint"> {{ row.rejectReason }}</span>
              </td>
              <td>
                <button v-if="row.verifyStatus === 'pending'" class="btn btn-sm" @click="onCheck(row)">检测解析</button>
                <button class="btn btn-sm" @click="onUnbind(row)">解绑</button>
              </td>
            </tr>
          </tbody>
        </table>
      </a-spin>
    </template>

    <template v-else-if="activeTab === 'mini'">
      <a-spin :spinning="mpLoading">
        <p class="card-title">微信小程序</p>
        <div class="form-item">
          <label class="form-label">接入方式</label>
          <div style="display: flex; gap: 10px">
            <button class="btn btn-sm" :class="{ 'btn-primary': miniMode === 'self' }" @click="miniMode = 'self'">自填 AppID</button>
            <button class="btn btn-sm" :class="{ 'btn-primary': miniMode === 'hosted' }" @click="miniMode = 'hosted'">授权托管</button>
          </div>
        </div>
        <template v-if="miniMode === 'self'">
          <div class="form-row form-item">
            <div>
              <label class="form-label"><span class="req">*</span>AppID</label>
              <input v-model="miniForm.appId" class="form-input" />
            </div>
            <div>
              <label class="form-label"><span class="req">*</span>AppSecret</label>
              <input v-model="miniForm.appSecret" class="form-input" type="password" autocomplete="off" />
            </div>
          </div>
          <div class="form-hint">Secret 加密存储，回显为掩码。下载的 ext.json 只注入 shopId / 域名，业务代码不要写租户分支。</div>
          <div style="display: flex; gap: 10px">
            <button class="btn btn-primary" :disabled="mpSaving" @click="onSaveSelf('mini')">保存</button>
            <button class="btn" @click="onDownloadExt('mini')">下载 ext.json</button>
            <button v-if="authorizerOf('mini')" class="btn" @click="onUnbindMp('mini')">解绑</button>
          </div>
        </template>
        <template v-else>
          <div class="form-hint" style="margin-bottom: 12px">
            平台配置：{{ mpOverview.componentConfigured ? '已填写第三方参数' : '尚未配置' }}
            · 票据：{{ mpOverview.ticketReady ? '已收到' : '尚未收到 component_verify_ticket' }}
            · 当前套餐需含「微信第三方托管」才能扫码。
          </div>
          <div v-if="authorizerOf('mini')" class="kv-row" style="margin-bottom: 12px">
            <div>授权状态 {{ statusText(authorizerOf('mini')) }} · AppID {{ authorizerOf('mini').appId }} · {{ authorizerOf('mini').nickName || '托管' }}</div>
          </div>
          <div style="display: flex; gap: 10px">
            <button class="btn btn-primary" @click="onStartAuth('mini')">{{ authorizerOf('mini') ? '重新扫码授权' : '去微信授权' }}</button>
            <button class="btn" @click="onDownloadExt('mini')">下载 ext.json</button>
            <button v-if="authorizerOf('mini')" class="btn" @click="onUnbindMp('mini')">解绑</button>
          </div>
        </template>
      </a-spin>
    </template>

    <template v-else-if="activeTab === 'wechat'">
      <a-spin :spinning="mpLoading">
        <p class="card-title">公众号</p>
        <div class="form-item">
          <label class="form-label">接入方式</label>
          <div style="display: flex; gap: 10px">
            <button class="btn btn-sm" :class="{ 'btn-primary': officialMode === 'self' }" @click="officialMode = 'self'">自填 AppID</button>
            <button class="btn btn-sm" :class="{ 'btn-primary': officialMode === 'hosted' }" @click="officialMode = 'hosted'">授权托管</button>
          </div>
        </div>
        <template v-if="officialMode === 'self'">
          <div class="form-row form-item">
            <div>
              <label class="form-label"><span class="req">*</span>公众号 AppID</label>
              <input v-model="officialForm.appId" class="form-input" />
            </div>
            <div>
              <label class="form-label"><span class="req">*</span>AppSecret</label>
              <input v-model="officialForm.appSecret" class="form-input" type="password" autocomplete="off" />
            </div>
          </div>
          <div style="display: flex; gap: 10px">
            <button class="btn btn-primary" :disabled="mpSaving" @click="onSaveSelf('official')">保存</button>
            <button v-if="authorizerOf('official')" class="btn" @click="onUnbindMp('official')">解绑</button>
          </div>
        </template>
        <template v-else>
          <div class="form-hint" style="margin-bottom: 12px">
            托管扫码与小程序共用第三方平台配置。网页授权 / 模板消息仍按已授权账号后续接线。
          </div>
          <div v-if="authorizerOf('official')" style="margin-bottom: 12px">
            {{ statusText(authorizerOf('official')) }} · {{ authorizerOf('official').appId }}
          </div>
          <div style="display: flex; gap: 10px">
            <button class="btn btn-primary" @click="onStartAuth('official')">去微信授权</button>
            <button v-if="authorizerOf('official')" class="btn" @click="onUnbindMp('official')">解绑</button>
          </div>
        </template>
      </a-spin>
    </template>

    <template v-else>
      <p class="card-title">APP 云打包</p>
      <p class="card-sub" style="margin-bottom: 0">
        uni-app 云打包任务队列按排期尚未对接。H5 可先用于推广与扫码访问。
      </p>
    </template>
  </div>
</template>
