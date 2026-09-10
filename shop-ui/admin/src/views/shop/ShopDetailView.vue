<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { disableShop, enableShop, getShop, impersonateShop, resetShopOwnerPassword, updateShop } from '@/api/shop'

/**
 * 商城详情。对照原型 admin/shop-detail.html。
 * 免密登录：一次性 ticket（5 分钟、单次使用）打开商户后台登录页兑换 JWT。
 */
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const impersonating = ref(false)
const shop = ref(null)

const resetOpen = ref(false)
const resetPwd = ref('')
const resetSubmitting = ref(false)

const disableOpen = ref(false)
const disableReason = ref('')
const disableSubmitting = ref(false)
const enableSubmitting = ref(false)

// ---- 编辑商城信息 ----
const editOpen = ref(false)
const editSubmitting = ref(false)
const editForm = reactive({ name: '', code: '', industry: '', contact: '', mobile: '', remark: '' })

const INDUSTRY_OPTIONS = ['服饰鞋包', '生鲜食品', '美妆个护', '3C数码', '其他']

function openEdit() {
  if (!shop.value) return
  editForm.name = shop.value.name || ''
  editForm.code = shop.value.code || ''
  editForm.industry = shop.value.industry || ''
  editForm.contact = shop.value.contact || ''
  editForm.mobile = shop.value.mobile || ''
  editForm.remark = shop.value.remark || ''
  editOpen.value = true
}

function closeEdit() {
  editOpen.value = false
}

async function submitEdit() {
  if (!editForm.name.trim()) { message.error('商城名称不能为空'); return }
  if (!/^[a-z0-9-]{3,32}$/.test(editForm.code)) {
    message.error('域名前缀仅支持小写字母、数字、短横线，长度 3-32')
    return
  }
  editSubmitting.value = true
  try {
    await updateShop(shop.value.id, {
      name: editForm.name.trim() || null,
      code: editForm.code.trim() || null,
      industry: editForm.industry || null,
      contact: editForm.contact.trim() || null,
      mobile: editForm.mobile.trim() || null,
      remark: editForm.remark.trim() || null,
    })
    message.success('商城信息已更新')
    closeEdit()
    await load()
  } catch (e) {
    // http 拦截器已提示
  } finally {
    editSubmitting.value = false
  }
}

const STATUS_META = {
  trial: { text: '试用中', cls: 'tag-good' },
  normal: { text: '正常', cls: 'tag-good' },
  expired: { text: '已过期', cls: 'tag-critical' },
  disabled: { text: '已停用', cls: 'tag-critical' },
  archived: { text: '已归档', cls: 'tag-muted' },
}

async function load() {
  loading.value = true
  try {
    shop.value = await getShop(route.params.id)
  } catch (e) {
    shop.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)

function statusOf() {
  if (!shop.value) return { text: '—', cls: 'tag-muted' }
  return STATUS_META[shop.value.status] || { text: shop.value.status, cls: 'tag-muted' }
}

function fmtDateTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '—'
}

function goList() {
  router.push({ name: 'shops' })
}

function canImpersonate() {
  return shop.value && ['trial', 'normal', 'expired'].includes(shop.value.status)
}

const isDisabled = computed(() => shop.value?.status === 'disabled')
const isArchived = computed(() => shop.value?.status === 'archived')

async function onImpersonate() {
  if (!shop.value || impersonating.value) return
  impersonating.value = true
  try {
    const data = await impersonateShop(shop.value.id)
    window.open(data.loginUrl, '_blank')
    message.success('已打开商户后台（5 分钟内有效，一次性）')
  } catch (e) {
    // http 拦截器已提示
  } finally {
    impersonating.value = false
  }
}

function openReset() {
  resetPwd.value = ''
  resetOpen.value = true
}

function closeReset() {
  resetOpen.value = false
}

async function submitReset() {
  const pwd = resetPwd.value.trim()
  if (pwd.length < 8 || pwd.length > 72) {
    message.error('密码长度需为 8～72 位')
    return
  }
  resetSubmitting.value = true
  try {
    await resetShopOwnerPassword(shop.value.id, pwd)
    message.success('店主密码已重置')
    closeReset()
  } catch (e) {
    // http 拦截器已提示
  } finally {
    resetSubmitting.value = false
  }
}

function openDisable() {
  disableReason.value = ''
  disableOpen.value = true
}

function closeDisable() {
  disableOpen.value = false
}

async function submitDisable() {
  const reason = disableReason.value.trim()
  if (!reason) {
    message.error('请填写停用原因')
    return
  }
  disableSubmitting.value = true
  try {
    await disableShop(shop.value.id, reason)
    message.success('商城已停用')
    closeDisable()
    await load()
  } catch (e) {
    // http 拦截器已提示
  } finally {
    disableSubmitting.value = false
  }
}

async function onEnable() {
  if (!shop.value || enableSubmitting.value) return
  enableSubmitting.value = true
  try {
    await enableShop(shop.value.id)
    message.success('商城已启用')
    await load()
  } catch (e) {
    // http 拦截器已提示
  } finally {
    enableSubmitting.value = false
  }
}

function previewH5() {
  if (!shop.value) return
  const h5Url = 'http://localhost:5175'
  const previewUrl = `${h5Url}/?_shopId=${shop.value.id}`
  const preview = window.open(previewUrl, '_blank')
  if (preview) {
    preview.addEventListener('load', () => {
      preview.postMessage({ type: 'setShopId', shopId: shop.value.id }, h5Url)
    })
  }
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="breadcrumb" style="margin-bottom: 6px">
        <a @click="goList" style="cursor: pointer">商城管理</a> ／ <b>{{ shop?.name || '商城详情' }}</b>
      </div>
      <div class="page-title">{{ shop?.name || '加载中…' }}</div>
      <div v-if="shop" class="page-desc">{{ shop.code }} · 创建于 {{ fmtDateTime(shop.createTime) }}</div>
    </div>
    <div style="display: flex; gap: 10px">
      <button class="btn btn-primary" :disabled="!shop" @click="openEdit">编辑信息</button>
      <button class="btn" :disabled="!shop" @click="openReset">重置密码</button>
      <a-tooltip v-if="isArchived" title="已归档的商城不能停用或启用">
        <button class="btn" disabled>停用商城</button>
      </a-tooltip>
      <button v-else-if="isDisabled" class="btn" :disabled="enableSubmitting" @click="onEnable">
        {{ enableSubmitting ? '启用中…' : '启用商城' }}
      </button>
      <button v-else class="btn" :disabled="!shop" @click="openDisable">停用商城</button>
      <a-tooltip :title="canImpersonate() ? '一次性链接，5 分钟有效' : '停用或归档的商城无法免密登录'">
        <button class="btn btn-primary" :disabled="!canImpersonate() || impersonating" @click="onImpersonate">
          {{ impersonating ? '正在打开…' : '🔑 免密登录商户后台' }}
        </button>
      </a-tooltip>
      <button class="btn" :disabled="!shop" @click="previewH5">👁️ 预览 H5</button>
    </div>
  </div>

  <a-spin :spinning="loading">
    <div v-if="shop" class="card card-pad">
      <p class="card-title">基本信息</p>
      <div class="kv-row"><div class="k">商城编号</div><div class="v">{{ shop.id }}</div></div>
      <div class="kv-row"><div class="k">商城名称</div><div class="v">{{ shop.name }}</div></div>
      <div class="kv-row"><div class="k">域名前缀</div><div class="v">{{ shop.code }}</div></div>
      <div class="kv-row"><div class="k">所属行业</div><div class="v">{{ shop.industry || '—' }}</div></div>
      <div class="kv-row"><div class="k">联系人</div><div class="v">{{ shop.contact || '—' }}</div></div>
      <div class="kv-row"><div class="k">联系手机</div><div class="v">{{ shop.mobile || '—' }}</div></div>
      <div class="kv-row"><div class="k">状态</div><div class="v"><span class="tag tag-dot" :class="statusOf().cls">{{ statusOf().text }}</span></div></div>
      <div class="kv-row"><div class="k">到期时间</div><div class="v">{{ fmtDateTime(shop.expireTime) }}</div></div>
      <div class="kv-row"><div class="k">内部备注</div><div class="v">{{ shop.remark || '—' }}</div></div>
    </div>
    <div v-else-if="!loading" class="card card-pad" style="text-align: center; color: var(--text-muted)">
      商城不存在或已被删除
    </div>
  </a-spin>

  <div v-if="resetOpen" class="modal-mask" @click.self="closeReset">
    <div class="modal">
      <div class="modal-header">
        <span>重置店主密码</span>
        <button class="modal-close" type="button" @click="closeReset">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>新密码</label>
          <input v-model="resetPwd" class="form-input" type="password" placeholder="8～72 位" @keyup.enter="submitReset" />
          <div class="form-hint">将重置该商城超级店主账号的登录密码，不影响员工账号。</div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" type="button" @click="closeReset">取消</button>
        <button class="btn btn-primary" type="button" :disabled="resetSubmitting" @click="submitReset">
          {{ resetSubmitting ? '提交中…' : '确认重置' }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="disableOpen" class="modal-mask" @click.self="closeDisable">
    <div class="modal">
      <div class="modal-header">
        <span>停用商城</span>
        <button class="modal-close" type="button" @click="closeDisable">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>停用原因</label>
          <textarea
            v-model="disableReason"
            class="form-textarea"
            rows="3"
            placeholder="例如：违规经营 / 租户申请下线 / 欠费超过宽限期"
          />
          <div class="form-hint">停用后商户后台无法登录，用户端小程序/H5 不可访问。可随时再启用。</div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" type="button" @click="closeDisable">取消</button>
        <button class="btn btn-danger" type="button" :disabled="disableSubmitting" @click="submitDisable">
          {{ disableSubmitting ? '提交中…' : '确认停用' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kv-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px dashed var(--gridline);
  font-size: 13px;
}
.kv-row:last-child {
  border-bottom: none;
}
.kv-row .k {
  width: 120px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.kv-row .v {
  font-weight: 500;
}
</style>
