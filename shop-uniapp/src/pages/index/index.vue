<script setup>
import { onMounted, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getCategoryTree, pageGoods, getDiyHome } from '@/api'
import { getShopId, setShopId } from '@/utils/request'
import DiyPage from '@/components/diy/DiyPage.vue'

/**
 * 首页。有已发布装修页时渲染 DIY 组件；否则走分类 + 商品瀑布流兜底。
 */
const shopId = ref('')
const inputShopId = ref('')
const needShopId = ref(false)
const diyPage = ref(null)

const categories = ref([])
const goodsList = ref([])
const loading = ref(false)
const keyword = ref('')

onMounted(() => {
  shopId.value = getShopId()
  inputShopId.value = shopId.value
  load()
})

// 从别的页面返回时刷新，比如在分类页改了筛选再回来
onShow(() => {
  if (needShopId.value || diyPage.value || loading.value) return
  if (goodsList.value.length === 0) load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function load() {
  loading.value = true
  try {
    const home = await getDiyHome().catch(() => null)
    if (home && home.exists) {
      diyPage.value = home
      needShopId.value = false
      categories.value = []
      goodsList.value = []
      return
    }
    diyPage.value = null
    const [cats, page] = await Promise.all([
      getCategoryTree(),
      pageGoods({ pageNum: 1, pageSize: 20, sort: 'default' }),
    ])
    categories.value = cats || []
    goodsList.value = page?.records || []
    needShopId.value = false
  } catch (e) {
    if (e && e.code === 20000) needShopId.value = true
    goodsList.value = []
    diyPage.value = null
  } finally {
    loading.value = false
  }
}

function onSaveShopId() {
  const v = String(inputShopId.value || '').trim()
  if (!v) {
    uni.showToast({ title: '请输入商城 ID', icon: 'none' })
    return
  }
  setShopId(v)
  shopId.value = v
  load()
}

function onSearch() {
  const kw = String(keyword.value || '').trim()
  uni.navigateTo({ url: `/pages/goods/list?keyword=${encodeURIComponent(kw)}` })
}

function goCategory(cat) {
  uni.navigateTo({ url: `/pages/goods/list?categoryId=${cat.id}&name=${encodeURIComponent(cat.name)}` })
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${id}` })
}

function fmtPrice(v) {
  return Number(v ?? 0).toFixed(2)
}
</script>

<template>
  <view class="page" :class="{ 'is-diy': !!diyPage }">
    <!-- 联调兜底：识别不到租户时才出现 -->
    <view v-if="needShopId" class="card">
      <view class="card-title">未能识别商城</view>
      <view class="card-body">
        <text class="muted small">
          用 IP 访问（测试机连开发机）时请填店铺数字 ID，演示店填 1001。有独立域名的正式环境不用填。
        </text>
        <input v-model="inputShopId" class="input" placeholder="输入 shopId，如 2084461863957803010" />
        <button class="btn" @click="onSaveShopId">保存并加载</button>
      </view>
    </view>

    <DiyPage v-else-if="diyPage" :items="diyPage.items" :page="diyPage.page" />

    <view v-else-if="loading" class="empty">加载中…</view>

    <template v-else>
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索商品" confirm-type="search" @confirm="onSearch" />
      <text class="search-btn" @click="onSearch">搜索</text>
    </view>

    <view class="hero">
      <view class="hero-title">多开云商城</view>
      <view class="hero-sub">好物上新 · 正品保障</view>
    </view>

    <view class="seckill-entry" @click="uni.navigateTo({ url: '/pages/seckill/index' })">
      <view class="seckill-entry-left">
        <text class="seckill-entry-title">⚡ 秒杀专场</text>
        <text class="seckill-entry-sub">限时抢购 · 限量秒杀</text>
      </view>
      <text class="seckill-entry-arrow">抢 ›</text>
    </view>

    <view class="promo-row">
      <view class="promo-item group-entry" @click="uni.navigateTo({ url: '/pages/group/index' })">
        <text class="promo-title">👥 拼团</text>
        <text class="promo-sub">人满成团</text>
      </view>
      <view class="promo-item bargain-entry" @click="uni.navigateTo({ url: '/pages/bargain/index' })">
        <text class="promo-title">🔪 砍价</text>
        <text class="promo-sub">好友助力</text>
      </view>
    </view>

    <view v-if="categories.length" class="nav-grid">
      <view v-for="cat in categories" :key="cat.id" class="nav-item" @click="goCategory(cat)">
        <image v-if="cat.image" class="nav-ico" :src="cat.image" mode="aspectFill" />
        <view v-else class="nav-ico nav-ico-ph">{{ cat.name.slice(0, 1) }}</view>
        <text class="nav-name">{{ cat.name }}</text>
      </view>
    </view>

    <view class="section-hd">
      <text class="section-title">为你推荐</text>
      <text class="section-more" @click="goDetail && uni.navigateTo({ url: '/pages/goods/list' })">全部 ›</text>
    </view>

    <view v-if="!goodsList.length" class="empty">暂无在售商品</view>
    <view v-else class="goods-grid">
      <view v-for="g in goodsList" :key="g.id" class="goods-card" @click="goDetail(g.id)">
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
          <text class="sales">已售 {{ g.sales ?? 0 }}</text>
        </view>
      </view>
    </view>
    </template>
  </view>
</template>

<style scoped>
.page {
  padding: 24rpx;
}
.page.is-diy {
  padding: 0;
  min-height: 100vh;
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
.hero {
  background: linear-gradient(135deg, #2a78d6, #1c5cab);
  border-radius: 24rpx;
  padding: 44rpx 32rpx;
  margin-bottom: 24rpx;
}
.hero-title {
  color: #fff;
  font-size: 38rpx;
  font-weight: 700;
}
.hero-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  margin-top: 8rpx;
}
.seckill-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #e34948, #ff6b35);
  border-radius: 18rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 24rpx;
  color: #fff;
}
.seckill-entry-left { display: flex; flex-direction: column; gap: 6rpx; }
.seckill-entry-title { font-size: 30rpx; font-weight: 700; }
.seckill-entry-sub { font-size: 22rpx; opacity: 0.9; }
.seckill-entry-arrow { font-size: 28rpx; font-weight: 700; }
.promo-row { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.promo-item {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6rpx;
  border-radius: 18rpx; padding: 24rpx 0; color: #fff;
}
.group-entry { background: linear-gradient(135deg, #2a78d6, #4ea0e8); }
.bargain-entry { background: linear-gradient(135deg, #722ed1, #9254de); }
.promo-title { font-size: 30rpx; font-weight: 700; }
.promo-sub { font-size: 22rpx; opacity: 0.9; }
.nav-grid {
  display: flex;
  flex-wrap: wrap;
  background: #fcfcfb;
  border-radius: 20rpx;
  padding: 24rpx 8rpx;
  margin-bottom: 24rpx;
}
.nav-item {
  width: 20%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 0;
}
.nav-ico {
  width: 84rpx;
  height: 84rpx;
  border-radius: 50%;
  background: #eef4fc;
}
.nav-ico-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2a78d6;
  font-size: 32rpx;
  font-weight: 600;
}
.nav-name {
  font-size: 22rpx;
  color: #4a4844;
}
.section-hd {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
}
.section-more {
  font-size: 24rpx;
  color: #898781;
}
.goods-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.goods-card {
  width: calc(50% - 10rpx);
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.08);
  border-radius: 20rpx;
  overflow: hidden;
}
.goods-img {
  width: 100%;
  height: 320rpx;
  background: #f0efec;
}
.goods-img-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b5b3ad;
  font-size: 24rpx;
}
.goods-info {
  padding: 16rpx 18rpx 20rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.goods-name {
  font-size: 27rpx;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.goods-sub {
  font-size: 22rpx;
  color: #898781;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.price {
  color: #d4380d;
  font-size: 32rpx;
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
.sales {
  font-size: 22rpx;
  color: #898781;
}
.empty {
  text-align: center;
  color: #898781;
  font-size: 26rpx;
  padding: 80rpx 0;
}
.card {
  background: #fcfcfb;
  border: 2rpx solid rgba(11, 11, 11, 0.1);
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.card-title {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}
.card-body {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.muted {
  color: #898781;
  font-size: 26rpx;
}
.small {
  font-size: 24rpx;
  line-height: 1.7;
}
.input {
  border: 2rpx solid rgba(11, 11, 11, 0.16);
  border-radius: 12rpx;
  height: 72rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
}
.btn {
  background: #2a78d6;
  color: #fff;
  border-radius: 12rpx;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 28rpx;
}
</style>
