<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { createStaff, createStaffRole, deleteStaff, deleteStaffRole, listStaffRoles, listStaffUsers, resetStaffPassword, updateStaff, updateStaffRole } from '@/api/staff'
import { listOfflineStores } from '@/api/offlineStore'

const loading = ref(false)
const saving = ref(false)
const users = ref([])
const roles = ref([])
const offlineStores = ref([])
const activeTab = ref('users')
const modalOpen = ref(false)
const editing = ref(null)
const passwordModalOpen = ref(false)
const passwordTarget = ref(null)
const roleModalOpen = ref(false)
const editingRole = ref(null)
const form = reactive({ username: '', password: '', realName: '', mobile: '', roleId: null, status: 1, storeOfflineId: null })
const passwordForm = reactive({ password: '', confirmPassword: '' })
const roleForm = reactive({ name: '', dataScope: 'all' })

const selectedRoleIsStoreScoped = computed(() => roles.value.find((role) => role.id === form.roleId)?.dataScope === 'store')

async function load() {
  loading.value = true
  try {
    const [userRows, roleRows, storeRows] = await Promise.all([listStaffUsers(), listStaffRoles(), listOfflineStores()])
    users.value = userRows || []
    roles.value = roleRows || []
    offlineStores.value = storeRows || []
  } catch (e) {
    users.value = []
    roles.value = []
    offlineStores.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function resetForm() {
  Object.assign(form, { username: '', password: '', realName: '', mobile: '', roleId: roles.value.find((role) => !role.builtin)?.id || roles.value[0]?.id || null, status: 1, storeOfflineId: null })
}

function openCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(user) {
  editing.value = user
  Object.assign(form, { username: user.username || '', password: '', realName: user.realName || '', mobile: user.mobile || '', roleId: user.roleId, status: user.status === 0 ? 0 : 1, storeOfflineId: user.storeOfflineId || null })
  modalOpen.value = true
}

function closeModal() {
  if (!saving.value) modalOpen.value = false
}

async function save() {
  if (!form.realName.trim()) return message.error('请输入员工姓名')
  if (!form.roleId) return message.error('请选择角色')
  if (!editing.value) {
    if (!form.username.trim()) return message.error('请输入登录账号')
    if (form.password.length < 8) return message.error('初始密码至少 8 位')
  }
  if (selectedRoleIsStoreScoped.value && !form.storeOfflineId) return message.error('门店店员角色必须指定所属门店')
  saving.value = true
  try {
    if (editing.value) {
      await updateStaff(editing.value.id, { realName: form.realName, mobile: form.mobile, roleId: form.roleId, status: form.status, storeOfflineId: form.storeOfflineId })
      message.success('员工资料已更新')
    } else {
      await createStaff({ username: form.username, password: form.password, realName: form.realName, mobile: form.mobile, roleId: form.roleId, storeOfflineId: form.storeOfflineId })
      message.success('员工账号已创建')
    }
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openPassword(user) {
  passwordTarget.value = user
  Object.assign(passwordForm, { password: '', confirmPassword: '' })
  passwordModalOpen.value = true
}

function openRoleCreate() {
  editingRole.value = null
  Object.assign(roleForm, { name: '', dataScope: 'all' })
  roleModalOpen.value = true
}

function openRoleEdit(role) {
  editingRole.value = role
  Object.assign(roleForm, { name: role.name, dataScope: role.dataScope || 'all' })
  roleModalOpen.value = true
}

async function saveRole() {
  if (!roleForm.name.trim()) return message.error('请输入角色名称')
  saving.value = true
  try {
    if (editingRole.value) {
      await updateStaffRole(editingRole.value.id, roleForm)
      message.success('角色已更新')
    } else {
      await createStaffRole(roleForm)
      message.success('角色已创建')
    }
    roleModalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function confirmDeleteRole(role) {
  Modal.confirm({
    title: '删除角色？',
    content: role.userCount ? `该角色仍关联 ${role.userCount} 名员工，删除前请先调整员工角色。` : `删除角色“${role.name}”后不可恢复。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true, disabled: role.userCount > 0 },
    async onOk() {
      await deleteStaffRole(role.id)
      message.success('角色已删除')
      await load()
    },
  })
}

async function savePassword() {
  if (passwordForm.password.length < 8) return message.error('新密码至少 8 位')
  if (passwordForm.password !== passwordForm.confirmPassword) return message.error('两次输入的密码不一致')
  saving.value = true
  try {
    await resetStaffPassword(passwordTarget.value.id, passwordForm.password)
    passwordModalOpen.value = false
    message.success('密码已重置')
  } finally {
    saving.value = false
  }
}

function confirmDelete(user) {
  Modal.confirm({
    title: '删除员工账号？',
    content: `账号“${user.username}”删除后将无法登录。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      await deleteStaff(user.id)
      message.success('员工账号已删除')
      await load()
    },
  })
}

function statusText(user) {
  return user.status === 1 ? { text: '正常', cls: 'tag-good' } : { text: '已停用', cls: 'tag-critical' }
}

function displayName(user) {
  return user.realName || user.username
}

function dateText(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <div class="page-header">
    <div><div class="page-title">员工管理</div><div class="page-desc">管理登录账号、角色和访问状态</div></div>
    <button v-if="activeTab === 'users'" class="btn btn-primary" @click="openCreate">+ 新建员工</button>
  </div>

  <div class="card card-pad">
    <div class="tabs" style="margin-bottom: 18px">
      <button class="tab" :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">员工账号 <span class="count">({{ users.length }})</span></button>
      <button class="tab" :class="{ active: activeTab === 'roles' }" @click="activeTab = 'roles'">角色权限 <span class="count">({{ roles.length }})</span></button>
    </div>

    <a-spin :spinning="loading">
      <table v-if="activeTab === 'users'" class="table">
        <thead><tr><th>员工</th><th>登录账号</th><th>角色</th><th>最近登录</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td><div class="user-cell"><div class="user-avatar">{{ displayName(user).slice(0, 1) }}</div><div><div class="strong">{{ displayName(user) }}</div><div class="muted">{{ user.mobile || '未填写手机号' }}</div></div></div></td>
            <td class="num">{{ user.username }}</td>
            <td><span class="tag" :class="user.superOwner ? 'tag-primary' : 'tag-muted'">{{ user.superOwner ? '超级店主' : (user.roleName || '未分配') }}</span></td>
            <td class="num">{{ dateText(user.lastLoginTime) }}</td>
            <td><span class="tag" :class="statusText(user).cls">{{ statusText(user).text }}</span></td>
            <td><template v-if="!user.superOwner"><button class="btn btn-sm" @click="openEdit(user)">编辑</button><button class="btn btn-sm" @click="openPassword(user)">重置密码</button><button class="btn btn-sm btn-danger-outline" @click="confirmDelete(user)">删除</button></template><span v-else class="muted">系统账号</span></td>
          </tr>
          <tr v-if="!loading && users.length === 0"><td colspan="6"><div class="empty-state"><div class="icon">◎</div><div>暂无员工账号</div></div></td></tr>
        </tbody>
      </table>

      <div v-else>
        <div class="role-toolbar"><div class="card-title" style="margin: 0">角色列表</div><button class="btn btn-sm btn-primary" @click="openRoleCreate">+ 新建角色</button></div>
        <table class="table">
        <thead><tr><th>角色名称</th><th>数据范围</th><th>类型</th><th>关联员工</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="role in roles" :key="role.id"><td class="strong">{{ role.name }}</td><td>{{ role.dataScope === 'store' ? '指定门店' : '全部数据' }}</td><td><span class="tag" :class="role.builtin ? 'tag-primary' : 'tag-muted'">{{ role.builtin ? '系统内置' : '自定义' }}</span></td><td class="num">{{ role.userCount }}</td><td><template v-if="!role.builtin"><button class="btn btn-sm" @click="openRoleEdit(role)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDeleteRole(role)">删除</button></template><span v-else class="muted">系统账号</span></td></tr>
          <tr v-if="!loading && roles.length === 0"><td colspan="5"><div class="empty-state"><div class="icon">◎</div><div>暂无角色</div></div></td></tr>
        </tbody>
        </table>
      </div>
    </a-spin>
  </div>

  <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
    <div class="modal staff-modal">
      <div class="modal-header"><span>{{ editing ? '编辑员工' : '新建员工' }}</span><button class="modal-close" aria-label="关闭" @click="closeModal">×</button></div>
      <div class="modal-body">
        <div v-if="!editing" class="form-item"><label class="form-label"><span class="req">*</span>登录账号</label><input v-model="form.username" class="form-input" placeholder="用于员工登录" /></div>
        <div v-else class="form-item"><label class="form-label">登录账号</label><input :value="form.username" class="form-input" disabled /></div>
        <div v-if="!editing" class="form-item"><label class="form-label"><span class="req">*</span>初始密码</label><input v-model="form.password" type="password" class="form-input" autocomplete="new-password" placeholder="至少 8 位" /></div>
        <div class="form-row form-item"><div><label class="form-label"><span class="req">*</span>姓名</label><input v-model="form.realName" class="form-input" /></div><div><label class="form-label">手机号</label><input v-model="form.mobile" class="form-input" /></div></div>
        <div class="form-item"><label class="form-label"><span class="req">*</span>角色</label><select v-model="form.roleId" class="form-select"><option v-for="role in roles" :key="role.id" :value="role.id" :disabled="role.builtin">{{ role.name }}{{ role.builtin ? '（内置）' : '' }}</option></select></div>
        <div v-if="selectedRoleIsStoreScoped" class="form-item"><label class="form-label"><span class="req">*</span>所属门店</label><select v-model="form.storeOfflineId" class="form-select"><option :value="null" disabled>请选择门店</option><option v-for="store in offlineStores" :key="store.id" :value="store.id">{{ store.name }}</option></select></div>
        <div v-if="editing" class="form-item"><label class="form-label">账号状态</label><select v-model.number="form.status" class="form-select"><option :value="1">正常</option><option :value="0">停用</option></select></div>
        <div class="form-hint">密码只在保存时提交并由后端加密存储，页面不会回显。</div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeModal">取消</button><button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button></div>
    </div>
  </div>

  <div v-if="passwordModalOpen" class="modal-mask" @click.self="passwordModalOpen = false">
    <div class="modal staff-modal">
      <div class="modal-header"><span>重置密码 · {{ passwordTarget?.username }}</span><button class="modal-close" aria-label="关闭" @click="passwordModalOpen = false">×</button></div>
      <div class="modal-body"><div class="form-item"><label class="form-label">新密码</label><input v-model="passwordForm.password" type="password" class="form-input" autocomplete="new-password" placeholder="至少 8 位" /></div><div class="form-item"><label class="form-label">确认密码</label><input v-model="passwordForm.confirmPassword" type="password" class="form-input" autocomplete="new-password" /></div></div>
      <div class="modal-footer"><button class="btn" @click="passwordModalOpen = false">取消</button><button class="btn btn-primary" :disabled="saving" @click="savePassword">确认重置</button></div>
    </div>
  </div>

  <div v-if="roleModalOpen" class="modal-mask" @click.self="roleModalOpen = false">
    <div class="modal staff-modal">
      <div class="modal-header"><span>{{ editingRole ? '编辑角色' : '新建角色' }}</span><button class="modal-close" aria-label="关闭" @click="roleModalOpen = false">×</button></div>
      <div class="modal-body"><div class="form-item"><label class="form-label"><span class="req">*</span>角色名称</label><input v-model="roleForm.name" class="form-input" placeholder="例如：运营专员" /></div><div class="form-item"><label class="form-label">数据范围</label><select v-model="roleForm.dataScope" class="form-select"><option value="all">全部数据</option><option value="store">指定门店</option></select></div><div class="form-hint">当前版本先管理角色范围；细粒度菜单权限沿用角色模板，后续可继续配置。</div></div>
      <div class="modal-footer"><button class="btn" @click="roleModalOpen = false">取消</button><button class="btn btn-primary" :disabled="saving" @click="saveRole">{{ saving ? '保存中…' : '保存' }}</button></div>
    </div>
  </div>
</template>

<style scoped>
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #ffb199, #e34948); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; flex-shrink: 0; }
.strong { font-weight: 600; }
.muted { color: var(--text-muted); font-size: 12px; }
.num { font-variant-numeric: tabular-nums; }
.tab { border: 0; background: transparent; cursor: pointer; }
.btn-danger-outline { color: var(--status-critical); border-color: var(--status-critical); margin-left: 6px; }
.staff-modal { width: 560px; }
.form-hint { color: var(--text-muted); font-size: 12px; line-height: 1.5; }
.role-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
</style>
