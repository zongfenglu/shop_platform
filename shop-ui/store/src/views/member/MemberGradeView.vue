<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { createMemberGrade, deleteMemberGrade, listMemberGrades, updateMemberGrade } from '@/api/memberGrade'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const modalOpen = ref(false)
const editing = ref(null)
const form = reactive({ name: '', weight: 0, growthValue: 0, discountRatio: 1, icon: '', remark: '' })

async function load() {
  loading.value = true
  try {
    rows.value = (await listMemberGrades()) || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function resetForm() {
  Object.assign(form, { name: '', weight: 0, growthValue: 0, discountRatio: 1, icon: '', remark: '' })
}

function openCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    name: row.name || '',
    weight: row.weight ?? 0,
    growthValue: row.growthValue ?? 0,
    discountRatio: row.discountRatio ?? 1,
    icon: row.icon || '',
    remark: row.remark || '',
  })
  modalOpen.value = true
}

function closeModal() {
  if (!saving.value) modalOpen.value = false
}

async function save() {
  if (!form.name.trim()) return message.error('请输入等级名称')
  if (form.discountRatio == null || form.discountRatio <= 0 || form.discountRatio > 1) {
    return message.error('折扣比例须在 (0, 1] 之间，1 表示无折扣')
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      weight: Number(form.weight) || 0,
      growthValue: Number(form.growthValue) || 0,
      discountRatio: Number(form.discountRatio),
      icon: form.icon || null,
      remark: form.remark || null,
    }
    if (editing.value) {
      await updateMemberGrade(editing.value.id, payload)
      message.success('等级已更新')
    } else {
      await createMemberGrade(payload)
      message.success('等级已创建')
    }
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function confirmDelete(row) {
  Modal.confirm({
    title: '删除会员等级？',
    content: `删除「${row.name}」后，引用该等级的会员会在下次升级任务时被重算。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      await deleteMemberGrade(row.id)
      message.success('等级已删除')
      await load()
    },
  })
}

function ratioText(r) {
  if (r == null) return '—'
  return Number(r) >= 1 ? '无折扣' : `${Math.round(Number(r) * 100)}%`
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">会员等级</div>
      <div class="page-desc">按成长值升级，等级折扣将接入价格引擎的 MemberDiscountHandler</div>
    </div>
    <button class="btn btn-primary" @click="openCreate">+ 新建等级</button>
  </div>

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead><tr><th>等级</th><th>权重</th><th>所需成长值</th><th>会员折扣</th><th>备注</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td class="strong">{{ row.name }}</td>
            <td class="num">{{ row.weight }}</td>
            <td class="num">{{ row.growthValue }}</td>
            <td><span class="tag tag-primary">{{ ratioText(row.discountRatio) }}</span></td>
            <td class="muted">{{ row.remark || '—' }}</td>
            <td><button class="btn btn-sm" @click="openEdit(row)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDelete(row)">删除</button></td>
          </tr>
          <tr v-if="!loading && rows.length === 0"><td colspan="6"><div class="empty-state"><div class="icon">◎</div><div>暂无会员等级，建店时已自动生成默认等级</div></div></td></tr>
        </tbody>
      </table>
    </a-spin>
  </div>

  <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
    <div class="modal" style="width: 520px">
      <div class="modal-header"><span>{{ editing ? '编辑等级' : '新建等级' }}</span><button class="modal-close" aria-label="关闭" @click="closeModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>等级名称</label><input v-model="form.name" class="form-input" placeholder="例如：金卡会员" /></div>
        <div class="form-row">
          <div class="form-item"><label class="form-label">权重（越大越高）</label><input v-model.number="form.weight" type="number" min="0" class="form-input" /></div>
          <div class="form-item"><label class="form-label">所需成长值</label><input v-model.number="form.growthValue" type="number" min="0" class="form-input" /></div>
        </div>
        <div class="form-item"><label class="form-label">会员折扣</label><input v-model.number="form.discountRatio" type="number" step="0.01" min="0.01" max="1" class="form-input" placeholder="1=无折扣，0.9=九折" /></div>
        <div class="form-item"><label class="form-label">备注</label><input v-model="form.remark" class="form-input" /></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeModal">取消</button><button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button></div>
    </div>
  </div>
</template>

<style scoped>
.strong { font-weight: 600; }
.muted { color: var(--text-muted); font-size: 12px; }
.num { font-variant-numeric: tabular-nums; }
.btn-danger-outline { color: var(--status-critical); border-color: var(--status-critical); margin-left: 6px; }
.form-row { display: flex; gap: 14px; }
.form-row > .form-item { flex: 1; }
</style>
