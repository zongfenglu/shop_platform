<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getCategoryPage, saveCategoryPage } from '@/api/diy'
import { listCategories } from '@/api/goods'

const STYLES = [
  { key: 'level1_large', label: '一级分类（大图）', hint: '分类图尺寸：宽 702 像素，高度建议 240' },
  { key: 'level1_small', label: '一级分类（小图）', hint: '分类图尺寸：宽 188 像素，高度不限' },
  { key: 'level2', label: '二级分类', hint: '左侧一级分类，右侧二级分类小图，建议宽 150 像素' },
]

const PLACEHOLDER = [
  { id: 'p1', name: '外套', image: '', children: [{ id: 'p11', name: '大衣' }, { id: 'p12', name: '风衣' }] },
  { id: 'p2', name: '毛衣', image: '', children: [{ id: 'p21', name: '开衫' }] },
  { id: 'p3', name: '短裤', image: '', children: [] },
  { id: 'p4', name: 'T恤', image: '', children: [] },
  { id: 'p5', name: '腰带', image: '', children: [] },
  { id: 'p6', name: '配饰', image: '', children: [] },
]

const loading = ref(false)
const saving = ref(false)
const style = ref('level1_small')
const shareTitle = ref('全部分类')
const categories = ref([])
const previewParent = ref(null)

const styleHint = computed(() => STYLES.find((s) => s.key === style.value)?.hint || '')

const roots = computed(() => {
  const tree = buildTree(categories.value)
  return tree.length ? tree : PLACEHOLDER
})

const previewChildren = computed(() => {
  const parent = roots.value.find((c) => String(c.id) === String(previewParent.value)) || roots.value[0]
  return parent?.children?.length ? parent.children : [{ id: 'empty', name: '暂无二级分类' }]
})

onMounted(async () => {
  loading.value = true
  try {
    const [cfg, cats] = await Promise.all([
      getCategoryPage().catch(() => null),
      listCategories().catch(() => []),
    ])
    if (cfg?.style) style.value = cfg.style
    if (cfg?.shareTitle) shareTitle.value = cfg.shareTitle
    categories.value = cats || []
    previewParent.value = roots.value[0]?.id ?? null
  } finally {
    loading.value = false
  }
})

function buildTree(flat) {
  const shown = (flat || []).filter((c) => c.isShow !== false && c.isShow !== 0)
  const byParent = new Map()
  for (const c of shown) {
    const pid = String(c.parentId ?? 0)
    if (!byParent.has(pid)) byParent.set(pid, [])
    byParent.get(pid).push(c)
  }
  const walk = (pid) => (byParent.get(String(pid)) || [])
    .slice()
    .sort((a, b) => (a.sort || 0) - (b.sort || 0))
    .map((c) => ({ ...c, children: walk(c.id) }))
  return walk(0)
}

async function onSave() {
  saving.value = true
  try {
    await saveCategoryPage({ style: style.value, shareTitle: shareTitle.value })
    message.success('分类页模板已保存，店铺端分类 Tab 立即生效')
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-spin :spinning="loading">
    <div class="cat-style">
      <div class="phone-shell">
        <div class="phone-screen cat-phone">
          <div class="phone-status">
            <span>9:41</span>
            <span>{{ shareTitle || '全部分类' }}</span>
            <span>▮▮▮</span>
          </div>
          <div class="pv-search">搜索</div>

          <div v-if="style === 'level1_large'" class="pv-large">
            <div v-for="c in roots" :key="c.id" class="pv-large-item">
              <div class="pv-large-img">
                <img v-if="c.image" :src="c.image" alt="" />
                <span v-else>{{ c.name.slice(0, 1) }}</span>
              </div>
              <div class="pv-large-name">{{ c.name }}</div>
            </div>
          </div>

          <div v-else-if="style === 'level1_small'" class="pv-small">
            <div v-for="c in roots" :key="c.id" class="pv-small-item">
              <div class="pv-small-img">
                <img v-if="c.image" :src="c.image" alt="" />
                <span v-else>{{ c.name.slice(0, 1) }}</span>
              </div>
              <div class="pv-small-name">{{ c.name }}</div>
            </div>
          </div>

          <div v-else class="pv-level2">
            <div class="pv-level2-nav">
              <div
                v-for="c in roots"
                :key="c.id"
                class="pv-level2-nav-item"
                :class="{ on: String(c.id) === String(previewParent || roots[0]?.id) }"
                @click="previewParent = c.id"
              >{{ c.name }}</div>
            </div>
            <div class="pv-level2-main">
              <div v-for="c in previewChildren" :key="c.id" class="pv-small-item">
                <div class="pv-small-img">
                  <img v-if="c.image" :src="c.image" alt="" />
                  <span v-else>{{ c.name.slice(0, 1) }}</span>
                </div>
                <div class="pv-small-name">{{ c.name }}</div>
              </div>
            </div>
          </div>

          <div class="pv-tabbar">
            <span>首页</span>
            <span class="on">分类</span>
            <span>购物车</span>
            <span>我的</span>
          </div>
        </div>
      </div>

      <div class="card card-pad cat-form">
        <p class="card-title">分类页模板</p>
        <div class="form-item">
          <label class="form-label">分类页样式</label>
          <div class="style-radios">
            <label v-for="s in STYLES" :key="s.key" class="style-radio" :class="{ selected: style === s.key }">
              <input v-model="style" type="radio" :value="s.key" />
              {{ s.label }}
            </label>
          </div>
          <div class="form-hint">{{ styleHint }}</div>
        </div>
        <div class="form-item">
          <label class="form-label">分享标题</label>
          <input v-model="shareTitle" class="form-input" maxlength="64" placeholder="全部分类" />
          <div class="form-hint">用户转发小程序/H5 时展示的标题</div>
        </div>
        <button class="btn btn-primary" :disabled="saving" @click="onSave">{{ saving ? '保存中…' : '提交' }}</button>
      </div>
    </div>
  </a-spin>
</template>

<style scoped>
.cat-style { display: flex; gap: 28px; align-items: flex-start; flex-wrap: wrap; }
.cat-phone { background: #f5f4f1; min-height: 640px; position: relative; padding-bottom: 56px; }
.pv-search {
  margin: 10px 12px; height: 32px; border-radius: 16px; background: #fff;
  color: var(--text-muted); font-size: 12px; display: flex; align-items: center; padding: 0 12px;
}
.pv-large { padding: 0 12px; display: flex; flex-direction: column; gap: 10px; }
.pv-large-item { background: #fff; border-radius: 8px; overflow: hidden; }
.pv-large-img {
  height: 92px; background: #e8e6e0; display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); font-size: 22px;
}
.pv-large-img img { width: 100%; height: 100%; object-fit: cover; }
.pv-large-name { padding: 8px 10px; font-size: 13px; font-weight: 600; }
.pv-small { padding: 4px 10px 12px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 8px; }
.pv-small-item { text-align: center; }
.pv-small-img {
  width: 72px; height: 72px; margin: 0 auto; border-radius: 8px; background: #e8e6e0;
  display: flex; align-items: center; justify-content: center; color: var(--text-muted); overflow: hidden;
}
.pv-small-img img { width: 100%; height: 100%; object-fit: cover; }
.pv-small-name { margin-top: 6px; font-size: 12px; }
.pv-level2 { display: flex; min-height: 420px; }
.pv-level2-nav { width: 88px; background: #fff; }
.pv-level2-nav-item { padding: 12px 8px; font-size: 12px; color: var(--text-secondary); }
.pv-level2-nav-item.on { background: #f5f4f1; color: var(--primary); font-weight: 600; }
.pv-level2-main { flex: 1; padding: 12px 8px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 6px; align-content: start; }
.pv-tabbar {
  position: absolute; left: 0; right: 0; bottom: 0; height: 48px; background: #fff;
  border-top: 1px solid var(--gridline); display: flex; align-items: center; justify-content: space-around;
  font-size: 11px; color: var(--text-muted);
}
.pv-tabbar .on { color: #e34948; font-weight: 600; }
.cat-form { flex: 1; min-width: 320px; max-width: 480px; }
.style-radios { display: flex; flex-direction: column; gap: 8px; }
.style-radio {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
  border: 1px solid var(--border-strong); border-radius: var(--r-sm); cursor: pointer; font-size: 13px;
}
.style-radio.selected { border-color: var(--primary); background: var(--primary-bg); }
.phone-shell { display: flex; justify-content: center; }
.phone-screen {
  width: 360px; min-height: 640px; border: 8px solid #222; border-radius: 28px;
  box-sizing: border-box; overflow: hidden;
}
.phone-status {
  display: flex; justify-content: space-between; align-items: center; height: 36px;
  padding: 0 14px; background: #fff; font-size: 11px; font-weight: 650;
  border-bottom: 1px solid var(--gridline);
}
</style>
