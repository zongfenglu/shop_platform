<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { getGoods, pageGoods } from '@/api/goods'

/**
 * 运营选商品：按名称/编码搜索，展示封面和名称，避免手填数字 ID。
 * 选中或外部写入 goodsId 后，会再拉一次详情并把 SKU 列表抛给父组件。
 */
const props = defineProps({
  modelValue: { default: null },
  placeholder: { type: String, default: '搜索商品名称或编码' },
  triggerText: { type: String, default: '' },
  resetOnSelect: { type: Boolean, default: false },
  /** 只列出出售中的商品（营销活动等场景仓库中商品不可选） */
  onlyOnSale: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'loaded', 'select'])

const root = ref(null)
const keyword = ref('')
const searching = ref(false)
const results = ref([])
const panelOpen = ref(false)
const selected = ref(null)
const loadingSelected = ref(false)

function firstImage(row) {
  if (!row?.images) return ''
  if (Array.isArray(row.images)) return row.images[0] || ''
  try {
    const arr = JSON.parse(row.images)
    return Array.isArray(arr) ? arr[0] || '' : ''
  } catch {
    return ''
  }
}

async function search() {
  searching.value = true
  try {
    const page = await pageGoods({
      keyword: keyword.value.trim() || undefined,
      status: props.onlyOnSale ? 'on' : undefined,
      pageNum: 1,
      pageSize: 20,
    })
    results.value = page?.records || []
  } catch {
    results.value = []
  } finally {
    searching.value = false
  }
}

function openPanel() {
  panelOpen.value = true
  if (!results.value.length) search()
}

async function hydrate(id) {
  if (!id) {
    selected.value = null
    emit('loaded', { goods: null, skus: [] })
    return
  }
  loadingSelected.value = true
  try {
    const detail = await getGoods(id)
    selected.value = detail?.goods || selected.value
    emit('loaded', { goods: detail?.goods || null, skus: detail?.skus || [] })
  } catch {
    emit('loaded', { goods: selected.value, skus: [] })
  } finally {
    loadingSelected.value = false
  }
}

async function pick(row) {
  selected.value = row
  emit('update:modelValue', row.id)
  panelOpen.value = false
  keyword.value = ''
  await hydrate(row.id)
  emit('select', { goods: selected.value, skus: [] })
  if (props.resetOnSelect) {
    selected.value = null
    emit('update:modelValue', null)
  }
}

function clear() {
  selected.value = null
  keyword.value = ''
  emit('update:modelValue', null)
  emit('loaded', { goods: null, skus: [] })
}

watch(
  () => props.modelValue,
  (id, prev) => {
    if (id == null || id === '') {
      if (prev != null && prev !== '') {
        selected.value = null
        emit('loaded', { goods: null, skus: [] })
      }
      return
    }
    if (selected.value && String(selected.value.id) === String(id)) return
    hydrate(id)
  },
  { immediate: true },
)

function onDocClick(e) {
  if (root.value && !root.value.contains(e.target)) panelOpen.value = false
}

onMounted(() => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))
</script>

<template>
  <div ref="root" class="goods-picker">
    <div v-if="selected" class="goods-picker-card">
      <img v-if="firstImage(selected)" :src="firstImage(selected)" alt="" class="goods-picker-thumb" />
      <div v-else class="goods-picker-thumb goods-picker-thumb-empty">无图</div>
      <div class="goods-picker-meta">
        <div class="goods-picker-name">{{ selected.name }}</div>
        <div class="goods-picker-sub">ID {{ selected.id }}{{ selected.code ? ' · ' + selected.code : '' }}</div>
      </div>
      <button type="button" class="btn btn-sm" @click.stop="openPanel">更换</button>
      <button type="button" class="btn btn-sm" @click.stop="clear">清除</button>
    </div>
    <button v-else type="button" class="goods-picker-trigger" @click.stop="openPanel">
      {{ loadingSelected ? '加载商品…' : (triggerText || '选择商品') }}
    </button>

    <div v-if="panelOpen" class="goods-picker-panel" @click.stop>
      <div class="goods-picker-search">
        <input
          v-model="keyword"
          class="form-input"
          :placeholder="placeholder"
          @keyup.enter="search"
        />
        <button type="button" class="btn btn-primary btn-sm" @click="search">搜索</button>
      </div>
      <div class="goods-picker-list">
        <div v-if="searching" class="goods-picker-empty">搜索中…</div>
        <button
          v-for="row in results"
          :key="row.id"
          type="button"
          class="goods-picker-row"
          :class="{ active: selected && String(selected.id) === String(row.id) }"
          @click="pick(row)"
        >
          <img v-if="firstImage(row)" :src="firstImage(row)" alt="" class="goods-picker-thumb" />
          <div v-else class="goods-picker-thumb goods-picker-thumb-empty">无图</div>
          <div class="goods-picker-meta">
            <div class="goods-picker-name">{{ row.name }}</div>
            <div class="goods-picker-sub">
              {{ row.status === 'on' ? '出售中' : '仓库中' }}
              <template v-if="row.code"> · {{ row.code }}</template>
            </div>
          </div>
        </button>
        <div v-if="!searching && !results.length" class="goods-picker-empty">没有匹配的商品</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.goods-picker { position: relative; }
.goods-picker-trigger {
  width: 100%;
  height: 40px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--r-sm);
  background: var(--surface-2);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
}
.goods-picker-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: var(--surface-2);
}
.goods-picker-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}
.goods-picker-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface);
  color: var(--text-muted);
  font-size: 11px;
  border: 1px solid var(--border);
}
.goods-picker-meta { flex: 1; min-width: 0; }
.goods-picker-name {
  font-weight: 600;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.goods-picker-sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.goods-picker-panel {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  z-index: 20;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-md);
  padding: 10px;
}
.goods-picker-search { display: flex; gap: 8px; }
.goods-picker-list { max-height: 240px; overflow-y: auto; margin-top: 8px; }
.goods-picker-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: var(--r-sm);
}
.goods-picker-row:hover,
.goods-picker-row.active { background: var(--surface-2); }
.goods-picker-empty {
  padding: 16px 8px;
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
}
</style>
