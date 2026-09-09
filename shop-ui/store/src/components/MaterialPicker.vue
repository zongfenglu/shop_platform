<script setup>
import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  createMaterialGroup,
  deleteMaterial,
  deleteMaterialGroup,
  listMaterialGroups,
  pageMaterials,
  renameMaterialGroup,
  uploadImage,
  uploadVideo,
} from '@/api/upload'

/**
 * 商用素材库：左侧分组、搜索、拖拽/多选上传、点选确认。
 * 单选回传 url 字符串，多选回传 url 数组。
 * mediaType=video 时列表/上传均切到视频（仅 MP4，上限 50MB）。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  multiple: { type: Boolean, default: false },
  max: { type: Number, default: 1 },
  mediaType: { type: String, default: 'image' },
})

const emit = defineEmits(['update:open', 'select'])

const loading = ref(false)
const uploading = ref(false)
// uploadJobs: { name, size, pct, done, error }[]  — one entry per file being uploaded
const uploadJobs = ref([])
const records = ref([])
const pageNum = ref(1)
const total = ref(0)
const pageSize = 18
const keyword = ref('')
const scope = ref('all')
const groupMeta = ref({ total: 0, ungrouped: 0, groups: [] })
const selectedUrls = ref([])
const fileInput = ref(null)
const dragOver = ref(false)
const creatingGroup = ref(false)
const newGroupName = ref('')
const renamingId = ref(null)
const renameDraft = ref('')

const currentGroupId = computed(() => (scope.value === 'all' || scope.value === 'none' ? null : scope.value))
const ungroupedMode = computed(() => scope.value === 'none')
const isVideo = computed(() => props.mediaType === 'video')
const unitLabel = computed(() => (isVideo.value ? '个' : '张'))
const mediaLabel = computed(() => (isVideo.value ? '视频' : '图片'))
const scopeTitle = computed(() => {
  if (scope.value === 'all') return '全部素材'
  if (scope.value === 'none') return '未分组'
  const found = groupMeta.value.groups.find((g) => String(g.id) === String(scope.value))
  return found ? found.name : '素材'
})

function sameId(a, b) {
  return a != null && b != null && String(a) === String(b)
}

function fmtSize(n) {
  const size = Number(n) || 0
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function loadGroups() {
  try {
    const data = await listMaterialGroups()
    groupMeta.value = {
      total: data?.total || 0,
      ungrouped: data?.ungrouped || 0,
      groups: data?.groups || [],
    }
  } catch {
    groupMeta.value = { total: 0, ungrouped: 0, groups: [] }
  }
}

async function loadMaterials() {
  loading.value = true
  try {
    const page = await pageMaterials({
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value.trim() || undefined,
      groupId: currentGroupId.value || undefined,
      ungrouped: ungroupedMode.value || undefined,
      type: props.mediaType,
    })
    records.value = page?.records || []
    total.value = page?.total || 0
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function reload() {
  await Promise.all([loadGroups(), loadMaterials()])
}

watch(
  () => props.open,
  (visible) => {
    if (!visible) return
    selectedUrls.value = []
    keyword.value = ''
    pageNum.value = 1
    creatingGroup.value = false
    renamingId.value = null
    reload()
  },
)

function close() {
  emit('update:open', false)
}

function pickScope(next) {
  scope.value = next
  pageNum.value = 1
  selectedUrls.value = []
  loadMaterials()
}

function isSelected(url) {
  return selectedUrls.value.includes(url)
}

function toggle(item) {
  const url = item.url
  if (!url) return
  if (props.multiple) {
    if (isSelected(url)) {
      selectedUrls.value = selectedUrls.value.filter((u) => u !== url)
      return
    }
    if (selectedUrls.value.length >= props.max) {
      message.warning(`最多选择 ${props.max} 张`)
      return
    }
    selectedUrls.value = [...selectedUrls.value, url]
    return
  }
  selectedUrls.value = [url]
}

function confirm() {
  if (!selectedUrls.value.length) {
    message.warning(`请先选择${mediaLabel.value}`)
    return
  }
  emit('select', props.multiple ? [...selectedUrls.value] : selectedUrls.value[0])
  close()
}

function onTileDblclick(item) {
  if (!item.url) return
  if (props.multiple) {
    toggle(item)
    return
  }
  emit('select', item.url)
  close()
}

async function uploadFiles(files) {
  const list = isVideo.value
    ? [...files].filter((f) => f.type === 'video/mp4' || /\.mp4$/i.test(f.name))
    : [...files].filter((f) => f.type.startsWith('image/'))
  if (!list.length) {
    message.warning(isVideo.value ? '请选择 MP4 视频文件' : '请选择图片文件')
    return
  }
  uploading.value = true
  uploadJobs.value = list.map((f) => ({ name: f.name, size: f.size, pct: 0, done: false, error: false }))
  const groupId = currentGroupId.value
  const uploaded = []
  try {
    for (let i = 0; i < list.length; i += 1) {
      uploadJobs.value[i].pct = 0
      const onProgress = (pct) => { uploadJobs.value[i].pct = pct }
      try {
        const material = isVideo.value
          ? await uploadVideo(list[i], groupId, onProgress)
          : await uploadImage(list[i], groupId, onProgress)
        uploadJobs.value[i].pct = 100
        uploadJobs.value[i].done = true
        if (material?.url) uploaded.push(material.url)
      } catch {
        uploadJobs.value[i].error = true
      }
    }
    const succeeded = uploadJobs.value.filter((j) => j.done).length
    if (succeeded) {
      message.success(succeeded > 1 ? `已上传 ${succeeded} ${unitLabel.value}` : '已上传')
    }
    pageNum.value = 1
    await reload()
    if (uploaded.length) {
      if (props.multiple) {
        const room = props.max - selectedUrls.value.length
        selectedUrls.value = [...selectedUrls.value, ...uploaded.slice(0, Math.max(room, 0))]
      } else {
        selectedUrls.value = [uploaded[0]]
      }
    }
  } finally {
    uploading.value = false
    // keep job list visible briefly so user can see final state, then clear
    setTimeout(() => { uploadJobs.value = [] }, 2000)
    if (fileInput.value) fileInput.value.value = ''
  }
}

function onFile(e) {
  uploadFiles(e.target.files || [])
}

function onDrop(e) {
  dragOver.value = false
  uploadFiles(e.dataTransfer?.files || [])
}

async function onDeleteMaterial(item, e) {
  e.stopPropagation()
  Modal.confirm({
    title: `删除这${unitLabel.value}素材？`,
    content: `仅从素材库移除，已用到页面里的${mediaLabel.value}不会一起删掉。`,
    okText: '删除',
    cancelText: '取消',
    onOk: async () => {
      await deleteMaterial(item.id)
      selectedUrls.value = selectedUrls.value.filter((u) => u !== item.url)
      await reload()
    },
  })
}

async function submitNewGroup() {
  const name = newGroupName.value.trim()
  if (!name) return
  try {
    const created = await createMaterialGroup(name)
    newGroupName.value = ''
    creatingGroup.value = false
    await loadGroups()
    if (created?.id != null) pickScope(created.id)
  } catch {
    // 拦截器已提示
  }
}

function startRename(group, e) {
  e.stopPropagation()
  renamingId.value = group.id
  renameDraft.value = group.name
}

async function submitRename(group) {
  const name = renameDraft.value.trim()
  if (!name || name === group.name) {
    renamingId.value = null
    return
  }
  try {
    await renameMaterialGroup(group.id, name)
    renamingId.value = null
    await loadGroups()
  } catch {
    // 拦截器已提示
  }
}

function onDeleteGroup(group, e) {
  e.stopPropagation()
  Modal.confirm({
    title: `删除分组「${group.name}」？`,
    content: '组内图片会回到未分组，不会删除文件。',
    okText: '删除',
    cancelText: '取消',
    onOk: async () => {
      await deleteMaterialGroup(group.id)
      if (sameId(scope.value, group.id)) scope.value = 'all'
      renamingId.value = null
      await reload()
    },
  })
}

const hasPrev = computed(() => pageNum.value > 1)
const hasNext = computed(() => pageNum.value * pageSize < total.value)

function prevPage() {
  if (!hasPrev.value) return
  pageNum.value -= 1
  loadMaterials()
}

function nextPage() {
  if (!hasNext.value) return
  pageNum.value += 1
  loadMaterials()
}

function onSearch() {
  pageNum.value = 1
  loadMaterials()
}
</script>

<template>
  <div v-if="open" class="modal-mask lib-mask" @click.self="close">
    <div class="lib">
      <div class="lib-head">
        <div>
          <div class="lib-title">素材库</div>
          <div class="lib-sub">按分组管理，上传后可在装修、商品、营销中复用</div>
        </div>
        <button class="modal-close" aria-label="关闭" @click="close">×</button>
      </div>

      <div class="lib-body">
        <aside class="lib-nav">
          <button type="button" class="lib-nav-item" :class="{ active: scope === 'all' }" @click="pickScope('all')">
            <span>全部</span>
            <em>{{ groupMeta.total }}</em>
          </button>
          <button type="button" class="lib-nav-item" :class="{ active: scope === 'none' }" @click="pickScope('none')">
            <span>未分组</span>
            <em>{{ groupMeta.ungrouped }}</em>
          </button>
          <div class="lib-nav-label">分组</div>
          <button
            v-for="g in groupMeta.groups"
            :key="g.id"
            type="button"
            class="lib-nav-item"
            :class="{ active: sameId(scope, g.id) }"
            @click="pickScope(g.id)"
          >
            <input
              v-if="sameId(renamingId, g.id)"
              v-model="renameDraft"
              class="lib-inline-input"
              @click.stop
              @keyup.enter="submitRename(g)"
              @blur="submitRename(g)"
            />
            <span v-else class="lib-nav-name">{{ g.name }}</span>
            <em>{{ g.count }}</em>
            <span class="lib-nav-ops">
              <i title="重命名" @click="startRename(g, $event)">改</i>
              <i title="删除分组" @click="onDeleteGroup(g, $event)">删</i>
            </span>
          </button>
          <div v-if="creatingGroup" class="lib-new-group">
            <input
              v-model="newGroupName"
              class="lib-inline-input"
              maxlength="16"
              placeholder="分组名称"
              @keyup.enter="submitNewGroup"
              @keyup.esc="creatingGroup = false"
            />
            <button type="button" class="btn btn-sm btn-primary" @click="submitNewGroup">创建</button>
          </div>
          <button v-else type="button" class="lib-add-group" @click="creatingGroup = true">＋ 新建分组</button>
        </aside>

        <section
          class="lib-main"
          :class="{ drop: dragOver }"
          @dragenter.prevent="dragOver = true"
          @dragover.prevent="dragOver = true"
          @dragleave.prevent="dragOver = false"
          @drop.prevent="onDrop"
        >
          <div class="lib-toolbar">
            <div class="lib-scope">{{ scopeTitle }} · {{ total }} {{ unitLabel }}</div>
            <input
              v-model="keyword"
              class="form-input lib-search"
              placeholder="搜索文件名"
              @keyup.enter="onSearch"
            />
            <button type="button" class="btn btn-sm" @click="onSearch">搜索</button>
            <label class="btn btn-sm btn-primary" :class="{ disabled: uploading }">
              {{ uploading ? '上传中…' : '上传到此分组' }}
              <input ref="fileInput" type="file" :accept="isVideo ? 'video/mp4' : 'image/*'" multiple hidden :disabled="uploading" @change="onFile" />
            </label>
          </div>

          <div v-if="dragOver" class="lib-drop-hint">松开鼠标，上传到「{{ scopeTitle }}」</div>

          <!-- per-file upload progress — shown while uploading, fades out 2s after done -->
          <div v-if="uploadJobs.length" class="lib-upload-jobs">
            <div v-for="(job, i) in uploadJobs" :key="i" class="lib-job">
              <div class="lib-job-name" :title="job.name">{{ job.name }}</div>
              <div class="lib-job-bar-wrap">
                <div
                  class="lib-job-bar"
                  :class="{ done: job.done, error: job.error }"
                  :style="{ width: job.pct + '%' }"
                />
              </div>
              <div class="lib-job-pct">
                <span v-if="job.error" class="lib-job-err">失败</span>
                <span v-else-if="job.done" class="lib-job-ok">✓</span>
                <span v-else>{{ job.pct }}%</span>
              </div>
            </div>
          </div>

          <div v-if="loading" class="lib-empty">加载中…</div>
          <div v-else-if="!records.length" class="lib-empty">
            <div class="lib-empty-title">{{ keyword ? `没有匹配的${mediaLabel}` : '这个分组还是空的' }}</div>
            <div class="lib-empty-desc">{{ isVideo ? '把 MP4 视频拖进来，或点击右上角上传（单个不超过 50MB）。' : '把图片拖进来，或点击右上角上传。支持一次选多张。' }}</div>
          </div>
          <div v-else class="lib-grid">
            <button
              v-for="item in records"
              :key="item.id"
              type="button"
              class="lib-card"
              :class="{ selected: isSelected(item.url) }"
              @click="toggle(item)"
              @dblclick="onTileDblclick(item)"
            >
              <video v-if="isVideo" :src="item.url" class="lib-video" preload="metadata" muted />
              <img v-else :src="item.url" alt="" />
              <span v-if="isVideo" class="lib-play">▶</span>
              <div class="lib-card-meta">
                <div class="lib-card-name" :title="item.name">{{ item.name }}</div>
                <div class="lib-card-size">{{ fmtSize(item.size) }}</div>
              </div>
              <span v-if="isSelected(item.url)" class="lib-check">✓</span>
              <span class="lib-del" title="从素材库移除" @click="onDeleteMaterial(item, $event)">×</span>
            </button>
          </div>

          <div v-if="total > pageSize" class="lib-pager">
            <button class="btn btn-sm" :disabled="!hasPrev" @click="prevPage">上一页</button>
            <span>第 {{ pageNum }} 页</span>
            <button class="btn btn-sm" :disabled="!hasNext" @click="nextPage">下一页</button>
          </div>
        </section>
      </div>

      <div class="lib-foot">
        <div class="lib-foot-hint">
          {{ multiple ? `已选 ${selectedUrls.length} / ${max} ${unitLabel}，双击或点确定完成` : '单击选中，双击直接使用' }}
        </div>
        <div class="lib-foot-actions">
          <button class="btn" @click="close">取消</button>
          <button class="btn btn-primary" :disabled="!selectedUrls.length" @click="confirm">使用已选{{ mediaLabel }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lib-mask { z-index: 120; }
.lib {
  width: 920px;
  max-width: min(920px, 94vw);
  /* Fixed height so the dialog never shrinks when there are few items */
  height: min(680px, 90vh);
  background: var(--surface);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-modal);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.lib-head {
  padding: 16px 20px 14px;
  border-bottom: 1px solid var(--gridline);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.lib-title { font-size: 16px; font-weight: 700; }
.lib-sub { margin-top: 4px; font-size: 12px; color: var(--text-muted); }
.lib-body { display: flex; min-height: 0; flex: 1; }
.lib-nav {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid var(--gridline);
  padding: 12px 10px;
  overflow-y: auto;
  background: var(--surface-2);
}
.lib-nav-label {
  margin: 12px 8px 6px;
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}
.lib-nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  padding: 8px;
  border-radius: var(--r-sm);
  cursor: pointer;
  text-align: left;
  font-size: 13px;
}
.lib-nav-item:hover,
.lib-nav-item.active { background: var(--surface); color: var(--text-primary); }
.lib-nav-item.active { box-shadow: inset 2px 0 0 var(--primary); }
.lib-nav-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lib-nav-item em {
  font-style: normal;
  font-size: 11px;
  color: var(--text-muted);
  margin-left: auto;
}
.lib-nav-ops {
  display: none;
  gap: 4px;
}
.lib-nav-item:hover .lib-nav-ops { display: inline-flex; }
.lib-nav-ops i {
  font-style: normal;
  font-size: 11px;
  color: var(--text-muted);
  padding: 0 3px;
}
.lib-nav-ops i:hover { color: var(--primary); }
.lib-add-group {
  width: 100%;
  margin-top: 8px;
  border: 1px dashed var(--border-strong);
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--r-sm);
  padding: 8px;
  cursor: pointer;
  font-size: 12px;
}
.lib-new-group { display: grid; gap: 6px; margin-top: 8px; }
.lib-inline-input {
  width: 100%;
  height: 28px;
  border: 1px solid var(--border-strong);
  border-radius: 4px;
  padding: 0 8px;
  background: var(--surface);
  color: var(--text-primary);
  font-size: 12px;
}
.lib-main {
  flex: 1;
  min-width: 0;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  position: relative;
}
.lib-main.drop { outline: 2px dashed var(--primary); outline-offset: -8px; }
.lib-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.lib-scope { font-size: 13px; font-weight: 600; margin-right: auto; }
.lib-search { width: 180px; height: 32px; }
.lib-toolbar .disabled { pointer-events: none; opacity: 0.6; }
.lib-drop-hint {
  position: absolute;
  inset: 48px 16px 16px;
  background: color-mix(in srgb, var(--primary) 10%, var(--surface));
  border: 1px dashed var(--primary);
  border-radius: var(--r-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  font-weight: 600;
  z-index: 2;
}
.lib-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  overflow-y: auto;
  flex: 1;
  align-content: start;
}
.lib-card {
  position: relative;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: var(--surface);
  overflow: hidden;
  cursor: pointer;
  text-align: left;
}
.lib-card:hover { border-color: var(--primary); }
.lib-card.selected { border-color: var(--primary); box-shadow: 0 0 0 2px var(--primary-bg-2); }
.lib-card img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  background: var(--surface-2);
}
.lib-video {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
  background: #000;
}
.lib-play {
  position: absolute;
  top: calc(50% - 26px);
  left: 50%;
  transform: translate(-50%, -50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.lib-card-meta { padding: 6px 8px 8px; }
.lib-card-name {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.lib-card-size { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.lib-check {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lib-del {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: none;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}
.lib-card:hover .lib-del { display: flex; }
.lib-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  text-align: center;
  padding: 40px 16px;
}
.lib-empty-title { font-size: 14px; color: var(--text-secondary); margin-bottom: 6px; }
.lib-empty-desc { font-size: 12px; }
.lib-pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  font-size: 12px;
  color: var(--text-muted);
}
.lib-foot {
  border-top: 1px solid var(--gridline);
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.lib-foot-hint { font-size: 12px; color: var(--text-muted); }
.lib-foot-actions { display: flex; gap: 8px; }

/* per-file upload progress */
.lib-upload-jobs {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.lib-job {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.lib-job-name {
  width: 160px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
}
.lib-job-bar-wrap {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: var(--surface-2);
  overflow: hidden;
  border: 1px solid var(--border);
}
.lib-job-bar {
  height: 100%;
  border-radius: 999px;
  background: var(--primary);
  transition: width 120ms ease;
}
.lib-job-bar.done { background: var(--status-good-text, #22a665); }
.lib-job-bar.error { background: var(--status-critical, #d63b3b); }
.lib-job-pct {
  width: 36px;
  text-align: right;
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.lib-job-ok { color: var(--status-good-text, #22a665); font-weight: 600; }
.lib-job-err { color: var(--status-critical, #d63b3b); }
</style>
