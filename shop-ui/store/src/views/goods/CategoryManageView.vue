<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { createCategory, deleteCategory, listCategories, updateCategory } from '@/api/goods'
import ImageField from '@/components/ImageField.vue'

/**
 * 商品分类：三级树、图标、排序、显隐。文档二 §2.2。
 * 分类页装修只改 C 端展示样式，分类数据在这里维护。
 */
const MAX_LEVEL = 3
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const modalOpen = ref(false)
const editing = ref(null)
const parentOfNew = ref(null)
const form = reactive({ name: '', image: '', sort: 0, isShow: true })

const treeRows = computed(() => flatten(buildTree(rows.value), 1))

async function load() {
  loading.value = true
  try {
    rows.value = (await listCategories()) || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function isRoot(parentId) {
  return parentId == null || parentId === 0 || parentId === '0'
}

function buildTree(list) {
  const map = new Map()
  for (const c of list) {
    map.set(String(c.id), { ...c, children: [] })
  }
  const roots = []
  for (const node of map.values()) {
    const pid = String(node.parentId ?? '0')
    if (isRoot(pid) || !map.has(pid)) {
      roots.push(node)
    } else {
      map.get(pid).children.push(node)
    }
  }
  const sortNodes = (arr) => {
    arr.sort((a, b) => (a.sort || 0) - (b.sort || 0) || String(a.id).localeCompare(String(b.id)))
    arr.forEach((n) => sortNodes(n.children))
  }
  sortNodes(roots)
  return roots
}

function flatten(nodes, level) {
  const out = []
  for (const n of nodes) {
    out.push({ ...n, level })
    if (n.children?.length) out.push(...flatten(n.children, level + 1))
  }
  return out
}

function resetForm() {
  Object.assign(form, { name: '', image: '', sort: 0, isShow: true })
}

function openCreate(parent) {
  editing.value = null
  parentOfNew.value = parent || null
  resetForm()
  modalOpen.value = true
}

function openEdit(row) {
  editing.value = row
  parentOfNew.value = null
  Object.assign(form, {
    name: row.name || '',
    image: row.image || '',
    sort: row.sort ?? 0,
    isShow: row.isShow !== false && row.isShow !== 0,
  })
  modalOpen.value = true
}

function closeModal() {
  if (!saving.value) modalOpen.value = false
}

const modalTitle = computed(() => {
  if (editing.value) return '编辑分类'
  if (parentOfNew.value) return `添加子分类 · ${parentOfNew.value.name}`
  return '新建一级分类'
})

async function save() {
  if (!form.name.trim()) return message.error('请输入分类名称')
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      image: form.image || null,
      sort: Number(form.sort) || 0,
      isShow: !!form.isShow,
    }
    if (editing.value) {
      await updateCategory(editing.value.id, payload)
      message.success('分类已更新')
    } else {
      payload.parentId = parentOfNew.value ? parentOfNew.value.id : 0
      await createCategory(payload)
      message.success('分类已创建')
    }
    modalOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onToggleShow(row) {
  const next = !(row.isShow !== false && row.isShow !== 0)
  try {
    await updateCategory(row.id, {
      name: row.name,
      image: row.image || null,
      sort: row.sort ?? 0,
      isShow: next,
    })
    message.success(next ? '已显示' : '已隐藏')
    await load()
  } catch (e) {
    // 拦截器已提示
  }
}

function confirmDelete(row) {
  Modal.confirm({
    title: '删除分类？',
    content: `删除「${row.name}」后不可恢复。有子分类或仍被商品引用时无法删除。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      await deleteCategory(row.id)
      message.success('分类已删除')
      await load()
    },
  })
}

function shown(row) {
  return row.isShow !== false && row.isShow !== 0
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">分类管理</div>
      <div class="page-desc">三级树。分类图会显示在店铺端「分类」Tab，版式在装修里单独设置</div>
    </div>
    <button class="btn btn-primary" @click="openCreate(null)">＋ 新建一级分类</button>
  </div>

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead>
          <tr>
            <th>分类</th>
            <th>图片</th>
            <th class="num">排序</th>
            <th>显示</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in treeRows" :key="row.id">
            <td>
              <span class="cat-indent" :style="{ paddingLeft: (row.level - 1) * 22 + 'px' }">
                <span v-if="row.level > 1" class="cat-branch">└</span>
                <b>{{ row.name }}</b>
                <span class="cat-level">{{ row.level }}级</span>
              </span>
            </td>
            <td>
              <img v-if="row.image" :src="row.image" alt="" class="cat-thumb" />
              <span v-else class="muted">—</span>
            </td>
            <td class="num">{{ row.sort ?? 0 }}</td>
            <td>
              <span class="tag tag-dot" :class="shown(row) ? 'tag-good' : 'tag-muted'">
                {{ shown(row) ? '显示' : '隐藏' }}
              </span>
            </td>
            <td class="cat-ops">
              <button v-if="row.level < MAX_LEVEL" class="btn btn-sm" @click="openCreate(row)">添加子分类</button>
              <button class="btn btn-sm" @click="openEdit(row)">编辑</button>
              <button class="btn btn-sm" @click="onToggleShow(row)">{{ shown(row) ? '隐藏' : '显示' }}</button>
              <button class="btn btn-sm btn-danger-outline" @click="confirmDelete(row)">删除</button>
            </td>
          </tr>
          <tr v-if="!loading && treeRows.length === 0">
            <td colspan="5">
              <div class="empty-state">
                <div class="icon">☰</div>
                <div>还没有分类，先建一个一级分类，发布商品时才能选用</div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </a-spin>
  </div>

  <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
    <div class="modal" style="width: 520px">
      <div class="modal-header">
        <span>{{ modalTitle }}</span>
        <button class="modal-close" aria-label="关闭" @click="closeModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>分类名称</label>
          <input v-model="form.name" class="form-input" maxlength="64" placeholder="例如：女装" />
        </div>
        <div class="form-item">
          <label class="form-label">分类图片</label>
          <ImageField v-model="form.image" size="sm" />
          <div class="form-hint">C 端分类页会展示这张图，尺寸按装修里选的版式来</div>
        </div>
        <div class="form-row">
          <div class="form-item">
            <label class="form-label">排序</label>
            <input v-model.number="form.sort" type="number" class="form-input" />
            <div class="form-hint">数字越小越靠前</div>
          </div>
          <div class="form-item">
            <label class="form-label">显示</label>
            <label class="show-toggle">
              <input v-model="form.isShow" type="checkbox" />
              在店铺端显示
            </label>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" @click="closeModal">取消</button>
        <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cat-indent { display: inline-flex; align-items: center; gap: 6px; }
.cat-branch { color: var(--text-muted); font-size: 12px; }
.cat-level { font-size: 11px; color: var(--text-muted); font-weight: 400; }
.cat-thumb { width: 40px; height: 40px; object-fit: cover; border-radius: 6px; background: var(--surface-2); }
.cat-ops { display: flex; flex-wrap: wrap; gap: 6px; }
.btn-danger-outline { color: var(--status-critical); border-color: var(--status-critical); }
.muted { color: var(--text-muted); }
.form-row { display: flex; gap: 14px; }
.form-row > .form-item { flex: 1; }
.show-toggle { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-secondary); height: 36px; }
</style>
