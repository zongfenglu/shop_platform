<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { createRechargePlan, deleteRechargePlan, listRechargePlans, updateRechargePlan } from '@/api/rechargePlan'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const modalOpen = ref(false)
const editing = ref(null)
const form = reactive({ money: 0, giftMoney: 0, giftPoints: 0, isShow: 1, sort: 0 })

async function load() {
  loading.value = true
  try {
    rows.value = (await listRechargePlans()) || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function resetForm() {
  Object.assign(form, { money: 0, giftMoney: 0, giftPoints: 0, isShow: 1, sort: 0 })
}

function openCreate() {
  editing.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    money: Number(row.money) || 0,
    giftMoney: Number(row.giftMoney) || 0,
    giftPoints: row.giftPoints ?? 0,
    isShow: row.isShow ?? 1,
    sort: row.sort ?? 0,
  })
  modalOpen.value = true
}

function closeModal() {
  if (!saving.value) modalOpen.value = false
}

async function save() {
  if (!(Number(form.money) > 0)) return message.error('实付金额必须大于 0')
  saving.value = true
  try {
    const payload = {
      money: Number(form.money),
      giftMoney: Number(form.giftMoney) || 0,
      giftPoints: Number(form.giftPoints) || 0,
      isShow: Number(form.isShow),
      sort: Number(form.sort) || 0,
    }
    if (editing.value) {
      await updateRechargePlan(editing.value.id, payload)
      message.success('方案已更新')
    } else {
      await createRechargePlan(payload)
      message.success('方案已创建')
    }
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function confirmDelete(row) {
  Modal.confirm({
    title: '删除充值方案？',
    content: `方案「${row.money} 元」删除后不可恢复，已生成的充值订单不受影响。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      await deleteRechargePlan(row.id)
      message.success('方案已删除')
      await load()
    },
  })
}

function money(row) {
  return Number(row.money || 0).toFixed(2)
}
function gift(row) {
  const g = Number(row.giftMoney || 0)
  return g > 0 ? `+${g.toFixed(2)}` : '—'
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">充值方案</div>
      <div class="page-desc">用户充值实付金额，到账金额 = 实付 + 赠送余额，另送积分</div>
    </div>
    <button class="btn btn-primary" @click="openCreate">+ 新建方案</button>
  </div>

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead><tr><th>实付金额</th><th>赠送余额</th><th>赠送积分</th><th>排序</th><th>上架</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td class="num strong">¥{{ money(row) }}</td>
            <td class="num">{{ gift(row) }}</td>
            <td class="num">{{ row.giftPoints || 0 }}</td>
            <td class="num">{{ row.sort ?? 0 }}</td>
            <td><span class="tag" :class="row.isShow === 1 ? 'tag-good' : 'tag-muted'">{{ row.isShow === 1 ? '上架' : '下架' }}</span></td>
            <td><button class="btn btn-sm" @click="openEdit(row)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="confirmDelete(row)">删除</button></td>
          </tr>
          <tr v-if="!loading && rows.length === 0"><td colspan="6"><div class="empty-state"><div class="icon">◎</div><div>暂无充值方案</div></div></td></tr>
        </tbody>
      </table>
    </a-spin>
  </div>

  <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
    <div class="modal" style="width: 520px">
      <div class="modal-header"><span>{{ editing ? '编辑方案' : '新建方案' }}</span><button class="modal-close" aria-label="关闭" @click="closeModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>实付金额（元）</label><input v-model.number="form.money" type="number" min="0.01" step="0.01" class="form-input" /></div>
        <div class="form-row">
          <div class="form-item"><label class="form-label">赠送余额（元）</label><input v-model.number="form.giftMoney" type="number" min="0" step="0.01" class="form-input" /></div>
          <div class="form-item"><label class="form-label">赠送积分</label><input v-model.number="form.giftPoints" type="number" min="0" class="form-input" /></div>
        </div>
        <div class="form-row">
          <div class="form-item"><label class="form-label">排序</label><input v-model.number="form.sort" type="number" min="0" class="form-input" /></div>
          <div class="form-item"><label class="form-label">是否上架</label><select v-model.number="form.isShow" class="form-select"><option :value="1">上架</option><option :value="0">下架</option></select></div>
        </div>
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
