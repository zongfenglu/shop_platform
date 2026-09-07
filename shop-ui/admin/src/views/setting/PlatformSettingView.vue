<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getPlatformSettings, savePlatformSetting } from '@/api/platformSetting'

const active = ref('site')
const loading = ref(false)
const savingKey = ref('')

const forms = reactive({
  site: {
    platformName: '多开云商城',
    customerPhone: '',
    icpNo: '',
    contactEmail: '',
  },
  storage: {
    provider: 'local',
    bucket: '',
    region: '',
  },
  sms: {
    provider: '',
    accessKey: '',
    accessSecret: '',
    signName: '',
  },
  express: {
    provider: '',
    apiKey: '',
  },
  wechat: {
    componentAppId: '',
    componentSecret: '',
    token: '',
    encodingAesKey: '',
    callbackUrl: 'http://localhost:8085/notify/wechat/component',
  },
  pay: {
    channel: '',
    mchId: '',
    certSerialNo: '',
  },
})

const sections = [
  { key: 'site', label: '基础信息' },
  { key: 'storage', label: '存储配置' },
  { key: 'sms', label: '短信通道' },
  { key: 'express', label: '物流查询' },
  { key: 'wechat', label: '微信开放平台' },
  { key: 'pay', label: '支付配置' },
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await getPlatformSettings()
    assignSection('site', res?.site)
    assignSection('storage', res?.storage)
    assignSection('sms', res?.sms)
    assignSection('express', res?.express)
    assignSection('wechat', res?.wechat)
    assignSection('pay', res?.pay)
  } catch (e) {
    // 拦截器已提示；不回填示例凭证
  } finally {
    loading.value = false
  }
}

function assignSection(key, value) {
  Object.assign(forms[key], value || {})
}

async function saveSection(key) {
  savingKey.value = key
  try {
    await savePlatformSetting(key, { ...forms[key] })
    message.success('保存成功')
  } catch (e) {
    // 统一由 http 层提示错误
  } finally {
    savingKey.value = ''
  }
}

function isActive(key) {
  return active.value === key
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">平台设置</div>
      <div class="page-desc">站点、本地存储与微信第三方参数已生效；短信 / 物流 / 平台收款尚未接线</div>
    </div>
  </div>

  <div class="setting-shell">
    <aside class="setting-nav">
      <div
        v-for="section in sections"
        :key="section.key"
        class="item"
        :class="{ active: isActive(section.key) }"
        @click="active = section.key"
      >
        {{ section.label }}
      </div>
    </aside>

    <div class="setting-panel">
      <a-spin :spinning="loading">
        <template v-if="active === 'site'">
          <div class="card card-pad">
            <div class="card-title">基础信息</div>
            <div class="form-row form-item">
              <div>
                <label class="form-label">平台名称</label>
                <input v-model="forms.site.platformName" class="form-input" />
              </div>
              <div>
                <label class="form-label">客服电话</label>
                <input v-model="forms.site.customerPhone" class="form-input" />
              </div>
            </div>
            <div class="form-row form-item">
              <div>
                <label class="form-label">备案号</label>
                <input v-model="forms.site.icpNo" class="form-input" />
              </div>
              <div>
                <label class="form-label">联系邮箱</label>
                <input v-model="forms.site.contactEmail" class="form-input" />
              </div>
            </div>
            <button class="btn btn-primary" :disabled="savingKey === 'site'" @click="saveSection('site')">保存</button>
          </div>
        </template>

        <template v-else-if="active === 'storage'">
          <div class="card card-pad">
            <div class="card-title">存储配置</div>
            <div class="form-item">
              <label class="form-label">对象存储服务商</label>
              <select v-model="forms.storage.provider" class="form-select" style="width: 240px">
                <option value="local">本地存储（当前生效）</option>
                <option value="aliyun-oss">阿里云 OSS（尚未接入）</option>
                <option value="tencent-cos">腾讯云 COS（尚未接入）</option>
              </select>
            </div>
            <div class="form-hint">上传目录由部署环境 STORAGE_DIR 决定，对象存储通道尚未接线。</div>
            <div class="form-row form-item">
              <div>
                <label class="form-label">Bucket</label>
                <input v-model="forms.storage.bucket" class="form-input" />
              </div>
              <div>
                <label class="form-label">Region</label>
                <input v-model="forms.storage.region" class="form-input" />
              </div>
            </div>
            <button class="btn btn-primary" :disabled="savingKey === 'storage'" @click="saveSection('storage')">保存</button>
          </div>
        </template>

        <template v-else-if="active === 'sms'">
          <div class="card card-pad">
            <div class="card-title">短信通道</div>
            <div class="form-item">
              <label class="form-label">服务商</label>
              <select v-model="forms.sms.provider" class="form-select" style="width: 240px">
                <option value="">尚未接入</option>
                <option value="aliyun">阿里云短信</option>
                <option value="tencent">腾讯云短信</option>
              </select>
            </div>
            <div class="form-row form-item">
              <div>
                <label class="form-label">AccessKey</label>
                <input v-model="forms.sms.accessKey" class="form-input" />
              </div>
              <div>
                <label class="form-label">AccessSecret</label>
                <input v-model="forms.sms.accessSecret" class="form-input" />
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">短信签名</label>
              <input v-model="forms.sms.signName" class="form-input" />
            </div>
            <div class="form-hint">短信通道尚未接线，保存后不会真正发信。</div>
            <button class="btn btn-primary" :disabled="savingKey === 'sms'" @click="saveSection('sms')">保存</button>
          </div>
        </template>

        <template v-else-if="active === 'express'">
          <div class="card card-pad">
            <div class="card-title">物流查询</div>
            <div class="form-item">
              <label class="form-label">快递服务商</label>
              <select v-model="forms.express.provider" class="form-select" style="width: 240px">
                <option value="">尚未接入</option>
                <option value="kuaidi100">快递100</option>
                <option value="kdniao">快递鸟</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">API Key</label>
              <input v-model="forms.express.apiKey" class="form-input" />
            </div>
            <div class="form-hint">物流轨迹查询尚未接线，保存后不会调用第三方。</div>
            <button class="btn btn-primary" :disabled="savingKey === 'express'" @click="saveSection('express')">保存</button>
          </div>
        </template>

        <template v-else-if="active === 'wechat'">
          <div class="card card-pad">
            <div class="card-title">微信开放平台第三方</div>
            <div class="form-row form-item">
              <div>
                <label class="form-label">component_appid</label>
                <input v-model="forms.wechat.componentAppId" class="form-input" />
              </div>
              <div>
                <label class="form-label">component_secret</label>
                <input v-model="forms.wechat.componentSecret" class="form-input" type="password" autocomplete="off" />
              </div>
            </div>
            <div class="form-row form-item">
              <div>
                <label class="form-label">消息校验 Token</label>
                <input v-model="forms.wechat.token" class="form-input" type="password" autocomplete="off" />
              </div>
              <div>
                <label class="form-label">EncodingAESKey（43 位）</label>
                <input v-model="forms.wechat.encodingAesKey" class="form-input" />
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">授权事件 URL</label>
              <input v-model="forms.wechat.callbackUrl" class="form-input" placeholder="https://mp.your-domain.com/notify/wechat/component" />
            </div>
            <div class="form-hint">
              填到微信开放平台「授权事件接收 URL」。本地默认走 shop-mp :8085。
              Secret / Token / AESKey 已保存时回显为掩码，不改请保持 ********。
              未配置时商户只能自填 AppID，不能扫码授权。
            </div>
            <button class="btn btn-primary" :disabled="savingKey === 'wechat'" @click="saveSection('wechat')">保存</button>
          </div>
        </template>

        <template v-else>
          <div class="card card-pad">
            <div class="card-title">支付配置</div>
            <div class="form-item">
              <label class="form-label">支付通道</label>
              <select v-model="forms.pay.channel" class="form-select" style="width: 240px">
                <option value="">尚未接入</option>
                <option value="wechat">微信支付（平台收款）</option>
                <option value="alipay">支付宝</option>
              </select>
            </div>
            <div class="form-row form-item">
              <div>
                <label class="form-label">商户号</label>
                <input v-model="forms.pay.mchId" class="form-input" />
              </div>
              <div>
                <label class="form-label">证书序列号</label>
                <input v-model="forms.pay.certSerialNo" class="form-input" />
              </div>
            </div>
            <div class="form-hint">套餐订购目前走线下转账 + 超管确认到账，平台商户号收款尚未接线。</div>
            <button class="btn btn-primary" :disabled="savingKey === 'pay'" @click="saveSection('pay')">保存</button>
          </div>
        </template>
      </a-spin>
    </div>
  </div>
</template>

<style scoped>
.setting-shell {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.setting-nav {
  width: 190px;
  flex-shrink: 0;
}

.item {
  display: block;
  width: 100%;
  padding: 9px 14px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  text-align: left;
  font-size: 13px;
  cursor: pointer;
  margin-bottom: 6px;
}

.item.active {
  background: var(--primary-bg);
  color: var(--primary-hover);
  font-weight: 600;
}

.setting-panel {
  flex: 1;
  min-width: 0;
}
</style>
