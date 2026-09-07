<script setup>
import { computed, ref } from 'vue'
import { onLoad, onReachBottom, onPullDownRefresh, onShareAppMessage } from '@dcloudio/uni-app'
import { getCategoryTree, getDiyCategoryPage, pageGoods } from '@/api'

/**
 * 商品列表 / 分类页。对应原型 docs/prototype/h5/goods-list.html。
 * Tab「分类」按店铺装修的分类页模板展示；带 categoryId / keyword 时走商品列表。
 */
const SORTS = [
  { key: 'default', label: '综合' },
  { key: 'sales', label: '销量' },
  { key: 'price_asc', label: '价格↑' },
  { key: 'price_desc', label: '价格↓' },
]

const categories = ref([])
const activeCategoryId = ref(null)
const keyword = ref('')
const sort = ref('default')
const catalogStyle = ref('level1_small')
const shareTitle = ref('全部分类')
const catalogMode = ref(true)
const fromCatalog = ref(false)
const level2ParentId = ref(null)

const rows = ref([])
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)

const paginated = computed(() => sort.value !== 'price_asc' && sort.value !== 'price_desc')
const pageSize = computed(() => (paginated.value ? 10 : 50))
const hasMore = computed(() => paginated.value && rows.value.length < total.value)

const roots = computed(() => categories.value || [])
const level2Parent = computed(() =>
  roots.value.find((c) => String(c.id) === String(level2ParentId.value)) || roots.value[0] || null)
const level2Children = computed(() => level2Parent.value?.children || [])

onLoad(async (query) => {
  if (query?.categoryId) activeCategoryId.value = query.categoryId
  if (query?.keyword) keyword.value = decodeURIComponent(query.keyword)
  if (query?.name) uni.setNavigationBarTitle({ title: decodeURIComponent(query.name) })

  const landedOnGoods = !!(query?.categoryId || query?.keyword)
  catalogMode.value = !landedOnGoods
  fromCatalog.value = false

  const [tree, cfg] = await Promise.all([
    getCategoryTree().catch(() => []),
    getDiyCategoryPage().catch(() => null),
  ])
  categories.value = tree || []
  if (cfg?.style) catalogStyle.value = cfg.style
  if (cfg?.shareTitle) shareTitle.value = cfg.shareTitle
  level2ParentId.value = categories.value[0]?.id ?? null
  if (catalogMode.value) {
    uni.setNavigationBarTitle({ title: shareTitle.value || '全部分类' })
  } else {
    await load(true)
  }
})

onShareAppMessage(() => ({
  title: shareTitle.value || '全部分类',
}))

onPullDownRefresh(async () => {
  if (catalogMode.value) {
    categories.value = await getCategoryTree().catch(() => categories.value)
    uni.stopPullDownRefresh()
    return
  }
  await load(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!catalogMode.value && hasMore.value && !loading.value) load(false)
})

async function load(reset) {
  if (loading.value) return
  loading.value = true
  if (reset) pageNum.value = 1
  try {
    const page = await pageGoods({
      categoryId: activeCategoryId.value || undefined,
      keyword: keyword.value || undefined,
      sort: sort.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    const records = page?.records || []
    rows.value = reset ? records : rows.value.concat(records)
    total.value = page?.total ?? records.length
    if (records.length) pageNum.value += 1
  } catch (e) {
    if (reset) rows.value = []
  } finally {
    loading.value = false
  }
}

function onCategory(id) {
  // categoryId 是雪花 id，从这里到 URL 到后端全程按字符串传递。
  // 千万不要 Number(id) —— 19 位 id 超过 Number.MAX_SAFE_INTEGER，会被静默改成另一个合法值，
  // 结果是筛选不到商品/接口 403，看起来完全不像精度问题。见 CONTRIBUTING.md 的 Code Review 一节。
  activeCategoryId.value = activeCategoryId.value === id ? null : id
  load(true)
}

function onSort(key) {
  sort.value = key
  load(true)
}

function onSearch() {
  catalogMode.value = false
  fromCatalog.value = true
  load(true)
}

function openCategory(cat) {
  if (!cat?.id) return
  activeCategoryId.value = cat.id
  catalogMode.value = false
  fromCatalog.value = true
  uni.setNavigationBarTitle({ title: cat.name || shareTitle.value })
  load(true)
}

function backToCatalog() {
  catalogMode.value = true
  fromCatalog.value = false
  activeCategoryId.value = null
  keyword.value = ''
  rows.value = []
  uni.setNavigationBarTitle({ title: shareTitle.value || '全部分类' })
}

function onLevel2Parent(id) {
  level2ParentId.value = id
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${id}` })
}

function fmtPrice(v) {
  return Number(v ?? 0).toFixed(2)
}
</script>

<template>
  <view v-if="catalogMode" class="catalog">
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索商品" confirm-type="search" @confirm="onSearch" />
      <text class="search-btn" @click="onSearch">搜索</text>
    </view>

    <view v-if="!roots.length" class="empty">暂无分类，请先在商户后台创建</view>

    <view v-else-if="catalogStyle === 'level1_large'" class="large-list">
      <view v-for="c in roots" :key="c.id" class="large-item" @click="openCategory(c)">
        <image v-if="c.image" class="large-img" :src="c.image" mode="aspectFill" />
        <view v-else class="large-img large-ph">{{ c.name.slice(0, 1) }}</view>
        <view class="large-name">{{ c.name }}</view>
      </view>
    </view>

    <view v-else-if="catalogStyle === 'level2'" class="level2">
      <scroll-view class="level2-nav" scroll-y>
        <view
          v-for="c in roots"
          :key="c.id"
          class="level2-nav-item"
          :class="{ on: String(c.id) === String(level2Parent?.id) }"
          @click="onLevel2Parent(c.id)"
        >{{ c.name }}</view>
      </scroll-view>
      <view class="level2-main">
        <view v-if="!level2Children.length" class="empty" style="padding: 40rpx 0">暂无二级分类</view>
        <view v-else class="small-grid">
          <view v-for="c in level2Children" :key="c.id" class="small-item" @click="openCategory(c)">
            <image v-if="c.image" class="small-img" :src="c.image" mode="aspectFill" />
            <view v-else class="small-img small-ph">{{ c.name.slice(0, 1) }}</view>
            <view class="small-name">{{ c.name }}</view>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="small-grid pad">
      <view v-for="c in roots" :key="c.id" class="small-item" @click="openCategory(c)">
        <image v-if="c.image" class="small-img" :src="c.image" mode="aspectFill" />
        <view v-else class="small-img small-ph">{{ c.name.slice(0, 1) }}</view>
        <view class="small-name">{{ c.name }}</view>
      </view>
    </view>
  </view>

  <view v-else class="page">
    <view v-if="fromCatalog" class="back-row" @click="backToCatalog">‹ 返回分类</view>
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索商品" confirm-type="search" @confirm="onSearch" />
      <text class="search-btn" @click="onSearch">搜索</text>
    </view>

    <scroll-view v-if="categories.length" class="cat-scroll" scroll-x>
      <view class="cat-row">
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="cat-chip"
          :class="{ active: activeCategoryId === cat.id }"
          @click="onCategory(cat.id)"
        >
          {{ cat.name }}
        </view>
      </view>
    </scroll-view>

    <view class="sort-row">
      <view
        v-for="s in SORTS"
        :key="s.key"
        class="sort-item"
        :class="{ active: sort === s.key }"
        @click="onSort(s.key)"
      >
        {{ s.label }}
      </view>
    </view>

    <view v-if="loading && !rows.length" class="empty">加载中…</view>
    <view v-else-if="!rows.length" class="empty">没有找到商品</view>
    <view v-else class="goods-list">
      <view v-for="g in rows" :key="g.id" class="goods-row" @click="goDetail(g.id)">
        <image v-if="g.image" class="goods-img" :src="g.image" mode="aspectFill" />
        <view v-else class="goods-img goods-img-ph">暂无图片</view>
        <view class="goods-info">
          <view class="goods-name">{{ g.name }}</view>
          <view v-if="g.subName" class="goods-sub">{{ g.subName }}</view>
          <view class="price-row">
            <text class="price">¥{{ fmtPrice(g.price) }}</text>
            <text v-if="g.specType === 'multi'" class="price-from">起</text>
            <text v-if="g.linePrice" class="price-old">¥{{ fmtPrice(g.linePrice) }}</text>
          </view>
          <view class="meta-row">
            <text class="meta">已售 {{ g.sales ?? 0 }}</text>
            <text class="meta">库存 {{ g.stockTotal ?? 0 }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="rows.length && paginated" class="foot">
      <text v-if="loading">加载中…</text>
      <text v-else-if="hasMore">上滑加载更多</text>
      <text v-else>没有更多了</text>
    </view>
    <view v-else-if="rows.length && !paginated" class="foot">
      <text>按价格排序时只在当前 {{ rows.length }} 条内排序</text>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx;
}
.catalog {
  min-height: 100vh;
  background: #f5f4f1;
  padding-bottom: 24rpx;
}
.catalog .search-bar {
  padding: 20rpx 24rpx 8rpx;
}
.back-row {
  font-size: 26rpx;
  color: #2a78d6;
  padding-bottom: 12rpx;
}
.large-list {
  padding: 8rpx 24rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.large-item {
  background: #fcfcfb;
  border-radius: 16rpx;
  overflow: hidden;
}
.large-img {
  width: 100%;
  height: 240rpx;
  background: #e8e6e0;
}
.large-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #898781;
  font-size: 48rpx;
}
.large-name {
  padding: 18rpx 20rpx;
  font-size: 28rpx;
  font-weight: 600;
}
.small-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 8rpx 12rpx 24rpx;
}
.small-grid.pad {
  padding: 8rpx 16rpx 24rpx;
}
.small-item {
  width: 33.33%;
  padding: 16rpx 8rpx;
  box-sizing: border-box;
  text-align: center;
}
.small-img {
  width: 148rpx;
  height: 148rpx;
  margin: 0 auto;
  border-radius: 12rpx;
  background: #e8e6e0;
}
.small-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #898781;
  font-size: 36rpx;
}
.small-name {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #333;
}
.level2 {
  display: flex;
  min-height: 70vh;
}
.level2-nav {
  width: 176rpx;
  background: #fcfcfb;
  height: 70vh;
}
.level2-nav-item {
  padding: 28rpx 16rpx;
  font-size: 26rpx;
  color: #52514e;
}
.level2-nav-item.on {
  background: #f5f4f1;
  color: #2a78d6;
  font-weight: 600;
}
.level2-main {
  flex: 1;
  background: #f5f4f1;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.search-input {
  flex: 1;
  background: #fff;
  border: 2rpx solid rgba(11, 11, 11, 0.1);
  border-radius: 36rpx;
  height: 68rpx;
  padding: 0 28rpx;
  font-size: 26rpx;
}
.search-btn {
  font-size: 27rpx;
  color: #2a78d6;
  padding: 0 8rpx;
}
.cat-scroll {
  white-space: nowrap;
  margin-bottom: 16rpx;
}
.cat-row {
  display: inline-flex;
  gap: 14rpx;
  padding: 2rpx;
}
.cat-chip {
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.12);
  border-radius: 32rpx;
  padding: 10rpx 26rpx;
  font-size: 25rpx;
  color: #4a4844;
}
.cat-chip.active {
  background: #eef4fc;
  border-color: #2a78d6;
  color: #2a78d6;
}
.sort-row {
  display: flex;
  gap: 32rpx;
  padding: 12rpx 8rpx 20rpx;
}
.sort-item {
  font-size: 26rpx;
  color: #898781;
}
.sort-item.active {
  color: #2a78d6;
  font-weight: 600;
}
.goods-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.goods-row {
  display: flex;
  gap: 20rpx;
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.08);
  border-radius: 20rpx;
  padding: 20rpx;
}
.goods-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 14rpx;
  background: #f0efec;
  flex-shrink: 0;
}
.goods-img-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b5b3ad;
  font-size: 24rpx;
}
.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  min-width: 0;
}
.goods-name {
  font-size: 28rpx;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.goods-sub {
  font-size: 23rpx;
  color: #898781;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
  margin-top: auto;
}
.price {
  color: #d4380d;
  font-size: 34rpx;
  font-weight: 700;
}
.price-from {
  color: #d4380d;
  font-size: 22rpx;
}
.price-old {
  color: #b5b3ad;
  font-size: 22rpx;
  text-decoration: line-through;
}
.meta-row {
  display: flex;
  gap: 24rpx;
}
.meta {
  font-size: 22rpx;
  color: #898781;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 80rpx 0;
}
.foot {
  text-align: center;
  color: #b5b3ad;
  font-size: 23rpx;
  padding: 32rpx 0;
}
</style>
