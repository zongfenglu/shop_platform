<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listCoupons, createCoupon, updateCoupon, deleteCoupon } from '@/api/coupon'
import { listFullReduceRules, createFullReduceRule, updateFullReduceRule, deleteFullReduceRule } from '@/api/fullReduceRule'
import {
  listSeckillTimes, createSeckillTime, updateSeckillTime, deleteSeckillTime,
  listSeckillActives, createSeckillActive, updateSeckillActive, deleteSeckillActive,
  listSeckillGoods, createSeckillGoods, updateSeckillGoods, deleteSeckillGoods,
} from '@/api/seckill'
import {
  listGroupActives, createGroupActive, updateGroupActive, deleteGroupActive,
  listBargainActives, createBargainActive, updateBargainActive, deleteBargainActive,
} from '@/api/group-bargain'
import { getSignConfig, saveSignConfig } from '@/api/sign'
import {
  listPointsGoods, createPointsGoods, updatePointsGoods, deletePointsGoods,
  listExchangeRecords,
} from '@/api/pointsGoods'
import { getGoods } from '@/api/goods'
import { listSpecs } from '@/api/goodsSpec'
import GoodsPicker from '@/components/GoodsPicker.vue'
import ImageField from '@/components/ImageField.vue'

const activeTab = ref('coupon')

/** http 拦截器已经解包 Result.data，列表接口直接返回数组。 */
function asList(res) {
  return Array.isArray(res) ? res : []
}

function parseIdArray(value) {
  // time_ids / apply_range_config 在库里是后端 List<Long> 序列化的裸数字 JSON（如 [2097137709666819951]），
  // 不能 JSON.parse：19 位雪花 id 会被取整成另一个 id，编辑/切状态时还会把坏 id 写回去。只按数字串提取。
  if (Array.isArray(value)) return value.map(String)
  if (typeof value !== 'string' || !value.trim()) return []
  return value.match(/\d+/g) || []
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

const specValueNames = ref({})
const goodsMeta = ref({})
const skuMeta = ref({})

function sameId(a, b) {
  return a != null && b != null && String(a) === String(b)
}

function goodsLabel(id) {
  if (id == null || id === '') return '—'
  return goodsMeta.value[String(id)]?.name || `#${id}`
}

function goodsCover(id) {
  if (id == null || id === '') return ''
  return goodsMeta.value[String(id)]?.image || ''
}

function skuLabelOf(skuId) {
  if (skuId == null || skuId === '') return '—'
  return skuMeta.value[String(skuId)]?.label || `SKU ${skuId}`
}

function couponLabel(id) {
  if (id == null || id === '') return '—'
  const c = coupons.value.find((item) => sameId(item.id, id))
  return c ? `${c.name}（${couponFace(c)}）` : `#${id}`
}

function skuLabel(sku) {
  if (!sku) return '默认规格'
  if (sku.skuCode) return sku.skuCode
  const ids = String(sku.specValueIds || '').split('_').filter(Boolean)
  if (!ids.length) return '默认规格'
  return ids.map((id) => specValueNames.value[String(id)] || id).join(' / ')
}

function indexGoodsDetail(detail) {
  const g = detail?.goods
  if (!g?.id) return
  goodsMeta.value[String(g.id)] = { name: g.name || `#${g.id}`, image: firstGoodsImage(g), code: g.code || '' }
  for (const sku of detail.skus || []) {
    skuMeta.value[String(sku.id)] = { label: skuLabel(sku), price: sku.price, goodsId: g.id }
  }
}

/** 列表里商品的第二行说明：优先展示商品编码；没有编码时退回 id 尾号（完整 19 位没人读得了） */
function goodsSubLabel(id) {
  if (id == null || id === '') return ''
  const meta = goodsMeta.value[String(id)]
  if (meta?.code) return `编码 ${meta.code}`
  const s = String(id)
  return s.length > 8 ? `ID …${s.slice(-8)}` : `ID ${s}`
}

async function rememberGoods(ids) {
  const uniq = [...new Set((ids || []).filter((id) => id != null && id !== '').map(String))]
  await Promise.all(uniq.map(async (id) => {
    if (goodsMeta.value[id]) return
    try {
      const detail = await getGoods(id)
      indexGoodsDetail(detail)
    } catch {
      goodsMeta.value[id] = { name: `#${id}`, image: '' }
    }
  }))
}

async function loadSpecNames() {
  try {
    const specs = asList(await listSpecs())
    const map = {}
    for (const spec of specs) {
      for (const v of spec.values || []) {
        map[String(v.id)] = v.value
      }
    }
    specValueNames.value = map
  } catch {
    specValueNames.value = {}
  }
}

// ---------- 优惠券 ----------
const coupons = ref([])
const couponModalOpen = ref(false)
const editingCouponId = ref(null)
const couponForm = reactive({
  name: '', type: 'reduce', reducePrice: 10, discountRatio: 0.9, minPrice: 100,
  expireType: 'fixed', startTime: '', endTime: '', expireDays: 7,
  totalNum: 1000, limitPerUser: 1, applyRange: 'all', applyRangeConfig: '', status: 'on',
})

async function loadCoupons() {
  coupons.value = asList(await listCoupons())
}

function openCreateCoupon() {
  editingCouponId.value = null
  Object.assign(couponForm, {
    name: '', type: 'reduce', reducePrice: 10, discountRatio: 0.9, minPrice: 100,
    expireType: 'fixed', startTime: '', endTime: '', expireDays: 7,
    totalNum: 1000, limitPerUser: 1, applyRange: 'all', applyRangeConfig: '', status: 'on',
  })
  couponModalOpen.value = true
}

function openEditCoupon(item) {
  editingCouponId.value = item.id
  Object.assign(couponForm, {
    name: item.name, type: item.type,
    reducePrice: item.reducePrice ?? 10, discountRatio: item.discountRatio ?? 0.9,
    minPrice: item.minPrice ?? 0, expireType: item.expireType,
    // 优惠时间精确到日，后端存的是 LocalDateTime，取日期部分展示
    startTime: (item.startTime || '').slice(0, 10), endTime: (item.endTime || '').slice(0, 10),
    expireDays: item.expireDays ?? 7,
    totalNum: item.totalNum ?? 0, limitPerUser: item.limitPerUser ?? 1,
    applyRange: item.applyRange || 'all',
    applyRangeConfig: Array.isArray(item.applyRangeConfig) ? item.applyRangeConfig.join(',') : (item.applyRangeConfig || ''),
    status: item.status || 'on',
  })
  couponModalOpen.value = true
}

function closeCouponModal() { couponModalOpen.value = false }

async function submitCoupon() {
  if (!couponForm.name.trim()) {
    message.warning('请填写优惠券名称')
    return
  }
  if (couponForm.expireType === 'fixed') {
    if (!couponForm.startTime || !couponForm.endTime) {
      message.warning('请选择优惠开始和结束日期')
      return
    }
    if (couponForm.startTime > couponForm.endTime) {
      message.warning('开始日期不能晚于结束日期')
      return
    }
  }
  const payload = {
    name: couponForm.name.trim(), type: couponForm.type,
    reducePrice: couponForm.type === 'reduce' ? Number(couponForm.reducePrice) : null,
    discountRatio: couponForm.type === 'discount' ? Number(couponForm.discountRatio) : null,
    minPrice: Number(couponForm.minPrice) || 0,
    expireType: couponForm.expireType,
    // 精确到日：起始日 00:00:00 生效、结束日 23:59:59 失效
    startTime: couponForm.expireType === 'fixed' ? couponForm.startTime + 'T00:00:00' : null,
    endTime: couponForm.expireType === 'fixed' ? couponForm.endTime + 'T23:59:59' : null,
    expireDays: couponForm.expireType === 'receive' ? Number(couponForm.expireDays) : null,
    totalNum: Number(couponForm.totalNum) || 0,
    limitPerUser: Math.max(0, Number(couponForm.limitPerUser) || 0),
    applyRange: couponForm.applyRange,
    applyRangeConfig: couponForm.applyRange === 'all' ? [] : parseIds(couponForm.applyRangeConfig),
    status: couponForm.status,
  }
  if (editingCouponId.value) await updateCoupon(editingCouponId.value, payload)
  else await createCoupon(payload)
  closeCouponModal()
  await loadCoupons()
}

async function removeCoupon(id) {
  if (!confirm('确认删除该优惠券？')) return
  await deleteCoupon(id)
  await loadCoupons()
}

async function toggleCouponStatus(item) {
  const next = item.status === 'on' ? 'off' : 'on'
  // applyRangeConfig 后端返回的是 JSON 字符串（如 '["1","2"]'），要还原成数组再回传
  await updateCoupon(item.id, { ...item, applyRangeConfig: parseIdArray(item.applyRangeConfig), status: next })
  await loadCoupons()
}

function couponFace(item) {
  if (item.type === 'discount') return (Number(item.discountRatio) * 10).toFixed(1).replace(/\.0$/, '') + '折'
  return '¥' + item.reducePrice
}
function couponCondition(item) { return Number(item.minPrice) > 0 ? `满${item.minPrice}可用` : '无门槛' }
function couponValidity(item) {
  if (item.expireType === 'receive') return `领取后 ${item.expireDays || 7} 天内有效`
  return `${(item.startTime || '').slice(0, 10)} ~ ${(item.endTime || '').slice(0, 10)}`
}
function couponScope(item) {
  if (item.applyRange === 'category') return '指定分类'
  if (item.applyRange === 'goods') return '指定商品'
  return '全场通用'
}
function couponProgress(item) {
  if (!item.totalNum) return 0
  return Math.min(100, Math.round((item.receivedNum / item.totalNum) * 100))
}
function couponStatusText(item) { return { on: '进行中', off: '已暂停', ended: '已结束' }[item.status] || item.status }
function couponStatusClass(item) { return { on: 'tag-good tag-dot', off: 'tag-warning', ended: 'tag-muted' }[item.status] || 'tag-muted' }

// ---------- 满减规则 ----------
const fullReduceRules = ref([])
const fullReduceModalOpen = ref(false)
const editingRuleId = ref(null)
const ruleForm = reactive({
  name: '', type: 'money',
  tiers: [{ threshold: 100, reduce: 10, discount: '' }],
  freeExpress: 0, status: 'on', sort: 0,
})

async function loadRules() {
  fullReduceRules.value = asList(await listFullReduceRules())
}

function openCreateRule() {
  editingRuleId.value = null
  Object.assign(ruleForm, {
    name: '', type: 'money',
    tiers: [{ threshold: 100, reduce: 10, discount: '' }],
    freeExpress: 0, status: 'on', sort: 0,
  })
  fullReduceModalOpen.value = true
}

function parseRuleTiers(raw) {
  let list = raw
  if (typeof raw === 'string') {
    try {
      list = JSON.parse(raw)
    } catch (e) {
      return []
    }
  }
  return Array.isArray(list) ? list : []
}

function openEditRule(item) {
  editingRuleId.value = item.id
  const tiers = parseRuleTiers(item.rules).map(t => ({ threshold: t.threshold, reduce: t.reduce ?? '', discount: t.discount ?? '' }))
  Object.assign(ruleForm, {
    name: item.name, type: item.type,
    tiers: tiers.length ? tiers : [{ threshold: 0, reduce: 0, discount: '' }],
    freeExpress: item.freeExpress || 0, status: item.status || 'on', sort: item.sort || 0,
  })
  fullReduceModalOpen.value = true
}

function closeRuleModal() { fullReduceModalOpen.value = false }
function addTier() { ruleForm.tiers.push({ threshold: 0, reduce: 0, discount: '' }) }
function removeTier(idx) { if (ruleForm.tiers.length > 1) ruleForm.tiers.splice(idx, 1) }

async function submitRule() {
  if (!ruleForm.name.trim()) return
  const tiers = ruleForm.tiers.map(t => {
    if (ruleForm.type === 'count') return { threshold: Number(t.threshold), discount: Number(t.discount) }
    return { threshold: Number(t.threshold), reduce: Number(t.reduce) }
  })
  const payload = {
    name: ruleForm.name.trim(), type: ruleForm.type, rules: tiers,
    freeExpress: Number(ruleForm.freeExpress) || 0, status: ruleForm.status, sort: Number(ruleForm.sort) || 0,
  }
  if (editingRuleId.value) await updateFullReduceRule(editingRuleId.value, payload)
  else await createFullReduceRule(payload)
  closeRuleModal()
  await loadRules()
}

async function removeRule(id) {
  if (!confirm('确认删除该满减规则？')) return
  await deleteFullReduceRule(id)
  await loadRules()
}

function ruleSummary(item) {
  const tiers = parseRuleTiers(item.rules)
  if (!tiers.length) return '—'
  return tiers.map(t => {
    if (item.type === 'count') return `满${t.threshold}件${(Number(t.discount) * 10).toFixed(1).replace(/\.0$/, '')}折`
    return `满${t.threshold}减${t.reduce}`
  }).join('，')
}

// ---------- 秒杀/限时折扣 ----------
const seckillTimes = ref([])
const seckillActives = ref([])
const seckillGoodsList = ref([])
const selectedActiveId = ref(null)

const timeModalOpen = ref(false)
const editingTimeId = ref(null)
const timeForm = reactive({ name: '', startTime: '10:00', endTime: '12:00', sort: 0, status: 'on' })

const activeModalOpen = ref(false)
const editingActiveId = ref(null)
const activeForm = reactive({ name: '', timeIds: [], startDate: '', endDate: '', status: 'on', remark: '' })

const goodsModalOpen = ref(false)
const editingGoodsId = ref(null)
const goodsForm = reactive({ activeId: null, goodsId: null, skuId: null, seckillPrice: '', seckillNum: 0, limitPerUser: 0, status: 'on', sort: 0 })
const seckillSkuOptions = ref([])
const seckillSelectedGoods = ref(null)

async function loadTimes() {
  seckillTimes.value = asList(await listSeckillTimes())
}
async function loadActives() {
  seckillActives.value = asList(await listSeckillActives())
}
async function loadGoods(activeId) {
  if (!activeId) { seckillGoodsList.value = []; return }
  seckillGoodsList.value = asList(await listSeckillGoods(activeId))
  await rememberGoods(seckillGoodsList.value.map((g) => g.goodsId))
}

function openCreateTime() {
  editingTimeId.value = null
  Object.assign(timeForm, { name: '', startTime: '10:00', endTime: '12:00', sort: 0, status: 'on' })
  timeModalOpen.value = true
}
function openEditTime(t) {
  editingTimeId.value = t.id
  Object.assign(timeForm, { name: t.name, startTime: t.startTime, endTime: t.endTime, sort: t.sort || 0, status: t.status || 'on' })
  timeModalOpen.value = true
}
function closeTimeModal() { timeModalOpen.value = false }
async function submitTime() {
  if (!timeForm.name.trim()) {
    message.warning('请填写场次名称')
    return
  }
  if (!timeForm.startTime || !timeForm.endTime) {
    message.warning('请选择场次开始和结束时间')
    return
  }
  if (timeForm.startTime >= timeForm.endTime) {
    message.warning('场次开始时间必须早于结束时间')
    return
  }
  const payload = { name: timeForm.name.trim(), startTime: timeForm.startTime, endTime: timeForm.endTime, sort: Number(timeForm.sort) || 0, status: timeForm.status }
  if (editingTimeId.value) await updateSeckillTime(editingTimeId.value, payload)
  else await createSeckillTime(payload)
  closeTimeModal()
  await loadTimes()
}
async function removeTime(id) {
  if (!confirm('确认删除该场次？')) return
  await deleteSeckillTime(id)
  await loadTimes()
}

function openCreateActive() {
  editingActiveId.value = null
  Object.assign(activeForm, { name: '', timeIds: [], startDate: todayStr(), endDate: todayStr(), status: 'on', remark: '' })
  activeModalOpen.value = true
}
function openEditActive(a) {
  editingActiveId.value = a.id
  Object.assign(activeForm, {
    // 统一转 String 与场次 checkbox 的 value 对齐——time_ids 里可能存的是数字（老数据），类型不一致会导致勾选状态丢失
    name: a.name, timeIds: parseIdArray(a.timeIds).map(String),
    startDate: a.startDate, endDate: a.endDate, status: a.status || 'on', remark: a.remark || '',
  })
  activeModalOpen.value = true
}
function closeActiveModal() { activeModalOpen.value = false }
async function submitActive() {
  if (!activeForm.name.trim()) {
    message.warning('请填写活动名称')
    return
  }
  if (!activeForm.startDate || !activeForm.endDate) {
    message.warning('请选择活动开始和结束日期')
    return
  }
  if (activeForm.startDate > activeForm.endDate) {
    message.warning('活动开始日期不能晚于结束日期')
    return
  }
  const payload = {
    name: activeForm.name.trim(),
    timeIds: activeForm.timeIds || [],
    startDate: activeForm.startDate, endDate: activeForm.endDate,
    status: activeForm.status, remark: activeForm.remark || undefined,
  }
  if (editingActiveId.value) await updateSeckillActive(editingActiveId.value, payload)
  else await createSeckillActive(payload)
  closeActiveModal()
  await loadActives()
}
async function removeActive(id) {
  if (!confirm('确认删除该活动？关联的活动商品也会一并失效。')) return
  await deleteSeckillActive(id)
  if (selectedActiveId.value === id) { selectedActiveId.value = null; seckillGoodsList.value = [] }
  await loadActives()
}
function selectActive(id) {
  selectedActiveId.value = id
  loadGoods(id)
}
function activeTimeNames(a) {
  const ids = parseIdArray(a.timeIds)
  if (!ids.length) return '限时折扣（全天）'
  // sameId：time_ids 里可能是数字（老数据）也可能是字符串，统一按字符串比
  return ids.map((id) => seckillTimes.value.find((t) => sameId(t.id, id))?.name || ('#' + id)).join('、')
}

function openCreateGoods() {
  if (!selectedActiveId.value) return
  editingGoodsId.value = null
  seckillSelectedGoods.value = null
  seckillSkuOptions.value = []
  Object.assign(goodsForm, { activeId: selectedActiveId.value, goodsId: null, skuId: null, seckillPrice: '', seckillNum: 0, limitPerUser: 0, status: 'on', sort: 0 })
  goodsModalOpen.value = true
}
function openEditGoods(g) {
  editingGoodsId.value = g.id
  seckillSelectedGoods.value = null
  seckillSkuOptions.value = []
  Object.assign(goodsForm, {
    activeId: g.activeId, goodsId: g.goodsId, skuId: g.skuId,
    seckillPrice: g.seckillPrice, seckillNum: g.seckillNum || 0, limitPerUser: g.limitPerUser || 0,
    status: g.status || 'on', sort: g.sort || 0,
  })
  goodsModalOpen.value = true
}
function closeGoodsModal() { goodsModalOpen.value = false }
function onSeckillGoodsLoaded({ goods, skus }) {
  indexGoodsDetail({ goods, skus })
  seckillSelectedGoods.value = goods || null
  seckillSkuOptions.value = skus || []
  if (goodsForm.skuId && !seckillSkuOptions.value.some((s) => sameId(s.id, goodsForm.skuId))) {
    goodsForm.skuId = null
  }
  if (!goodsForm.skuId && seckillSkuOptions.value.length === 1) {
    goodsForm.skuId = seckillSkuOptions.value[0].id
    if (goodsForm.seckillPrice === '' || goodsForm.seckillPrice == null) {
      goodsForm.seckillPrice = seckillSkuOptions.value[0].price
    }
  }
}
function onSeckillSkuChange() {
  const sku = seckillSkuOptions.value.find((s) => sameId(s.id, goodsForm.skuId))
  if (sku && (goodsForm.seckillPrice === '' || goodsForm.seckillPrice == null)) {
    goodsForm.seckillPrice = sku.price
  }
}
async function submitGoods() {
  if (!goodsForm.goodsId || !goodsForm.skuId || goodsForm.seckillPrice === '' || goodsForm.seckillPrice == null) {
    message.warning('请选择商品、SKU 并填写秒杀价')
    return
  }
  if (Number(goodsForm.seckillPrice) <= 0) {
    message.warning('秒杀价必须大于 0')
    return
  }
  if (seckillSelectedGoods.value && seckillSelectedGoods.value.status !== 'on') {
    message.warning('该商品当前在仓库中（未上架），请先上架再参与活动')
    return
  }
  // 同一活动同一 SKU 不允许重复添加，先在前端拦一道，后端也有校验兜底
  const dup = seckillGoodsList.value.find((g) =>
    sameId(g.skuId, goodsForm.skuId) && (!editingGoodsId.value || !sameId(g.id, editingGoodsId.value)))
  if (dup) {
    message.warning('该商品规格已在当前活动中，请勿重复添加')
    return
  }
  const payload = {
    activeId: selectedActiveId.value || goodsForm.activeId,
    // id 一律保持字符串直传：19 位雪花 id 经 Number() 会丢精度，存进去的就是另一个 id（见 CONTRIBUTING.md）
    goodsId: String(goodsForm.goodsId), skuId: String(goodsForm.skuId),
    seckillPrice: Number(goodsForm.seckillPrice),
    seckillNum: Number(goodsForm.seckillNum) || 0, limitPerUser: Number(goodsForm.limitPerUser) || 0,
    status: goodsForm.status, sort: Number(goodsForm.sort) || 0,
  }
  if (editingGoodsId.value) await updateSeckillGoods(editingGoodsId.value, payload)
  else await createSeckillGoods(payload)
  closeGoodsModal()
  await loadGoods(selectedActiveId.value)
}
async function removeGoods(id) {
  if (!confirm('确认删除该秒杀商品？')) return
  await deleteSeckillGoods(id)
  await loadGoods(selectedActiveId.value)
}
function goodsProgress(g) {
  if (!g.seckillNum) return 0
  return Math.min(100, Math.round(((g.sold || 0) / g.seckillNum) * 100))
}
function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

// 拼团/砍价列表展示工具
function minGroupPrice(a) {
  try {
    const obj = JSON.parse(a.groupPrice || '{}')
    const vals = Object.values(obj).map((v) => Number(v))
    if (!vals.length) return '-'
    return Math.min(...vals)
  } catch (e) { return '-' }
}
function fmtTime(dt) {
  if (!dt) return ''
  return String(dt).replace('T', ' ').slice(0, 16)
}

const summary = computed(() => ({
  runningCoupons: coupons.value.filter((c) => c.status === 'on').length,
  activeRules: fullReduceRules.value.filter((r) => r.status === 'on').length,
  runningActives: seckillActives.value.filter((a) => a.status === 'on').length,
  runningGroups: groupActives.value.filter((a) => a.status === 'on').length,
  runningBargains: bargainActives.value.filter((a) => a.status === 'on').length,
}))

// ---------- 拼团 ----------
const groupActives = ref([])
const groupModalOpen = ref(false)
const editingGroupId = ref(null)
const groupForm = reactive({
  goodsId: null, groupNum: 2, validHours: 24, isMock: 0,
  startTime: '', endTime: '', status: 'on',
})
const groupSkuRows = ref([])
let pendingGroupPrices = null

async function loadGroups() {
  groupActives.value = asList(await listGroupActives())
  await rememberGoods(groupActives.value.map((a) => a.goodsId))
}
function openCreateGroup() {
  editingGroupId.value = null
  pendingGroupPrices = null
  groupSkuRows.value = []
  Object.assign(groupForm, { goodsId: null, groupNum: 2, validHours: 24, isMock: 0, startTime: nowLocal(), endTime: nowLocalPlusHours(24), status: 'on' })
  groupModalOpen.value = true
}
function parseGroupPriceMap(raw) {
  if (!raw) return {}
  if (typeof raw === 'object' && !Array.isArray(raw)) return raw
  try { return JSON.parse(raw) || {} } catch { return {} }
}
function openEditGroup(a) {
  editingGroupId.value = a.id
  pendingGroupPrices = parseGroupPriceMap(a.groupPrice)
  groupSkuRows.value = []
  Object.assign(groupForm, {
    goodsId: a.goodsId, groupNum: a.groupNum,
    validHours: a.validHours, isMock: a.isMock || 0,
    startTime: toLocal(a.startTime), endTime: toLocal(a.endTime), status: a.status || 'on',
  })
  groupModalOpen.value = true
}
function closeGroupModal() { groupModalOpen.value = false }
function onGroupGoodsLoaded({ goods, skus }) {
  indexGoodsDetail({ goods, skus })
  const prices = pendingGroupPrices || {}
  pendingGroupPrices = null
  groupSkuRows.value = (skus || []).map((sku) => {
    const saved = prices[sku.id] ?? prices[String(sku.id)]
    return {
      skuId: sku.id,
      label: skuLabel(sku),
      price: sku.price,
      stock: sku.stock,
      groupPrice: saved ?? sku.price,
    }
  })
}
async function submitGroup() {
  if (!groupForm.goodsId) {
    message.warning('请选择商品')
    return
  }
  const groupPrice = {}
  for (const row of groupSkuRows.value) {
    if (row.groupPrice === '' || row.groupPrice == null) continue
    groupPrice[String(row.skuId)] = Number(row.groupPrice)
  }
  if (!Object.keys(groupPrice).length) {
    message.warning('请至少填写一个 SKU 的拼团价')
    return
  }
  const payload = {
    goodsId: String(groupForm.goodsId), groupNum: Number(groupForm.groupNum) || 2,
    groupPrice, validHours: Number(groupForm.validHours) || 24,
    isMock: Number(groupForm.isMock) || 0,
    startTime: groupForm.startTime, endTime: groupForm.endTime, status: groupForm.status,
  }
  if (editingGroupId.value) await updateGroupActive(editingGroupId.value, payload)
  else await createGroupActive(payload)
  closeGroupModal()
  await loadGroups()
}
async function removeGroup(id) {
  if (!confirm('确认删除该拼团活动？')) return
  await deleteGroupActive(id)
  await loadGroups()
}

// ---------- 砍价 ----------
const bargainActives = ref([])
const bargainModalOpen = ref(false)
const editingBargainId = ref(null)
const bargainForm = reactive({
  goodsId: null, floorPrice: '', validHours: 24, helpLimit: 0,
  startTime: '', endTime: '', status: 'on',
})

async function loadBargains() {
  bargainActives.value = asList(await listBargainActives())
  await rememberGoods(bargainActives.value.map((a) => a.goodsId))
}
function openCreateBargain() {
  editingBargainId.value = null
  Object.assign(bargainForm, { goodsId: null, floorPrice: '', validHours: 24, helpLimit: 0, startTime: nowLocal(), endTime: nowLocalPlusHours(48), status: 'on' })
  bargainModalOpen.value = true
}
function openEditBargain(a) {
  editingBargainId.value = a.id
  Object.assign(bargainForm, {
    goodsId: a.goodsId, floorPrice: a.floorPrice, validHours: a.validHours, helpLimit: a.helpLimit || 0,
    startTime: toLocal(a.startTime), endTime: toLocal(a.endTime), status: a.status || 'on',
  })
  bargainModalOpen.value = true
}
function closeBargainModal() { bargainModalOpen.value = false }
function onBargainGoodsLoaded({ goods, skus }) {
  indexGoodsDetail({ goods, skus })
}
async function submitBargain() {
  if (!bargainForm.goodsId || bargainForm.floorPrice === '' || bargainForm.floorPrice == null) {
    message.warning('请选择商品并填写底价')
    return
  }
  const payload = {
    goodsId: String(bargainForm.goodsId), floorPrice: Number(bargainForm.floorPrice),
    validHours: Number(bargainForm.validHours) || 24, helpLimit: Number(bargainForm.helpLimit) || 0,
    startTime: bargainForm.startTime, endTime: bargainForm.endTime, status: bargainForm.status,
  }
  if (editingBargainId.value) await updateBargainActive(editingBargainId.value, payload)
  else await createBargainActive(payload)
  closeBargainModal()
  await loadBargains()
}
async function removeBargain(id) {
  if (!confirm('确认删除该砍价活动？')) return
  await deleteBargainActive(id)
  await loadBargains()
}

// 时间工具：LocalDateTime(无时区) → datetime-local 需要 YYYY-MM-DDTHH:mm
function toLocal(dt) {
  if (!dt) return ''
  return String(dt).replace(' ', 'T').slice(0, 16)
}
function nowLocal() {
  const d = new Date(); d.setSeconds(0, 0)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function nowLocalPlusHours(h) {
  const d = new Date(); d.setHours(d.getHours() + h); d.setSeconds(0, 0)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function parseIds(text) {
  // id 保持字符串：Number() 会把 19 位雪花 id 四舍五入成另一个 id，后端 Long 反序列化接受数字字符串
  if (!text) return []
  return text.split(',').map((s) => s.trim()).filter((s) => /^\d+$/.test(s))
}

function headerAction() {
  if (activeTab.value === 'coupon') openCreateCoupon()
  else if (activeTab.value === 'fullReduce') openCreateRule()
  else if (activeTab.value === 'seckill') openCreateActive()
  else if (activeTab.value === 'group') openCreateGroup()
  else if (activeTab.value === 'bargain') openCreateBargain()
  else if (activeTab.value === 'pointsMall') openCreatePointsGoods()
}

const headerBtnLabel = computed(() => ({
  coupon: '新建优惠券', fullReduce: '新建满减', seckill: '新建秒杀活动',
  group: '新建拼团', bargain: '新建砍价', sign: '', pointsMall: '新建兑换项',
}[activeTab.value]))

// ---------- 签到配置 ----------
const signConfig = ref(null)
const signDailyPoints = ref(2)
const signRules = ref([])

function emptySignRule() {
  return { days: 7, type: 'points', value: 10, couponId: null }
}

function parseSignRules(raw) {
  let list = raw
  if (typeof raw === 'string') {
    try { list = JSON.parse(raw) } catch { return [] }
  }
  if (!Array.isArray(list)) return []
  return list.map((r) => ({
    days: r.days ?? 7,
    type: r.type === 'coupon' ? 'coupon' : 'points',
    value: r.value ?? 10,
    couponId: r.couponId ?? null,
  }))
}

async function loadSignConfig() {
  try {
    const data = await getSignConfig()
    signConfig.value = data
    signDailyPoints.value = data?.dailyPoints ?? 2
    signRules.value = parseSignRules(data?.continuousRules)
  } catch (e) { /* 后端未部署时保持默认值 */ }
}

function addSignRule() {
  signRules.value.push(emptySignRule())
}
function removeSignRule(idx) {
  signRules.value.splice(idx, 1)
}

async function submitSignConfig() {
  const rules = []
  for (const r of signRules.value) {
    if (!r.days) continue
    if (r.type === 'coupon') {
      if (!r.couponId) {
        message.warning(`连续 ${r.days} 天的奖励请选择优惠券`)
        return
      }
      rules.push({ days: Number(r.days), type: 'coupon', couponId: String(r.couponId) })
    } else {
      rules.push({ days: Number(r.days), type: 'points', value: Number(r.value) || 0 })
    }
  }
  await saveSignConfig({ dailyPoints: Number(signDailyPoints.value) || 2, continuousRules: rules })
  message.success('签到配置已保存')
  await loadSignConfig()
}

// ---------- 积分商城 ----------
const pointsGoods = ref([])
const pointsMallModalOpen = ref(false)
const editingPointsGoodsId = ref(null)
const pointsGoodsForm = reactive({
  name: '', image: '', type: 'coupon', goodsId: null, couponId: null,
  points: 100, cash: 0, stock: 0, status: 'on', sort: 0,
})
const exchangeRecords = ref([])

async function loadPointsGoods() {
  try {
    pointsGoods.value = asList(await listPointsGoods())
    await rememberGoods(pointsGoods.value.map((item) => item.goodsId))
  } catch (e) { /* */ }
}
async function loadExchangeRecs() {
  try { exchangeRecords.value = asList(await listExchangeRecords()) } catch (e) { /* */ }
}
function openCreatePointsGoods() {
  editingPointsGoodsId.value = null
  Object.assign(pointsGoodsForm, { name: '', image: '', type: 'coupon', goodsId: null, couponId: null, points: 100, cash: 0, stock: 0, status: 'on', sort: 0 })
  pointsMallModalOpen.value = true
}
function openEditPointsGoods(item) {
  editingPointsGoodsId.value = item.id
  const isCoupon = item.couponId != null
  Object.assign(pointsGoodsForm, {
    name: item.name, image: item.image || '', type: isCoupon ? 'coupon' : 'goods',
    goodsId: item.goodsId || null, couponId: item.couponId || null,
    points: item.points, cash: item.cash || 0, stock: item.stock || 0,
    status: item.status || 'on', sort: item.sort || 0,
  })
  pointsMallModalOpen.value = true
}
function closePointsMallModal() { pointsMallModalOpen.value = false }
function onPointsGoodsLoaded({ goods, skus }) {
  indexGoodsDetail({ goods, skus })
}
async function submitPointsGoods() {
  if (!pointsGoodsForm.name.trim()) {
    message.warning('请填写兑换项名称')
    return
  }
  if (pointsGoodsForm.type === 'coupon' && !pointsGoodsForm.couponId) {
    message.warning('请选择要发放的优惠券')
    return
  }
  if (pointsGoodsForm.type === 'goods' && !pointsGoodsForm.goodsId) {
    message.warning('请选择要发放的商品')
    return
  }
  const payload = {
    name: pointsGoodsForm.name.trim(), image: pointsGoodsForm.image || null,
    goodsId: pointsGoodsForm.type === 'goods' ? String(pointsGoodsForm.goodsId) : null,
    couponId: pointsGoodsForm.type === 'coupon' ? String(pointsGoodsForm.couponId) : null,
    points: Number(pointsGoodsForm.points), cash: Number(pointsGoodsForm.cash) || 0,
    stock: Number(pointsGoodsForm.stock) || 0, status: pointsGoodsForm.status,
    sort: Number(pointsGoodsForm.sort) || 0,
  }
  if (editingPointsGoodsId.value) await updatePointsGoods(editingPointsGoodsId.value, payload)
  else await createPointsGoods(payload)
  closePointsMallModal()
  await loadPointsGoods()
}
async function removePointsGoods(id) {
  if (!confirm('确认删除？')) return
  await deletePointsGoods(id)
  await loadPointsGoods()
}

onMounted(async () => {
  await loadSpecNames()
  loadCoupons()
  loadRules()
  loadTimes()
  loadActives()
  loadGroups()
  loadBargains()
  loadSignConfig()
  loadPointsGoods()
  loadExchangeRecs()
})
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">营销中心</div>
      <div class="page-desc">优惠券、满减、秒杀/限时折扣、拼团、砍价统一管理。</div>
    </div>
    <button v-if="headerBtnLabel" class="btn btn-primary" @click="headerAction">＋ {{ headerBtnLabel }}</button>
  </div>

  <div class="grid grid-2" style="margin-bottom: 16px">
    <div class="card kpi"><div class="kpi-label">进行中优惠券</div><div class="kpi-value">{{ summary.runningCoupons }}</div></div>
    <div class="card kpi"><div class="kpi-label">启用满减规则</div><div class="kpi-value">{{ summary.activeRules }}</div></div>
  </div>

  <div class="tabs" style="margin-bottom: 16px">
    <div class="tab" :class="{ active: activeTab === 'coupon' }" @click="activeTab = 'coupon'">优惠券</div>
    <div class="tab" :class="{ active: activeTab === 'fullReduce' }" @click="activeTab = 'fullReduce'">满减</div>
    <div class="tab" :class="{ active: activeTab === 'seckill' }" @click="activeTab = 'seckill'">秒杀/限时折扣</div>
    <div class="tab" :class="{ active: activeTab === 'group' }" @click="activeTab = 'group'">拼团</div>
    <div class="tab" :class="{ active: activeTab === 'bargain' }" @click="activeTab = 'bargain'">砍价</div>
    <div class="tab" :class="{ active: activeTab === 'sign' }" @click="activeTab = 'sign'">签到</div>
    <div class="tab" :class="{ active: activeTab === 'pointsMall' }" @click="activeTab = 'pointsMall'">积分商城</div>
  </div>

  <div v-if="activeTab === 'coupon'" style="display: flex; flex-direction: column; gap: 14px">
    <div v-for="item in coupons" :key="item.id" class="card" style="display: flex; overflow: hidden">
      <div class="coupon-face">
        <div class="amt">{{ couponFace(item) }}</div>
        <div class="cond">{{ couponCondition(item) }}</div>
      </div>
      <div class="coupon-body">
        <div class="coupon-row">
          <div>
            <div style="font-weight: 600">{{ item.name }}</div>
            <div class="coupon-meta">
              <span>有效期：{{ couponValidity(item) }}</span>
              <span>适用：{{ couponScope(item) }}</span>
              <span>每人限领 {{ item.limitPerUser > 0 ? item.limitPerUser : '不限' }} 张</span>
            </div>
          </div>
          <div style="text-align: right">
            <span class="tag" :class="couponStatusClass(item)">{{ couponStatusText(item) }}</span>
            <div style="margin-top: 8px">
              <button class="btn btn-sm" @click="openEditCoupon(item)">编辑</button>
              <button class="btn btn-sm" @click="toggleCouponStatus(item)">{{ item.status === 'on' ? '暂停' : '上架' }}</button>
              <button class="btn btn-sm" @click="removeCoupon(item.id)">删除</button>
            </div>
          </div>
        </div>
        <div class="progress-track"><div class="progress-fill" :style="{ width: `${couponProgress(item)}%` }" /></div>
        <div style="font-size: 11.5px; color: var(--text-muted); margin-top: 4px">已领取 {{ item.receivedNum || 0 }} / {{ item.totalNum || 0 }} 张</div>
      </div>
    </div>
    <div v-if="!coupons.length" class="card card-pad" style="text-align:center;color:var(--text-muted)">暂无优惠券，点击右上角新建</div>
  </div>

  <!-- 优惠券弹窗 -->
  <div v-if="couponModalOpen" class="modal-mask" @click.self="closeCouponModal">
    <div class="modal" style="width: 560px">
      <div class="modal-header">
        <span>{{ editingCouponId ? '编辑优惠券' : '新建优惠券' }}</span>
        <button class="modal-close" aria-label="关闭" @click="closeCouponModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>优惠券名称</label>
          <input v-model="couponForm.name" class="form-input" placeholder="例如：新人专享券" />
        </div>
        <div class="form-item">
          <label class="form-label">优惠类型</label>
          <select v-model="couponForm.type" class="form-select">
            <option value="reduce">满减券（固定金额抵扣）</option>
            <option value="discount">折扣券（按比例打折）</option>
          </select>
        </div>
        <div class="form-row form-item">
          <div v-if="couponForm.type === 'reduce'">
            <label class="form-label"><span class="req">*</span>抵扣金额</label>
            <input v-model.number="couponForm.reducePrice" type="number" min="0" step="0.01" class="form-input" />
          </div>
          <div v-else>
            <label class="form-label"><span class="req">*</span>折扣（0.9=九折）</label>
            <input v-model.number="couponForm.discountRatio" type="number" min="0" max="1" step="0.01" class="form-input" />
          </div>
          <div>
            <label class="form-label">使用门槛（满X可用，0=无门槛）</label>
            <input v-model.number="couponForm.minPrice" type="number" min="0" step="0.01" class="form-input" />
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">有效期</label>
          <select v-model="couponForm.expireType" class="form-select">
            <option value="fixed">固定时间段</option>
            <option value="receive">领取后 N 天内有效</option>
          </select>
        </div>
        <div v-if="couponForm.expireType === 'fixed'" class="form-row form-item">
          <div><label class="form-label"><span class="req">*</span>开始日期</label><input v-model="couponForm.startTime" type="date" class="form-input" /></div>
          <div><label class="form-label"><span class="req">*</span>结束日期（当天 23:59 失效）</label><input v-model="couponForm.endTime" type="date" class="form-input" /></div>
        </div>
        <div v-else class="form-item">
          <label class="form-label">领取后有效天数</label>
          <input v-model.number="couponForm.expireDays" type="number" min="1" class="form-input" />
        </div>
        <div class="form-row form-item">
          <div><label class="form-label">发放总量（0=不限）</label><input v-model.number="couponForm.totalNum" type="number" min="0" class="form-input" /></div>
          <div><label class="form-label">每人限领（0=不限）</label><input v-model.number="couponForm.limitPerUser" type="number" min="0" class="form-input" /></div>
        </div>
        <div class="form-item">
          <label class="form-label">适用范围</label>
          <select v-model="couponForm.applyRange" class="form-select">
            <option value="all">全场通用</option>
            <option value="category">指定分类（填分类 id，逗号分隔）</option>
            <option value="goods">指定商品（填商品 id，逗号分隔）</option>
          </select>
        </div>
        <div v-if="couponForm.applyRange !== 'all'" class="form-item">
          <label class="form-label">适用 id（逗号分隔）</label>
          <input v-model="couponForm.applyRangeConfig" class="form-input" placeholder="例如：10,11,12" />
        </div>
        <div class="form-item">
          <label class="form-label">状态</label>
          <select v-model="couponForm.status" class="form-select">
            <option value="on">上架</option>
            <option value="off">暂停</option>
          </select>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" @click="closeCouponModal">取消</button>
        <button class="btn btn-primary" @click="submitCoupon">保存</button>
      </div>
    </div>
  </div>

  <!-- 满减 Tab -->
  <div v-if="activeTab === 'fullReduce'">
    <table class="table">
      <thead>
        <tr><th>规则名称</th><th>类型</th><th>档位</th><th>包邮</th><th>状态</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in fullReduceRules" :key="item.id">
          <td>{{ item.name }}</td>
          <td>{{ item.type === 'count' ? '满件折' : '满金额' }}</td>
          <td>{{ ruleSummary(item) }}</td>
          <td>{{ item.freeExpress ? '包邮' : '否' }}</td>
          <td><span class="tag" :class="item.status === 'on' ? 'tag-good tag-dot' : 'tag-muted'">{{ item.status === 'on' ? '启用' : '停用' }}</span></td>
          <td><button class="btn btn-sm" @click="openEditRule(item)">编辑</button> <button class="btn btn-sm" @click="removeRule(item.id)">删除</button></td>
        </tr>
        <tr v-if="!fullReduceRules.length"><td colspan="6" style="text-align:center;color:var(--text-muted)">暂无满减规则，点击右上角新建</td></tr>
      </tbody>
    </table>
  </div>

  <!-- 满减弹窗 -->
  <div v-if="fullReduceModalOpen" class="modal-mask" @click.self="closeRuleModal">
    <div class="modal" style="width: 600px">
      <div class="modal-header">
        <span>{{ editingRuleId ? '编辑满减规则' : '新建满减规则' }}</span>
        <button class="modal-close" aria-label="关闭" @click="closeRuleModal">×</button>
      </div>
      <div class="modal-body">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>规则名称</label>
          <input v-model="ruleForm.name" class="form-input" placeholder="例如：满100减10" />
        </div>
        <div class="form-item">
          <label class="form-label">类型</label>
          <select v-model="ruleForm.type" class="form-select">
            <option value="money">满金额减（满X元减Y元）</option>
            <option value="count">满件折（满N件打M折）</option>
          </select>
        </div>
        <div class="form-item">
          <label class="form-label">档位</label>
          <div v-for="(tier, idx) in ruleForm.tiers" :key="idx" class="form-row" style="margin-bottom: 8px; align-items: flex-end">
            <div>
              <label class="form-label">门槛</label>
              <input v-model.number="tier.threshold" type="number" min="0" class="form-input" :placeholder="ruleForm.type === 'count' ? '件数' : '金额'" />
            </div>
            <div v-if="ruleForm.type === 'money'">
              <label class="form-label">减</label>
              <input v-model.number="tier.reduce" type="number" min="0" step="0.01" class="form-input" placeholder="金额" />
            </div>
            <div v-else>
              <label class="form-label">折扣（0.9=九折）</label>
              <input v-model.number="tier.discount" type="number" min="0" max="1" step="0.01" class="form-input" />
            </div>
            <button class="btn btn-sm" @click="removeTier(idx)" :disabled="ruleForm.tiers.length <= 1">删除</button>
          </div>
          <button class="btn btn-sm" @click="addTier">＋ 添加档位</button>
        </div>
        <div class="form-row form-item">
          <div>
            <label class="form-label">满X包邮</label>
            <select v-model="ruleForm.freeExpress" class="form-select">
              <option :value="0">否</option>
              <option :value="1">是</option>
            </select>
          </div>
          <div>
            <label class="form-label">状态</label>
            <select v-model="ruleForm.status" class="form-select">
              <option value="on">启用</option>
              <option value="off">停用</option>
            </select>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn" @click="closeRuleModal">取消</button>
        <button class="btn btn-primary" @click="submitRule">保存</button>
      </div>
    </div>
  </div>

  <!-- 秒杀/限时折扣 Tab -->
  <div v-if="activeTab === 'seckill'">
    <!-- 场次管理 -->
    <div class="card" style="margin-bottom: 16px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <div style="font-weight:600">秒杀场次（每日固定时段）</div>
        <button class="btn btn-sm btn-primary" @click="openCreateTime">＋ 新建场次</button>
      </div>
      <table class="table">
        <thead><tr><th>名称</th><th>开始</th><th>结束</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="t in seckillTimes" :key="t.id">
            <td>{{ t.name }}</td><td>{{ t.startTime }}</td><td>{{ t.endTime }}</td><td>{{ t.sort }}</td>
            <td><span class="tag" :class="t.status === 'on' ? 'tag-good tag-dot' : 'tag-muted'">{{ t.status === 'on' ? '启用' : '停用' }}</span></td>
            <td><button class="btn btn-sm" @click="openEditTime(t)">编辑</button> <button class="btn btn-sm" @click="removeTime(t.id)">删除</button></td>
          </tr>
          <tr v-if="!seckillTimes.length"><td colspan="6" style="text-align:center;color:var(--text-muted)">暂无场次</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 活动列表 -->
    <div class="card" style="margin-bottom: 16px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <div style="font-weight:600">活动列表</div>
        <button class="btn btn-sm btn-primary" @click="openCreateActive">＋ 新建活动</button>
      </div>
      <table class="table">
        <thead><tr><th>名称</th><th>场次</th><th>日期</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="a in seckillActives" :key="a.id" :class="{ 'row-selected': selectedActiveId === a.id }">
            <td>{{ a.name }}</td>
            <td>{{ activeTimeNames(a) }}</td>
            <td>{{ a.startDate }} ~ {{ a.endDate }}</td>
            <td><span class="tag" :class="a.status === 'on' ? 'tag-good tag-dot' : 'tag-muted'">{{ a.status === 'on' ? '上架' : '下架' }}</span></td>
            <td>
              <button class="btn btn-sm" @click="selectActive(a.id)">查看商品</button>
              <button class="btn btn-sm" @click="openEditActive(a)">编辑</button>
              <button class="btn btn-sm" @click="removeActive(a.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!seckillActives.length"><td colspan="5" style="text-align:center;color:var(--text-muted)">暂无活动，点击右上角新建</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 活动内商品 -->
    <div v-if="selectedActiveId" class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <div style="font-weight:600">活动商品</div>
        <button class="btn btn-sm btn-primary" @click="openCreateGoods">＋ 添加秒杀商品</button>
      </div>
      <table class="table">
        <thead><tr><th>商品</th><th>规格</th><th>秒杀价</th><th>限量</th><th>限购</th><th>已售</th><th>进度</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="g in seckillGoodsList" :key="g.id">
            <td>
              <div class="goods-cell">
                <img v-if="goodsCover(g.goodsId)" :src="goodsCover(g.goodsId)" alt="" class="goods-cell-thumb" />
                <div>
                  <div class="goods-cell-name">{{ goodsLabel(g.goodsId) }}</div>
                  <div class="goods-cell-sub">{{ goodsSubLabel(g.goodsId) }}</div>
                </div>
              </div>
            </td>
            <td>{{ skuLabelOf(g.skuId) }}</td>
            <td style="color:var(--price);font-weight:600">¥{{ g.seckillPrice }}</td>
            <td>{{ g.seckillNum || '不限' }}</td><td>{{ g.limitPerUser || '不限' }}</td><td>{{ g.sold || 0 }}</td>
            <td><div class="progress-track" style="width:120px"><div class="progress-fill" :style="{ width: goodsProgress(g) + '%' }" /></div></td>
            <td><span class="tag" :class="g.status === 'on' ? 'tag-good tag-dot' : 'tag-muted'">{{ g.status === 'on' ? '上架' : '下架' }}</span></td>
            <td><button class="btn btn-sm" @click="openEditGoods(g)">编辑</button> <button class="btn btn-sm" @click="removeGoods(g.id)">删除</button></td>
          </tr>
          <tr v-if="!seckillGoodsList.length"><td colspan="9" style="text-align:center;color:var(--text-muted)">暂无秒杀商品，点击右上角添加</td></tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- 场次弹窗 -->
  <div v-if="timeModalOpen" class="modal-mask" @click.self="closeTimeModal">
    <div class="modal" style="width: 460px">
      <div class="modal-header"><span>{{ editingTimeId ? '编辑场次' : '新建场次' }}</span><button class="modal-close" @click="closeTimeModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>场次名称</label><input v-model="timeForm.name" class="form-input" placeholder="如 10:00场" /></div>
        <div class="form-row form-item">
          <div><label class="form-label"><span class="req">*</span>开始时间</label><input v-model="timeForm.startTime" type="time" class="form-input" /></div>
          <div><label class="form-label"><span class="req">*</span>结束时间</label><input v-model="timeForm.endTime" type="time" class="form-input" /></div>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label">排序</label><input v-model.number="timeForm.sort" type="number" min="0" class="form-input" /></div>
          <div><label class="form-label">状态</label><select v-model="timeForm.status" class="form-select"><option value="on">启用</option><option value="off">停用</option></select></div>
        </div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeTimeModal">取消</button><button class="btn btn-primary" @click="submitTime">保存</button></div>
    </div>
  </div>

  <!-- 活动弹窗 -->
  <div v-if="activeModalOpen" class="modal-mask" @click.self="closeActiveModal">
    <div class="modal" style="width: 560px">
      <div class="modal-header"><span>{{ editingActiveId ? '编辑活动' : '新建活动' }}</span><button class="modal-close" @click="closeActiveModal">×</button></div>
      <div class="modal-body">
        <div class="form-item"><label class="form-label"><span class="req">*</span>活动名称</label><input v-model="activeForm.name" class="form-input" placeholder="如 双11秒杀" /></div>
        <div class="form-item">
          <label class="form-label">参与场次（不选 = 限时折扣，全天仅换价、无限购）</label>
          <div style="display:flex;flex-wrap:wrap;gap:8px">
            <label v-for="t in seckillTimes" :key="t.id" style="display:flex;align-items:center;gap:4px;font-size:13px">
              <input type="checkbox" :value="t.id" v-model="activeForm.timeIds" /> {{ t.name }}（{{ t.startTime }}-{{ t.endTime }}）
            </label>
            <span v-if="!seckillTimes.length" style="color:var(--text-muted)">请先在上方创建场次</span>
          </div>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label"><span class="req">*</span>开始日期</label><input v-model="activeForm.startDate" type="date" class="form-input" /></div>
          <div><label class="form-label"><span class="req">*</span>结束日期</label><input v-model="activeForm.endDate" type="date" class="form-input" /></div>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label">状态</label><select v-model="activeForm.status" class="form-select"><option value="on">上架</option><option value="off">下架</option></select></div>
        </div>
        <div class="form-item"><label class="form-label">备注</label><input v-model="activeForm.remark" class="form-input" /></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeActiveModal">取消</button><button class="btn btn-primary" @click="submitActive">保存</button></div>
    </div>
  </div>

  <!-- 秒杀商品弹窗 -->
  <div v-if="goodsModalOpen" class="modal-mask" @click.self="closeGoodsModal">
    <div class="modal" style="width: 560px">
      <div class="modal-header"><span>{{ editingGoodsId ? '编辑秒杀商品' : '添加秒杀商品' }}</span><button class="modal-close" @click="closeGoodsModal">×</button></div>
      <div class="modal-body" style="max-height: 70vh; overflow-y: auto">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>商品（仅出售中的商品可参与）</label>
          <GoodsPicker v-model="goodsForm.goodsId" only-on-sale @loaded="onSeckillGoodsLoaded" />
        </div>
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>规格</label>
          <select v-model="goodsForm.skuId" class="form-select" :disabled="!seckillSkuOptions.length" @change="onSeckillSkuChange">
            <option :value="null">请选择规格</option>
            <option v-for="sku in seckillSkuOptions" :key="sku.id" :value="sku.id">
              {{ skuLabel(sku) }} · 原价 ¥{{ sku.price }}{{ sku.stock != null ? ' · 库存 ' + sku.stock : '' }}
            </option>
          </select>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label"><span class="req">*</span>秒杀价</label><input v-model.number="goodsForm.seckillPrice" type="number" min="0" step="0.01" class="form-input" /></div>
          <div><label class="form-label">秒杀限量（0=不限）</label><input v-model.number="goodsForm.seckillNum" type="number" min="0" class="form-input" /></div>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label">每人限购（0=不限）</label><input v-model.number="goodsForm.limitPerUser" type="number" min="0" class="form-input" /></div>
          <div><label class="form-label">排序</label><input v-model.number="goodsForm.sort" type="number" min="0" class="form-input" /></div>
        </div>
        <div class="form-item"><label class="form-label">状态</label><select v-model="goodsForm.status" class="form-select"><option value="on">上架</option><option value="off">下架</option></select></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeGoodsModal">取消</button><button class="btn btn-primary" @click="submitGoods">保存</button></div>
    </div>
  </div>

  <!-- 拼团 Tab -->
  <div v-if="activeTab === 'group'">
    <table class="table">
      <thead><tr><th>商品</th><th>成团人数</th><th>最低拼团价</th><th>有效时长</th><th>活动时间</th><th>模拟成团</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="a in groupActives" :key="a.id">
          <td>
            <div class="goods-cell">
              <img v-if="goodsCover(a.goodsId)" :src="goodsCover(a.goodsId)" alt="" class="goods-cell-thumb" />
              <div>
                <div class="goods-cell-name">{{ goodsLabel(a.goodsId) }}</div>
                <div class="goods-cell-sub">{{ goodsSubLabel(a.goodsId) }}</div>
              </div>
            </div>
          </td>
          <td>{{ a.groupNum }} 人</td>
          <td style="color:var(--price);font-weight:600">¥{{ minGroupPrice(a) }}</td>
          <td>{{ a.validHours }} 小时</td>
          <td>{{ fmtTime(a.startTime) }} ~ {{ fmtTime(a.endTime) }}</td>
          <td>{{ a.isMock ? '是' : '否' }}</td>
          <td><span class="tag" :class="a.status === 'on' ? 'tag-good tag-dot' : 'tag-muted'">{{ a.status === 'on' ? '上架' : '下架' }}</span></td>
          <td><button class="btn btn-sm" @click="openEditGroup(a)">编辑</button> <button class="btn btn-sm" @click="removeGroup(a.id)">删除</button></td>
        </tr>
        <tr v-if="!groupActives.length"><td colspan="8" style="text-align:center;color:var(--text-muted)">暂无拼团活动，点击右上角新建</td></tr>
      </tbody>
    </table>
  </div>

  <!-- 拼团弹窗 -->
  <div v-if="groupModalOpen" class="modal-mask" @click.self="closeGroupModal">
    <div class="modal" style="width: 640px">
      <div class="modal-header"><span>{{ editingGroupId ? '编辑拼团' : '新建拼团' }}</span><button class="modal-close" @click="closeGroupModal">×</button></div>
      <div class="modal-body" style="max-height: 70vh; overflow-y: auto">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>商品</label>
          <GoodsPicker v-model="groupForm.goodsId" @loaded="onGroupGoodsLoaded" />
        </div>
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>成团人数（含团长）</label>
          <input v-model.number="groupForm.groupNum" type="number" min="2" class="form-input" />
        </div>
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>拼团价</label>
          <div v-if="!groupSkuRows.length" class="form-hint">请先选择商品，将按规格列出原价和拼团价</div>
          <table v-else class="table sku-price-table">
            <thead>
              <tr><th>规格</th><th>原价</th><th>库存</th><th>拼团价</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in groupSkuRows" :key="row.skuId">
                <td>{{ row.label }}</td>
                <td>¥{{ row.price }}</td>
                <td>{{ row.stock ?? '—' }}</td>
                <td><input v-model.number="row.groupPrice" type="number" min="0" step="0.01" class="form-input" /></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label">成团有效时长（小时）</label><input v-model.number="groupForm.validHours" type="number" min="1" class="form-input" /></div>
          <div><label class="form-label">模拟成团（人数不足时自动成团）</label><select v-model="groupForm.isMock" class="form-select"><option :value="0">否</option><option :value="1">是</option></select></div>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label"><span class="req">*</span>开始时间</label><input v-model="groupForm.startTime" type="datetime-local" class="form-input" /></div>
          <div><label class="form-label"><span class="req">*</span>结束时间</label><input v-model="groupForm.endTime" type="datetime-local" class="form-input" /></div>
        </div>
        <div class="form-item"><label class="form-label">状态</label><select v-model="groupForm.status" class="form-select"><option value="on">上架</option><option value="off">下架</option></select></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeGroupModal">取消</button><button class="btn btn-primary" @click="submitGroup">保存</button></div>
    </div>
  </div>

  <!-- 砍价 Tab -->
  <div v-if="activeTab === 'bargain'">
    <table class="table">
      <thead><tr><th>商品</th><th>底价</th><th>有效时长</th><th>助力上限</th><th>活动时间</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="a in bargainActives" :key="a.id">
          <td>
            <div class="goods-cell">
              <img v-if="goodsCover(a.goodsId)" :src="goodsCover(a.goodsId)" alt="" class="goods-cell-thumb" />
              <div>
                <div class="goods-cell-name">{{ goodsLabel(a.goodsId) }}</div>
                <div class="goods-cell-sub">{{ goodsSubLabel(a.goodsId) }}</div>
              </div>
            </div>
          </td>
          <td style="color:var(--price);font-weight:600">¥{{ a.floorPrice }}</td>
          <td>{{ a.validHours }} 小时</td>
          <td>{{ a.helpLimit || '不限' }}</td>
          <td>{{ fmtTime(a.startTime) }} ~ {{ fmtTime(a.endTime) }}</td>
          <td><span class="tag" :class="a.status === 'on' ? 'tag-good tag-dot' : 'tag-muted'">{{ a.status === 'on' ? '上架' : '下架' }}</span></td>
          <td><button class="btn btn-sm" @click="openEditBargain(a)">编辑</button> <button class="btn btn-sm" @click="removeBargain(a.id)">删除</button></td>
        </tr>
        <tr v-if="!bargainActives.length"><td colspan="7" style="text-align:center;color:var(--text-muted)">暂无砍价活动，点击右上角新建</td></tr>
      </tbody>
    </table>
  </div>

  <!-- 砍价弹窗 -->
  <div v-if="bargainModalOpen" class="modal-mask" @click.self="closeBargainModal">
    <div class="modal" style="width: 520px">
      <div class="modal-header"><span>{{ editingBargainId ? '编辑砍价' : '新建砍价' }}</span><button class="modal-close" @click="closeBargainModal">×</button></div>
      <div class="modal-body" style="max-height: 70vh; overflow-y: auto">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>商品</label>
          <GoodsPicker v-model="bargainForm.goodsId" @loaded="onBargainGoodsLoaded" />
        </div>
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>底价（砍到不能再砍）</label>
          <input v-model.number="bargainForm.floorPrice" type="number" min="0" step="0.01" class="form-input" />
        </div>
        <div class="form-row form-item">
          <div><label class="form-label">发起后有效时长（小时）</label><input v-model.number="bargainForm.validHours" type="number" min="1" class="form-input" /></div>
          <div><label class="form-label">助力次数上限（0=不限）</label><input v-model.number="bargainForm.helpLimit" type="number" min="0" class="form-input" /></div>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label"><span class="req">*</span>开始时间</label><input v-model="bargainForm.startTime" type="datetime-local" class="form-input" /></div>
          <div><label class="form-label"><span class="req">*</span>结束时间</label><input v-model="bargainForm.endTime" type="datetime-local" class="form-input" /></div>
        </div>
        <div class="form-item"><label class="form-label">状态</label><select v-model="bargainForm.status" class="form-select"><option value="on">上架</option><option value="off">下架</option></select></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closeBargainModal">取消</button><button class="btn btn-primary" @click="submitBargain">保存</button></div>
    </div>
  </div>

  <!-- 签到配置 Tab -->
  <div v-if="activeTab === 'sign'" class="card card-pad">
    <div class="form-item">
      <label class="form-label">每日签到积分</label>
      <input v-model.number="signDailyPoints" type="number" min="1" class="form-input" style="width:200px" />
      <div class="form-hint">用户每天签到即可获得的基础积分</div>
    </div>
    <div class="form-item" style="margin-top:14px">
      <label class="form-label">连续签到奖励</label>
      <div class="form-hint" style="margin-bottom:10px">达到指定连续天数时额外发放积分或优惠券</div>
      <div v-for="(rule, idx) in signRules" :key="idx" class="sign-rule-row">
        <div>
          <label class="form-label">连续天数</label>
          <input v-model.number="rule.days" type="number" min="1" class="form-input" />
        </div>
        <div>
          <label class="form-label">奖励类型</label>
          <select v-model="rule.type" class="form-select">
            <option value="points">积分</option>
            <option value="coupon">优惠券</option>
          </select>
        </div>
        <div v-if="rule.type === 'points'">
          <label class="form-label">奖励积分</label>
          <input v-model.number="rule.value" type="number" min="0" class="form-input" />
        </div>
        <div v-else>
          <label class="form-label">优惠券</label>
          <select v-model="rule.couponId" class="form-select">
            <option :value="null">请选择优惠券</option>
            <option v-for="c in coupons" :key="c.id" :value="c.id">{{ c.name }}（{{ couponFace(c) }}）</option>
          </select>
        </div>
        <button class="btn btn-sm" style="align-self:flex-end" @click="removeSignRule(idx)">删除</button>
      </div>
      <button class="btn btn-sm" @click="addSignRule">＋ 添加奖励档位</button>
      <div v-if="!signRules.length" class="form-hint" style="margin-top:8px">尚未配置连续奖励，可只保留每日积分</div>
    </div>
    <button class="btn btn-primary" style="margin-top:14px" @click="submitSignConfig">保存签到配置</button>
  </div>

  <!-- 积分商城 Tab -->
  <div v-if="activeTab === 'pointsMall'">
    <table class="table">
      <thead><tr><th>兑换项</th><th>类型</th><th>积分</th><th>现金</th><th>库存</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="item in pointsGoods" :key="item.id">
          <td>
            <div class="goods-cell">
              <img v-if="item.image" :src="item.image" alt="" class="goods-cell-thumb" />
              <div>
                <div class="goods-cell-name">{{ item.name }}</div>
                <div class="goods-cell-sub">{{ item.couponId ? couponLabel(item.couponId) : goodsLabel(item.goodsId) }}</div>
              </div>
            </div>
          </td>
          <td><span class="tag" :class="item.couponId ? 'tag-primary' : 'tag-good'">{{ item.couponId ? '发券' : '发货' }}</span></td>
          <td style="color:var(--price);font-weight:600">{{ item.points }}</td>
          <td>{{ item.cash && Number(item.cash) > 0 ? '¥' + Number(item.cash).toFixed(2) : '纯积分' }}</td>
          <td>{{ item.stock || '不限' }}</td>
          <td>{{ item.sort }}</td>
          <td><span class="tag" :class="item.status === 'on' ? 'tag-good tag-dot' : 'tag-muted'">{{ item.status === 'on' ? '上架' : '下架' }}</span></td>
          <td><button class="btn btn-sm" @click="openEditPointsGoods(item)">编辑</button> <button class="btn btn-sm" @click="removePointsGoods(item.id)">删除</button></td>
        </tr>
        <tr v-if="!pointsGoods.length"><td colspan="8" style="text-align:center;color:var(--text-muted)">暂无兑换项，点击右上角新建</td></tr>
      </tbody>
    </table>

    <div v-if="exchangeRecords.length" class="card card-pad" style="margin-top:20px">
      <h4 style="margin-bottom:10px">兑换记录</h4>
      <table class="table">
        <thead><tr><th>用户ID</th><th>名称</th><th>积分</th><th>现金</th><th>支付</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="r in exchangeRecords" :key="r.id">
            <td>{{ r.userId }}</td>
            <td>{{ r.name }}</td>
            <td>{{ r.pointsCost }}</td>
            <td>¥{{ Number(r.cashPrice || 0).toFixed(2) }}</td>
            <td><span class="tag" :class="r.payStatus === 'paid' ? 'tag-good' : 'tag-warning'">{{ r.payStatus === 'paid' ? '已支付' : '未支付' }}</span></td>
            <td>{{ r.status === 'pending' ? '待发货' : r.status }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- 积分商城弹窗 -->
  <div v-if="pointsMallModalOpen" class="modal-mask" @click.self="closePointsMallModal">
    <div class="modal" style="width: 520px">
      <div class="modal-header"><span>{{ editingPointsGoodsId ? '编辑兑换项' : '新建兑换项' }}</span><button class="modal-close" @click="closePointsMallModal">×</button></div>
      <div class="modal-body" style="max-height: 70vh; overflow-y: auto">
        <div class="form-item"><label class="form-label"><span class="req">*</span>名称</label><input v-model="pointsGoodsForm.name" class="form-input" placeholder="兑换项名称" /></div>
        <div class="form-item">
          <label class="form-label">封面图</label>
          <ImageField v-model="pointsGoodsForm.image" />
        </div>
        <div class="form-item"><label class="form-label">类型</label><select v-model="pointsGoodsForm.type" class="form-select"><option value="coupon">发券</option><option value="goods">发货</option></select></div>
        <div v-if="pointsGoodsForm.type === 'goods'" class="form-item">
          <label class="form-label"><span class="req">*</span>发放商品</label>
          <GoodsPicker v-model="pointsGoodsForm.goodsId" @loaded="onPointsGoodsLoaded" />
        </div>
        <div v-if="pointsGoodsForm.type === 'coupon'" class="form-item">
          <label class="form-label"><span class="req">*</span>发放优惠券</label>
          <select v-model="pointsGoodsForm.couponId" class="form-select">
            <option :value="null">请选择优惠券</option>
            <option v-for="c in coupons" :key="c.id" :value="c.id">{{ c.name }}（{{ couponFace(c) }}）</option>
          </select>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label"><span class="req">*</span>所需积分</label><input v-model.number="pointsGoodsForm.points" type="number" min="1" class="form-input" /></div>
          <div><label class="form-label">现金（0=纯积分）</label><input v-model.number="pointsGoodsForm.cash" type="number" min="0" step="0.01" class="form-input" /></div>
        </div>
        <div class="form-row form-item">
          <div><label class="form-label">库存（0=不限）</label><input v-model.number="pointsGoodsForm.stock" type="number" min="0" class="form-input" /></div>
          <div><label class="form-label">排序</label><input v-model.number="pointsGoodsForm.sort" type="number" min="0" class="form-input" /></div>
        </div>
        <div class="form-item"><label class="form-label">状态</label><select v-model="pointsGoodsForm.status" class="form-select"><option value="on">上架</option><option value="off">下架</option></select></div>
      </div>
      <div class="modal-footer"><button class="btn" @click="closePointsMallModal">取消</button><button class="btn btn-primary" @click="submitPointsGoods">保存</button></div>
    </div>
  </div>
</template>

<style scoped>
.coupon-face {
  width: 120px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #fff3ec, #ffe4d6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--price);
  position: relative;
}
.coupon-face::after {
  content: '';
  position: absolute;
  right: -1px;
  top: 0;
  bottom: 0;
  width: 1px;
  background: repeating-linear-gradient(0deg, var(--border-strong) 0 6px, transparent 6px 12px);
}
.coupon-face .amt { font-size: 22px; font-weight: 800; }
.coupon-face .cond { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
.coupon-body { flex: 1; padding: 14px 18px; }
.coupon-row { display: flex; justify-content: space-between; align-items: flex-start; }
.coupon-meta { font-size: 12px; color: var(--text-muted); margin-top: 6px; display: flex; gap: 16px; flex-wrap: wrap; }
.progress-track { height: 6px; border-radius: 3px; background: var(--surface-2); margin-top: 8px; width: 220px; overflow: hidden; }
.progress-fill { height: 100%; background: var(--series-1); border-radius: 3px; }
.row-selected { background: var(--surface-2); }
.goods-cell { display: flex; align-items: center; gap: 10px; }
.goods-cell-thumb { width: 40px; height: 40px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }
.goods-cell-name { font-weight: 600; }
.goods-cell-sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.sku-price-table { margin-top: 4px; }
.sku-price-table .form-input { height: 32px; }
.sign-rule-row {
  display: grid;
  grid-template-columns: 120px 140px 1fr auto;
  gap: 12px;
  align-items: end;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
  background: var(--surface-2);
}
@media (max-width: 900px) {
  .sign-rule-row { grid-template-columns: 1fr 1fr; }
}
</style>
