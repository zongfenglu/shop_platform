<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { createPackage, listPackages, togglePackageShow, updatePackage } from '@/api/package'

/**
 * 套餐管理。对应原型 docs/prototype/admin/package-list.html。
 *
 * 功能树与原型有意简化：原型里展示的"商品发布/分类品牌/批量导入导出"等三级子项只是 UI 演示，
 * 后端 package_tpl.menus 实际存储的是一份扁平 key 数组（见 V4 种子数据：goods.*、order.*、
 * marketing.coupon 等），没有比这更细的粒度。这里的功能树按后端真实支持的 key 集合来画，
 * 不画后端不认识的复选框——那样点了也不会有任何效果，是假交互。
 */
const FEATURE_CATALOG = [
  {
    group: '核心交易',
    items: [
      { key: 'goods.*', label: '商品管理' },
      { key: 'order.*', label: '订单管理' },
      { key: 'after_sale.*', label: '售后管理' },
    ],
  },
  {
    group: '营销中心',
    // marketing.* 是"全功能"，与下面四个单项互斥没有强校验，但选了 marketing.* 就不需要再勾细项
    items: [
      { key: 'marketing.*', label: '营销中心（全功能）' },
      { key: 'marketing.coupon', label: '优惠券' },
      { key: 'marketing.seckill', label: '秒杀' },
      { key: 'marketing.group', label: '拼团' },
      { key: 'marketing.bargain', label: '砍价' },
    ],
  },
  {
    group: '分销与门店',
    items: [
      { key: 'distribution.*', label: '分销体系' },
      { key: 'store.offline', label: '门店自提' },
    ],
  },
  {
    group: '高级能力',
    items: [
      { key: 'mp.authorize', label: '微信第三方托管' },
      { key: 'live.*', label: '直播组件' },
    ],
  },
]

const QUOTA_FIELDS = [
  { key: 'goods_max', label: '商品数上限', hint: '-1 表示不限' },
  { key: 'staff_max', label: '员工数上限', hint: '-1 表示不限' },
  { key: 'storage_mb', label: '存储空间(MB)', hint: '-1 表示不限' },
  { key: 'sms_month', label: '月短信条数', hint: '-1 表示不限' },
  { key: 'store_max', label: '门店数上限', hint: '-1 表示不限' },
  { key: 'diy_page_max', label: 'DIY页面数上限', hint: '-1 表示不限' },
]

const loading = ref(false)
const packages = ref([])

async function load() {
  loading.value = true
  try {
    packages.value = await listPackages()
  } catch (e) {
    packages.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ---------- 编辑弹窗 ----------
const modalOpen = ref(false)
const modalTitle = ref('')
const saving = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  intro: '',
  sort: 0,
  isTrial: false,
  isShow: true,
  priceMonth: 0,
  priceQuarter: 0,
  priceYear: 0,
  featureKeys: [],
  quota: Object.fromEntries(QUOTA_FIELDS.map((f) => [f.key, 0])),
})

function resetForm() {
  form.name = ''
  form.intro = ''
  form.sort = 0
  form.isTrial = false
  form.isShow = true
  form.priceMonth = 0
  form.priceQuarter = 0
  form.priceYear = 0
  form.featureKeys = []
  form.quota = Object.fromEntries(QUOTA_FIELDS.map((f) => [f.key, 0]))
}

function openCreate() {
  resetForm()
  editingId.value = null
  modalTitle.value = '新建套餐'
  modalOpen.value = true
}

function openEdit(pkg) {
  editingId.value = pkg.id
  modalTitle.value = `编辑套餐 · ${pkg.name}`
  form.name = pkg.name
  form.intro = pkg.intro || ''
  form.sort = pkg.sort || 0
  form.isTrial = !!pkg.isTrial
  form.isShow = !!pkg.isShow

  const price = safeParse(pkg.price, {})
  form.priceMonth = price.month ?? 0
  form.priceQuarter = price.quarter ?? 0
  form.priceYear = price.year ?? 0

  form.featureKeys = safeParse(pkg.menus, [])

  const quota = safeParse(pkg.quota, {})
  form.quota = Object.fromEntries(QUOTA_FIELDS.map((f) => [f.key, quota[f.key] ?? 0]))

  modalOpen.value = true
}

function safeParse(jsonStr, fallback) {
  if (!jsonStr) return fallback
  try {
    return JSON.parse(jsonStr)
  } catch (e) {
    return fallback
  }
}

async function onSave() {
  if (!form.name.trim()) {
    message.error('请输入套餐名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      intro: form.intro.trim(),
      sort: form.sort,
      isTrial: form.isTrial,
      isShow: form.isShow,
      menus: JSON.stringify(form.featureKeys),
      quota: JSON.stringify(form.quota),
      price: JSON.stringify({ month: form.priceMonth, quarter: form.priceQuarter, year: form.priceYear }),
    }
    if (editingId.value) {
      await updatePackage(editingId.value, payload)
    } else {
      await createPackage(payload)
    }
    message.success('保存成功')
    modalOpen.value = false
    await load()
  } catch (e) {
    // 错误提示已由 http 拦截器统一处理
  } finally {
    saving.value = false
  }
}

function onToggleShow(pkg) {
  const nextShow = !pkg.isShow
  Modal.confirm({
    title: nextShow ? '确认上架该套餐？' : '确认下架该套餐？',
    content: nextShow ? '上架后新开店时可选择该套餐' : '下架不影响已开通商城，仅新开店时不可再选择',
    okText: nextShow ? '上架' : '下架',
    cancelText: '取消',
    onOk: async () => {
      try {
        await togglePackageShow(pkg.id, nextShow)
        message.success(nextShow ? '已上架' : '已下架')
        await load()
      } catch (e) {
        // 已由拦截器提示
      }
    },
  })
}

const totalApplied = computed(() => packages.value.reduce((s, p) => s + (p.appliedShopCount || 0), 0))

function featureSummary(pkg) {
  const keys = safeParse(pkg.menus, [])
  const labels = FEATURE_CATALOG.flatMap((g) => g.items)
    .filter((i) => keys.includes(i.key))
    .map((i) => i.label)
  return labels.length ? labels.slice(0, 4) : ['无功能项']
}

function priceOf(pkg) {
  return safeParse(pkg.price, {})
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">套餐管理</div>
      <div class="page-desc">共 {{ packages.length }} 个套餐 · 已应用于 {{ totalApplied }} 家商城</div>
    </div>
    <button class="btn btn-primary" @click="openCreate">＋ 新建套餐</button>
  </div>

  <a-spin :spinning="loading">
    <div class="grid grid-3">
      <div v-for="pkg in packages" :key="pkg.id" class="plan-card">
        <div class="plan-badge">
          <span class="tag" :class="pkg.isShow ? 'tag-good' : 'tag-muted'">{{ pkg.isShow ? '已上架' : '已下架' }}</span>
        </div>
        <div style="font-weight: 700; font-size: 15px">{{ pkg.name }}</div>
        <div class="plan-price">¥{{ priceOf(pkg).month ?? 0 }}<span class="unit">/月</span></div>
        <div v-if="pkg.intro" style="font-size: 12px; color: var(--text-muted); margin-bottom: 6px">{{ pkg.intro }}</div>
        <div v-for="label in featureSummary(pkg)" :key="label" class="plan-feat">✓ {{ label }}</div>
        <div style="font-size: 11.5px; color: var(--text-muted); margin-top: 10px">
          应用商城：{{ pkg.appliedShopCount }} 家
        </div>
        <div style="display: flex; gap: 8px; margin-top: 14px">
          <button class="btn btn-sm" @click="openEdit(pkg)">编辑</button>
          <button class="btn btn-sm" @click="onToggleShow(pkg)">{{ pkg.isShow ? '下架' : '上架' }}</button>
        </div>
      </div>

      <div v-if="!loading && packages.length === 0" class="card card-pad" style="grid-column: 1 / -1; text-align: center; color: var(--text-muted)">
        暂无套餐，点击右上角新建
      </div>
    </div>
  </a-spin>

  <a-modal
    v-model:open="modalOpen"
    :title="modalTitle"
    :confirm-loading="saving"
    width="640px"
    ok-text="保存"
    cancel-text="取消"
    @ok="onSave"
  >
    <div class="form-row form-item">
      <div>
        <label class="form-label"><span class="req">*</span>套餐名称</label>
        <input v-model="form.name" class="form-input" />
      </div>
      <div>
        <label class="form-label">排序</label>
        <input v-model.number="form.sort" type="number" class="form-input" />
      </div>
    </div>

    <div class="form-item">
      <label class="form-label">简介</label>
      <input v-model="form.intro" class="form-input" placeholder="展示在套餐卡片上的一句话简介" />
    </div>

    <div class="form-row form-item">
      <div>
        <label class="form-label">月价</label>
        <input v-model.number="form.priceMonth" type="number" class="form-input" />
      </div>
      <div>
        <label class="form-label">季价</label>
        <input v-model.number="form.priceQuarter" type="number" class="form-input" />
      </div>
      <div>
        <label class="form-label">年价</label>
        <input v-model.number="form.priceYear" type="number" class="form-input" />
      </div>
    </div>

    <div class="form-item">
      <label class="form-label">功能项配置</label>
      <div class="tree">
        <div v-for="g in FEATURE_CATALOG" :key="g.group" class="tree-group">
          <div class="tree-group-title">{{ g.group }}</div>
          <div class="tree-items">
            <label v-for="item in g.items" :key="item.key" class="tree-item">
              <input type="checkbox" :value="item.key" v-model="form.featureKeys" />{{ item.label }}
            </label>
          </div>
        </div>
      </div>
    </div>

    <div class="form-item">
      <label class="form-label">配额配置</label>
      <div class="form-row" style="flex-wrap: wrap">
        <div v-for="f in QUOTA_FIELDS" :key="f.key" style="min-width: 160px">
          <label class="form-label" style="font-size: 12px">{{ f.label }}</label>
          <input v-model.number="form.quota[f.key]" type="number" class="form-input" :placeholder="f.hint" />
        </div>
      </div>
    </div>

    <div class="form-item" style="display: flex; gap: 24px">
      <label class="tree-item"><input type="checkbox" v-model="form.isTrial" />设为试用套餐</label>
      <label class="tree-item"><input type="checkbox" v-model="form.isShow" />上架（新开店时可选）</label>
    </div>
  </a-modal>
</template>

<style scoped>
/* 这些类只在原型 admin/package-list.html 的页内 <style> 里定义，不在共享的
   shared/style.css 里，所以照抄到这里作为组件私有样式，其余通用类（card/tag/grid...）
   仍然来自全局引入的 prototype.css。 */
.plan-card {
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  padding: 20px;
  position: relative;
}
.plan-badge {
  position: absolute;
  top: 14px;
  right: 14px;
}
.plan-price {
  font-size: 22px;
  font-weight: 800;
  margin: 8px 0;
}
.plan-price .unit {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}
.plan-feat {
  font-size: 12.5px;
  color: var(--text-secondary);
  padding: 4px 0;
}
.tree {
  font-size: 13px;
}
.tree-group {
  margin-bottom: 14px;
}
.tree-group-title {
  font-weight: 600;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tree-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  padding-left: 22px;
}
.tree-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  cursor: pointer;
}
</style>

