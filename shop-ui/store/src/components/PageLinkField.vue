<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  pages: { type: Array, default: () => [] },
  resources: { type: Object, default: () => ({}) },
  placeholder: { type: String, default: '选择或填写页面链接' },
})

const emit = defineEmits(['update:modelValue'])
const open = ref(false)
const keyword = ref('')

const FIXED_GROUPS = [
  {
    name: '商城页面',
    links: [
      ['商城首页', '/pages/index/index', '店铺首页'],
      ['商品分类', '/pages/goods/list', '商品分类与列表'],
      ['购物车', '/pages/cart/index', '当前用户购物车'],
      ['个人中心', '/pages/my/index', '会员中心'],
      ['我的订单', '/pages/order/list', '订单列表'],
      ['收货地址', '/pages/address/list', '地址管理'],
    ],
  },
  {
    name: '营销页面',
    links: [
      ['领券中心', '/pages/coupon/center', '可领取优惠券'],
      ['我的优惠券', '/pages/coupon/mine', '会员优惠券'],
      ['秒杀专场', '/pages/seckill/index', '限时秒杀列表'],
      ['拼团专场', '/pages/group/index', '拼团活动列表'],
      ['砍价专场', '/pages/bargain/index', '砍价活动列表'],
      ['每日签到', '/pages/my/sign-in', '签到领积分'],
      ['积分商城', '/pages/points-mall/index', '积分兑换商品'],
      ['分销中心', '/pages/my/dealer', '分销账户与团队'],
      ['附近门店', '/pages/store/locator', '门店与自提点'],
    ],
  },
]

const PARAM_LINKS = [
  ['商品详情', '/pages/goods/detail?id=商品ID', 'id 必填'],
  ['文章详情', '/pages/article/detail?id=文章ID', 'id 必填'],
  ['自定义页面', '/pages/diy/custom?id=页面ID', 'id 必填'],
  ['拼团详情', '/pages/group/detail?id=活动ID', 'id 必填'],
  ['砍价详情', '/pages/bargain/detail?id=活动ID', 'id 必填'],
]

const customLinks = computed(() => (props.pages || [])
  .filter((page) => !page.isDefault)
  .map((page) => ({
    name: page.name || `页面 ${page.id}`,
    path: `/pages/diy/custom?id=${page.id}`,
    desc: page.pageData ? '已发布' : '草稿未发布',
  })))

const goodsById = computed(() => new Map((props.resources.goods || []).map((row) => [String(row.id), row])))

function statusText(status) {
  if (status === 'on') return '上架'
  if (status === 'off') return '下架'
  if (status === 'visible') return '显示中'
  if (status === 'hidden') return '已隐藏'
  return ''
}

function activityName(row, type) {
  const goods = goodsById.value.get(String(row.goodsId))
  return goods?.name || `${type}活动 ${row.id}`
}

const resourceGroups = computed(() => [
  {
    name: '商品详情',
    links: (props.resources.goods || []).map((row) => ({
      id: row.id,
      name: row.name || `商品 ${row.id}`,
      path: `/pages/goods/detail?id=${row.id}`,
      idLabel: '商品ID',
      meta: [row.code ? `编码 ${row.code}` : '', statusText(row.status)].filter(Boolean).join(' · '),
    })),
  },
  {
    name: '文章详情',
    links: (props.resources.articles || []).map((row) => ({
      id: row.id,
      name: row.title || `文章 ${row.id}`,
      path: `/pages/article/detail?id=${row.id}`,
      idLabel: '文章ID',
      meta: statusText(row.status),
    })),
  },
  {
    name: '拼团详情',
    links: (props.resources.groups || []).map((row) => ({
      id: row.id,
      name: activityName(row, '拼团'),
      path: `/pages/group/detail?id=${row.id}`,
      idLabel: '活动ID',
      meta: [row.goodsId ? `商品ID ${row.goodsId}` : '', statusText(row.status)].filter(Boolean).join(' · '),
    })),
  },
  {
    name: '砍价详情',
    links: (props.resources.bargains || []).map((row) => ({
      id: row.id,
      name: activityName(row, '砍价'),
      path: `/pages/bargain/detail?id=${row.id}`,
      idLabel: '活动ID',
      meta: [row.goodsId ? `商品ID ${row.goodsId}` : '', statusText(row.status)].filter(Boolean).join(' · '),
    })),
  },
])

function matches(...values) {
  const term = keyword.value.trim().toLowerCase()
  return !term || values.some((value) => String(value || '').toLowerCase().includes(term))
}

const visibleGroups = computed(() => FIXED_GROUPS.map((group) => ({
  ...group,
  links: group.links.filter((link) => matches(...link)),
})).filter((group) => group.links.length))

const visibleCustomLinks = computed(() => customLinks.value.filter((link) => matches(link.name, link.path, link.desc)))
const visibleParamLinks = computed(() => PARAM_LINKS.filter((link) => matches(...link)))
const visibleResourceGroups = computed(() => resourceGroups.value.map((group) => ({
  ...group,
  links: group.links.filter((link) => matches(group.name, link.name, link.id, link.idLabel, link.path, link.meta)),
})).filter((group) => group.links.length))

function update(value) {
  emit('update:modelValue', value)
}

function choose(path) {
  update(path)
  open.value = false
}

function showLinks() {
  keyword.value = ''
  open.value = true
}
</script>

<template>
  <div class="page-link-field">
    <div class="link-input-row">
      <input
        class="form-input"
        :value="modelValue"
        :placeholder="placeholder"
        @input="update($event.target.value)"
      />
      <button type="button" class="btn btn-sm" @click="showLinks">选择</button>
    </div>
    <button type="button" class="link-reference" @click="showLinks">查看页面链接</button>
  </div>

  <Teleport to="body">
    <div v-if="open" class="link-modal-mask" @click.self="open = false">
      <section class="link-modal" role="dialog" aria-modal="true" aria-label="页面链接">
        <header class="link-modal-head">
          <div>
            <h3>页面链接</h3>
            <p>选择具体商品或页面后自动填写完整路径，无需手工查找 ID。</p>
          </div>
          <button type="button" class="link-close" aria-label="关闭" @click="open = false">×</button>
        </header>

        <div class="link-toolbar">
          <input v-model="keyword" class="form-input" placeholder="搜索页面、商品名称、编码或 ID" />
        </div>

        <div class="link-modal-body">
          <div v-for="group in visibleGroups" :key="group.name" class="link-section">
            <h4>{{ group.name }}</h4>
            <button v-for="link in group.links" :key="link[1]" type="button" class="link-row" @click="choose(link[1])">
              <span class="link-copy"><strong>{{ link[0] }}</strong><small>{{ link[2] }}</small></span>
              <code>{{ link[1] }}</code>
              <span class="link-use">使用</span>
            </button>
          </div>

          <div v-if="visibleCustomLinks.length" class="link-section">
            <h4>自定义装修页面</h4>
            <button v-for="link in visibleCustomLinks" :key="link.path" type="button" class="link-row" @click="choose(link.path)">
              <span class="link-copy"><strong>{{ link.name }}</strong><small>{{ link.desc }}</small></span>
              <code>{{ link.path }}</code>
              <span class="link-use">使用</span>
            </button>
          </div>

          <div v-for="group in visibleResourceGroups" :key="group.name" class="link-section resource-section">
            <h4>{{ group.name }} <small>点击记录自动填写</small></h4>
            <button v-for="link in group.links" :key="link.path" type="button" class="link-row resource-row" @click="choose(link.path)">
              <span class="link-copy">
                <strong>{{ link.name }}</strong>
                <small><b>{{ link.idLabel }}：{{ link.id }}</b><template v-if="link.meta"> · {{ link.meta }}</template></small>
              </span>
              <code>{{ link.path }}</code>
              <span class="link-use">使用</span>
            </button>
          </div>

          <div v-if="visibleParamLinks.length" class="link-section">
            <h4>手动填写格式</h4>
            <div v-for="link in visibleParamLinks" :key="link[1]" class="link-row parameter-row">
              <span class="link-copy"><strong>{{ link[0] }}</strong><small>{{ link[2] }}</small></span>
              <code>{{ link[1] }}</code>
            </div>
          </div>

          <div v-if="!visibleGroups.length && !visibleCustomLinks.length && !visibleResourceGroups.length && !visibleParamLinks.length" class="link-empty">没有匹配的页面、名称或 ID</div>
        </div>

        <footer class="link-modal-foot">
          <span>H5 外部链接可直接填写 https:// 开头的完整地址</span>
          <button type="button" class="btn" @click="open = false">关闭</button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.page-link-field { margin-top: 8px; }
.link-input-row { display: flex; align-items: center; gap: 8px; }
.link-input-row .form-input { min-width: 0; flex: 1; }
.link-reference { margin-top: 7px; padding: 0; border: 0; background: transparent; color: var(--primary); cursor: pointer; font-size: 12px; }
.link-reference:hover { text-decoration: underline; }
.link-modal-mask { position: fixed; inset: 0; z-index: 1200; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(24, 29, 34, .5); }
.link-modal { width: min(780px, calc(100vw - 48px)); max-height: min(760px, calc(100vh - 48px)); display: flex; flex-direction: column; overflow: hidden; border: 1px solid var(--border); border-radius: 8px; background: var(--surface); box-shadow: 0 18px 50px rgba(17, 24, 31, .2); }
.link-modal-head { display: flex; align-items: flex-start; justify-content: space-between; padding: 20px 22px 16px; border-bottom: 1px solid var(--border); }
.link-modal-head h3 { margin: 0; font-size: 18px; }
.link-modal-head p { margin: 6px 0 0; color: var(--text-muted); font-size: 12px; }
.link-close { width: 32px; height: 32px; padding: 0; border: 1px solid var(--border); border-radius: 6px; background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 22px; line-height: 28px; }
.link-toolbar { padding: 14px 22px; border-bottom: 1px solid var(--border); background: var(--surface-2); }
.link-modal-body { overflow-y: auto; padding: 4px 22px 18px; }
.link-section h4 { margin: 18px 0 8px; padding-left: 9px; border-left: 3px solid var(--primary); font-size: 14px; }
.link-section h4 small { margin-left: 6px; color: var(--text-muted); font-size: 11px; font-weight: 400; }
.link-row { width: 100%; min-height: 58px; display: grid; grid-template-columns: minmax(150px, .8fr) minmax(280px, 1.5fr) 42px; align-items: center; gap: 14px; padding: 10px 8px; border: 0; border-bottom: 1px dashed var(--border); border-radius: 0; background: transparent; color: var(--text); cursor: pointer; text-align: left; }
.link-row:hover { background: var(--surface-2); }
.link-copy { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.link-copy strong { font-size: 13px; }
.link-copy small { color: var(--text-muted); font-size: 11px; }
.link-copy small b { color: var(--text-secondary); font-weight: 600; }
.link-row code { min-width: 0; overflow: hidden; color: #27824f; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.link-use { color: var(--primary); font-size: 12px; text-align: right; }
.parameter-row { cursor: default; grid-template-columns: minmax(150px, .8fr) minmax(280px, 1.5fr); }
.parameter-row:hover { background: transparent; }
.resource-section { margin-top: 4px; }
.resource-row { min-height: 64px; }
.link-empty { padding: 60px 0; color: var(--text-muted); text-align: center; }
.link-modal-foot { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 22px; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 12px; }
@media (max-width: 720px) {
  .link-modal-mask { padding: 12px; }
  .link-modal { width: calc(100vw - 24px); max-height: calc(100vh - 24px); }
  .link-row, .parameter-row { grid-template-columns: 1fr; gap: 5px; }
  .link-use { text-align: left; }
}
</style>
