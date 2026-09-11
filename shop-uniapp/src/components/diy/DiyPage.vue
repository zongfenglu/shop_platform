<script setup>
import { computed, ref, watch } from 'vue'
import { receiveCoupon } from '@/api/index'
import { getToken } from '@/utils/request'
import { mediaUrl } from '@/utils/request'
import { openGoodsSearch } from '@/utils/goodsSearch'

const props = defineProps({
  items: { type: Array, default: () => [] },
  page: { type: Object, default: () => ({}) },
})

const COUPON_TONES = ['#e34948', '#8b6cc9', '#eb6834']
const searchKeyword = ref('')

const wrapStyle = computed(() => {
  const meta = props.page || {}
  const color = meta.bgColor || '#f5f4f1'
  const style = { backgroundColor: color }
  if (meta.bgType === 'image' && meta.bgImage) {
    style.backgroundImage = `url(${meta.bgImage})`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center top'
    style.backgroundRepeat = 'no-repeat'
  }
  return style
})

watch(
  () => wrapStyle.value.backgroundColor,
  (color) => {
    if (!color || typeof uni.setBackgroundColor !== 'function') return
    uni.setBackgroundColor({
      backgroundColor: color,
      backgroundColorTop: color,
      backgroundColorBottom: color,
    })
  },
  { immediate: true },
)

function fmtPrice(v) {
  return Number(v ?? 0).toFixed(2)
}

function isIconUrl(icon) {
  return typeof icon === 'string' && /^(https?:\/\/|\/)/.test(icon)
}

function blockStyle(item) {
  const s = {}
  if (item.bgColor) s.backgroundColor = item.bgColor
  const margin = clampNumber(item.margin, 0, 80, 0)
  if (margin) {
    s.marginTop = margin + 'px'
    s.marginBottom = margin + 'px'
  }
  return s
}

function clampNumber(value, min, max, fallback) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return fallback
  return Math.max(min, Math.min(max, parsed))
}

function videoHeight(item) {
  return clampNumber(item?.height, 80, 400, 190)
}

function groupList(item) {
  if (item.list && item.list.length) return item.list
  return item.active ? [item.active] : []
}

function couponFace(c) {
  if (!c) return ''
  if (c.type === 'discount') return (Number(c.discountRatio) * 10).toFixed(1).replace(/\.0$/, '') + '折'
  return '¥' + c.reducePrice
}

function couponThreshold(c) {
  const min = Number(c?.minPrice || 0)
  return min > 0 ? `满${min.toFixed(2)}元可用` : '无门槛'
}

function goLink(link) {
  if (!link) return
  const s = String(link).trim()
  if (s.startsWith('/pages/')) {
    const path = s.split('?')[0]
    if (path === '/pages/index/index' || path === '/pages/cart/index' || path === '/pages/my/index' || path === '/pages/goods/list') {
      uni.switchTab({ url: path })
    } else {
      uni.navigateTo({ url: s })
    }
    return
  }
  const diyId = s.match(/^(?:diy:|page:)?(\d+)$/)
  if (diyId) {
    uni.navigateTo({ url: `/pages/diy/custom?id=${diyId[1]}` })
    return
  }
  const shortcuts = {
    seckill: '/pages/seckill/index',
    group: '/pages/group/index',
    bargain: '/pages/bargain/index',
    coupon: '/pages/coupon/center',
    goods: '/pages/goods/list',
    sign: '/pages/my/sign-in',
    points: '/pages/points-mall/index',
    store: '/pages/store/locator',
  }
  if (shortcuts[s]) {
    uni.navigateTo({ url: shortcuts[s] })
    return
  }
  if (/^https?:\/\//.test(s)) {
    // #ifdef H5
    window.open(s, '_blank')
    // #endif
  }
}

function goGoods(id) {
  uni.navigateTo({ url: `/pages/goods/detail?id=${id}` })
}

function goSeckill(activeId, skuId, goodsId) {
  uni.navigateTo({ url: `/pages/order/checkout?skuId=${skuId}&goodsId=${goodsId}&quantity=1&activityType=seckill&activityId=${activeId}` })
}

function goGroup(id) {
  if (id) uni.navigateTo({ url: `/pages/group/detail?id=${id}` })
}

function goBargain(id) {
  if (id) uni.navigateTo({ url: `/pages/bargain/detail?id=${id}` })
}

async function onReceive(id) {
  if (!getToken()) {
    uni.navigateTo({ url: '/pages/my/login' })
    return
  }
  try {
    await receiveCoupon(id)
    uni.showToast({ title: '领取成功', icon: 'success' })
  } catch (e) {
    // request.js 已提示
  }
}

function submitSearch() {
  openGoodsSearch(searchKeyword.value)
}
</script>

<template>
  <view class="diy" :style="wrapStyle">
    <view v-for="(item, idx) in items" :key="item._cid || idx" class="block" :style="blockStyle(item)">
      <view v-if="item.type === 'search'" class="search">
        <text>🔍</text>
        <input
          v-model="searchKeyword"
          class="search-input"
          :placeholder="item.placeholder || '搜索商品'"
          confirm-type="search"
          @confirm="submitSearch"
        />
        <text class="search-action" @click="submitSearch">搜索</text>
      </view>

      <view v-else-if="item.type === 'banner'" class="banner-wrap">
        <view class="banner">
          <image
            v-if="item.images && item.images.length && item.images[item._idx || 0]?.url"
            class="banner-img"
            :src="item.images[item._idx || 0].url"
            mode="aspectFill"
            @click="goLink(item.images[item._idx || 0].link)"
          />
          <view v-else class="banner-ph">轮播图</view>
          <view v-if="(item.images || []).length > 1" class="banner-dots">
            <text v-for="(_, i) in item.images" :key="i" class="dot" :class="{ on: (item._idx || 0) === i }" @click="item._idx = i" />
          </view>
        </view>
      </view>

      <view v-else-if="item.type === 'imageGroup'" class="img-stack">
        <image
          v-for="(im, i) in (item.images || []).filter((x) => x.url)"
          :key="i"
          class="stack-img"
          :src="im.url"
          mode="widthFix"
          @click="goLink(im.link)"
        />
      </view>

      <view v-else-if="item.type === 'imageWindow'" class="window">
        <image v-if="item.images?.[0]?.url" class="window-main" :src="item.images[0].url" mode="aspectFill" @click="goLink(item.images[0].link)" />
        <view v-else class="window-main ph">橱窗</view>
        <view class="window-side">
          <image v-if="item.images?.[1]?.url" class="window-sm" :src="item.images[1].url" mode="aspectFill" @click="goLink(item.images[1].link)" />
          <view v-else class="window-sm ph" />
          <image v-if="item.images?.[2]?.url" class="window-sm" :src="item.images[2].url" mode="aspectFill" @click="goLink(item.images[2].link)" />
          <view v-else class="window-sm ph" />
        </view>
      </view>

      <view v-else-if="item.type === 'video'" class="video-wrap">
        <video
          v-if="item.url"
          class="video"
          :src="mediaUrl(item.url)"
          :poster="item.cover ? mediaUrl(item.cover) : undefined"
          :autoplay="!!item.autoplay"
          :muted="!!item.autoplay"
          :style="{ height: videoHeight(item) + 'px' }"
          object-fit="contain"
          playsinline
          webkit-playsinline
          controls
        />
        <image v-else-if="item.cover" class="video" :src="mediaUrl(item.cover)" mode="aspectFill" :style="{ height: videoHeight(item) + 'px' }" />
        <view v-else class="video ph" :style="{ height: videoHeight(item) + 'px' }">视频</view>
      </view>

      <view v-else-if="item.type === 'article'">
        <view v-for="(art, i) in (item.items || [])" :key="i" class="article-row" @click="goLink(art.link)">
          <view class="article-txt">
            <text class="article-title">{{ art.title || '文章标题' }}</text>
            <text class="muted">{{ art.views || 0 }}次浏览</text>
          </view>
          <image v-if="art.cover" class="article-cover" :src="art.cover" mode="aspectFill" />
          <view v-else class="article-cover ph" />
        </view>
      </view>

      <view v-else-if="item.type === 'news'" class="news" @click="goLink(item.items?.[0]?.link)">
        <text class="news-tag">头条</text>
        <text class="news-label">{{ item.label || '快报' }}</text>
        <text class="news-title">{{ item.items?.[0]?.title || '暂无头条' }}</text>
        <text class="muted">›</text>
      </view>

      <view v-else-if="item.type === 'notice'" class="notice">
        <text>📢</text>
        <text class="news-title">{{ item.text || '公告' }}</text>
      </view>

      <view v-else-if="item.type === 'navBar'" class="nav-grid">
        <view v-for="(nav, i) in (item.items || [])" :key="i" class="nav-item" @click="goLink(nav.link)">
          <image v-if="isIconUrl(nav.icon)" class="nav-ico" :src="nav.icon" mode="aspectFill" />
          <view v-else class="nav-ico nav-ph">{{ (nav.icon || nav.text || '◎').slice(0, 1) }}</view>
          <text class="nav-text">{{ nav.text || '入口' }}</text>
        </view>
      </view>

      <view v-else-if="item.type === 'goods'" class="goods-grid" :class="item.style === 'list' ? 'cols-1' : 'cols-2'">
        <view v-for="g in (item.goodsList || [])" :key="g.id" class="goods-card" @click="goGoods(g.id)">
          <image v-if="g.image" class="goods-img" :src="g.image" mode="aspectFill" />
          <view v-else class="goods-img ph">无图</view>
          <view v-if="item.showName !== false" class="goods-name">{{ g.name }}</view>
          <view class="price-row">
            <text v-if="item.showPrice !== false" class="price">¥{{ fmtPrice(g.price) }}</text>
            <text v-if="item.showLinePrice !== false && g.linePrice" class="line">¥{{ fmtPrice(g.linePrice) }}</text>
          </view>
        </view>
        <view v-if="!(item.goodsList || []).length" class="empty-hint">暂无商品</view>
      </view>

      <view v-else-if="item.type === 'coupon'" class="coupon-row">
        <view v-for="(c, i) in (item.couponList || [])" :key="c.id" class="ticket" @click="onReceive(c.id)">
          <view class="ticket-main" :style="{ background: COUPON_TONES[i % COUPON_TONES.length] }">
            <text class="ticket-face">{{ couponFace(c) }}</text>
            <text class="ticket-sub">{{ couponThreshold(c) }}</text>
          </view>
          <view class="ticket-act">立即领取</view>
        </view>
        <view v-if="!(item.couponList || []).length" class="empty-hint">暂无可领优惠券</view>
      </view>

      <view v-else-if="item.type === 'seckill'" class="seckill">
        <view class="seckill-hd" @click="uni.navigateTo({ url: '/pages/seckill/index' })">
          <text class="seckill-badge">⚡ 限时秒杀</text>
          <text class="seckill-live">正在疯抢</text>
          <view class="timer"><text>00</text><text>:</text><text>58</text><text>:</text><text>04</text></view>
          <text class="more">更多 ›</text>
        </view>
        <view class="seckill-grid" :class="'cols-' + (item.columns || 3)">
          <view
            v-for="g in (item.active?.goods || [])"
            :key="g.skuId"
            class="seckill-card"
            @click="goSeckill(item.active.id, g.skuId, g.goodsId)"
          >
            <image v-if="g.image" class="seckill-img" :src="g.image" mode="aspectFill" />
            <view v-else class="seckill-img ph" />
            <text v-if="item.showName !== false" class="goods-name">{{ g.goodsName }}</text>
            <view class="price-row">
              <text v-if="item.showPrice !== false" class="price">{{ fmtPrice(g.seckillPrice) }}</text>
              <text v-if="item.showLinePrice !== false && g.originalPrice" class="line">{{ fmtPrice(g.originalPrice) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else-if="item.type === 'group'">
        <view v-for="row in groupList(item)" :key="row.id" class="list-row" @click="goGroup(row.id)">
          <image v-if="row.goodsImage" class="list-img" :src="row.goodsImage" mode="aspectFill" />
          <view v-else class="list-img ph" />
          <view class="list-body">
            <text class="list-title">{{ row.goodsName || '拼团商品' }}</text>
            <text v-if="item.showSellingPoint !== false" class="muted">{{ row.sellingPoint || ((row.groupNum || 2) + '人团') }}</text>
            <view class="list-foot">
              <view>
                <text v-if="item.showPrice !== false" class="price">¥{{ fmtPrice(row.minGroupPrice) }}</text>
                <text v-if="item.showLinePrice !== false && row.originalPrice" class="line">¥{{ fmtPrice(row.originalPrice) }}</text>
              </view>
              <text class="cta">去拼团</text>
            </view>
          </view>
        </view>
        <view v-if="!groupList(item).length" class="empty-hint">暂无拼团</view>
      </view>

      <view v-else-if="item.type === 'bargain'">
        <view v-for="row in groupList(item)" :key="row.id" class="list-row" @click="goBargain(row.id)">
          <image v-if="row.goodsImage" class="list-img" :src="row.goodsImage" mode="aspectFill" />
          <view v-else class="list-img ph" />
          <view class="list-body">
            <text class="list-title">{{ row.goodsName || '砍价商品' }}</text>
            <text class="muted">邀好友砍价</text>
            <view class="list-foot">
              <view>
                <text v-if="item.showLinePrice !== false && row.originalPrice" class="line">¥ {{ fmtPrice(row.originalPrice) }}</text>
                <text v-if="item.showPrice !== false" class="price">最低 ¥ {{ fmtPrice(row.floorPrice) }}</text>
              </view>
              <text class="cta tan">立即参加</text>
            </view>
          </view>
        </view>
        <view v-if="!groupList(item).length" class="empty-hint">暂无砍价</view>
      </view>

      <view v-else-if="item.type === 'store'">
        <view v-for="s in (item.storeList || [])" :key="s.id" class="store-row" @click="uni.navigateTo({ url: '/pages/store/locator' })">
          <image v-if="s.logo" class="store-logo" :src="s.logo" mode="aspectFill" />
          <view v-else class="store-logo ph" />
          <view>
            <text class="store-name">{{ s.name }}</text>
            <text class="muted">门店地址：{{ s.address || '—' }}</text>
            <text class="muted">联系电话：{{ s.phone || '—' }}</text>
          </view>
        </view>
        <view v-if="!(item.storeList || []).length" class="empty-hint">暂无门店</view>
      </view>

      <view v-else-if="item.type === 'followMp'" class="follow">
        <image v-if="item.logo" class="store-logo" :src="item.logo" mode="aspectFill" />
        <view v-else class="store-logo ph" />
        <view class="list-body">
          <text class="list-title">{{ item.title || '关注公众号' }}</text>
          <text class="muted">{{ item.desc || '获取更多优惠与新品' }}</text>
        </view>
        <text class="cta" @click="uni.showToast({ title: '请在微信内关注店铺公众号', icon: 'none' })">关注</text>
      </view>

      <view v-else-if="item.type === 'richText'" class="rich">
        <rich-text v-if="item.html" :nodes="item.html" />
        <view v-else class="empty-hint">暂无内容</view>
      </view>

      <view v-else-if="item.type === 'blank'" :style="{ height: (item.height || 20) + 'px' }" />

      <view v-else-if="item.type === 'divider'" class="divider">
        <view class="divider-line" :style="{ borderTopStyle: item.style || 'solid', borderTopColor: item.color || '#e1e0d9' }" />
      </view>

      <view v-else-if="item.type === 'customerService'" class="cs" @click="uni.showToast({ title: '请联系店铺客服', icon: 'none' })">
        🎧 在线客服
      </view>
    </view>
  </view>
</template>

<style scoped>
.diy { min-height: 100vh; box-sizing: border-box; }
.block { padding: 0 24rpx; }
.search {
  display: flex; align-items: center; gap: 12rpx; margin: 16rpx 0;
  background: #fff; border-radius: 36rpx; height: 68rpx; padding: 0 28rpx;
  color: #898781; font-size: 26rpx;
}
.search-input {
  flex: 1; min-width: 0; height: 68rpx; line-height: 68rpx;
  color: #2b2a27; font-size: 26rpx;
}
.search-action { color: #2a78d6; flex-shrink: 0; }
.banner-wrap { padding: 16rpx; }
.banner { position: relative; height: 280rpx; overflow: hidden; background: #eef4fc; border-radius: 16rpx; }
.banner-img, .banner-ph { width: 100%; height: 100%; }
.banner-ph { display: flex; align-items: center; justify-content: center; color: #fff; background: linear-gradient(135deg,#2a78d6,#1c5cab); }
.banner-dots { position: absolute; bottom: 12rpx; left: 0; right: 0; display: flex; justify-content: center; gap: 8rpx; }
.dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: rgba(255,255,255,0.45); }
.dot.on { background: #fff; }
.img-stack { display: flex; flex-direction: column; gap: 8rpx; }
.stack-img { width: 100%; }
.window { display: flex; gap: 8rpx; height: 280rpx; }
.window-main { flex: 1.4; height: 100%; background: #eef4fc; }
.window-side { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.window-sm { height: 136rpx; background: #eef4fc; }
.video-wrap { padding: 0; }
.video { width: 100%; background: #111; }
.ph { display: flex; align-items: center; justify-content: center; background: #dceeff; color: #7aa4d4; }

.article-row { display: flex; gap: 20rpx; padding: 20rpx 0; border-bottom: 1rpx solid #f0efec; }
.article-txt { flex: 1; display: flex; flex-direction: column; gap: 10rpx; }
.article-title { font-size: 28rpx; font-weight: 650; }
.article-cover { width: 160rpx; height: 112rpx; background: #dceeff; flex-shrink: 0; }
.muted { color: #9a9892; font-size: 22rpx; }

.news, .notice { display: flex; align-items: center; gap: 12rpx; padding: 16rpx 0; }
.news-tag { background: #eb6834; color: #fff; border-radius: 8rpx; padding: 2rpx 12rpx; font-size: 20rpx; }
.news-label { font-weight: 700; font-size: 26rpx; }
.news-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 26rpx; }

.nav-grid { display: flex; flex-wrap: wrap; padding: 16rpx 0; }
.nav-item { width: 20%; display: flex; flex-direction: column; align-items: center; gap: 8rpx; padding: 12rpx 0; }
.nav-ico { width: 80rpx; height: 80rpx; border-radius: 50%; background: #eef4fc; }
.nav-ph { display: flex; align-items: center; justify-content: center; color: #2a78d6; font-size: 28rpx; }
.nav-text { font-size: 22rpx; color: #4a4844; }

.goods-grid { display: flex; flex-wrap: wrap; gap: 16rpx; padding: 8rpx 0; }
.goods-grid.cols-2 .goods-card { width: calc(50% - 8rpx); }
.goods-grid.cols-1 .goods-card { width: 100%; }
.goods-card { background: #fff; overflow: hidden; }
.goods-img { width: 100%; height: 300rpx; background: #dceeff; }
.goods-name { font-size: 26rpx; padding: 12rpx 8rpx 0; line-height: 1.4; }
.price-row { padding: 8rpx; display: flex; align-items: baseline; gap: 8rpx; }
.price { color: #d4380d; font-size: 30rpx; font-weight: 700; }
.line { color: #b5b3ad; font-size: 22rpx; text-decoration: line-through; }

.coupon-row { display: flex; gap: 16rpx; padding: 16rpx 0; overflow-x: auto; }
.ticket { display: flex; min-width: 300rpx; flex: 1; overflow: hidden; border-radius: 12rpx; }
.ticket-main { flex: 1; color: #fff; padding: 20rpx 16rpx 20rpx 28rpx; }
.ticket-face { font-size: 40rpx; font-weight: 800; display: block; }
.ticket-sub { font-size: 20rpx; opacity: .9; }
.ticket-act {
  width: 56rpx; background: #2b2b2b; color: #fff; writing-mode: vertical-rl;
  display: flex; align-items: center; justify-content: center; letter-spacing: 4rpx; font-size: 22rpx;
}

.seckill { padding: 16rpx 0; }
.seckill-hd { display: flex; align-items: center; gap: 12rpx; margin-bottom: 16rpx; }
.seckill-badge { background: #eb6834; color: #fff; border-radius: 999rpx; padding: 4rpx 16rpx; font-size: 22rpx; font-weight: 700; }
.seckill-live { font-size: 22rpx; }
.timer { display: flex; align-items: center; gap: 4rpx; font-size: 20rpx; }
.timer text:nth-child(odd) { background: #111; color: #fff; padding: 2rpx 8rpx; border-radius: 6rpx; }
.more { margin-left: auto; color: #898781; font-size: 22rpx; }
.seckill-grid { display: flex; gap: 12rpx; }
.seckill-grid.cols-3 .seckill-card { width: calc(33.33% - 8rpx); }
.seckill-grid.cols-2 .seckill-card { width: calc(50% - 6rpx); }
.seckill-img { width: 100%; height: 200rpx; background: #dceeff; }

.list-row { display: flex; gap: 20rpx; padding: 20rpx 0; border-bottom: 1rpx solid #f0efec; }
.list-img { width: 160rpx; height: 160rpx; background: #dceeff; flex-shrink: 0; }
.list-body { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.list-title { font-size: 28rpx; font-weight: 650; }
.list-foot { margin-top: auto; display: flex; justify-content: space-between; align-items: flex-end; }
.cta { background: #e34948; color: #fff; border-radius: 999rpx; padding: 8rpx 20rpx; font-size: 22rpx; }
.cta.tan { background: #c4a574; }

.store-row { display: flex; gap: 20rpx; padding: 20rpx 0; border-bottom: 1rpx solid #f0efec; }
.store-logo { width: 88rpx; height: 88rpx; border-radius: 50%; background: #dceeff; flex-shrink: 0; }
.store-name { color: #d4380d; font-weight: 650; font-size: 28rpx; display: block; margin-bottom: 8rpx; }
.follow { display: flex; align-items: center; gap: 20rpx; padding: 20rpx 0; }
.rich { padding: 16rpx 0; font-size: 26rpx; }
.divider { padding: 16rpx 0; }
.divider-line { border-top-width: 1rpx; }
.cs { text-align: center; padding: 20rpx; color: #2a78d6; font-size: 26rpx; }
.empty-hint { text-align: center; color: #898781; font-size: 24rpx; padding: 24rpx 0; }
</style>
