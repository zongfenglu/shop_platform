<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  pagePlatformUsers, createPlatformUser, updatePlatformUser,
  resetPlatformUserPassword, togglePlatformUserStatus,
  listPlatformRoles, listPlatformMenus, getRoleMenus, saveRoleMenus,
  pageSysLogs,
} from '@/api/platformUser'

const activeTab = ref(1)
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const summary = ref({ total: 0, activeCount: 0, disabledCount: 0 })

const roles = ref([])

// ---- 角色权限编辑 ----
const permModalOpen = ref(false)
const permRoleId = ref(null)
const permRoleName = ref('')
const allMenus = ref([])
const checkedMenuIds = ref([])

async function loadRoles() {
  try { const res = await listPlatformRoles(); roles.value = (res || []).map(r => ({ ...r, count: 0 })) } catch (e) { /* */ }
}

async function openPermModal(role) {
  permRoleId.value = role.id
  permRoleName.value = role.name
  try { allMenus.value = await listPlatformMenus() || [] } catch (e) { allMenus.value = [] }
  try { checkedMenuIds.value = await getRoleMenus(role.id) || [] } catch (e) { checkedMenuIds.value = [] }
  permModalOpen.value = true
}

async function savePerms() {
  await saveRoleMenus(permRoleId.value, checkedMenuIds.value)
  permModalOpen.value = false
  message.success('权限已保存')
}

const permBuiltin = computed(() => !!roles.value.find((r) => String(r.id) === String(permRoleId.value))?.isBuiltin)

const permGroups = computed(() => {
  const menus = allMenus.value || []
  const byId = new Map()
  for (const m of menus) {
    byId.set(String(m.id), { ...m, children: [] })
  }
  const roots = []
  for (const m of menus) {
    const node = byId.get(String(m.id))
    const pid = m.parentId == null || Number(m.parentId) === 0 ? '0' : String(m.parentId)
    if (pid === '0' || !byId.has(pid)) {
      roots.push(node)
    } else {
      byId.get(pid).children.push(node)
    }
  }
  const bySort = (a, b) => (a.sort || 0) - (b.sort || 0)
  roots.sort(bySort)
  roots.forEach((r) => r.children.sort(bySort))
  return roots
})

function isMenuChecked(id) {
  return checkedMenuIds.value.some((x) => String(x) === String(id))
}

function grantedChildren(group) {
  return (group.children || []).filter((c) => isMenuChecked(c.id))
}

function toggleGroup(group) {
  if (permBuiltin.value) return
  const ids = [group.id, ...(group.children || []).map((c) => c.id)]
  const allOn = ids.every((id) => isMenuChecked(id))
  if (allOn) {
    const drop = new Set(ids.map(String))
    checkedMenuIds.value = checkedMenuIds.value.filter((id) => !drop.has(String(id)))
    return
  }
  for (const id of ids) {
    if (!isMenuChecked(id)) checkedMenuIds.value.push(id)
  }
}

function toggleMenu(id) {
  const idx = checkedMenuIds.value.indexOf(id)
  if (idx >= 0) checkedMenuIds.value.splice(idx, 1)
  else checkedMenuIds.value.push(id)
}

const latestLogin = computed(() => {
  const times = rows.value.map((r) => r.lastLoginTime).filter(Boolean).sort().reverse()
  return times[0] ? String(times[0]).replace('T', ' ').slice(11, 16) : '—'
})

const logLoading = ref(false)
const logRows = ref([])
const logTotal = ref(0)

async function loadLogs() {
  logLoading.value = true
  try {
    const page = await pageSysLogs({
      kind: activeTab.value === 4 ? 'login' : 'op',
      pageNum: 1,
      pageSize: 50,
    })
    logRows.value = page?.records || []
    logTotal.value = page?.total || 0
  } catch (e) {
    logRows.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 3 || tab === 4) loadLogs()
})

function actionText(action) {
  return ({
    login: '登录',
    'confirm-paid': '确认到账',
    'domain-approve': '域名通过',
    'domain-reject': '域名驳回',
    impersonate: '免密登录',
    'reset-owner-password': '重置店主密码',
    'shop-disable': '停用商城',
    'shop-enable': '启用商城',
    'package-order': '套餐订购',
    'shop-expire-remind': '到期提醒',
    'invoice-apply': '申请发票',
    'invoice-issue': '开票',
    'invoice-reject': '驳回发票',
    'db-backup': '数据库备份',
    'cache-flush': '清除缓存',
  })[action] || action || '—'
}

function operatorTypeText(type) {
  return Number(type) === 1 ? '平台' : Number(type) === 2 ? '商户' : '—'
}

// (roles/loadRoles 已在上面定义)

// ---- 新建/编辑弹窗 ----
const modalOpen = ref(false)
const editingId = ref(null)
const form = reactive({ username: '', password: '', realName: '', mobile: '', status: 1, roleId: null })

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', password: '', realName: '', mobile: '', status: 1, roleId: roles.value[0]?.id || null })
  modalOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username, password: '',
    realName: row.realName || '', mobile: row.mobile || '',
    status: row.status, roleId: row.roleId || null,
  })
  modalOpen.value = true
}

function closeModal() { modalOpen.value = false }

async function submitForm() {
  if (!form.username.trim()) return
  const payload = {
    username: form.username.trim(),
    password: form.password || '',
    realName: form.realName.trim(),
    mobile: form.mobile.trim(),
    status: Number(form.status),
    roleId: form.roleId || null,
  }
  if (editingId.value) {
    await updatePlatformUser(editingId.value, { ...payload, password: form.password || undefined })
  } else {
    if (!form.password) { alert('请填写密码'); return }
    await createPlatformUser(payload)
  }
  closeModal()
  await load()
}

const resetOpen = ref(false)
const resetTarget = ref(null)
const resetPwd = ref('')
const resetSubmitting = ref(false)

function openReset(row) {
  resetTarget.value = row
  resetPwd.value = ''
  resetOpen.value = true
}

function closeReset() {
  resetOpen.value = false
  resetTarget.value = null
  resetPwd.value = ''
}

async function submitReset() {
  if (!resetTarget.value) return
  resetSubmitting.value = true
  try {
    await resetPlatformUserPassword(resetTarget.value.id, resetPwd.value.trim() || '123456')
    message.success('密码已重置')
    closeReset()
  } catch (e) {
    // http 拦截器已提示
  } finally {
    resetSubmitting.value = false
  }
}

async function onToggleStatus(row) {
  if (row.username === 'admin') { alert('不能禁用超级管理员'); return }
  await togglePlatformUserStatus(row.id)
  await load()
}

// ----
onMounted(() => { load(); loadRoles() })

async function load() {
  loading.value = true
  try {
    const page = await pagePlatformUsers({ pageNum: 1, pageSize: 50 })
    rows.value = page?.records || []
    total.value = page?.total || 0
    summary.value = page?.summary || summary.value
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function fmtTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 19)
}

function statusClass(status) {
  return Number(status) === 1 ? 'tag-good' : 'tag-critical'
}

function statusText(status) {
  return Number(status) === 1 ? '正常' : '停用'
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">平台账号</div>
      <div class="page-desc">管理平台管理员账号、角色权限与操作审计</div>
    </div>
    <button class="btn btn-primary" @click="openCreate">+ 新建管理员</button>
  </div>

  <div class="grid grid-4" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">账号总数</div><div class="kpi-value">{{ summary.total || total }}</div></div>
    <div class="card kpi"><div class="kpi-label">正常账号</div><div class="kpi-value">{{ summary.activeCount }}</div></div>
    <div class="card kpi"><div class="kpi-label">停用账号</div><div class="kpi-value" style="color: var(--status-critical)">{{ summary.disabledCount }}</div></div>
    <div class="card kpi"><div class="kpi-label">最近登录</div><div class="kpi-value" style="font-size: 16px">{{ latestLogin }}</div></div>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <div class="tab" :class="{ active: activeTab === 1 }" @click="activeTab = 1">管理员账号</div>
      <div class="tab" :class="{ active: activeTab === 2 }" @click="activeTab = 2">角色权限</div>
      <div class="tab" :class="{ active: activeTab === 3 }" @click="activeTab = 3">操作日志</div>
      <div class="tab" :class="{ active: activeTab === 4 }" @click="activeTab = 4">登录日志</div>
    </div>

    <template v-if="activeTab === 1">
      <a-spin :spinning="loading">
        <table class="table">
          <thead><tr><th>账号</th><th>角色</th><th>手机号</th><th>最近登录</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-if="!rows.length && !loading">
              <td colspan="6" style="text-align: center; color: var(--text-muted)">暂无管理员账号</td>
            </tr>
            <tr v-for="row in rows" :key="row.id">
              <td>
                <div class="user-cell">
                  <div class="user-avatar">{{ row.username?.slice(0, 1)?.toUpperCase() || '平' }}</div>
                  <div>
                    <div style="font-weight: 600">{{ row.username }}</div>
                    <div style="font-size: 12px; color: var(--text-muted)">{{ row.realName }}</div>
                  </div>
                </div>
              </td>
              <td><span class="tag tag-primary">{{ row.roleName }}</span></td>
              <td>{{ row.mobile || '—' }}</td>
              <td>{{ fmtTime(row.lastLoginTime) }}</td>
              <td><span class="tag" :class="statusClass(row.status)">{{ statusText(row.status) }}</span></td>
              <td>
                <button class="btn btn-sm" @click="openEdit(row)">编辑</button>
                <button class="btn btn-sm" style="margin-left:4px" @click="openReset(row)">重置密码</button>
                <button class="btn btn-sm" style="margin-left:4px" @click="onToggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="form-hint" style="margin-top: 12px">共 {{ total }} 条</div>
      </a-spin>
    </template>

    <template v-else-if="activeTab === 2">
      <table class="table">
        <thead><tr><th>角色名称</th><th>说明</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in roles" :key="row.id">
            <td>{{ row.name }}<span v-if="row.isBuiltin" class="tag tag-muted" style="margin-left:8px">内置</span></td>
            <td>{{ row.remark }}</td>
            <td><button class="btn btn-sm" @click="openPermModal(row)">{{ row.isBuiltin ? '查看权限' : '编辑权限' }}</button></td>
          </tr>
        </tbody>
      </table>
    </template>

    <template v-else>
      <a-spin :spinning="logLoading">
        <table class="table">
          <thead>
            <tr>
              <th>时间</th>
              <th>操作人</th>
              <th>类型</th>
              <th>动作</th>
              <th>说明</th>
              <th>商城</th>
              <th>IP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!logRows.length && !logLoading">
              <td colspan="7" style="text-align: center; color: var(--text-muted)">暂无记录</td>
            </tr>
            <tr v-for="row in logRows" :key="row.id">
              <td>{{ fmtTime(row.createTime) }}</td>
              <td>
                {{ row.operatorName || '—' }}
                <span v-if="row.byPlatform" class="tag tag-muted" style="margin-left:6px">代管</span>
              </td>
              <td>{{ operatorTypeText(row.operatorType) }}</td>
              <td>{{ actionText(row.action) }}</td>
              <td>{{ row.description || '—' }}</td>
              <td>{{ row.shopName || (row.shopId ? '#' + row.shopId : '—') }}</td>
              <td>{{ row.ip || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <div class="form-hint" style="margin-top: 12px">共 {{ logTotal }} 条</div>
      </a-spin>
    </template>
  </div>

  <!-- 角色权限弹窗：内置角色只读分组展示，自定义角色可勾选 -->
  <div v-if="permModalOpen" class="modal-mask" @click.self="permModalOpen = false">
    <div class="modal" style="width: 560px">
      <div class="modal-header">
        <span>{{ permBuiltin ? '查看权限' : '编辑权限' }} — {{ permRoleName }}</span>
        <button class="modal-close" type="button" @click="permModalOpen = false">×</button>
      </div>
      <div class="modal-body" style="max-height: 60vh; overflow-y: auto; padding-top: 12px">
        <div v-if="permBuiltin" class="form-hint" style="margin: 0 0 14px">内置角色权限由系统预置，仅可查看。</div>
        <div v-for="group in permGroups" :key="group.id" class="perm-group">
          <div class="perm-group-hd">
            <label v-if="!permBuiltin" class="perm-check">
              <input type="checkbox" :checked="isMenuChecked(group.id)" @change="toggleGroup(group)" />
              <span>{{ group.icon ? group.icon + ' ' : '' }}{{ group.name }}</span>
            </label>
            <span v-else class="perm-group-title">{{ group.icon ? group.icon + ' ' : '' }}{{ group.name }}</span>
            <span class="perm-count">
              {{ group.children.length ? `${grantedChildren(group).length}/${group.children.length}` : (isMenuChecked(group.id) ? '已开通' : '未开通') }}
            </span>
          </div>
          <div v-if="group.children.length" class="perm-children">
            <template v-if="permBuiltin">
              <span
                v-for="child in group.children"
                :key="child.id"
                class="perm-chip"
                :class="isMenuChecked(child.id) ? 'on' : 'off'"
              >{{ child.name }}</span>
            </template>
            <label v-else v-for="child in group.children" :key="child.id" class="perm-check perm-child">
              <input type="checkbox" :checked="isMenuChecked(child.id)" @change="toggleMenu(child.id)" />
              <span>{{ child.name }}</span>
            </label>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" type="button" @click="permModalOpen = false">{{ permBuiltin ? '关闭' : '取消' }}</button>
        <button v-if="!permBuiltin" class="btn btn-primary" type="button" @click="savePerms">保存</button>
      </div>
    </div>
  </div>

  <!-- 重置管理员密码：取消只关窗，不请求接口 -->
  <div v-if="resetOpen" class="modal-mask" @click.self="closeReset">
    <div class="modal" style="width: 420px">
      <div class="modal-header">
        <span>重置密码 — {{ resetTarget?.username }}</span>
        <button class="modal-close" type="button" @click="closeReset">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label">新密码</label>
          <input v-model="resetPwd" class="form-input" type="password" placeholder="留空则重置为 123456" @keyup.enter="submitReset" />
          <div class="form-hint">点取消不会改密码。确认时若未填写，将重置为默认密码 123456。</div>
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

  <!-- 新建/编辑弹窗 -->
  <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
    <div class="modal" style="width: 440px">
      <div class="modal-header"><span>{{ editingId ? '编辑管理员' : '新建管理员' }}</span><button class="modal-close" @click="closeModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>用户名</label><input v-model="form.username" class="form-input" placeholder="登录账号" /></div>
        <div class="form-item" style="margin-top:12px"><label class="form-label">角色</label><select v-model="form.roleId" class="form-select"><option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}{{ r.isBuiltin ? '（内置）' : '' }}</option></select></div>
        <div class="form-item" style="margin-top:12px"><label class="form-label">{{ editingId ? '新密码（留空不修改）' : '* 密码' }}</label><input v-model="form.password" type="password" class="form-input" :placeholder="editingId ? '留空则不修改密码' : '登录密码'" /></div>
        <div class="form-item" style="margin-top:12px"><label class="form-label">真实姓名</label><input v-model="form.realName" class="form-input" placeholder="真实姓名" /></div>
        <div class="form-item" style="margin-top:12px"><label class="form-label">手机号</label><input v-model="form.mobile" class="form-input" placeholder="手机号" /></div>
        <div class="form-item" style="margin-top:12px"><label class="form-label">状态</label><select v-model="form.status" class="form-select"><option :value="1">正常</option><option :value="0">停用</option></select></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeModal">取消</button><button class="btn btn-primary" @click="submitForm">保存</button></div>
    </div>
  </div>
</template>

<style scoped>
.user-cell { display: flex; gap: 10px; align-items: center; }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, #ffb199, #e34948); color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.perm-group { padding: 12px 0; border-bottom: 1px dashed var(--gridline); }
.perm-group:last-child { border-bottom: none; }
.perm-group-hd { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.perm-group-title { font-weight: 600; font-size: 13px; }
.perm-count { font-size: 12px; color: var(--text-muted); flex-shrink: 0; }
.perm-children { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; padding-left: 2px; }
.perm-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
}
.perm-chip.on { background: var(--status-good-bg); color: var(--status-good-text); }
.perm-chip.off { background: var(--surface-2); color: var(--text-muted); }
.perm-check { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; cursor: pointer; }
.perm-child { min-width: 42%; }
</style>
