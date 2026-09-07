<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getGoods, listCategories, listFreightTemplates, publishGoods, updateGoods } from '@/api/goods'
import { createSpec, createSpecValue, listSpecs } from '@/api/goodsSpec'
import ImageField from '@/components/ImageField.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'

/**
 * 发布商品。对照原型 docs/prototype/store/goods-edit.html，单规格 + 多规格均已支持。
 *
 * 多规格数据模型（对齐 GoodsService.SkuItem 与 GoodsServiceImpl 的实现）：
 * - 规格库（goods_spec/goods_spec_value）跨商品复用，见 StoreGoodsSpecController；
 *   这里选中的"颜色/尺码"规格值是全店共享的库，不是本商品私有的
 * - SKU 矩阵 = 各已选规格的规格值做笛卡尔积，每个组合一行；specValueIds 传给后端的格式是
 *   "规格值ID_规格值ID"（GoodsService.SkuItem 文档："有序规格值ID串，如 12_35"），
 *   顺序必须和"选中的规格顺序"一致，不能按 id 大小排——所以这里显式维护 selectedSpecIds 数组顺序
 *
 * 商品服务标签仍待对应后端 controller；编辑复用本页表单并调用 PUT /store/goods/{id}。
 */
const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const loading = ref(false)
const isEdit = Boolean(route.params.id)
const goodsId = route.params.id

const categories = ref([])
const freightTemplates = ref([])
const specs = ref([])
const specsLoading = ref(false)

async function loadSpecs() {
  specsLoading.value = true
  try {
    specs.value = await listSpecs()
  } catch (e) {
    specs.value = []
  } finally {
    specsLoading.value = false
  }
}

onMounted(async () => {
  try {
    categories.value = await listCategories()
  } catch (e) {
    categories.value = []
  }
  try {
    freightTemplates.value = await listFreightTemplates()
  } catch (e) {
    freightTemplates.value = []
  }
  await loadSpecs()
  if (isEdit) await loadForEdit()
})

const specType = ref('single')
function onPickSpecType(type) {
  specType.value = type
}

// ---------- 多规格：已选规格 + 规格值 ----------
// selectedSpecIds 的顺序就是 SKU 矩阵里"规格1/规格2"列的顺序，也是 specValueIds 拼接的顺序
const selectedSpecIds = ref([])

function toggleSpec(specId) {
  const i = selectedSpecIds.value.indexOf(specId)
  if (i >= 0) {
    selectedSpecIds.value.splice(i, 1)
  } else {
    selectedSpecIds.value.push(specId)
  }
}

const newSpecName = ref('')
async function onCreateSpec() {
  const name = newSpecName.value.trim()
  if (!name) return
  try {
    const created = await createSpec(name)
    newSpecName.value = ''
    await loadSpecs()
    selectedSpecIds.value.push(created.id)
  } catch (e) {
    // 已由拦截器提示
  }
}

const newValueDrafts = reactive({})
async function onAddSpecValue(specId) {
  const value = (newValueDrafts[specId] || '').trim()
  if (!value) return
  try {
    await createSpecValue(specId, value)
    newValueDrafts[specId] = ''
    await loadSpecs()
  } catch (e) {
    // 已由拦截器提示
  }
}

function selectedSpecs() {
  return selectedSpecIds.value
    .map((id) => specs.value.find((s) => s.id === id))
    .filter(Boolean)
}

// 每个已选规格里，勾选了哪些规格值（默认全选——新增规格值时体验上更顺，用户可以再取消勾选）
const checkedValueIds = reactive({})
function isValueChecked(specId, valueId) {
  const set = checkedValueIds[specId]
  return set ? set.has(valueId) : true
}
function toggleValueChecked(specId, valueId) {
  if (!checkedValueIds[specId]) {
    const spec = specs.value.find((s) => s.id === specId)
    checkedValueIds[specId] = new Set((spec?.values || []).map((v) => v.id))
  }
  const set = checkedValueIds[specId]
  if (set.has(valueId)) {
    set.delete(valueId)
  } else {
    set.add(valueId)
  }
  // Set 的 add/delete 不是 Vue 能追踪的响应式变更（不像数组/对象赋值），
  // 矩阵重算必须在这里显式触发，不能只指望 watch(selectedSpecIds) 兜底。
  rebuildSkuMatrix()
}

// SKU 矩阵：已选规格 × 各自勾选的规格值 的笛卡尔积
const skuRows = ref([])

function checkedValuesOf(spec) {
  return (spec.values || []).filter((v) => isValueChecked(spec.id, v.id))
}

function rebuildSkuMatrix() {
  const dims = selectedSpecs().map((spec) => checkedValuesOf(spec))
  if (dims.length === 0 || dims.some((d) => d.length === 0)) {
    skuRows.value = []
    return
  }
  let combos = [[]]
  for (const dim of dims) {
    const next = []
    for (const combo of combos) {
      for (const v of dim) {
        next.push([...combo, v])
      }
    }
    combos = next
  }
  // 保留同一组合已经填过的价格/库存，避免每次勾选变化都把用户输入清空
  const prevByKey = new Map(skuRows.value.map((r) => [r.key, r]))
  skuRows.value = combos.map((combo) => {
    const key = combo.map((v) => v.id).join('_')
    const prev = prevByKey.get(key)
    return (
      prev || {
        key,
        labels: combo.map((v) => v.value),
        specValueIds: key,
        price: null,
        linePrice: null,
        stock: null,
        weight: null,
        volume: null,
        skuCode: '',
      }
    )
  })
}

// selectedSpecIds/规格值勾选变化时自动重算矩阵；deep 是因为 checkedValueIds 里存的是 Set，
// Vue 的响应式代理追不到 Set.add/delete 内部变化，只能靠 rebuildSkuMatrix 显式调用兜底触发。
watch(selectedSpecIds, rebuildSkuMatrix, { deep: true })
watch(specs, rebuildSkuMatrix, { deep: true })

function onBatchFill() {
  if (skuRows.value.length === 0) return
  const first = skuRows.value[0]
  if (first.price === null && first.stock === null) {
    message.info('请先填写第一行的销售价/库存，再批量填充其余行')
    return
  }
  for (const row of skuRows.value) {
    if (row.price === null) row.price = first.price
    if (row.linePrice === null) row.linePrice = first.linePrice
    if (row.stock === null) row.stock = first.stock
  }
}

const form = reactive({
  name: '',
  subName: '',
  code: '',
  categoryIds: [],
  images: [],
  deliveryType: ['express'],
  freightTemplateId: null,
  isVirtual: false,
  content: '',
  price: null,
  linePrice: null,
  costPrice: null,
  stock: null,
  weight: null,
  volume: null,
})

const errors = reactive({})

function validate() {
  errors.name = form.name.trim() ? '' : '请输入商品名称'
  errors.categoryIds = form.categoryIds.length ? '' : '请至少选择一个分类'
  errors.deliveryType = form.deliveryType.length ? '' : '请至少选择一种配送方式'

  if (specType.value === 'single') {
    errors.price = form.price !== null && form.price >= 0 ? '' : '请输入销售价'
    errors.stock = form.stock !== null && form.stock >= 0 ? '' : '请输入库存'
    return !errors.name && !errors.categoryIds && !errors.price && !errors.stock && !errors.deliveryType
  }

  errors.sku = ''
  if (selectedSpecIds.value.length === 0) {
    errors.sku = '请至少选择一个规格（如颜色）'
  } else if (skuRows.value.length === 0) {
    errors.sku = '所选规格下没有可用的规格值组合，请勾选至少一个规格值'
  } else if (skuRows.value.some((r) => r.price === null || r.price < 0 || r.stock === null || r.stock < 0)) {
    errors.sku = 'SKU 矩阵里每一行的销售价和库存都必须填写'
  }
  return !errors.name && !errors.categoryIds && !errors.deliveryType && !errors.sku
}

function contentOrNull(html) {
  if (!html) return null
  const hasImage = /<img[\s>]/i.test(html)
  const text = html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
  if (!text && !hasImage) return null
  return html
}

function parseJson(value, fallback) {
  if (!value) return fallback
  try {
    const parsed = JSON.parse(value)
    return parsed ?? fallback
  } catch (e) {
    return fallback
  }
}

async function loadForEdit() {
  loading.value = true
  try {
    const detail = await getGoods(goodsId)
    const goods = detail.goods
    const skus = detail.skus || []
    form.name = goods.name || ''
    form.subName = goods.subName || ''
    form.code = goods.code || ''
    form.categoryIds = parseJson(goods.categoryIds, []).map(Number)
    form.images = parseJson(goods.images, [])
    form.deliveryType = parseJson(goods.deliveryType, ['express'])
    form.freightTemplateId = goods.freightTemplateId ?? null
    form.isVirtual = Boolean(goods.isVirtual)
    form.content = goods.content || ''
    specType.value = goods.specType === 'multi' ? 'multi' : 'single'

    if (specType.value === 'single') {
      const sku = skus[0]
      if (sku) {
        form.price = sku.price
        form.linePrice = sku.linePrice
        form.costPrice = sku.costPrice
        form.stock = sku.stock
        form.weight = sku.weight
        form.volume = sku.volume
      }
      return
    }

    const valueById = new Map(specs.value.flatMap((s) => (s.values || []).map((v) => [String(v.id), s.id])))
    const selectedIds = []
    const valueIdsBySpec = new Map()
    for (const sku of skus) {
      for (const valueId of String(sku.specValueIds || '').split('_').filter(Boolean)) {
        const specId = valueById.get(valueId)
        if (!specId) continue
        if (!selectedIds.includes(specId)) selectedIds.push(specId)
        if (!valueIdsBySpec.has(specId)) valueIdsBySpec.set(specId, new Set())
        valueIdsBySpec.get(specId).add(Number(valueId))
      }
    }
    selectedSpecIds.value = selectedIds
    for (const specId of selectedIds) checkedValueIds[specId] = valueIdsBySpec.get(specId)
    rebuildSkuMatrix()
    for (const sku of skus) {
      const row = skuRows.value.find((r) => r.key === String(sku.specValueIds || ''))
      if (row) Object.assign(row, { price: sku.price, linePrice: sku.linePrice, stock: sku.stock, weight: sku.weight, volume: sku.volume, skuCode: sku.skuCode || '' })
    }
  } catch (e) {
    message.error('商品详情加载失败')
    router.push({ name: 'goods' })
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  rebuildSkuMatrix()
  if (!validate()) {
    message.error(errors.sku || '请检查表单中标红的必填项')
    return
  }
  const images = (form.images || []).map((s) => String(s).trim()).filter(Boolean)

  const skuItems =
    specType.value === 'single'
      ? [
          {
            specValueIds: null,
            skuCode: form.code.trim() || null,
            price: form.price,
            linePrice: form.linePrice,
            costPrice: form.costPrice,
            stock: form.stock,
            weight: form.weight,
            volume: form.volume,
            image: images[0] || null,
          },
        ]
      : skuRows.value.map((row) => ({
          specValueIds: row.specValueIds,
          skuCode: row.skuCode.trim() || null,
          price: row.price,
          linePrice: row.linePrice,
          costPrice: null,
            stock: row.stock,
            weight: row.weight,
            volume: row.volume,
          image: images[0] || null,
        }))

  submitting.value = true
  try {
    const payload = {
      categoryIds: form.categoryIds,
      brandId: null,
      name: form.name.trim(),
      subName: form.subName.trim() || null,
      code: form.code.trim() || null,
      images,
      specType: specType.value,
      content: contentOrNull(form.content),
      deliveryType: form.deliveryType,
      freightTemplateId: form.freightTemplateId,
      freightFee: null,
      serviceIds: [],
      isVirtual: form.isVirtual,
      skuItems,
    }
    if (isEdit) {
      await updateGoods(goodsId, payload)
      message.success('商品已保存')
    } else {
      await publishGoods(payload)
      message.success('发布成功，商品已进入仓库中（未上架），可在商品列表里上架')
    }
    router.push({ name: 'goods' })
  } catch (e) {
    // 错误提示已由 http 拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="breadcrumb" style="margin-bottom: 6px">
        <a @click="router.push({ name: 'goods' })" style="cursor: pointer">商品</a> ／ <b>发布商品</b>
      </div>
      <div class="page-title">{{ isEdit ? '编辑商品' : '发布商品' }}</div>
    </div>
  </div>

  <a-spin :spinning="loading">
  <div class="card card-pad" style="max-width: 800px">
    <p class="card-title">基本信息</p>
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>商品名称</label>
      <input v-model="form.name" class="form-input" />
      <div v-if="errors.name" class="field-error">{{ errors.name }}</div>
    </div>
    <div class="form-row form-item">
      <div>
        <label class="form-label">副标题</label>
        <input v-model="form.subName" class="form-input" />
      </div>
      <div>
        <label class="form-label">商品编码</label>
        <input v-model="form.code" class="form-input" placeholder="不填则不设置编码" />
      </div>
    </div>
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>商品分类</label>
      <div style="display: flex; flex-wrap: wrap; gap: 8px 20px">
        <label v-for="c in categories" :key="c.id" style="display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-secondary)">
          <input type="checkbox" :value="c.id" v-model="form.categoryIds" />{{ c.name }}
        </label>
        <span v-if="categories.length === 0" style="font-size: 12px; color: var(--text-muted)">
          暂无分类，请先到
          <a href="javascript:;" @click="router.push({ name: 'goods-categories' })">分类管理</a>
          创建
        </span>
      </div>
      <div v-if="errors.categoryIds" class="field-error">{{ errors.categoryIds }}</div>
    </div>
    <div class="form-item">
      <label class="form-label">商品主图</label>
      <ImageField v-model="form.images" multiple :max="10" />
    </div>

    <p class="card-title" style="margin-top: 24px">规格与库存</p>
    <div class="form-item">
      <label class="form-label">规格类型</label>
      <div style="display: flex; width: 220px; border: 1px solid var(--border-strong); border-radius: 8px; overflow: hidden">
        <div
          class="spec-type-btn"
          :class="{ active: specType === 'single' }"
          @click="onPickSpecType('single')"
        >单规格</div>
        <div
          class="spec-type-btn"
          :class="{ active: specType === 'multi' }"
          @click="onPickSpecType('multi')"
        >多规格</div>
      </div>
    </div>

    <!-- 单规格 -->
    <div v-if="specType === 'single'" class="form-row form-item">
      <div>
        <label class="form-label"><span class="req">*</span>销售价</label>
        <input v-model.number="form.price" type="number" step="0.01" class="form-input" />
        <div v-if="errors.price" class="field-error">{{ errors.price }}</div>
      </div>
      <div>
        <label class="form-label">划线价</label>
        <input v-model.number="form.linePrice" type="number" step="0.01" class="form-input" />
      </div>
      <div>
        <label class="form-label">成本价</label>
        <input v-model.number="form.costPrice" type="number" step="0.01" class="form-input" />
      </div>
      <div>
        <label class="form-label"><span class="req">*</span>库存</label>
        <input v-model.number="form.stock" type="number" class="form-input" />
        <div v-if="errors.stock" class="field-error">{{ errors.stock }}</div>
      </div>
      <div>
        <label class="form-label">重量（kg）</label>
        <input v-model.number="form.weight" type="number" min="0" step="0.001" class="form-input" />
      </div>
      <div>
        <label class="form-label">体积（m³）</label>
        <input v-model.number="form.volume" type="number" min="0" step="0.001" class="form-input" />
      </div>
    </div>

    <!-- 多规格 -->
    <template v-else>
      <div class="form-item">
        <label class="form-label">选择规格（全店共享的规格库，点击选中/取消）</label>
        <a-spin :spinning="specsLoading">
          <div style="display: flex; flex-wrap: wrap; gap: 10px">
            <span
              v-for="s in specs"
              :key="s.id"
              class="spec-chip"
              :class="{ selected: selectedSpecIds.includes(s.id) }"
              @click="toggleSpec(s.id)"
            >{{ s.name }}</span>
          </div>
        </a-spin>
        <div style="display: flex; gap: 8px; margin-top: 10px">
          <input v-model="newSpecName" class="form-input" style="width: 200px" placeholder="新增规格，如：材质" @keyup.enter="onCreateSpec" />
          <button class="btn btn-sm" @click="onCreateSpec">添加规格</button>
        </div>
      </div>

      <div v-for="spec in selectedSpecs()" :key="spec.id" class="form-item">
        <label class="form-label">规格：{{ spec.name }}</label>
        <div style="display: flex; flex-wrap: wrap; gap: 8px; align-items: center">
          <span
            v-for="v in spec.values"
            :key="v.id"
            class="spec-tag"
            :class="{ active: isValueChecked(spec.id, v.id) }"
            @click="toggleValueChecked(spec.id, v.id)"
          >{{ v.value }}</span>
          <input
            v-model="newValueDrafts[spec.id]"
            class="form-input"
            style="width: 140px; display: inline-block"
            placeholder="＋ 添加规格值"
            @keyup.enter="onAddSpecValue(spec.id)"
          />
        </div>
      </div>

      <div v-if="errors.sku" class="field-error" style="margin-bottom: 12px">{{ errors.sku }}</div>

      <div v-if="skuRows.length" class="form-item">
        <button class="btn btn-sm" style="margin-bottom: 12px" @click="onBatchFill">批量填充价格/库存</button>
        <div style="overflow-x: auto">
          <table class="table sku-table">
            <thead>
              <tr>
                <th v-for="spec in selectedSpecs()" :key="spec.id">{{ spec.name }}</th>
                <th style="width: 110px">销售价</th>
                <th style="width: 110px">划线价</th>
                <th style="width: 90px">库存</th>
                <th>重量(kg)</th>
                <th>体积(m³)</th>
                <th>SKU编码</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in skuRows" :key="row.key">
                <td v-for="(label, i) in row.labels" :key="i">{{ label }}</td>
                <td><input v-model.number="row.price" type="number" step="0.01" class="form-input" /></td>
                <td><input v-model.number="row.linePrice" type="number" step="0.01" class="form-input" /></td>
                <td><input v-model.number="row.stock" type="number" class="form-input" /></td>
                <td><input v-model.number="row.weight" type="number" min="0" step="0.001" class="form-input" /></td>
                <td><input v-model.number="row.volume" type="number" min="0" step="0.001" class="form-input" /></td>
                <td><input v-model="row.skuCode" class="form-input" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <p class="card-title" style="margin-top: 24px">物流</p>
    <div class="form-item">
      <label class="form-label"><span class="req">*</span>配送方式</label>
      <div style="display: flex; gap: 16px; font-size: 13px; color: var(--text-secondary)">
        <label><input type="checkbox" value="express" v-model="form.deliveryType" /> 快递发货</label>
        <label><input type="checkbox" value="pickup" v-model="form.deliveryType" /> 门店自提</label>
      </div>
      <div v-if="errors.deliveryType" class="field-error">{{ errors.deliveryType }}</div>
    </div>
    <div class="form-row form-item">
      <div>
        <label class="form-label">运费模板</label>
        <select v-model="form.freightTemplateId" class="form-select">
          <option :value="null">不使用运费模板</option>
          <option v-for="t in freightTemplates" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>
      <div>
        <label class="form-label">是否虚拟商品</label>
        <select v-model="form.isVirtual" class="form-select">
          <option :value="false">否，需要物流发货</option>
          <option :value="true">是，无需物流</option>
        </select>
      </div>
    </div>

    <p class="card-title" style="margin-top: 24px">商品详情</p>
    <div class="form-item">
      <RichTextEditor v-model="form.content" />
    </div>

    <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--gridline)">
      <button class="btn btn-ghost" @click="router.push({ name: 'goods' })">取消</button>
      <button class="btn btn-primary" :disabled="submitting" @click="onSubmit">
        {{ submitting ? (isEdit ? '保存中…' : '发布中…') : (isEdit ? '保存商品' : '发布商品') }}
      </button>
    </div>
  </div>
  </a-spin>
</template>

<style scoped>
.spec-type-btn {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
}
.spec-type-btn.active {
  background: var(--primary);
  color: #fff;
  font-weight: 600;
}
.spec-chip {
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}
.spec-chip.selected {
  background: var(--primary-bg);
  border-color: var(--primary-bg-2);
  color: var(--primary-hover);
  font-weight: 600;
}
.spec-tag {
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid var(--border-strong);
  font-size: 12.5px;
  color: var(--text-muted);
  cursor: pointer;
  background: var(--surface-2);
}
.spec-tag.active {
  background: var(--primary-bg);
  border-color: var(--primary-bg-2);
  color: var(--primary-hover);
}
.sku-table th,
.sku-table td {
  white-space: nowrap;
}
.sku-table input.form-input {
  height: 30px;
  padding: 0 8px;
}
.field-error {
  font-size: 12px;
  color: var(--status-critical);
  margin-top: 4px;
}
</style>
