<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import draggable from 'vuedraggable'
import BlockPreview from './BlockPreview.vue'
import CategoryStylePanel from '@/components/CategoryStylePanel.vue'
import {
  copyDiyPage,
  createDiyPage,
  deleteDiyPage,
  getDiyMenus,
  getDiyTabbar,
  listDiyPages,
  listDiyTemplates,
  listMyTemplates,
  publishDiyPage,
  saveDiyTabbar,
  saveMyTemplate,
  setDiyPageHome,
  updateDiyPageDraft,
} from '@/api/diy'
import { listCoupons } from '@/api/coupon'
import { listSeckillActives } from '@/api/seckill'
import { listGroupActives, listBargainActives } from '@/api/group-bargain'
import { getGoods } from '@/api/goods'
import { listOfflineStores } from '@/api/offlineStore'
import GoodsPicker from '@/components/GoodsPicker.vue'
import ImageField from '@/components/ImageField.vue'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const activeTab = ref('pages')
const pageModalOpen = ref(false)
const selectedPageId = ref(null)

const newPageForm = reactive({
  name: '',
  setAsHome: false,
})

const allPages = ref([])

function pageMetaText(page) {
  const time = page.updateTime ? String(page.updateTime).replace('T', ' ').slice(0, 16) : ''
  return page.pageData ? `已发布 · ${time} 更新` : `草稿 · ${time} 更新`
}

function isCurrentHome(page) {
  return !!(page && page.isDefault)
}

const currentHome = computed(() => allPages.value.find((p) => isCurrentHome(p)) || null)

async function load() {
  loading.value = true
  try {
    allPages.value = (await listDiyPages()) || []
  } catch (e) {
    allPages.value = []
  } finally {
    loading.value = false
  }
}

// ---------------- 编辑器 tab ----------------

const unlockedMenus = ref([])
const REQUIRED_MENU = {
  coupon: 'marketing.coupon',
  seckill: 'marketing.seckill',
  group: 'marketing.group',
  bargain: 'marketing.bargain',
  store: 'store.offline',
}

function isLocked(type) {
  const menu = REQUIRED_MENU[type]
  if (!menu) return false
  const namespace = menu.includes('.') ? menu.slice(0, menu.indexOf('.')) + '.*' : null
  return !unlockedMenus.value.includes(menu) && !(namespace && unlockedMenus.value.includes(namespace))
}

async function loadMenus() {
  try {
    unlockedMenus.value = (await getDiyMenus()) || []
  } catch (e) {
    unlockedMenus.value = []
  }
}

const couponList = ref([])
const seckillActives = ref([])
const groupActives = ref([])
const bargainActives = ref([])
const storeList = ref([])

async function loadMarketingLists() {
  const [coupons, seckills, groups, bargains, stores] = await Promise.all([
    listCoupons().catch(() => []),
    listSeckillActives().catch(() => []),
    listGroupActives().catch(() => []),
    listBargainActives().catch(() => []),
    listOfflineStores().catch(() => []),
  ])
  couponList.value = coupons || []
  seckillActives.value = seckills || []
  groupActives.value = groups || []
  bargainActives.value = bargains || []
  storeList.value = stores || []
}

const blockList = [
  { type: 'banner', icon: '🖼', name: '图片轮播', group: '媒体组件' },
  { type: 'imageGroup', icon: '🖼', name: '单图组', group: '媒体组件' },
  { type: 'imageWindow', icon: '🪟', name: '图片橱窗', group: '媒体组件' },
  { type: 'video', icon: '▶', name: '视频组', group: '媒体组件' },
  { type: 'article', icon: '📰', name: '文章组', group: '媒体组件' },
  { type: 'news', icon: '📣', name: '头条新闻', group: '媒体组件' },
  { type: 'search', icon: '🔍', name: '搜索框', group: '商城组件' },
  { type: 'notice', icon: '📢', name: '公告组', group: '商城组件' },
  { type: 'navBar', icon: '▦', name: '导航组', group: '商城组件' },
  { type: 'goods', icon: '🛍', name: '商品组', group: '商城组件' },
  { type: 'coupon', icon: '🎁', name: '优惠券组', group: '商城组件' },
  { type: 'group', icon: '👥', name: '拼团商品', group: '商城组件' },
  { type: 'bargain', icon: '🔪', name: '砍价商品', group: '商城组件' },
  { type: 'seckill', icon: '⚡', name: '秒杀商品', group: '商城组件' },
  { type: 'store', icon: '🏬', name: '线下门店', group: '商城组件' },
  { type: 'customerService', icon: '🎧', name: '在线客服', group: '工具组件' },
  { type: 'followMp', icon: '✿', name: '关注公众号', group: '工具组件' },
  { type: 'richText', icon: '📝', name: '富文本', group: '工具组件' },
  { type: 'blank', icon: '▭', name: '辅助空白', group: '工具组件' },
  { type: 'divider', icon: '—', name: '辅助线', group: '工具组件' },
]

const BLOCK_META = {
  search: { title: '搜索框', hint: '点击后进入商品搜索。' },
  banner: { title: '图片轮播', hint: '从素材库选图，可为每张配置跳转。' },
  imageGroup: { title: '单图组', hint: '一张或多张竖向排列的图片。' },
  imageWindow: { title: '图片橱窗', hint: '左侧大图 + 右侧两张小图。' },
  video: { title: '视频组', hint: '填写视频地址与封面，店铺端可播放。' },
  article: { title: '文章组', hint: '标题 + 封面列表，适合种草/资讯。' },
  news: { title: '头条新闻', hint: '单行快报，点击跳转。' },
  notice: { title: '公告组', hint: '店铺公告。' },
  navBar: { title: '导航组', hint: '入口图标、文字与跳转。' },
  goods: { title: '商品组', hint: '手动挑选要展示的商品。' },
  coupon: { title: '优惠券组', hint: '关联可领取优惠券，画布按票券样式展示。' },
  seckill: { title: '秒杀商品组', hint: '关联秒杀专场，可调列数与展示内容。' },
  group: { title: '拼团商品组', hint: '自动拉取进行中的拼团，或手动勾选。' },
  bargain: { title: '砍价商品组', hint: '自动拉取进行中的砍价，或手动勾选。' },
  store: { title: '线下门店', hint: '展示自提门店名称、地址与电话。' },
  followMp: { title: '关注公众号', hint: '引导用户关注，可换 Logo 与文案。' },
  richText: { title: '富文本', hint: '自定义 HTML。' },
  blank: { title: '辅助空白', hint: '调整组件间距。' },
  divider: { title: '辅助线', hint: '分隔内容区块。' },
  customerService: { title: '在线客服', hint: '唤起客服会话。' },
}

const editorPage = reactive({ id: null, name: '', published: false })
const items = ref([])
const selectedIndex = ref(null)

const DEFAULT_PAGE_BG = '#f5f4f1'
const PAGE_BG_PRESETS = ['#f5f4f1', '#ffffff', '#fff7f2', '#f3e7e0', '#eef4fc', '#e8f6ef', '#1c1c1e']
const pageStyle = reactive({
  bgType: 'color',
  bgColor: DEFAULT_PAGE_BG,
  bgImage: '',
})

function applyPageMeta(page) {
  const meta = page && typeof page === 'object' ? page : {}
  pageStyle.bgType = meta.bgType === 'image' ? 'image' : 'color'
  pageStyle.bgColor = meta.bgColor || DEFAULT_PAGE_BG
  pageStyle.bgImage = meta.bgImage || ''
}

function serializePageMeta() {
  return {
    name: editorPage.name,
    bgType: pageStyle.bgType,
    bgColor: pageStyle.bgColor || DEFAULT_PAGE_BG,
    bgImage: pageStyle.bgImage || '',
  }
}

function pageCanvasStyle() {
  const color = pageStyle.bgColor || DEFAULT_PAGE_BG
  if (pageStyle.bgType === 'image' && pageStyle.bgImage) {
    return {
      backgroundColor: color,
      backgroundImage: `url(${pageStyle.bgImage})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center top',
      backgroundRepeat: 'no-repeat',
    }
  }
  return { backgroundColor: color }
}

const selectedItem = computed(() => (selectedIndex.value != null ? items.value[selectedIndex.value] : null))
const blockMeta = computed(() => (selectedItem.value ? BLOCK_META[selectedItem.value.type] : null))

let itemSeq = 0
function nextCid() {
  itemSeq += 1
  return `c${itemSeq}`
}

function defaultItemData(type) {
  const base = { _cid: nextCid(), type }
  switch (type) {
    case 'search':
      return { ...base, placeholder: '搜索心动好物' }
    case 'banner':
      return { ...base, images: [{ url: '', link: '' }], bgColor: '#ffffff' }
    case 'imageGroup':
      return { ...base, images: [{ url: '', link: '' }], bgColor: '#ffffff' }
    case 'imageWindow':
      return { ...base, images: [{ url: '', link: '' }, { url: '', link: '' }, { url: '', link: '' }], bgColor: '#ffffff' }
    case 'video':
      return { ...base, cover: '', url: '', height: 190, autoplay: false, margin: 0, bgColor: '#000000' }
    case 'article':
      return { ...base, items: [{ title: '', cover: '', views: 0, link: '' }], bgColor: '#ffffff' }
    case 'news':
      return { ...base, label: '快报', items: [{ title: '', link: '' }], bgColor: '#ffffff' }
    case 'notice':
      return { ...base, text: '欢迎光临本店', bgColor: '#fff7e8' }
    case 'navBar':
      return { ...base, items: [{ icon: '', text: '', link: '' }], bgColor: '#ffffff' }
    case 'goods':
      return { ...base, style: 'grid', goodsIds: [], limit: 6, bgColor: '#ffffff', showName: true, showPrice: true, showLinePrice: true }
    case 'richText':
      return { ...base, html: '', bgColor: '#ffffff' }
    case 'blank':
      return { ...base, height: 20 }
    case 'divider':
      return { ...base, color: '#e1e0d9', style: 'solid' }
    case 'followMp':
      return { ...base, title: '关注公众号', desc: '获取更多优惠与新品', logo: '', bgColor: '#ffffff' }
    case 'store':
      return { ...base, source: 'auto', storeIds: [], bgColor: '#ffffff' }
    case 'coupon':
      return { ...base, couponIds: [], bgColor: '#ffffff' }
    case 'seckill':
      return { ...base, activeId: null, limit: 6, columns: 3, bgColor: '#ffffff', showName: true, showPrice: true, showLinePrice: true }
    case 'group':
      return { ...base, source: 'auto', activeId: null, activeIds: [], limit: 6, bgColor: '#ffffff', showSellingPoint: true, showPrice: true, showLinePrice: true }
    case 'bargain':
      return { ...base, source: 'auto', activeId: null, activeIds: [], limit: 6, bgColor: '#ffffff', showPrice: true, showLinePrice: true }
    default:
      return { ...base, bgColor: '#ffffff' }
  }
}

function blockLabel(type) {
  return BLOCK_META[type]?.title || type
}

function blockDesc(item) {
  switch (item.type) {
    case 'banner':
      return `${(item.images || []).length} 张轮播图`
    case 'navBar':
      return `${(item.items || []).length} 个入口`
    case 'goods':
      return `${(item.goodsIds || []).length} 个商品`
    case 'coupon':
      return `${(item.couponIds || []).length} 张优惠券`
    case 'seckill':
    case 'group':
    case 'bargain':
      return item.activeId ? `已关联活动 #${item.activeId}` : '未关联活动'
    case 'richText':
      return item.html ? '已填写内容' : '未填写内容'
    case 'blank':
      return `高度 ${item.height || 0}px`
    default:
      return blockLabel(item.type)
  }
}

function addBlock(type) {
  if (isLocked(type)) {
    message.warning('当前套餐未开通该组件所需功能，可继续编辑草稿，发布时将被拦截')
  }
  items.value.push(defaultItemData(type))
  selectedIndex.value = items.value.length - 1
}

function selectItem(index) {
  selectedIndex.value = index
}

function removeItem(index) {
  items.value.splice(index, 1)
  if (selectedIndex.value === index) {
    selectedIndex.value = null
  } else if (selectedIndex.value > index) {
    selectedIndex.value -= 1
  }
}

const goodsMeta = ref({})
const materialOpen = ref(false)
const materialTarget = ref(null)
const videoPickerOpen = ref(false)
const videoPickerItem = ref(null)

function openVideoPicker(item) {
  videoPickerItem.value = item
  videoPickerOpen.value = true
}

function onVideoSelect(url) {
  if (videoPickerItem.value) videoPickerItem.value.url = url
}

function firstGoodsImage(row) {
  if (!row?.images) return ''
  if (Array.isArray(row.images)) return row.images[0] || ''
  try {
    const arr = JSON.parse(row.images)
    return Array.isArray(arr) ? arr[0] || '' : ''
  } catch {
    return ''
  }
}

function collectGoodsIds(list) {
  return (list || [])
    .filter((it) => it.type === 'goods')
    .flatMap((it) => it.goodsIds || [])
}

async function rememberGoods(ids) {
  const uniq = [...new Set((ids || []).filter((id) => id != null && id !== '').map(String))]
  await Promise.all(uniq.map(async (id) => {
    if (goodsMeta.value[id]) return
    try {
      const detail = await getGoods(id)
      const g = detail?.goods
      goodsMeta.value[id] = { name: g?.name || `#${id}`, image: firstGoodsImage(g) }
    } catch {
      goodsMeta.value[id] = { name: `#${id}`, image: '' }
    }
  }))
}

function goodsName(id) {
  return goodsMeta.value[String(id)]?.name || `商品 #${id}`
}

function goodsCover(id) {
  return goodsMeta.value[String(id)]?.image || ''
}

function onSelectDiyGoods({ goods }) {
  if (!goods?.id || !selectedItem.value || selectedItem.value.type !== 'goods') return
  if (!selectedItem.value.goodsIds) selectedItem.value.goodsIds = []
  if (selectedItem.value.goodsIds.some((id) => String(id) === String(goods.id))) {
    message.info('该商品已在列表中')
    return
  }
  selectedItem.value.goodsIds.push(goods.id)
  goodsMeta.value[String(goods.id)] = { name: goods.name, image: firstGoodsImage(goods) }
}

function removeDiyGoods(item, goodsId) {
  item.goodsIds = (item.goodsIds || []).filter((id) => String(id) !== String(goodsId))
}

function openMaterialFor(target) {
  materialTarget.value = target
  materialOpen.value = true
}

function onMaterialSelect(url) {
  const target = materialTarget.value
  if (!target?.item) return
  if (target.type === 'banner') {
    if (!target.item.images) target.item.images = []
    if (!target.item.images[target.index]) target.item.images[target.index] = { url: '', link: '' }
    target.item.images[target.index].url = url
  } else if (target.type === 'nav') {
    if (target.item.items?.[target.index]) target.item.items[target.index].icon = url
  }
}

function onCanvasPickImage(index) {
  selectItem(index)
  const item = items.value[index]
  if (!item || !['banner', 'imageGroup', 'imageWindow'].includes(item.type)) return
  if (!item.images?.length) item.images = [{ url: '', link: '' }]
  const emptyIdx = item.images.findIndex((im) => !im.url)
  openMaterialFor({ type: 'banner', item, index: emptyIdx >= 0 ? emptyIdx : 0 })
}

function addBannerImage(item) {
  if (!item.images) item.images = []
  item.images.push({ url: '', link: '' })
  openMaterialFor({ type: 'banner', item, index: item.images.length - 1 })
}

function removeBannerImage(item, index) {
  item.images.splice(index, 1)
}

function addNavItem(item) {
  item.items.push({ icon: '', text: '', link: '' })
}

function removeNavItem(item, index) {
  item.items.splice(index, 1)
}

function resetBlockBg(item) {
  item.bgColor = '#ffffff'
}

function addArticleItem(item) {
  if (!item.items) item.items = []
  item.items.push({ title: '', cover: '', views: 0, link: '' })
}

function addNewsItem(item) {
  if (!item.items) item.items = []
  item.items.push({ title: '', link: '' })
}

function stripClientFields(item) {
  const { _cid, ...rest } = item
  return rest
}

function editPage(page) {
  editorPage.id = page.id
  editorPage.name = page.name
  editorPage.published = !!page.pageData
  let draft = { page: {}, items: [] }
  if (page.draftData) {
    try {
      draft = JSON.parse(page.draftData)
    } catch (e) {
      draft = { page: {}, items: [] }
    }
  }
  items.value = (draft.items || []).map((it) => ({ _cid: nextCid(), ...it }))
  applyPageMeta(draft.page)
  selectedIndex.value = items.value.length ? 0 : null
  activeTab.value = 'editor'
  rememberGoods(collectGoodsIds(items.value))
}

const saving = ref(false)
async function saveDraft() {
  if (!editorPage.id) {
    message.error('请先在"页面列表"选择要编辑的页面')
    return
  }
  saving.value = true
  try {
    const draftData = JSON.stringify({ page: serializePageMeta(), items: items.value.map(stripClientFields) })
    await updateDiyPageDraft(editorPage.id, draftData)
    message.success('草稿已保存')
    await load()
  } finally {
    saving.value = false
  }
}

const publishing = ref(false)
async function publishPage() {
  if (!editorPage.id) {
    message.error('请先在"页面列表"选择要编辑的页面')
    return
  }
  publishing.value = true
  try {
    await saveDraft()
    await publishDiyPage(editorPage.id)
    message.success('已发布')
    editorPage.published = true
    await load()
  } finally {
    publishing.value = false
  }
}

// ---------------- 预览 / 存为模板 ----------------

const previewModalOpen = ref(false)
function openPreview() {
  previewModalOpen.value = true
}
function closePreview() {
  previewModalOpen.value = false
}

const saveTemplateModalOpen = ref(false)
const saveTemplateForm = reactive({ name: '' })
const savingTemplate = ref(false)

function openSaveTemplateModal() {
  saveTemplateForm.name = ''
  saveTemplateModalOpen.value = true
}
function closeSaveTemplateModal() {
  saveTemplateModalOpen.value = false
}

async function confirmSaveTemplate() {
  if (!saveTemplateForm.name.trim()) return message.error('请输入模板名称')
  savingTemplate.value = true
  try {
    const pageData = JSON.stringify({ page: serializePageMeta(), items: items.value.map(stripClientFields) })
    await saveMyTemplate(saveTemplateForm.name.trim(), pageData)
    message.success('已存为模板')
    saveTemplateModalOpen.value = false
    await loadTemplates()
  } finally {
    savingTemplate.value = false
  }
}

// ---------------- 页面模板快速开始 ----------------

const industryTemplates = ref([])
const myTemplates = ref([])

async function loadTemplates() {
  const [industry, mine] = await Promise.all([listDiyTemplates().catch(() => []), listMyTemplates().catch(() => [])])
  industryTemplates.value = industry || []
  myTemplates.value = mine || []
}

/** 模板摘要：底色 + 组件构成，让不同风格在列表里一眼可辨（模板本身只是 JSON，没有截图） */
function templateSummary(template) {
  try {
    const parsed = JSON.parse(template.pageData)
    const names = (parsed.items || []).map((it) => BLOCK_META[it.type]?.title || it.type)
    return { bg: parsed.page?.bgColor || '#f8f8f8', parts: names.join(' · ') || '空页面' }
  } catch {
    return { bg: '#f8f8f8', parts: '' }
  }
}

function applyTemplateToCanvas(template) {
  Modal.confirm({
    title: '应用该模板？',
    content: '将替换当前画布内容，请确认已保存当前草稿。',
    okText: '确定',
    cancelText: '取消',
    onOk() {
      let parsed = { items: [] }
      try {
        parsed = JSON.parse(template.pageData)
      } catch (e) {
        message.error('模板内容解析失败')
        return
      }
      items.value = (parsed.items || []).map((it) => ({ _cid: nextCid(), ...it }))
      applyPageMeta(parsed.page)
      selectedIndex.value = items.value.length ? 0 : null
      rememberGoods(collectGoodsIds(items.value))
      message.success('已应用模板，请记得保存草稿')
    },
  })
}

// ---------------- 底部导航 tab ----------------

const ICON_OPTIONS = ['⌂', '▦', '🛒', '🎟', '⭐', '📦', '🎁', '🏬', '👥', '💬', '☺', '🏷', '❤', '📍', '🔔']

const tabbarLoading = ref(false)
const tabbarItems = ref([])
const tabbarStyleText = ref('')
const iconPickerIndex = ref(null)
const previewActiveIndex = ref(0)

async function loadTabbar() {
  tabbarLoading.value = true
  try {
    const tabbar = await getDiyTabbar()
    tabbarStyleText.value = tabbar?.style || ''
    tabbarItems.value = tabbar?.items ? JSON.parse(tabbar.items) : []
  } catch (e) {
    tabbarItems.value = []
  } finally {
    tabbarLoading.value = false
  }
}

function addTabbarItem() {
  if (tabbarItems.value.length >= 5) {
    message.warning('最多支持 5 个菜单项')
    return
  }
  tabbarItems.value.splice(tabbarItems.value.length - 1, 0, { icon: '⭐', activeIcon: '⭐', text: '新入口', path: '' })
}

function removeTabbarItem(index) {
  if (index === 0 || index === tabbarItems.value.length - 1) return
  if (tabbarItems.value.length <= 2) {
    message.warning('至少保留 2 个菜单项')
    return
  }
  tabbarItems.value.splice(index, 1)
}

function onTabbarDragMove(evt) {
  const oldIndex = evt.draggedContext.index
  const newIndex = evt.draggedContext.futureIndex
  const last = tabbarItems.value.length - 1
  return oldIndex !== 0 && oldIndex !== last && newIndex !== 0 && newIndex !== last
}

function pickIcon(index, icon) {
  tabbarItems.value[index].icon = icon
  tabbarItems.value[index].activeIcon = icon
  iconPickerIndex.value = null
}

const tabbarSaving = ref(false)
async function saveTabbar() {
  if (tabbarItems.value.length < 2 || tabbarItems.value.length > 5) {
    message.error('菜单项需在 2~5 个之间')
    return
  }
  tabbarSaving.value = true
  try {
    await saveDiyTabbar(JSON.stringify(tabbarItems.value), tabbarStyleText.value || null)
    message.success('底部导航已保存')
    await loadTabbar()
  } finally {
    tabbarSaving.value = false
  }
}

onMounted(async () => {
  await load()
  await Promise.all([loadMenus(), loadMarketingLists(), loadTabbar(), loadTemplates()])
})

function openPageModal() {
  newPageForm.setAsHome = !currentHome.value
  pageModalOpen.value = true
}

function closePageModal() {
  pageModalOpen.value = false
}

async function createPage() {
  if (!newPageForm.name.trim()) return message.error('请输入页面名称')
  const created = await createDiyPage({
    name: newPageForm.name.trim(),
    pageType: newPageForm.setAsHome ? 'home' : 'custom',
  })
  pageModalOpen.value = false
  newPageForm.name = ''
  newPageForm.setAsHome = false
  await load()
  if (created) {
    editPage(created)
  }
}

async function setHome(page) {
  await setDiyPageHome(page.id)
  message.success('已设为当前首页')
  await load()
}

async function copyPage(page) {
  await copyDiyPage(page.id)
  message.success('已复制页面')
  await load()
}

function removePage(page) {
  Modal.confirm({
    title: '删除该页面？',
    content: '删除后不可恢复，已挂载该页面的入口将失效。',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      await deleteDiyPage(page.id)
      message.success('页面已删除')
      await load()
    },
  })
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">页面装修</div>
      <div class="page-desc">共 {{ allPages.length }} 个页面</div>
    </div>
    <button v-if="activeTab === 'pages'" class="btn btn-primary" @click="openPageModal">＋ 新建页面</button>
  </div>

  <div class="tabs" style="margin-bottom: 16px">
    <div class="tab" :class="{ active: activeTab === 'pages' }" @click="activeTab = 'pages'">页面列表</div>
    <div class="tab" :class="{ active: activeTab === 'editor' }" @click="activeTab = 'editor'">页面编辑器</div>
    <div class="tab" :class="{ active: activeTab === 'category' }" @click="activeTab = 'category'">分类页</div>
    <div class="tab" :class="{ active: activeTab === 'tabbar' }" @click="activeTab = 'tabbar'">底部导航</div>
  </div>

  <template v-if="activeTab === 'pages'">
    <a-spin :spinning="loading">
      <div class="card card-pad home-slot">
        <div class="section-block-hd" style="margin-bottom: 0">
          <div>
            <span class="section-block-title">当前首页</span>
            <span class="section-block-desc">小程序 / H5 打开后的落地页，全店只能有一个</span>
          </div>
        </div>
        <div v-if="currentHome" class="home-slot-body">
          <div class="home-slot-main">
            <div class="home-slot-name">
              {{ currentHome.name }}
              <span class="tag tag-primary">当前首页</span>
              <span class="tag" :class="currentHome.pageData ? 'tag-good' : 'tag-muted'">
                {{ currentHome.pageData ? '已发布' : '未发布' }}
              </span>
            </div>
            <div class="page-meta">{{ pageMetaText(currentHome) }}</div>
            <div v-if="!currentHome.pageData" class="home-slot-warn">已设为首页，但还未发布。店铺端仍会走商品瀑布流，请进入编辑器发布后再看。</div>
          </div>
          <div class="page-actions" style="margin-top: 0">
            <button class="btn btn-sm btn-primary" @click="editPage(currentHome)">进入编辑</button>
            <button class="btn btn-sm" @click="copyPage(currentHome)">复制</button>
          </div>
        </div>
        <div v-else class="home-slot-empty">
          <div>还没有生效的首页。把下表里的页面点「设为首页」，或新建时勾选该项。</div>
          <button class="btn btn-sm btn-primary" @click="openPageModal">新建并设为首页</button>
        </div>
      </div>

      <div class="card" style="margin-top: 16px">
        <div class="card-pad" style="padding-bottom: 8px">
          <div class="section-block-title">全部页面</div>
          <div class="section-block-desc">自定义页可挂在导航宫格、优惠券、海报等入口；任一页都可设为首页</div>
        </div>
        <table v-if="allPages.length" class="table">
          <thead>
            <tr>
              <th>页面</th>
              <th>角色</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="page in allPages" :key="page.id">
              <td>
                <div class="page-name">{{ page.name }}</div>
              </td>
              <td>
                <span v-if="isCurrentHome(page)" class="tag tag-primary">当前首页</span>
                <span v-else class="tag tag-muted">{{ page.pageType === 'home' ? '首页稿' : '自定义' }}</span>
              </td>
              <td>
                <span class="tag" :class="page.pageData ? 'tag-good' : 'tag-muted'">{{ page.pageData ? '已发布' : '草稿' }}</span>
              </td>
              <td class="num">{{ page.updateTime ? String(page.updateTime).replace('T', ' ').slice(0, 16) : '—' }}</td>
              <td>
                <div class="page-actions" style="margin-top: 0">
                  <button class="btn btn-sm" @click="editPage(page)">编辑</button>
                  <button v-if="!isCurrentHome(page)" class="btn btn-sm btn-primary" @click="setHome(page)">设为首页</button>
                  <button class="btn btn-sm" @click="copyPage(page)">复制</button>
                  <button v-if="!isCurrentHome(page)" class="btn btn-sm" @click="removePage(page)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="!loading" class="empty-state" style="padding: 36px 0">
          <div class="icon">◎</div>
          <div>还没有装修页，点击右上角新建</div>
        </div>
      </div>

      <div class="card card-pad" style="margin-top: 16px">
        <div class="section-block-hd" style="margin-bottom: 0">
          <div>
            <span class="section-block-title">分类页样式</span>
            <span class="section-block-desc">全局唯一，控制店铺端「分类」Tab 的展示风格</span>
          </div>
          <button class="btn btn-sm btn-primary" @click="activeTab = 'category'">编辑样式</button>
        </div>
      </div>
    </a-spin>
  </template>

  <template v-else-if="activeTab === 'category'">
    <CategoryStylePanel />
  </template>

  <template v-else-if="activeTab === 'editor'">
    <div v-if="!editorPage.id" class="empty-state" style="padding: 40px 0">
      <div class="icon">◎</div>
      <div>请先在"页面列表"tab 点击某页面的"编辑"按钮进入编辑器</div>
    </div>
    <div v-else class="editor-shell">
      <aside class="editor-left">
        <div v-if="industryTemplates.length || myTemplates.length">
          <div class="comp-group-title">页面模板</div>
          <div class="tpl-list">
            <div v-for="t in industryTemplates" :key="'i' + t.id" class="tpl-item" @click="applyTemplateToCanvas(t)">
              <span class="tpl-swatch" :style="{ background: templateSummary(t).bg }"></span>
              <span class="tpl-meta">
                <span class="tpl-name">{{ t.name }}</span>
                <span class="tpl-parts">{{ templateSummary(t).parts }}</span>
              </span>
            </div>
          </div>
          <template v-if="myTemplates.length">
            <div class="comp-group-title" style="margin-top: 4px; font-size: 12px; color: var(--text-muted)">我的模板</div>
            <div class="tpl-list">
              <div v-for="t in myTemplates" :key="'m' + t.id" class="tpl-item" @click="applyTemplateToCanvas(t)">
                <span class="tpl-swatch" :style="{ background: templateSummary(t).bg }"></span>
                <span class="tpl-meta">
                  <span class="tpl-name">{{ t.name }}</span>
                  <span class="tpl-parts">{{ templateSummary(t).parts }}</span>
                </span>
              </div>
            </div>
          </template>
        </div>
        <div v-for="groupName in ['媒体组件', '商城组件', '工具组件']" :key="groupName">
          <div class="comp-group-title">{{ groupName }}</div>
          <div class="comp-grid">
            <div
              v-for="b in blockList.filter((item) => item.group === groupName)"
              :key="b.type"
              class="comp-item"
              :class="{ locked: isLocked(b.type) }"
              @click="addBlock(b.type)"
            >
              <span class="ico">{{ b.icon }}</span>
              <span>{{ b.name }}</span>
              <span v-if="isLocked(b.type)" class="lock-badge" title="当前套餐未开通">🔒</span>
            </div>
          </div>
        </div>
        <button class="btn btn-primary" style="width: 100%; margin-top: 8px" :disabled="saving" @click="saveDraft">
          {{ saving ? '保存中…' : '保存页面' }}
        </button>
      </aside>

      <main class="editor-canvas">
        <div class="editor-toolbar">
          <div class="tag" :class="editorPage.published ? 'tag-good' : 'tag-muted'">{{ editorPage.published ? '已发布' : '草稿未发布' }}</div>
          <div style="flex: 1; font-size: 13px; font-weight: 600">{{ editorPage.name }}</div>
          <button class="btn btn-sm" @click="openPreview">预览</button>
          <button class="btn btn-sm" @click="openSaveTemplateModal">存为模板</button>
          <button class="btn btn-sm" :disabled="saving" @click="saveDraft">{{ saving ? '保存中…' : '保存草稿' }}</button>
          <button class="btn btn-sm btn-primary" :disabled="publishing" @click="publishPage">{{ publishing ? '发布中…' : '发布' }}</button>
        </div>

        <div class="phone-shell">
          <div class="phone-screen" :style="{ ...pageCanvasStyle(), '--canvas-bg': pageStyle.bgColor || DEFAULT_PAGE_BG }" @click.self="selectedIndex = null">
            <div class="phone-status">
              <span>9:41</span>
              <span>{{ editorPage.name || '页面标题' }}</span>
              <span>▮▮▮</span>
            </div>
            <div v-if="items.length === 0" class="empty-state" style="padding: 30px 0">
              <div class="icon">◎</div>
              <div>从左侧点击组件添加到画布</div>
            </div>
            <draggable v-model="items" item-key="_cid" :animation="150" ghost-class="dragging-ghost">
              <template #item="{ element, index }">
                <div class="canvas-block" :class="{ active: selectedIndex === index }" @click="selectItem(index)">
                  <BlockPreview
                    :item="element"
                    :coupon-list="couponList"
                    :seckill-actives="seckillActives"
                    :group-actives="groupActives"
                    :bargain-actives="bargainActives"
                    :store-list="storeList"
                    :goods-meta="goodsMeta"
                    @pick-image="onCanvasPickImage(index)"
                  />
                  <button class="block-remove" title="删除" @click.stop="removeItem(index)">删除</button>
                </div>
              </template>
            </draggable>
          </div>
        </div>
      </main>

      <aside class="editor-props">
        <div class="prop-group-title">页面背景</div>
        <div class="form-hint" style="margin-bottom: 12px">整页底色或背景图，保存并发布后在店铺端生效</div>
        <div class="form-item">
          <label class="form-label">背景类型</label>
          <select v-model="pageStyle.bgType" class="form-select">
            <option value="color">纯色</option>
            <option value="image">背景图</option>
          </select>
        </div>
        <div v-if="pageStyle.bgType === 'image'" class="form-item">
          <label class="form-label">背景图</label>
          <ImageField v-model="pageStyle.bgImage" />
        </div>
        <div class="form-item">
          <label class="form-label">{{ pageStyle.bgType === 'image' ? '底色（图片未铺满时露出）' : '背景色' }}</label>
          <div class="bg-swatches">
            <button
              v-for="color in PAGE_BG_PRESETS"
              :key="color"
              type="button"
              class="bg-swatch"
              :class="{ active: pageStyle.bgColor === color }"
              :style="{ background: color }"
              :title="color"
              @click="pageStyle.bgColor = color"
            />
            <input v-model="pageStyle.bgColor" type="color" class="bg-color-input" />
          </div>
        </div>

        <template v-if="!selectedItem">
          <div class="form-hint">点击画布中的组件可继续编辑该项</div>
        </template>
        <template v-else>
          <div class="prop-group-title" style="margin-top: 8px">{{ blockMeta.title }}</div>
          <div class="form-hint" style="margin-bottom: 16px">{{ blockMeta.hint }}</div>

          <div v-if="selectedItem.bgColor !== undefined" class="form-item">
            <label class="form-label">背景颜色</label>
            <div class="bg-swatches">
              <input v-model="selectedItem.bgColor" type="color" class="bg-color-input" />
              <button class="btn btn-sm" @click="resetBlockBg(selectedItem)">重置</button>
            </div>
          </div>

          <template v-if="selectedItem.type === 'search'">
            <div class="form-item">
              <label class="form-label">占位文字</label>
              <input class="form-input" v-model="selectedItem.placeholder" />
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'banner' || selectedItem.type === 'imageGroup' || selectedItem.type === 'imageWindow'">
            <div class="form-item" v-for="(img, i) in selectedItem.images" :key="i">
              <label class="form-label">{{ selectedItem.type === 'imageWindow' ? ['主图', '右上', '右下'][i] || ('图片 ' + (i + 1)) : ('图片 ' + (i + 1)) }}</label>
              <ImageField v-model="img.url" />
              <input class="form-input" style="margin-top: 8px" v-model="img.link" placeholder="点击跳转链接" />
              <button v-if="selectedItem.type !== 'imageWindow'" class="btn btn-sm" style="margin-top: 6px" @click="removeBannerImage(selectedItem, i)">删除此图</button>
            </div>
            <button v-if="selectedItem.type !== 'imageWindow'" class="btn btn-sm" @click="addBannerImage(selectedItem)">＋ 添加图片</button>
          </template>

          <template v-else-if="selectedItem.type === 'video'">
            <div class="form-item">
              <label class="form-label">上下边距（px）</label>
              <input class="form-input" type="number" v-model.number="selectedItem.margin" min="0" max="80" />
            </div>
            <div class="form-item">
              <label class="form-label">视频高度（px）</label>
              <input class="form-input" type="number" v-model.number="selectedItem.height" min="80" max="400" />
            </div>
            <div class="form-item">
              <label class="form-label">视频封面</label>
              <ImageField v-model="selectedItem.cover" />
            </div>
            <div class="form-item">
              <label class="form-label">视频来源</label>
              <!-- 从素材库选择后，url 以 /uploads/ 开头；手填外链以 http(s):// 开头 -->
              <div v-if="selectedItem.url && !selectedItem.url.startsWith('http')" class="video-source-card">
                <span class="video-source-icon">▶</span>
                <span class="video-source-name">{{ selectedItem.url.split('/').pop() }}</span>
                <button class="btn btn-sm" @click="openVideoPicker(selectedItem)">更换</button>
                <button class="btn btn-sm" @click="selectedItem.url = ''">清除</button>
              </div>
              <template v-else>
                <div style="display: flex; gap: 8px; margin-bottom: 6px">
                  <input class="form-input" v-model="selectedItem.url" placeholder="https://... 外链地址" style="flex: 1" />
                </div>
                <button class="btn btn-sm" style="width: 100%" @click="openVideoPicker(selectedItem)">从素材库选择</button>
                <div class="form-hint" style="margin-top: 6px">外链或从素材库选 MP4（上限 50MB）</div>
              </template>
            </div>
            <div class="form-item">
              <label class="form-label">自动播放</label>
              <select class="form-select" v-model="selectedItem.autoplay">
                <option :value="false">否</option>
                <option :value="true">是</option>
              </select>
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'article'">
            <div class="form-item" v-for="(art, i) in selectedItem.items" :key="i">
              <label class="form-label">文章 {{ i + 1 }}</label>
              <input class="form-input" v-model="art.title" placeholder="标题" />
              <ImageField v-model="art.cover" />
              <input class="form-input" style="margin-top: 6px" type="number" v-model.number="art.views" placeholder="浏览次数" />
              <input class="form-input" style="margin-top: 6px" v-model="art.link" placeholder="跳转链接" />
              <button class="btn btn-sm" style="margin-top: 6px" @click="selectedItem.items.splice(i, 1)">删除</button>
            </div>
            <button class="btn btn-sm" @click="addArticleItem(selectedItem)">＋ 添加文章</button>
          </template>

          <template v-else-if="selectedItem.type === 'news'">
            <div class="form-item">
              <label class="form-label">栏目标签</label>
              <input class="form-input" v-model="selectedItem.label" placeholder="快报" />
            </div>
            <div class="form-item" v-for="(row, i) in selectedItem.items" :key="i">
              <label class="form-label">头条 {{ i + 1 }}</label>
              <input class="form-input" v-model="row.title" placeholder="标题" />
              <input class="form-input" style="margin-top: 6px" v-model="row.link" placeholder="跳转链接" />
            </div>
            <button class="btn btn-sm" @click="addNewsItem(selectedItem)">＋ 添加一条</button>
          </template>

          <template v-else-if="selectedItem.type === 'notice'">
            <div class="form-item">
              <label class="form-label">公告内容</label>
              <input class="form-input" v-model="selectedItem.text" />
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'followMp'">
            <div class="form-item">
              <label class="form-label">标题</label>
              <input class="form-input" v-model="selectedItem.title" />
            </div>
            <div class="form-item">
              <label class="form-label">描述</label>
              <input class="form-input" v-model="selectedItem.desc" />
            </div>
            <div class="form-item">
              <label class="form-label">Logo</label>
              <ImageField v-model="selectedItem.logo" size="sm" />
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'divider'">
            <div class="form-item">
              <label class="form-label">线条颜色</label>
              <input v-model="selectedItem.color" type="color" class="bg-color-input" />
            </div>
            <div class="form-item">
              <label class="form-label">样式</label>
              <select class="form-select" v-model="selectedItem.style">
                <option value="solid">实线</option>
                <option value="dashed">虚线</option>
              </select>
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'navBar'">
            <div class="form-item" v-for="(nav, i) in selectedItem.items" :key="i">
              <label class="form-label">入口 {{ i + 1 }}</label>
              <input class="form-input" style="margin-bottom: 6px" v-model="nav.text" placeholder="文字" />
              <ImageField v-model="nav.icon" size="sm" />
              <input class="form-input" style="margin-top: 6px" v-model="nav.icon" placeholder="也可填写 emoji，如 ◎" />
              <input class="form-input" style="margin-top: 6px" v-model="nav.link" placeholder="如 /pages/diy/custom?id=页面ID 或 store" />
              <button class="btn btn-sm" style="margin-top: 6px" @click="removeNavItem(selectedItem, i)">删除此项</button>
            </div>
            <button class="btn btn-sm" @click="addNavItem(selectedItem)">＋ 添加入口</button>
          </template>

          <template v-else-if="selectedItem.type === 'goods'">
            <div class="form-item">
              <label class="form-label">展示样式</label>
              <select class="form-select" v-model="selectedItem.style">
                <option value="grid">双列瀑布流</option>
                <option value="scroll">横向滑动</option>
                <option value="list">单列大图</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">显示内容</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showName" /> 商品名称</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showPrice" /> 售价</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showLinePrice" /> 划线价</label>
            </div>
            <div class="form-item">
              <label class="form-label">已选商品</label>
              <div v-if="!(selectedItem.goodsIds && selectedItem.goodsIds.length)" class="form-hint" style="margin-bottom: 8px">尚未添加商品</div>
              <div v-else class="diy-goods-list">
                <div v-for="id in selectedItem.goodsIds" :key="id" class="diy-goods-row">
                  <img v-if="goodsCover(id)" :src="goodsCover(id)" alt="" class="diy-goods-thumb" />
                  <div v-else class="diy-goods-thumb diy-goods-thumb-empty">无图</div>
                  <div class="diy-goods-meta">
                    <div class="diy-goods-name">{{ goodsName(id) }}</div>
                    <div class="form-hint">ID {{ id }}</div>
                  </div>
                  <button class="btn btn-sm" @click="removeDiyGoods(selectedItem, id)">移除</button>
                </div>
              </div>
              <GoodsPicker reset-on-select trigger-text="＋ 添加商品" @select="onSelectDiyGoods" />
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'richText'">
            <div class="form-item">
              <label class="form-label">HTML 内容</label>
              <textarea class="form-input" rows="6" v-model="selectedItem.html"></textarea>
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'blank'">
            <div class="form-item">
              <label class="form-label">高度（px）</label>
              <input class="form-input" type="number" v-model.number="selectedItem.height" />
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'coupon'">
            <div class="form-item">
              <label class="form-label">关联优惠券</label>
              <div v-if="couponList.length === 0" class="form-hint">暂无可关联的优惠券，请先在「营销中心 → 优惠券」创建。</div>
              <div class="nav-list" v-else>
                <label v-for="c in couponList" :key="c.id" class="nav-item-row" style="cursor: pointer">
                  <input type="checkbox" :value="c.id" v-model="selectedItem.couponIds" />
                  <span style="flex: 1; font-size: 13px">{{ c.name }}</span>
                </label>
              </div>
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'seckill'">
            <div class="form-item">
              <label class="form-label">关联秒杀专场</label>
              <select class="form-select" v-model.number="selectedItem.activeId">
                <option :value="null">请选择</option>
                <option v-for="a in seckillActives" :key="a.id" :value="a.id">{{ a.name }}</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">商品数量</label>
              <input class="form-input" type="number" v-model.number="selectedItem.limit" min="1" max="20" />
            </div>
            <div class="form-item">
              <label class="form-label">分列数量</label>
              <label class="form-check"><input type="radio" :value="2" v-model.number="selectedItem.columns" /> 两列</label>
              <label class="form-check"><input type="radio" :value="3" v-model.number="selectedItem.columns" /> 三列</label>
            </div>
            <div class="form-item">
              <label class="form-label">显示内容</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showName" /> 商品名称</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showPrice" /> 秒杀价格</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showLinePrice" /> 商品原价</label>
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'group' || selectedItem.type === 'bargain'">
            <div class="form-item">
              <label class="form-label">商品来源</label>
              <label class="form-check"><input type="radio" value="auto" v-model="selectedItem.source" /> 自动获取</label>
              <label class="form-check"><input type="radio" value="manual" v-model="selectedItem.source" /> 手动选择</label>
            </div>
            <div v-if="selectedItem.source === 'manual'" class="form-item">
              <label class="form-label">选择活动</label>
              <div class="nav-list">
                <label v-for="a in (selectedItem.type === 'group' ? groupActives : bargainActives)" :key="a.id" class="nav-item-row" style="cursor: pointer">
                  <input type="checkbox" :value="a.id" v-model="selectedItem.activeIds" />
                  <span style="flex: 1; font-size: 13px">{{ a.name || a.goodsName || ((selectedItem.type === 'group' ? '拼团 #' : '砍价 #') + a.id) }}</span>
                </label>
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">显示数量</label>
              <input class="form-input" type="number" v-model.number="selectedItem.limit" min="1" max="20" />
            </div>
            <div class="form-item">
              <label class="form-label">显示内容</label>
              <label v-if="selectedItem.type === 'group'" class="form-check"><input type="checkbox" v-model="selectedItem.showSellingPoint" /> 商品卖点</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showPrice" /> {{ selectedItem.type === 'group' ? '拼团价格' : '砍价底价' }}</label>
              <label class="form-check"><input type="checkbox" v-model="selectedItem.showLinePrice" /> 划线价</label>
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'store'">
            <div class="form-item">
              <label class="form-label">门店来源</label>
              <label class="form-check"><input type="radio" value="auto" v-model="selectedItem.source" /> 全部门店</label>
              <label class="form-check"><input type="radio" value="manual" v-model="selectedItem.source" /> 手动选择</label>
            </div>
            <div v-if="selectedItem.source === 'manual'" class="form-item">
              <div v-if="!storeList.length" class="form-hint">暂无门店，请先在「门店」里创建。</div>
              <label v-for="s in storeList" :key="s.id" class="form-check">
                <input type="checkbox" :value="s.id" v-model="selectedItem.storeIds" /> {{ s.name }}
              </label>
            </div>
          </template>

          <template v-else-if="selectedItem.type === 'customerService'">
            <div class="form-hint">点击后跳转至客服会话，无需额外配置。</div>
          </template>
        </template>
      </aside>
    </div>
  </template>

  <template v-else>
    <div class="page-header" style="margin-bottom: 14px">
      <div>
        <div class="card-title" style="margin: 0">底部导航</div>
        <div class="card-sub" style="margin-top: 4px">全局唯一，2~5 个菜单项，首尾项固定不可拖动/删除</div>
      </div>
      <button class="btn btn-sm btn-primary" :disabled="tabbarSaving" @click="saveTabbar">{{ tabbarSaving ? '保存中…' : '保存导航' }}</button>
    </div>

    <a-spin :spinning="tabbarLoading">
      <div class="tb-shell">
        <div class="card card-pad tb-left">
          <div class="form-item">
            <label class="form-label">菜单项</label>
            <draggable v-model="tabbarItems" item-key="path" class="nav-list" handle=".nav-drag-handle" :move="onTabbarDragMove" :animation="150">
              <template #item="{ element, index }">
                <div class="nav-item-row" :class="{ locked: index === 0 || index === tabbarItems.length - 1 }">
                  <span class="nav-drag-handle" title="拖拽排序">⠿</span>
                  <span class="nav-icon" style="cursor: pointer" @click="iconPickerIndex = iconPickerIndex === index ? null : index">{{ element.icon || '⭐' }}</span>
                  <div style="flex: 1">
                    <input class="form-input" style="margin-bottom: 6px" v-model="element.text" placeholder="菜单名称" />
                    <input class="form-input" v-model="element.path" placeholder="页面路径，如 /pages/index/index" />
                    <div v-if="iconPickerIndex === index" class="icon-picker-grid">
                      <span
                        v-for="ic in ICON_OPTIONS"
                        :key="ic"
                        class="icon-option"
                        :class="{ selected: element.icon === ic }"
                        @click="pickIcon(index, ic)"
                        >{{ ic }}</span
                      >
                    </div>
                  </div>
                  <span v-if="index === 0 || index === tabbarItems.length - 1" class="tag tag-good">固定项</span>
                  <button v-else class="btn btn-sm" @click="removeTabbarItem(index)">删除</button>
                </div>
              </template>
            </draggable>
          </div>
          <button class="btn btn-sm" :disabled="tabbarItems.length >= 5" @click="addTabbarItem">＋ 新增菜单项</button>
          <div class="form-hint" style="margin-top: 10px">首项与末项为固定入口（不可拖动、不可删除），中间项可拖拽排序、编辑或删除，共 2~5 项。</div>
        </div>

        <div class="card card-pad tb-right">
          <div class="form-label" style="margin-bottom: 10px">实时预览</div>
          <div class="tb-preview-phone">
            <div class="tb-preview-content">页面内容区域</div>
            <div class="tb-preview-bar">
              <div
                v-for="(item, i) in tabbarItems"
                :key="i"
                class="tb-preview-item"
                :class="{ active: previewActiveIndex === i }"
                @click="previewActiveIndex = i"
              >
                <span class="tb-preview-icon">{{ item.icon || '⭐' }}</span>
                <span class="tb-preview-text">{{ item.text || '菜单' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </template>

  <div v-if="pageModalOpen" class="modal-mask" @click.self="closePageModal">
    <div class="modal" style="width: 520px">
      <div class="modal-header">
        <span>新建页面</span>
        <button class="modal-close" aria-label="关闭" @click="closePageModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>页面名称</label>
          <input v-model="newPageForm.name" class="form-input" placeholder="例如：秋冬新品专题页" />
        </div>
        <div class="form-item">
          <label class="form-check">
            <input v-model="newPageForm.setAsHome" type="checkbox" />
            同时设为当前首页
          </label>
        </div>
        <div class="form-hint">创建后进入草稿。设为首页后还需在编辑器里发布，店铺端才会换成这页。</div>
      </div>
      <div class="modal-footer">
        <button class="btn" @click="closePageModal">取消</button>
        <button class="btn btn-primary" @click="createPage">创建并进入编辑</button>
      </div>
    </div>
  </div>

  <div v-if="previewModalOpen" class="modal-mask preview-mask" @click.self="closePreview">
    <div class="preview-dialog">
      <div class="preview-dialog-head">
        <span class="preview-dialog-title">预览 · {{ editorPage.name }}</span>
        <span class="preview-dialog-hint">仅供参考，实际效果以真机为准</span>
        <button class="modal-close" aria-label="关闭" @click="closePreview">×</button>
      </div>
      <div class="preview-dialog-body">
        <div class="preview-phone">
          <div class="preview-notch" />
          <div class="preview-screen" :style="pageCanvasStyle()">
            <div class="preview-status">
              <span>9:41</span>
              <span>{{ editorPage.name || '页面标题' }}</span>
              <span>▮▮▮</span>
            </div>
            <div class="preview-scroll">
              <div v-if="items.length === 0" class="empty-state" style="padding: 30px 0">
                <div class="icon">◎</div>
                <div>暂无内容</div>
              </div>
              <div v-for="element in items" :key="element._cid" class="preview-block">
                <BlockPreview
                  :item="element"
                  :coupon-list="couponList"
                  :seckill-actives="seckillActives"
                  :group-actives="groupActives"
                  :bargain-actives="bargainActives"
                  :store-list="storeList"
                  :goods-meta="goodsMeta"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-if="saveTemplateModalOpen" class="modal-mask" @click.self="closeSaveTemplateModal">
    <div class="modal" style="width: 420px">
      <div class="modal-header">
        <span>存为模板</span>
        <button class="modal-close" aria-label="关闭" @click="closeSaveTemplateModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>模板名称</label>
          <input v-model="saveTemplateForm.name" class="form-input" placeholder="例如：秋季主题模板" />
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" @click="closeSaveTemplateModal">取消</button>
        <button class="btn btn-primary" :disabled="savingTemplate" @click="confirmSaveTemplate">{{ savingTemplate ? '保存中…' : '确定' }}</button>
      </div>
    </div>
  </div>
  <MaterialPicker v-model:open="materialOpen" @select="onMaterialSelect" />
  <MaterialPicker v-model:open="videoPickerOpen" media-type="video" @select="onVideoSelect" />
</template>

<style scoped>
.section-block {
  margin-bottom: 12px;
}

.section-block-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-block-title {
  font-size: 14px;
  font-weight: 700;
  margin-right: 10px;
}

.section-block-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.home-slot-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 14px;
}
.home-slot-main { min-width: 0; }
.home-slot-name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 650;
}
.home-slot-warn {
  margin-top: 8px;
  font-size: 12px;
  color: var(--status-warning-text);
}
.home-slot-empty {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 8px;
  background: var(--surface-2);
  color: var(--text-secondary);
  font-size: 13px;
}
.form-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}

.page-name {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
}

.page-meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.page-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.editor-shell {
  display: grid;
  grid-template-columns: 228px minmax(0, 1fr) 300px;
  gap: 16px;
  align-items: stretch;
  /* 最低高度保证内容稀少时三列仍有可用空间 */
  min-height: calc(100vh - 180px);
}

.editor-left,
.editor-props {
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  padding: 14px;
  /* 在 stretch 的 grid 行中用 100% 拉满，各自独立滚动 */
  height: 100%;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  box-sizing: border-box;
}

.editor-canvas {
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  padding: 14px;
  overflow-y: auto;
  max-height: calc(100vh - 180px);
  box-sizing: border-box;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}

.comp-group-title,
.prop-group-title {
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--text-muted);
}

.comp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;
}

.comp-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 6px;
  border-radius: 8px;
  border: 1px solid var(--border);
  cursor: pointer;
  font-size: 11.5px;
  text-align: center;
  color: var(--text-secondary);
}

.comp-item:hover {
  border-color: var(--primary);
  color: var(--primary-hover);
}

.comp-item.locked {
  opacity: 0.75;
}

.comp-item .ico {
  width: 20px;
  text-align: center;
}

.lock-badge {
  margin-left: auto;
  font-size: 12px;
}

.tpl-list {
  display: grid;
  gap: 6px;
  margin-bottom: 16px;
}

.tpl-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12.5px;
  color: var(--text-secondary);
}

.tpl-item:hover {
  background: var(--surface-2);
  color: var(--primary-hover);
}

.tpl-item .ico {
  width: 16px;
  text-align: center;
}

.tpl-swatch {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  border: 1px solid var(--border);
  flex-shrink: 0;
}

.tpl-meta {
  display: grid;
  min-width: 0;
}

.tpl-name {
  font-weight: 600;
}

.tpl-parts {
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-only {
  cursor: default;
  padding-right: 12px;
}

.phone-shell {
  display: flex;
  justify-content: center;
}

.phone-screen {
  width: 360px;
  min-height: 640px;
  border: 8px solid #222;
  border-radius: 28px;
  background: var(--surface-2);
  padding: 0 0 16px;
  box-sizing: border-box;
  overflow: hidden;
}
.phone-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 36px;
  padding: 0 14px;
  background: #fff;
  font-size: 11px;
  font-weight: 650;
  border-bottom: 1px solid var(--gridline);
}
.preview-screen {
  width: 375px;
}

.canvas-block {
  position: relative;
  border: 1px dashed transparent;
  padding: 8px 12px;
  background: transparent;
  cursor: pointer;
}

.canvas-block:hover {
  border-color: var(--primary);
}

.canvas-block.active {
  border-color: var(--primary);
  border-style: dashed;
  box-shadow: none;
}

.dragging-ghost {
  opacity: 0.4;
}

.block-title {
  font-size: 13px;
  font-weight: 600;
}

.block-desc {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--text-muted);
}

.block-remove {
  display: none;
  position: absolute;
  right: 0;
  bottom: 0;
  height: 22px;
  padding: 0 8px;
  border: none;
  border-radius: 4px 0 0 0;
  background: rgba(43, 43, 43, 0.78);
  color: #fff;
  cursor: pointer;
  font-size: 11px;
}
.canvas-block:hover .block-remove,
.canvas-block.active .block-remove {
  display: block;
}

.block-remove:hover {
  color: #e34948;
  border-color: #e34948;
}

.nav-list {
  display: grid;
  gap: 8px;
}

.nav-item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
}

.nav-item-row.locked {
  background: var(--surface-2);
}

.nav-drag-handle {
  cursor: grab;
  color: var(--text-muted);
}

.nav-icon {
  width: 22px;
  text-align: center;
}

.nav-item-row:last-child {
  margin-bottom: 0;
}

.icon-picker-grid {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.icon-option {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
}

.icon-option:hover {
  border-color: var(--primary);
}

.icon-option.selected {
  border-color: var(--primary);
  background: var(--primary-bg-2);
}

.tb-shell {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.tb-left {
  flex: 1;
  min-width: 0;
}

.tb-right {
  width: 320px;
  flex-shrink: 0;
}

.tb-preview-phone {
  border: 10px solid var(--surface-2);
  border-radius: 24px;
  background: var(--surface-2);
  overflow: hidden;
}

.tb-preview-content {
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface);
  color: var(--text-muted);
  font-size: 12px;
}

.tb-preview-bar {
  display: flex;
  background: var(--surface);
  border-top: 1px solid var(--gridline);
  padding: 6px 0;
}

.tb-preview-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  color: var(--text-muted);
}

.tb-preview-item.active {
  color: var(--primary);
}

.tb-preview-icon {
  font-size: 16px;
}

.tb-preview-text {
  font-size: 10px;
}

.bg-swatches {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.bg-swatch {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid var(--border-strong);
  cursor: pointer;
  padding: 0;
}
.bg-swatch.active {
  box-shadow: 0 0 0 2px var(--primary-bg-2);
  border-color: var(--primary);
}
.bg-color-input {
  width: 36px;
  height: 28px;
  padding: 0;
  border: 1px solid var(--border-strong);
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
}

.media-tile {
  width: 100%;
  height: 88px;
  padding: 0;
  border: 1px dashed var(--border-strong);
  border-radius: var(--r-sm);
  background: var(--surface-2);
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.media-tile-sm {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.nav-icon-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.diy-goods-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.diy-goods-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: var(--surface-2);
}

.diy-goods-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.diy-goods-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-muted);
  background: var(--surface);
  border: 1px solid var(--border);
}

.diy-goods-meta { flex: 1; min-width: 0; }
.diy-goods-name {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- 视频组属性面板 ---- */
.video-source-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: var(--surface-2);
}
.video-source-icon { color: var(--text-muted); flex-shrink: 0; }
.video-source-name {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
}

/* ---- 预览弹窗 ---- */
.preview-mask { z-index: 110; }
.preview-dialog {
  width: 420px;
  max-width: 96vw;
  background: var(--surface);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-modal);
  display: flex;
  flex-direction: column;
  max-height: 92vh;
  overflow: hidden;
}
.preview-dialog-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px 12px;
  border-bottom: 1px solid var(--gridline);
  flex-shrink: 0;
}
.preview-dialog-title { font-size: 14px; font-weight: 700; }
.preview-dialog-hint { font-size: 12px; color: var(--text-muted); flex: 1; }
.preview-dialog-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 0;
  display: flex;
  justify-content: center;
  background: var(--surface-2);
}
.preview-phone {
  width: 375px;
  border: 8px solid #1c1c1e;
  border-radius: 44px;
  background: #1c1c1e;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0,0,0,.3);
  flex-shrink: 0;
}
.preview-notch {
  height: 30px;
  background: #1c1c1e;
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-notch::after {
  content: '';
  width: 100px;
  height: 18px;
  background: #0a0a0a;
  border-radius: 0 0 14px 14px;
}
.preview-screen {
  min-height: 550px;
  border-radius: 0 0 34px 34px;
  overflow: hidden;
}
.preview-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  background: rgba(255,255,255,.06);
  font-size: 11px;
  font-weight: 650;
  color: #fff;
}
.preview-scroll {
  overflow-y: auto;
  max-height: 560px;
}
.preview-block {
  pointer-events: none;
}
/* 视频控件需要可点击才能播放 */
.preview-block :deep(video) {
  pointer-events: auto;
}
</style>
