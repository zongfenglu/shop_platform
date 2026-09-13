<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import ImageField from '@/components/ImageField.vue'
import {
  createArticle, createArticleCategory, deleteArticle, deleteArticleCategory, getArticle,
  listArticleCategories, pageArticles, updateArticle, updateArticleCategory,
} from '@/api/content'
import {
  createMaterialGroup, deleteMaterial, deleteMaterialGroup, listMaterialGroups,
  moveMaterial, pageMaterials, permanentlyDeleteMaterial, renameMaterialGroup,
  restoreMaterial, uploadImage, uploadVideo,
} from '@/api/upload'

const tab = ref('articles')
const categories = ref([])
const loading = ref(false)
const articleRows = ref([])
const articleTotal = ref(0)
const query = reactive({ keyword: '', categoryId: '', status: '', pageNum: 1, pageSize: 20 })
const articleOpen = ref(false)
const articleSaving = ref(false)
const editingArticleId = ref(null)
const articleForm = reactive({
  title: '', categoryId: '', displayMode: 'small', coverUrl: '', content: '',
  virtualViews: 0, status: 'visible', sortNo: 0,
})

const categoryOpen = ref(false)
const categorySaving = ref(false)
const editingCategoryId = ref(null)
const categoryForm = reactive({ name: '', sortNo: 0, isShow: true })

const materialLoading = ref(false)
const materialRows = ref([])
const materialTotal = ref(0)
const materialQuery = reactive({ keyword: '', groupId: '', type: '', pageNum: 1, pageSize: 24 })
const groupMeta = ref({ total: 0, ungrouped: 0, groups: [] })
const fileInput = ref(null)
const videoInput = ref(null)
const uploading = ref(false)

const categoryName = computed(() => Object.fromEntries(categories.value.map((x) => [String(x.id), x.name])))

async function loadCategories() {
  categories.value = (await listArticleCategories().catch(() => [])) || []
}

async function loadArticles() {
  loading.value = true
  try {
    const page = await pageArticles({ ...query, categoryId: query.categoryId || undefined, status: query.status || undefined, keyword: query.keyword || undefined })
    articleRows.value = page?.records || []
    articleTotal.value = page?.total || 0
  } finally { loading.value = false }
}

function searchArticles() { query.pageNum = 1; loadArticles() }
function resetArticleForm() {
  Object.assign(articleForm, { title: '', categoryId: categories.value[0]?.id || '', displayMode: 'small', coverUrl: '', content: '', virtualViews: 0, status: 'visible', sortNo: 0 })
}
async function openArticle(row) {
  editingArticleId.value = row?.id || null
  resetArticleForm()
  if (row) {
    const detail = await getArticle(row.id)
    Object.assign(articleForm, { ...detail, categoryId: detail.categoryId ?? '' })
  }
  articleOpen.value = true
}
async function saveArticle() {
  if (!articleForm.title.trim()) return message.error('请输入文章标题')
  if (!articleForm.categoryId) return message.error('请选择文章分类')
  if (!articleForm.coverUrl) return message.error('请选择文章封面图')
  if (!articleForm.content) return message.error('请输入文章内容')
  articleSaving.value = true
  try {
    const payload = { ...articleForm, title: articleForm.title.trim(), virtualViews: Number(articleForm.virtualViews) || 0, sortNo: Number(articleForm.sortNo) || 0 }
    if (editingArticleId.value) await updateArticle(editingArticleId.value, payload)
    else await createArticle(payload)
    message.success(editingArticleId.value ? '文章已更新' : '文章已创建')
    articleOpen.value = false
    await loadArticles()
  } finally { articleSaving.value = false }
}
async function toggleArticle(row) {
  await updateArticle(row.id, { ...row, status: row.status === 'visible' ? 'hidden' : 'visible' })
  message.success(row.status === 'visible' ? '文章已隐藏' : '文章已显示')
  await loadArticles()
}
function removeArticle(row) {
  Modal.confirm({ title: '删除文章？', content: `「${row.title}」删除后不可恢复。`, okText: '删除', cancelText: '取消', okButtonProps: { danger: true }, async onOk() { await deleteArticle(row.id); message.success('文章已删除'); await loadArticles() } })
}

function openCategory(row) {
  editingCategoryId.value = row?.id || null
  Object.assign(categoryForm, row ? { name: row.name, sortNo: row.sortNo || 0, isShow: row.isShow !== false } : { name: '', sortNo: 0, isShow: true })
  categoryOpen.value = true
}
async function saveCategory() {
  if (!categoryForm.name.trim()) return message.error('请输入分类名称')
  categorySaving.value = true
  try {
    const payload = { name: categoryForm.name.trim(), sortNo: Number(categoryForm.sortNo) || 0, isShow: !!categoryForm.isShow }
    if (editingCategoryId.value) await updateArticleCategory(editingCategoryId.value, payload)
    else await createArticleCategory(payload)
    categoryOpen.value = false
    message.success(editingCategoryId.value ? '分类已更新' : '分类已创建')
    await Promise.all([loadCategories(), loadArticles()])
  } finally { categorySaving.value = false }
}
function removeCategory(row) {
  Modal.confirm({ title: '删除文章分类？', content: '分类下有文章时不能删除。', okText: '删除', cancelText: '取消', okButtonProps: { danger: true }, async onOk() { await deleteArticleCategory(row.id); message.success('分类已删除'); await loadCategories() } })
}

async function loadFiles() {
  materialLoading.value = true
  try {
    const recycled = tab.value === 'recycle'
    const [page, groups] = await Promise.all([
      pageMaterials({ pageNum: materialQuery.pageNum, pageSize: materialQuery.pageSize, keyword: materialQuery.keyword || undefined, groupId: !recycled && materialQuery.groupId ? materialQuery.groupId : undefined, type: materialQuery.type || undefined, recycled }),
      listMaterialGroups(),
    ])
    materialRows.value = page?.records || []
    materialTotal.value = page?.total || 0
    groupMeta.value = groups || { total: 0, ungrouped: 0, groups: [] }
  } finally { materialLoading.value = false }
}
async function switchTab(next) {
  tab.value = next
  if (next === 'articles') await loadArticles()
  if (next === 'categories') await loadCategories()
  if (next === 'files' || next === 'recycle') { materialQuery.pageNum = 1; await loadFiles() }
}
async function uploadFiles(files, type) {
  const list = [...(files || [])]
  if (!list.length) return
  uploading.value = true
  try {
    for (const file of list) {
      if (type === 'video') await uploadVideo(file, materialQuery.groupId || undefined)
      else await uploadImage(file, materialQuery.groupId || undefined)
    }
    message.success(`已上传 ${list.length} 个文件`)
    await loadFiles()
  } finally { uploading.value = false; if (fileInput.value) fileInput.value.value = ''; if (videoInput.value) videoInput.value.value = '' }
}
async function changeFileGroup(row, value) { await moveMaterial(row.id, value || null); message.success('文件已移动'); await loadFiles() }
function trashFile(row) { Modal.confirm({ title: '移入回收站？', content: '文件记录可从回收站恢复，已发布页面不受影响。', okText: '移入', cancelText: '取消', async onOk() { await deleteMaterial(row.id); await loadFiles() } }) }
async function restoreFile(row) { await restoreMaterial(row.id); message.success('文件已恢复'); await loadFiles() }
function purgeFile(row) { Modal.confirm({ title: '永久移除记录？', content: '将无法从文件库恢复，但物理文件会保留，避免已发布页面失效。', okText: '永久移除', cancelText: '取消', okButtonProps: { danger: true }, async onOk() { await permanentlyDeleteMaterial(row.id); await loadFiles() } }) }
async function newGroup() {
  const name = window.prompt('请输入文件分组名称')?.trim()
  if (!name) return
  await createMaterialGroup(name); message.success('分组已创建'); await loadFiles()
}
async function editGroup(group) {
  const name = window.prompt('修改文件分组名称', group.name)?.trim()
  if (!name || name === group.name) return
  await renameMaterialGroup(group.id, name); message.success('分组已更新'); await loadFiles()
}
function removeGroup(group) { Modal.confirm({ title: `删除分组「${group.name}」？`, content: '组内文件将回到未分组。', okText: '删除', cancelText: '取消', async onOk() { await deleteMaterialGroup(group.id); materialQuery.groupId = ''; await loadFiles() } }) }
function fileSize(size) { const n = Number(size) || 0; return n >= 1048576 ? `${(n / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(n / 1024))} KB` }

onMounted(async () => { await loadCategories(); await loadArticles() })
</script>

<template>
  <div class="page-header">
    <div><div class="page-title">内容管理</div><div class="page-desc">统一管理文章与店铺文件资源</div></div>
    <button v-if="tab === 'articles'" class="btn btn-primary" @click="openArticle(null)">＋ 新建文章</button>
    <button v-else-if="tab === 'categories'" class="btn btn-primary" @click="openCategory(null)">＋ 新建分类</button>
  </div>

  <div class="content-tabs">
    <button v-for="item in [{k:'articles',n:'文章列表'},{k:'categories',n:'文章分类'},{k:'files',n:'文件库'},{k:'recycle',n:'回收站'}]" :key="item.k" :class="{active: tab === item.k}" @click="switchTab(item.k)">{{ item.n }}</button>
  </div>

  <template v-if="tab === 'articles'">
    <div class="card card-pad filter-bar">
      <input v-model="query.keyword" class="form-input" placeholder="搜索文章标题" @keyup.enter="searchArticles" />
      <select v-model="query.categoryId" class="form-select"><option value="">全部分类</option><option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option></select>
      <select v-model="query.status" class="form-select"><option value="">全部状态</option><option value="visible">显示</option><option value="hidden">隐藏</option></select>
      <button class="btn btn-primary" @click="searchArticles">查询</button>
    </div>
    <div class="card"><a-spin :spinning="loading"><table class="table"><thead><tr><th>文章</th><th>分类</th><th>列表样式</th><th class="num">阅读量</th><th class="num">排序</th><th>状态</th><th>操作</th></tr></thead><tbody>
      <tr v-for="row in articleRows" :key="row.id"><td><div class="article-cell"><img :src="row.coverUrl" alt="" /><b>{{ row.title }}</b></div></td><td>{{ categoryName[String(row.categoryId)] || '—' }}</td><td>{{ row.displayMode === 'large' ? '大图' : '小图' }}</td><td class="num">{{ (row.virtualViews || 0) + (row.actualViews || 0) }}</td><td class="num">{{ row.sortNo || 0 }}</td><td><span class="tag tag-dot" :class="row.status === 'visible' ? 'tag-good' : 'tag-muted'">{{ row.status === 'visible' ? '显示' : '隐藏' }}</span></td><td class="ops"><button class="btn btn-sm" @click="openArticle(row)">编辑</button><button class="btn btn-sm" @click="toggleArticle(row)">{{ row.status === 'visible' ? '隐藏' : '显示' }}</button><button class="btn btn-sm btn-danger-outline" @click="removeArticle(row)">删除</button></td></tr>
      <tr v-if="!loading && !articleRows.length"><td colspan="7"><div class="empty-state">暂无文章</div></td></tr>
    </tbody></table></a-spin><div v-if="articleTotal > query.pageSize" class="pager"><a-pagination :current="query.pageNum" :page-size="query.pageSize" :total="articleTotal" :show-size-changer="false" @change="p => { query.pageNum = p; loadArticles() }" /></div></div>
  </template>

  <div v-else-if="tab === 'categories'" class="card"><table class="table"><thead><tr><th>分类名称</th><th class="num">排序</th><th>状态</th><th>操作</th></tr></thead><tbody>
    <tr v-for="c in categories" :key="c.id"><td><b>{{ c.name }}</b></td><td class="num">{{ c.sortNo || 0 }}</td><td>{{ c.isShow === false ? '隐藏' : '显示' }}</td><td class="ops"><button class="btn btn-sm" @click="openCategory(c)">编辑</button><button class="btn btn-sm btn-danger-outline" @click="removeCategory(c)">删除</button></td></tr>
    <tr v-if="!categories.length"><td colspan="4"><div class="empty-state">暂无文章分类</div></td></tr>
  </tbody></table></div>

  <template v-else>
    <div class="card card-pad file-toolbar">
      <select v-if="tab === 'files'" v-model="materialQuery.groupId" class="form-select" @change="materialQuery.pageNum = 1; loadFiles()"><option value="">全部分组</option><option v-for="g in groupMeta.groups" :key="g.id" :value="g.id">{{ g.name }}（{{ g.count }}）</option></select>
      <select v-model="materialQuery.type" class="form-select" @change="materialQuery.pageNum = 1; loadFiles()"><option value="">全部类型</option><option value="image">图片</option><option value="video">视频</option></select>
      <input v-model="materialQuery.keyword" class="form-input" placeholder="搜索文件名" @keyup.enter="materialQuery.pageNum = 1; loadFiles()" />
      <button class="btn" @click="materialQuery.pageNum = 1; loadFiles()">查询</button>
      <template v-if="tab === 'files'"><button class="btn" @click="newGroup">管理分组</button><label class="btn btn-primary">上传图片<input ref="fileInput" hidden multiple type="file" accept="image/*" :disabled="uploading" @change="uploadFiles($event.target.files, 'image')" /></label><label class="btn btn-primary">上传视频<input ref="videoInput" hidden multiple type="file" accept="video/mp4" :disabled="uploading" @change="uploadFiles($event.target.files, 'video')" /></label></template>
    </div>
    <div v-if="tab === 'files' && groupMeta.groups.length" class="group-strip"><span>文件分组：</span><span v-for="g in groupMeta.groups" :key="g.id" class="group-chip">{{ g.name }}<button title="重命名" @click="editGroup(g)">改</button><button title="删除" @click="removeGroup(g)">删</button></span></div>
    <div class="card card-pad"><a-spin :spinning="materialLoading"><div class="file-grid">
      <div v-for="row in materialRows" :key="row.id" class="file-card"><video v-if="row.type === 'video'" :src="row.url" muted preload="metadata" /><img v-else :src="row.url" alt="" /><div class="file-name" :title="row.name">{{ row.name }}</div><div class="file-meta">{{ row.type === 'video' ? '视频' : '图片' }} · {{ fileSize(row.size) }}</div>
        <select v-if="tab === 'files'" class="file-group" :value="row.groupId || ''" @change="changeFileGroup(row, $event.target.value)"><option value="">未分组</option><option v-for="g in groupMeta.groups" :key="g.id" :value="g.id">{{ g.name }}</option></select>
        <div class="file-actions"><button v-if="tab === 'files'" class="btn btn-sm" @click="trashFile(row)">移入回收站</button><template v-else><button class="btn btn-sm" @click="restoreFile(row)">恢复</button><button class="btn btn-sm btn-danger-outline" @click="purgeFile(row)">永久移除</button></template></div>
      </div>
      <div v-if="!materialLoading && !materialRows.length" class="empty-state file-empty">{{ tab === 'recycle' ? '回收站为空' : '暂无文件，上传后会自动保存到这里' }}</div>
    </div></a-spin><div v-if="materialTotal > materialQuery.pageSize" class="pager"><a-pagination :current="materialQuery.pageNum" :page-size="materialQuery.pageSize" :total="materialTotal" :show-size-changer="false" @change="p => { materialQuery.pageNum = p; loadFiles() }" /></div></div>
  </template>

  <div v-if="articleOpen" class="modal-mask" @click.self="articleOpen = false"><div class="modal article-modal"><div class="modal-header"><span>{{ editingArticleId ? '编辑文章' : '新建文章' }}</span><button class="modal-close" @click="articleOpen = false">×</button></div><div class="modal-body article-form">
    <div class="form-row"><div class="form-item grow"><label class="form-label"><span class="req">*</span>文章标题</label><input v-model="articleForm.title" maxlength="120" class="form-input" /></div><div class="form-item"><label class="form-label"><span class="req">*</span>文章分类</label><select v-model="articleForm.categoryId" class="form-select"><option value="">请选择</option><option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option></select></div></div>
    <div class="form-row"><div class="form-item grow"><label class="form-label"><span class="req">*</span>文章封面图</label><ImageField v-model="articleForm.coverUrl" /><div class="form-hint">{{ articleForm.displayMode === 'large' ? '大图模式建议尺寸：750 × 455' : '小图模式建议尺寸：300 × 188' }}</div></div><div class="form-item settings"><label class="form-label">列表显示方式</label><select v-model="articleForm.displayMode" class="form-select"><option value="small">小图</option><option value="large">大图</option></select><label class="form-label top">状态</label><select v-model="articleForm.status" class="form-select"><option value="visible">显示</option><option value="hidden">隐藏</option></select></div></div>
    <div class="form-row"><div class="form-item"><label class="form-label">虚拟阅读量</label><input v-model.number="articleForm.virtualViews" min="0" type="number" class="form-input" /></div><div class="form-item"><label class="form-label">排序</label><input v-model.number="articleForm.sortNo" type="number" class="form-input" /><div class="form-hint">数字越小越靠前</div></div></div>
    <div class="form-item"><label class="form-label"><span class="req">*</span>文章内容</label><RichTextEditor v-model="articleForm.content" placeholder="请输入文章正文" /></div>
  </div><div class="modal-footer"><button class="btn" @click="articleOpen = false">取消</button><button class="btn btn-primary" :disabled="articleSaving" @click="saveArticle">{{ articleSaving ? '保存中…' : '保存' }}</button></div></div></div>

  <div v-if="categoryOpen" class="modal-mask" @click.self="categoryOpen = false"><div class="modal" style="width:500px"><div class="modal-header"><span>{{ editingCategoryId ? '编辑文章分类' : '新建文章分类' }}</span><button class="modal-close" @click="categoryOpen = false">×</button></div><div class="modal-body"><div class="form-item"><label class="form-label">分类名称</label><input v-model="categoryForm.name" maxlength="64" class="form-input" /></div><div class="form-row"><div class="form-item"><label class="form-label">排序</label><input v-model.number="categoryForm.sortNo" type="number" class="form-input" /></div><div class="form-item"><label class="form-label">状态</label><select v-model="categoryForm.isShow" class="form-select"><option :value="true">显示</option><option :value="false">隐藏</option></select></div></div></div><div class="modal-footer"><button class="btn" @click="categoryOpen = false">取消</button><button class="btn btn-primary" :disabled="categorySaving" @click="saveCategory">保存</button></div></div></div>
</template>

<style scoped>
.content-tabs{display:flex;gap:20px;border-bottom:1px solid var(--gridline);margin-bottom:16px}.content-tabs button{border:0;background:none;padding:10px 4px 12px;color:var(--text-secondary);cursor:pointer}.content-tabs button.active{color:var(--primary);font-weight:700;border-bottom:2px solid var(--primary)}
.filter-bar,.file-toolbar{display:flex;gap:10px;margin-bottom:14px;align-items:center}.filter-bar .form-input,.file-toolbar .form-input{width:240px}.filter-bar .form-select,.file-toolbar .form-select{width:150px}.article-cell{display:flex;align-items:center;gap:10px;max-width:420px}.article-cell img{width:64px;height:40px;object-fit:cover;border-radius:4px}.ops{display:flex;gap:6px;flex-wrap:wrap}.btn-danger-outline{color:var(--status-critical);border-color:var(--status-critical)}.pager{display:flex;justify-content:flex-end;padding:14px}.article-modal{width:min(920px,calc(100vw - 40px));max-height:92vh;display:flex;flex-direction:column}.article-modal .modal-body{overflow:auto}.form-row{display:flex;gap:16px}.form-row>.form-item{width:220px}.form-row>.grow{flex:1}.settings{width:240px!important}.top{margin-top:14px}.group-strip{display:flex;gap:8px;align-items:center;margin:0 0 12px;font-size:13px}.group-chip{display:inline-flex;align-items:center;gap:5px;border:1px solid var(--border);padding:5px 8px;border-radius:4px;background:var(--surface)}.group-chip button{border:0;background:none;color:var(--text-muted);cursor:pointer;font-size:11px}.file-grid{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:14px}.file-card{border:1px solid var(--border);border-radius:6px;overflow:hidden;background:var(--surface)}.file-card>img,.file-card>video{width:100%;aspect-ratio:1;object-fit:cover;background:#111;display:block}.file-name{font-size:12px;font-weight:600;padding:8px 8px 2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.file-meta{font-size:11px;color:var(--text-muted);padding:0 8px 7px}.file-group{width:calc(100% - 16px);margin:0 8px 7px;height:28px;border:1px solid var(--border);background:var(--surface)}.file-actions{display:flex;gap:5px;padding:0 8px 8px;flex-wrap:wrap}.file-empty{grid-column:1/-1;padding:70px 0}@media(max-width:1100px){.file-grid{grid-template-columns:repeat(4,minmax(0,1fr))}}@media(max-width:760px){.form-row,.filter-bar,.file-toolbar{flex-direction:column;align-items:stretch}.form-row>.form-item,.settings,.filter-bar .form-input,.filter-bar .form-select,.file-toolbar .form-input,.file-toolbar .form-select{width:100%!important}.file-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
