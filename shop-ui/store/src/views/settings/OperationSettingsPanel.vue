<script setup>
import { reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  createExpressCompany, createPrinter, createReturnAddress, createSmsChannel,
  deleteExpressCompany, deletePrinter, deleteReturnAddress, deleteSmsChannel,
  getOperationSettings, listExpressCompanies, listPrinters, listReturnAddresses,
  listSmsChannels, savePrintRules, saveSmsRules, saveUploadSettings,
  setDefaultReturnAddress, updateExpressCompany, updatePrinter,
  updateReturnAddress, updateSmsChannel,
} from '@/api/operationSettings'

const props = defineProps({ section: { type: String, required: true } })
const loading = ref(false)
const submitting = ref(false)
const rows = ref([])
const operations = ref(null)
const modalOpen = ref(false)
const editingId = ref(null)

const expressForm = reactive({ name: '', code: '', sort: 100, enabled: true })
const addressForm = reactive({
  contactName: '', phone: '', province: '', city: '', district: '', detail: '',
  postalCode: '', isDefault: false, sort: 100, enabled: true,
})
const printerForm = reactive({
  name: '', provider: 'feie', deviceNo: '', accessKey: '', accessSecret: '',
  endpoint: '', sort: 100, enabled: true,
})
const smsForm = reactive({
  name: '', provider: 'aliyun', appId: '', accessKeyId: '', accessKeySecret: '',
  signName: '', endpoint: '', priority: 100, enabled: true,
})
const uploadForm = reactive({
  provider: 'local', bucket: '', region: '', endpoint: '', domain: '',
  accessKeyId: '', accessKeySecret: '', imageMaxMb: 5, videoMaxMb: 50,
})
const printForm = reactive({ enabled: false, printerId: null, onPaid: true, onRefund: false, copies: 1 })
const smsRulesForm = reactive({
  enabled: false, newOrderTemplate: '', paidTemplate: '', shippedTemplate: '',
  refundTemplate: '', notifyPhones: '',
})

const titles = {
  express: ['物流公司', '维护发货时可选择的承运商'],
  returns: ['退货地址', '管理售后退货的收件地址'],
  upload: ['文件上传', '设置文件限制与访问域名'],
  printers: ['小票打印', '管理云打印机和订单自动打印规则'],
  sms: ['短信通知', '配置短信渠道、优先级和通知模板'],
}

const providerLabel = (provider) => ({
  feie: '飞鹅云', yilianyun: '易联云', cloud: '通用云打印', custom_http: '自定义 HTTP',
  aliyun: '阿里云', tencent: '腾讯云', huawei: '华为云', yunpian: '云片',
}[provider] || provider)

async function load() {
  loading.value = true
  try {
    if (props.section === 'express') rows.value = await listExpressCompanies()
    if (props.section === 'returns') rows.value = await listReturnAddresses()
    if (props.section === 'printers') {
      const [items, setting] = await Promise.all([listPrinters(), getOperationSettings()])
      rows.value = items
      operations.value = setting
      Object.assign(printForm, {
        enabled: !!setting.printEnabled, printerId: setting.printPrinterId || null,
        onPaid: setting.printOnPaid !== false, onRefund: !!setting.printOnRefund,
        copies: setting.printCopies || 1,
      })
    }
    if (props.section === 'sms') {
      const [items, setting] = await Promise.all([listSmsChannels(), getOperationSettings()])
      rows.value = items
      operations.value = setting
      Object.assign(smsRulesForm, {
        enabled: !!setting.smsEnabled,
        newOrderTemplate: setting.smsNewOrderTemplate || '',
        paidTemplate: setting.smsPaidTemplate || '',
        shippedTemplate: setting.smsShippedTemplate || '',
        refundTemplate: setting.smsRefundTemplate || '',
        notifyPhones: setting.smsNotifyPhones || '',
      })
    }
    if (props.section === 'upload') {
      const setting = await getOperationSettings()
      operations.value = setting
      Object.assign(uploadForm, {
        provider: setting.uploadProvider || 'local', bucket: setting.uploadBucket || '',
        region: setting.uploadRegion || '', endpoint: setting.uploadEndpoint || '',
        domain: setting.uploadDomain || '', accessKeyId: '', accessKeySecret: '',
        imageMaxMb: setting.imageMaxMb || 5, videoMaxMb: setting.videoMaxMb || 50,
      })
    }
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.section, load, { immediate: true })

function openCreate() {
  editingId.value = null
  if (props.section === 'express') Object.assign(expressForm, { name: '', code: '', sort: 100, enabled: true })
  if (props.section === 'returns') Object.assign(addressForm, {
    contactName: '', phone: '', province: '', city: '', district: '', detail: '',
    postalCode: '', isDefault: rows.value.length === 0, sort: 100, enabled: true,
  })
  if (props.section === 'printers') Object.assign(printerForm, {
    name: '', provider: 'feie', deviceNo: '', accessKey: '', accessSecret: '', endpoint: '', sort: 100, enabled: true,
  })
  if (props.section === 'sms') Object.assign(smsForm, {
    name: '', provider: 'aliyun', appId: '', accessKeyId: '', accessKeySecret: '', signName: '', endpoint: '', priority: 100, enabled: true,
  })
  modalOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  if (props.section === 'express') Object.assign(expressForm, {
    name: row.name, code: row.code, sort: row.sort, enabled: row.status === 'enabled',
  })
  if (props.section === 'returns') Object.assign(addressForm, {
    contactName: row.contactName, phone: row.phone, province: row.province, city: row.city,
    district: row.district, detail: row.detail, postalCode: row.postalCode || '',
    isDefault: !!row.isDefault, sort: row.sort, enabled: row.status === 'enabled',
  })
  if (props.section === 'printers') Object.assign(printerForm, {
    name: row.name, provider: row.provider, deviceNo: row.deviceNo, accessKey: '',
    accessSecret: '', endpoint: row.endpoint || '', sort: row.sort, enabled: row.status === 'enabled',
  })
  if (props.section === 'sms') Object.assign(smsForm, {
    name: row.name, provider: row.provider, appId: row.appId || '', accessKeyId: '',
    accessKeySecret: '', signName: row.signName || '', endpoint: row.endpoint || '',
    priority: row.priority, enabled: row.status === 'enabled',
  })
  modalOpen.value = true
}

function validateModal() {
  if (props.section === 'express') {
    if (!expressForm.name.trim() || !/^[a-z0-9_-]{2,32}$/.test(expressForm.code.trim())) {
      message.warning('请填写名称，代码使用 2-32 位小写字母、数字、下划线或短横线')
      return false
    }
  }
  if (props.section === 'returns' && (!addressForm.contactName.trim() || !addressForm.phone.trim()
    || !addressForm.province.trim() || !addressForm.city.trim() || !addressForm.district.trim() || !addressForm.detail.trim())) {
    message.warning('请填写完整的联系人和省市区地址')
    return false
  }
  if (props.section === 'printers' && (!printerForm.name.trim() || !printerForm.deviceNo.trim())) {
    message.warning('请填写打印机名称和终端编号')
    return false
  }
  if (props.section === 'sms' && (!smsForm.name.trim() || (smsForm.provider !== 'custom_http' && !smsForm.signName.trim()))) {
    message.warning('请填写渠道名称和短信签名')
    return false
  }
  return true
}

async function saveModal() {
  if (!validateModal()) return
  submitting.value = true
  try {
    if (props.section === 'express') {
      const payload = { ...expressForm, name: expressForm.name.trim(), code: expressForm.code.trim() }
      await (editingId.value ? updateExpressCompany(editingId.value, payload) : createExpressCompany(payload))
    }
    if (props.section === 'returns') {
      const payload = { ...addressForm }
      await (editingId.value ? updateReturnAddress(editingId.value, payload) : createReturnAddress(payload))
    }
    if (props.section === 'printers') {
      const payload = { ...printerForm }
      await (editingId.value ? updatePrinter(editingId.value, payload) : createPrinter(payload))
    }
    if (props.section === 'sms') {
      const payload = { ...smsForm }
      await (editingId.value ? updateSmsChannel(editingId.value, payload) : createSmsChannel(payload))
    }
    message.success(editingId.value ? '已保存修改' : '已创建')
    modalOpen.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

function confirmDelete(row) {
  Modal.confirm({
    title: `删除“${row.name || row.contactName}”？`,
    content: '删除后不可恢复，历史订单中的快照不受影响。',
    okText: '删除', cancelText: '取消', okType: 'danger',
    async onOk() {
      if (props.section === 'express') await deleteExpressCompany(row.id)
      if (props.section === 'returns') await deleteReturnAddress(row.id)
      if (props.section === 'printers') await deletePrinter(row.id)
      if (props.section === 'sms') await deleteSmsChannel(row.id)
      message.success('已删除')
      await load()
    },
  })
}

async function makeDefault(row) {
  await setDefaultReturnAddress(row.id)
  message.success('已设为默认退货地址')
  await load()
}

async function saveUpload() {
  if (uploadForm.domain && !/^https?:\/\//.test(uploadForm.domain)) {
    message.warning('访问域名必须以 http:// 或 https:// 开头')
    return
  }
  if (uploadForm.provider !== 'local') {
    if (!uploadForm.bucket || !uploadForm.region) {
      message.warning('请填写存储空间 Bucket 和地域 Region')
      return
    }
    if (uploadForm.provider === 'aliyun_oss' && !uploadForm.endpoint) {
      message.warning('请填写阿里云 OSS Endpoint')
      return
    }
    const providerChanged = operations.value?.uploadProvider !== uploadForm.provider
    if ((!uploadForm.accessKeyId && (providerChanged || !operations.value?.uploadAccessKeyIdSet))
      || (!uploadForm.accessKeySecret && (providerChanged || !operations.value?.uploadAccessKeySecretSet))) {
      message.warning('请填写当前存储渠道的 AccessKey ID 和 Secret')
      return
    }
  }
  submitting.value = true
  try {
    operations.value = await saveUploadSettings({ ...uploadForm })
    uploadForm.accessKeyId = ''
    uploadForm.accessKeySecret = ''
    message.success('文件上传设置已保存')
  } finally { submitting.value = false }
}

async function savePrint() {
  submitting.value = true
  try {
    operations.value = await savePrintRules({ ...printForm })
    message.success('打印规则已保存')
  } finally { submitting.value = false }
}

async function saveSmsRulesForm() {
  submitting.value = true
  try {
    operations.value = await saveSmsRules({ ...smsRulesForm })
    message.success('短信通知规则已保存')
  } finally { submitting.value = false }
}

function modalTitle() {
  const noun = { express: '物流公司', returns: '退货地址', printers: '打印机', sms: '短信渠道' }[props.section]
  return `${editingId.value ? '编辑' : '新增'}${noun}`
}
</script>

<template>
  <div class="settings-panel">
    <div class="page-header panel-header">
      <div><div class="card-title panel-title">{{ titles[section][0] }}</div><div class="card-sub panel-sub">{{ titles[section][1] }}</div></div>
      <button v-if="section !== 'upload'" class="btn btn-sm btn-primary" @click="openCreate">新增</button>
    </div>

    <a-spin :spinning="loading">
      <div v-if="section === 'express'" class="card">
        <table v-if="rows.length" class="table"><thead><tr><th>物流公司</th><th>代码</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody>
          <tr v-for="row in rows" :key="row.id"><td class="strong">{{ row.name }}</td><td>{{ row.code }}</td><td>{{ row.sort }}</td><td><span class="tag" :class="row.status === 'enabled' ? 'tag-good' : 'tag-muted'">{{ row.status === 'enabled' ? '启用' : '停用' }}</span></td><td><button class="btn btn-sm" @click="openEdit(row)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDelete(row)">删除</button></td></tr>
        </tbody></table>
      </div>

      <div v-else-if="section === 'returns'" class="card">
        <table v-if="rows.length" class="table"><thead><tr><th>联系人</th><th>联系电话</th><th>退货地址</th><th>状态</th><th>操作</th></tr></thead><tbody>
          <tr v-for="row in rows" :key="row.id"><td class="strong">{{ row.contactName }} <span v-if="row.isDefault" class="tag tag-primary">默认</span></td><td>{{ row.phone }}</td><td>{{ row.province }}{{ row.city }}{{ row.district }}{{ row.detail }}</td><td><span class="tag" :class="row.status === 'enabled' ? 'tag-good' : 'tag-muted'">{{ row.status === 'enabled' ? '启用' : '停用' }}</span></td><td><button v-if="!row.isDefault" class="btn btn-sm" @click="makeDefault(row)">设为默认</button><button class="btn btn-sm" @click="openEdit(row)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDelete(row)">删除</button></td></tr>
        </tbody></table>
        <div v-else class="empty-state">暂无退货地址</div>
      </div>

      <div v-else-if="section === 'upload'" class="card card-pad form-panel">
        <div class="form-item"><label class="form-label">存储方式</label><select v-model="uploadForm.provider" class="form-select"><option value="local">服务器本地存储</option><option value="aliyun_oss">阿里云 OSS</option><option value="tencent_cos">腾讯云 COS</option></select></div>
        <template v-if="uploadForm.provider !== 'local'">
          <div class="form-row form-item"><div><label class="form-label">存储空间 Bucket</label><input v-model.trim="uploadForm.bucket" class="form-input" :placeholder="uploadForm.provider === 'tencent_cos' ? 'example-1250000000' : 'example-bucket'" /></div><div><label class="form-label">地域 Region</label><input v-model.trim="uploadForm.region" class="form-input" :placeholder="uploadForm.provider === 'tencent_cos' ? 'ap-guangzhou' : 'cn-hangzhou'" /></div></div>
          <div v-if="uploadForm.provider === 'aliyun_oss'" class="form-item"><label class="form-label">OSS Endpoint</label><input v-model.trim="uploadForm.endpoint" class="form-input" placeholder="https://oss-cn-hangzhou.aliyuncs.com" /></div>
          <div class="form-row form-item"><div><label class="form-label">AccessKey ID</label><input v-model.trim="uploadForm.accessKeyId" type="password" autocomplete="new-password" class="form-input" :placeholder="operations?.uploadAccessKeyIdSet && operations?.uploadProvider === uploadForm.provider ? '已配置，留空不修改' : '请输入 AccessKey ID'" /></div><div><label class="form-label">AccessKey Secret</label><input v-model.trim="uploadForm.accessKeySecret" type="password" autocomplete="new-password" class="form-input" :placeholder="operations?.uploadAccessKeySecretSet && operations?.uploadProvider === uploadForm.provider ? '已配置，留空不修改' : '请输入 AccessKey Secret'" /></div></div>
        </template>
        <div class="form-item"><label class="form-label">文件访问域名</label><input v-model.trim="uploadForm.domain" class="form-input" placeholder="https://static.example.com/uploads" /></div>
        <div class="form-row form-item"><div><label class="form-label">图片大小上限（MB）</label><input v-model.number="uploadForm.imageMaxMb" type="number" min="1" max="20" class="form-input" /></div><div><label class="form-label">视频大小上限（MB）</label><input v-model.number="uploadForm.videoMaxMb" type="number" min="1" max="50" class="form-input" /></div></div>
        <div class="secret-state">{{ uploadForm.provider === 'local' ? '访问域名留空时使用系统的 /uploads 地址。' : '访问域名留空时使用存储桶默认域名；存储桶需允许公开读取，生产环境建议配置 CDN 或自定义域名。' }}</div>
        <div class="actions"><button class="btn btn-primary" :disabled="submitting" @click="saveUpload">保存</button></div>
      </div>

      <template v-else-if="section === 'printers'">
        <div class="card" style="margin-bottom: 14px"><table v-if="rows.length" class="table"><thead><tr><th>打印机</th><th>渠道</th><th>终端编号</th><th>凭证</th><th>状态</th><th>操作</th></tr></thead><tbody>
          <tr v-for="row in rows" :key="row.id"><td class="strong">{{ row.name }}</td><td>{{ providerLabel(row.provider) }}</td><td>{{ row.deviceNo }}</td><td>{{ row.accessKeySet || row.accessSecretSet ? '已配置' : '未配置' }}</td><td><span class="tag" :class="row.status === 'enabled' ? 'tag-good' : 'tag-muted'">{{ row.status === 'enabled' ? '启用' : '停用' }}</span></td><td><button class="btn btn-sm" @click="openEdit(row)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDelete(row)">删除</button></td></tr>
        </tbody></table><div v-else class="empty-state">暂无打印机</div></div>
        <div class="card card-pad form-panel"><div class="form-row form-item"><label class="switch-line"><input v-model="printForm.enabled" type="checkbox" /><span>开启自动打印</span></label><div><label class="form-label">订单打印机</label><select v-model="printForm.printerId" class="form-select"><option :value="null">请选择</option><option v-for="row in rows.filter(r => r.status === 'enabled')" :key="row.id" :value="row.id">{{ row.name }}</option></select></div></div><div class="form-row form-item"><label class="switch-line"><input v-model="printForm.onPaid" type="checkbox" /><span>订单付款后打印</span></label><label class="switch-line"><input v-model="printForm.onRefund" type="checkbox" /><span>订单退款后打印</span></label></div><div class="form-item compact"><label class="form-label">打印份数</label><input v-model.number="printForm.copies" type="number" min="1" max="5" class="form-input" /></div><div class="actions"><button class="btn btn-primary" :disabled="submitting" @click="savePrint">保存打印规则</button></div></div>
      </template>

      <template v-else-if="section === 'sms'">
        <div class="card" style="margin-bottom: 14px"><table v-if="rows.length" class="table"><thead><tr><th>渠道</th><th>服务商</th><th>短信签名</th><th>优先级</th><th>凭证</th><th>状态</th><th>操作</th></tr></thead><tbody>
          <tr v-for="row in rows" :key="row.id"><td class="strong">{{ row.name }}</td><td>{{ providerLabel(row.provider) }}</td><td>{{ row.signName || '—' }}</td><td>{{ row.priority }}</td><td>{{ row.accessKeyIdSet || row.accessKeySecretSet ? '已配置' : '未配置' }}</td><td><span class="tag" :class="row.status === 'enabled' ? 'tag-good' : 'tag-muted'">{{ row.status === 'enabled' ? '启用' : '停用' }}</span></td><td><button class="btn btn-sm" @click="openEdit(row)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDelete(row)">删除</button></td></tr>
        </tbody></table><div v-else class="empty-state">暂无短信渠道</div></div>
        <div class="card card-pad form-panel"><label class="switch-line form-item"><input v-model="smsRulesForm.enabled" type="checkbox" /><span>开启短信通知</span></label><div class="form-row form-item"><div><label class="form-label">新订单模板</label><input v-model.trim="smsRulesForm.newOrderTemplate" class="form-input" /></div><div><label class="form-label">支付成功模板</label><input v-model.trim="smsRulesForm.paidTemplate" class="form-input" /></div></div><div class="form-row form-item"><div><label class="form-label">发货通知模板</label><input v-model.trim="smsRulesForm.shippedTemplate" class="form-input" /></div><div><label class="form-label">退款通知模板</label><input v-model.trim="smsRulesForm.refundTemplate" class="form-input" /></div></div><div class="form-item"><label class="form-label">新订单接收手机号</label><input v-model.trim="smsRulesForm.notifyPhones" class="form-input" placeholder="多个手机号用英文逗号分隔" /></div><div class="actions"><button class="btn btn-primary" :disabled="submitting" @click="saveSmsRulesForm">保存通知规则</button></div></div>
      </template>
    </a-spin>

    <a-modal v-model:open="modalOpen" :title="modalTitle()" :confirm-loading="submitting" ok-text="保存" cancel-text="取消" width="620px" @ok="saveModal">
      <template v-if="section === 'express'"><div class="form-row form-item"><div><label class="form-label">物流公司名称</label><input v-model.trim="expressForm.name" class="form-input" /></div><div><label class="form-label">物流公司代码</label><input v-model.trim="expressForm.code" class="form-input" placeholder="如 shunfeng" /></div></div><div class="form-row form-item"><div><label class="form-label">排序</label><input v-model.number="expressForm.sort" type="number" min="0" class="form-input" /></div><label class="switch-line"><input v-model="expressForm.enabled" type="checkbox" /><span>启用</span></label></div></template>
      <template v-else-if="section === 'returns'"><div class="form-row form-item"><div><label class="form-label">联系人</label><input v-model.trim="addressForm.contactName" class="form-input" /></div><div><label class="form-label">联系电话</label><input v-model.trim="addressForm.phone" class="form-input" /></div></div><div class="region-row form-item"><div><label class="form-label">省</label><input v-model.trim="addressForm.province" class="form-input" /></div><div><label class="form-label">市</label><input v-model.trim="addressForm.city" class="form-input" /></div><div><label class="form-label">区/县</label><input v-model.trim="addressForm.district" class="form-input" /></div></div><div class="form-item"><label class="form-label">详细地址</label><input v-model.trim="addressForm.detail" class="form-input" /></div><div class="form-row form-item"><div><label class="form-label">邮政编码</label><input v-model.trim="addressForm.postalCode" class="form-input" /></div><div><label class="form-label">排序</label><input v-model.number="addressForm.sort" type="number" min="0" class="form-input" /></div></div><div class="toggle-row"><label class="switch-line"><input v-model="addressForm.isDefault" type="checkbox" /><span>默认地址</span></label><label class="switch-line"><input v-model="addressForm.enabled" type="checkbox" /><span>启用</span></label></div></template>
      <template v-else-if="section === 'printers'"><div class="form-row form-item"><div><label class="form-label">打印机名称</label><input v-model.trim="printerForm.name" class="form-input" /></div><div><label class="form-label">打印渠道</label><select v-model="printerForm.provider" class="form-select"><option value="feie">飞鹅云</option><option value="yilianyun">易联云</option><option value="cloud">通用云打印</option><option value="custom_http">自定义 HTTP</option></select></div></div><div class="form-item"><label class="form-label">终端编号</label><input v-model.trim="printerForm.deviceNo" class="form-input" /></div><div class="form-row form-item"><div><label class="form-label">用户密钥 / Key</label><input v-model.trim="printerForm.accessKey" type="password" class="form-input" autocomplete="new-password" /></div><div><label class="form-label">应用密钥 / Secret</label><input v-model.trim="printerForm.accessSecret" type="password" class="form-input" autocomplete="new-password" /></div></div><div v-if="printerForm.provider === 'custom_http'" class="form-item"><label class="form-label">接口地址</label><input v-model.trim="printerForm.endpoint" class="form-input" /></div><div class="toggle-row"><label class="switch-line"><input v-model="printerForm.enabled" type="checkbox" /><span>启用</span></label><div class="compact"><label class="form-label">排序</label><input v-model.number="printerForm.sort" type="number" min="0" class="form-input" /></div></div></template>
      <template v-else-if="section === 'sms'"><div class="form-row form-item"><div><label class="form-label">渠道名称</label><input v-model.trim="smsForm.name" class="form-input" /></div><div><label class="form-label">服务商</label><select v-model="smsForm.provider" class="form-select"><option value="aliyun">阿里云</option><option value="tencent">腾讯云</option><option value="huawei">华为云</option><option value="yunpian">云片</option><option value="custom_http">自定义 HTTP</option></select></div></div><div class="form-row form-item"><div><label class="form-label">App ID</label><input v-model.trim="smsForm.appId" class="form-input" /></div><div><label class="form-label">短信签名</label><input v-model.trim="smsForm.signName" class="form-input" /></div></div><div class="form-row form-item"><div><label class="form-label">AccessKey ID</label><input v-model.trim="smsForm.accessKeyId" type="password" class="form-input" autocomplete="new-password" /></div><div><label class="form-label">AccessKey Secret</label><input v-model.trim="smsForm.accessKeySecret" type="password" class="form-input" autocomplete="new-password" /></div></div><div v-if="smsForm.provider === 'custom_http'" class="form-item"><label class="form-label">接口地址</label><input v-model.trim="smsForm.endpoint" class="form-input" placeholder="https://..." /></div><div class="toggle-row"><label class="switch-line"><input v-model="smsForm.enabled" type="checkbox" /><span>启用</span></label><div class="compact"><label class="form-label">优先级</label><input v-model.number="smsForm.priority" type="number" min="0" class="form-input" /></div></div></template>
    </a-modal>
  </div>
</template>

<style scoped>
.panel-header { margin-bottom: 14px; }
.panel-title { margin: 0; }
.panel-sub { margin: 4px 0 0; }
.strong { font-weight: 600; }
.btn-danger-outline { color: var(--status-critical); border-color: var(--status-critical); margin-left: 6px; }
.form-panel { max-width: 760px; }
.form-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; align-items: end; }
.region-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.switch-line { display: flex; align-items: center; gap: 8px; min-height: 36px; font-size: 13px; cursor: pointer; }
.toggle-row { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; }
.compact { width: 140px; }
.actions { display: flex; justify-content: flex-end; border-top: 1px solid var(--gridline); padding-top: 18px; margin-top: 22px; }
.secret-state { color: var(--text-muted); font-size: 12px; }
.empty-state { padding: 48px 20px; text-align: center; color: var(--text-muted); }
table { min-width: 680px; }
code { background: var(--surface-subtle); padding: 2px 5px; border-radius: 4px; }
@media (max-width: 760px) {
  .form-row, .region-row { grid-template-columns: 1fr; }
}
</style>
