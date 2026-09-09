<script setup>
const props = defineProps({
  item: { type: Object, required: true },
  couponList: { type: Array, default: () => [] },
  seckillActives: { type: Array, default: () => [] },
  groupActives: { type: Array, default: () => [] },
  bargainActives: { type: Array, default: () => [] },
  storeList: { type: Array, default: () => [] },
  goodsMeta: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['pick-image'])

const COUPON_TONES = ['#e34948', '#8b6cc9', '#eb6834']

function isUrl(v) {
  return typeof v === 'string' && /^(https?:\/\/|\/)/.test(v)
}

function wrapBg(extra = {}) {
  return { background: props.item.bgColor || '#fff', ...extra }
}

function couponById(id) {
  return props.couponList.find((c) => String(c.id) === String(id))
}

function couponFace(c) {
  if (!c) return '券'
  if (c.type === 'discount') return `${Number(c.discountRatio || 0) * 10}折`
  return `¥${Number(c.reducePrice || 0)}`
}

function couponThreshold(c) {
  const min = Number(c?.minPrice || 0)
  return min > 0 ? `满${min.toFixed(2)}元可用` : '无门槛'
}

function selectedCoupons() {
  return (props.item.couponIds || []).map(couponById).filter(Boolean)
}

function goodsName(id) {
  return props.goodsMeta[String(id)]?.name || (id ? `商品 #${id}` : '此处显示商品名称')
}

function goodsCover(id) {
  return props.goodsMeta[String(id)]?.image || ''
}

function previewGoodsIds() {
  const ids = props.item.goodsIds || []
  return ids.length ? ids.slice(0, props.item.limit || 6) : [null, null]
}

function seckillGoods() {
  const found = props.seckillActives.find((a) => String(a.id) === String(props.item.activeId))
  const n = Math.min(props.item.limit || 6, props.item.columns === 2 ? 4 : 6)
  return Array.from({ length: found ? Math.min(n, 3) : 3 })
}

function groupRows() {
  const list = props.item.source === 'manual' && props.item.activeIds?.length
    ? props.groupActives.filter((a) => props.item.activeIds.some((id) => String(id) === String(a.id)))
    : props.groupActives
  const sliced = list.slice(0, props.item.limit || 6)
  return sliced.length ? sliced : [{}, {}]
}

function bargainRows() {
  const list = props.item.source === 'manual' && props.item.activeIds?.length
    ? props.bargainActives.filter((a) => props.item.activeIds.some((id) => String(id) === String(a.id)))
    : props.bargainActives
  const sliced = list.slice(0, props.item.limit || 6)
  return sliced.length ? sliced : [{}, {}]
}

function stores() {
  const ids = props.item.storeIds || []
  const list = ids.length
    ? props.storeList.filter((s) => ids.some((id) => String(id) === String(s.id)))
    : props.storeList
  return list.length ? list : [{}, {}]
}

function articleItems() {
  const items = props.item.items || []
  return items.length ? items : [{ title: '此处显示文章标题', views: 309 }, { title: '此处显示文章标题', views: 128 }]
}

function newsItems() {
  const items = props.item.items || []
  return items.length ? items : [{ title: '点击右侧填写头条内容' }]
}

function windowImages() {
  const images = props.item.images || []
  return [0, 1, 2].map((i) => images[i] || {})
}

function imageGroupList() {
  const images = (props.item.images || []).filter((im) => im.url)
  return images.length ? images : [{}]
}
</script>

<template>
  <div class="pv" :style="wrapBg()">
    <div v-if="item.type === 'search'" class="pv-search">
      <span>🔍</span>
      <span>{{ item.placeholder || '搜索商品' }}</span>
    </div>

    <div v-else-if="item.type === 'banner'" class="pv-banner-wrap" @click.stop="emit('pick-image')">
      <div class="pv-banner">
        <img v-if="item.images?.[0]?.url" :src="item.images[0].url" alt="" />
        <div v-else class="pv-ph tall">点击选择轮播图</div>
        <span v-if="(item.images || []).length > 1" class="pv-badge">1/{{ item.images.length }}</span>
      </div>
    </div>

    <div v-else-if="item.type === 'imageGroup'" class="pv-img-stack">
      <div v-for="(im, i) in imageGroupList()" :key="i" class="pv-banner">
        <img v-if="im.url" :src="im.url" alt="" />
        <div v-else class="pv-ph tall">单图 {{ i + 1 }}</div>
      </div>
    </div>

    <div v-else-if="item.type === 'imageWindow'" class="pv-window">
      <div class="pv-window-main">
        <img v-if="windowImages()[0].url" :src="windowImages()[0].url" alt="" />
        <div v-else class="pv-ph">橱窗主图</div>
      </div>
      <div class="pv-window-side">
        <div v-for="i in [1, 2]" :key="i">
          <img v-if="windowImages()[i].url" :src="windowImages()[i].url" alt="" />
          <div v-else class="pv-ph">图{{ i + 1 }}</div>
        </div>
      </div>
    </div>

    <div v-else-if="item.type === 'video'" class="pv-video" :style="{ height: (item.height || 190) + 'px' }">
      <img v-if="item.cover" :src="item.cover" alt="" />
      <div v-else class="pv-ph">视频封面</div>
      <span class="pv-play">▶</span>
    </div>

    <div v-else-if="item.type === 'article'" class="pv-article">
      <div v-for="(art, i) in articleItems()" :key="i" class="pv-article-row">
        <div class="pv-article-txt">
          <div class="pv-article-title">{{ art.title || '此处显示文章标题' }}</div>
          <div class="pv-muted">{{ art.views || 0 }}次浏览</div>
        </div>
        <div class="pv-article-cover">
          <img v-if="art.cover" :src="art.cover" alt="" />
          <div v-else class="pv-ph sm">🖼</div>
        </div>
      </div>
    </div>

    <div v-else-if="item.type === 'news'" class="pv-news">
      <span class="pv-news-tag">头条</span>
      <span class="pv-news-label">{{ item.label || '快报' }}</span>
      <span class="pv-news-title">{{ newsItems()[0].title || '填写头条标题' }}</span>
      <span class="pv-muted">›</span>
    </div>

    <div v-else-if="item.type === 'notice'" class="pv-notice">
      <span>📢</span>
      <span class="pv-news-title">{{ item.text || '公告内容' }}</span>
    </div>

    <div v-else-if="item.type === 'navBar'" class="pv-nav">
      <div v-for="(nav, i) in (item.items?.length ? item.items : [{}])" :key="i" class="pv-nav-item">
        <div class="pv-nav-ico">
          <img v-if="isUrl(nav.icon)" :src="nav.icon" alt="" />
          <span v-else>{{ nav.icon || '◎' }}</span>
        </div>
        <div>{{ nav.text || '入口' }}</div>
      </div>
    </div>

    <div v-else-if="item.type === 'goods'" class="pv-goods" :class="'cols-' + (item.style === 'list' ? '1' : '2')">
      <div v-for="(id, i) in previewGoodsIds()" :key="i" class="pv-goods-card">
        <div class="pv-goods-img">
          <img v-if="id && goodsCover(id)" :src="goodsCover(id)" alt="" />
          <div v-else class="pv-ph">🖼</div>
        </div>
        <div class="pv-goods-name">{{ id ? goodsName(id) : '此处显示商品名称' }}</div>
        <div class="pv-price-row">
          <span class="pv-price">¥69.00</span>
          <span class="pv-line">¥139.00</span>
        </div>
      </div>
    </div>

    <div v-else-if="item.type === 'coupon'" class="pv-coupons">
      <div
        v-for="(c, i) in (selectedCoupons().length ? selectedCoupons() : [{}, {}])"
        :key="c.id || i"
        class="pv-ticket"
      >
        <div class="pv-ticket-main" :style="{ background: COUPON_TONES[i % COUPON_TONES.length] }">
          <div class="pv-ticket-face">{{ couponFace(c.id ? c : null) === '券' ? '¥ 10' : couponFace(c) }}</div>
          <div class="pv-ticket-sub">{{ c.id ? couponThreshold(c) : '满100.00元可用' }}</div>
        </div>
        <div class="pv-ticket-act">立即领取</div>
      </div>
    </div>

    <div v-else-if="item.type === 'seckill'" class="pv-seckill">
      <div class="pv-seckill-hd">
        <span class="pv-seckill-badge">⚡ 限时秒杀</span>
        <span class="pv-seckill-live">正在疯抢</span>
        <span class="pv-timer"><i>00</i>:<i>58</i>:<i>04</i></span>
        <span class="pv-more">更多 ›</span>
      </div>
      <div class="pv-seckill-grid" :class="'cols-' + (item.columns || 3)">
        <div v-for="(_, i) in seckillGoods()" :key="i" class="pv-seckill-card">
          <div class="pv-goods-img"><div class="pv-ph">🖼</div></div>
          <div v-if="item.showName !== false" class="pv-goods-name">此处是秒杀商品</div>
          <div class="pv-price-row">
            <span v-if="item.showPrice !== false" class="pv-price">69.00</span>
            <span v-if="item.showLinePrice !== false" class="pv-line">139.00</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="item.type === 'group'" class="pv-list">
      <div v-for="(row, i) in groupRows()" :key="row.id || i" class="pv-list-row">
        <div class="pv-list-img"><div class="pv-ph">🖼</div></div>
        <div class="pv-list-body">
          <div class="pv-list-title">{{ row.name || row.goodsName || '此处是拼团商品' }}</div>
          <div v-if="item.showSellingPoint !== false" class="pv-muted">{{ row.groupNum || 2 }}人团 · 正在拼团</div>
          <div class="pv-list-foot">
            <div>
              <span v-if="item.showPrice !== false" class="pv-price">¥{{ row.minGroupPrice || '69.00' }}</span>
              <span v-if="item.showLinePrice !== false" class="pv-line">¥139.00</span>
            </div>
            <span class="pv-cta red">去拼团</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="item.type === 'bargain'" class="pv-list">
      <div v-for="(row, i) in bargainRows()" :key="row.id || i" class="pv-list-row">
        <div class="pv-list-img"><div class="pv-ph">🖼</div></div>
        <div class="pv-list-body">
          <div class="pv-list-title">{{ row.name || row.goodsName || '此处是砍价商品' }}</div>
          <div class="pv-muted">2人正在砍价</div>
          <div class="pv-list-foot">
            <div>
              <div v-if="item.showLinePrice !== false" class="pv-line">¥ 139.00</div>
              <div v-if="item.showPrice !== false" class="pv-price">最低 ¥ {{ row.floorPrice || '0.01' }}</div>
            </div>
            <span class="pv-cta tan">立即参加</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="item.type === 'store'" class="pv-store">
      <div v-for="(s, i) in stores()" :key="s.id || i" class="pv-store-row">
        <div class="pv-store-logo">
          <img v-if="s.logo" :src="s.logo" alt="" />
          <div v-else class="pv-ph round">🏪</div>
        </div>
        <div>
          <div class="pv-store-name">{{ s.name || '此处显示门店名称' }}</div>
          <div class="pv-muted">门店地址：{{ s.address || s.region || 'xx省xx市xx区xx街道' }}</div>
          <div class="pv-muted">联系电话：{{ s.phone || '010-6666666' }}</div>
        </div>
      </div>
    </div>

    <div v-else-if="item.type === 'followMp'" class="pv-follow">
      <div class="pv-store-logo">
        <img v-if="item.logo" :src="item.logo" alt="" />
        <div v-else class="pv-ph round">✿</div>
      </div>
      <div>
        <div class="pv-list-title">{{ item.title || '关注公众号' }}</div>
        <div class="pv-muted">{{ item.desc || '获取更多优惠与新品' }}</div>
      </div>
      <span class="pv-cta red">关注</span>
    </div>

    <div v-else-if="item.type === 'richText'" class="pv-rich">
      <div v-if="item.html" v-html="item.html"></div>
      <div v-else class="pv-muted">暂无内容</div>
    </div>

    <div v-else-if="item.type === 'blank'" class="pv-blank" :style="{ height: (item.height || 20) + 'px' }">
      辅助空白 {{ item.height || 0 }}px
    </div>

    <div v-else-if="item.type === 'divider'" class="pv-divider">
      <i :style="{ borderTopStyle: item.style || 'solid', borderTopColor: item.color || '#e1e0d9' }" />
    </div>

    <div v-else-if="item.type === 'customerService'" class="pv-cs">🎧 在线客服</div>

    <div v-else class="pv-muted">{{ item.type }}</div>
  </div>
</template>

<style scoped>
.pv { font-size: 12px; color: #2b2a27; }
.pv img { width: 100%; height: 100%; object-fit: cover; display: block; }
.pv-muted { color: #9a9892; font-size: 11px; }
.pv-ph {
  width: 100%; height: 100%; min-height: 48px;
  display: flex; align-items: center; justify-content: center;
  background: #dceeff; color: #7aa4d4; font-size: 12px;
}
.pv-ph.tall { min-height: 110px; }
.pv-ph.sm { min-height: 52px; }
.pv-ph.round { border-radius: 50%; min-height: 0; }

.pv-search {
  display: flex; align-items: center; gap: 6px;
  height: 32px; padding: 0 12px; border-radius: 999px;
  background: #f3f2ee; color: #898781;
}
.pv-banner-wrap { padding: 8px; }
.pv-banner { position: relative; height: 110px; overflow: hidden; background: #eef4fc; border-radius: 8px; }
.pv-badge {
  position: absolute; top: 6px; right: 8px;
  background: rgba(0,0,0,.45); color: #fff; font-size: 10px; padding: 1px 6px; border-radius: 999px;
}
.pv-img-stack { display: grid; gap: 6px; }
.pv-window { display: grid; grid-template-columns: 1.4fr 1fr; gap: 4px; height: 140px; }
.pv-window-main, .pv-window-side > div { height: 100%; overflow: hidden; background: #eef4fc; }
.pv-window-side { display: grid; gap: 4px; }
.pv-window-side > div { height: 68px; }

.pv-video { position: relative; overflow: hidden; background: #111; }
.pv-play {
  position: absolute; left: 50%; top: 50%; transform: translate(-50%,-50%);
  width: 36px; height: 36px; border-radius: 50%; background: rgba(255,255,255,.9);
  display: flex; align-items: center; justify-content: center; color: #111;
}

.pv-article-row {
  display: flex; gap: 10px; padding: 10px 0;
  border-bottom: 1px solid #f0efec;
}
.pv-article-row:last-child { border-bottom: none; }
.pv-article-txt { flex: 1; min-width: 0; }
.pv-article-title { font-weight: 650; line-height: 1.4; margin-bottom: 6px; }
.pv-article-cover { width: 72px; height: 52px; flex-shrink: 0; overflow: hidden; background: #dceeff; }

.pv-news, .pv-notice {
  display: flex; align-items: center; gap: 6px; padding: 8px 0;
}
.pv-news-tag {
  background: #eb6834; color: #fff; border-radius: 4px; padding: 1px 6px; font-size: 10px; flex-shrink: 0;
}
.pv-news-label { font-weight: 650; flex-shrink: 0; }
.pv-news-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.pv-nav { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; padding: 8px 0; }
.pv-nav-item { text-align: center; font-size: 10.5px; color: #4a4844; }
.pv-nav-ico {
  width: 34px; height: 34px; margin: 0 auto 4px; border-radius: 8px;
  background: #eef4fc; display: flex; align-items: center; justify-content: center; overflow: hidden;
}

.pv-goods { display: grid; gap: 8px; }
.pv-goods.cols-2 { grid-template-columns: 1fr 1fr; }
.pv-goods.cols-1 { grid-template-columns: 1fr; }
.pv-goods-card { background: #fff; }
.pv-goods-img { height: 86px; background: #dceeff; overflow: hidden; }
.pv-goods-name { padding: 6px 4px 0; line-height: 1.35; }
.pv-price-row { padding: 4px; display: flex; align-items: baseline; gap: 6px; }
.pv-price { color: #d4380d; font-weight: 700; }
.pv-line { color: #b5b3ad; text-decoration: line-through; font-size: 11px; }

.pv-coupons { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.pv-ticket { display: flex; border-radius: 8px; overflow: hidden; min-height: 64px; }
.pv-ticket-main {
  flex: 1; color: #fff; padding: 10px 8px 10px 14px; position: relative;
}
.pv-ticket-main::before {
  content: ''; position: absolute; left: -8px; top: 50%; width: 16px; height: 16px;
  border-radius: 50%; background: var(--canvas-bg, #f5f4f1); transform: translateY(-50%);
}
.pv-ticket-face { font-size: 18px; font-weight: 800; }
.pv-ticket-sub { font-size: 10px; opacity: .9; margin-top: 2px; }
.pv-ticket-act {
  width: 28px; background: #2b2b2b; color: #fff; writing-mode: vertical-rl;
  display: flex; align-items: center; justify-content: center; letter-spacing: 2px; font-size: 11px;
}

.pv-seckill-hd { display: flex; align-items: center; gap: 6px; padding: 6px 0 10px; }
.pv-seckill-badge {
  background: #eb6834; color: #fff; border-radius: 999px; padding: 2px 8px; font-size: 11px; font-weight: 700;
}
.pv-seckill-live { font-size: 11px; }
.pv-timer { display: flex; align-items: center; gap: 2px; font-size: 10px; }
.pv-timer i {
  display: inline-block; min-width: 16px; padding: 1px 3px; border-radius: 3px;
  background: #111; color: #fff; font-style: normal; text-align: center;
}
.pv-more { margin-left: auto; color: #898781; font-size: 11px; }
.pv-seckill-grid { display: grid; gap: 8px; }
.pv-seckill-grid.cols-3 { grid-template-columns: repeat(3, 1fr); }
.pv-seckill-grid.cols-2 { grid-template-columns: repeat(2, 1fr); }

.pv-list-row { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f0efec; }
.pv-list-row:last-child { border-bottom: none; }
.pv-list-img { width: 72px; height: 72px; flex-shrink: 0; background: #dceeff; overflow: hidden; }
.pv-list-body { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.pv-list-title { font-weight: 650; line-height: 1.35; }
.pv-list-foot { margin-top: auto; display: flex; justify-content: space-between; align-items: flex-end; }
.pv-cta {
  border-radius: 999px; color: #fff; font-size: 11px; padding: 4px 10px; flex-shrink: 0;
}
.pv-cta.red { background: #e34948; }
.pv-cta.tan { background: #c4a574; }

.pv-store-row { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid #f0efec; }
.pv-store-row:last-child { border-bottom: none; }
.pv-store-logo { width: 44px; height: 44px; border-radius: 50%; overflow: hidden; flex-shrink: 0; background: #dceeff; }
.pv-store-name { color: #d4380d; font-weight: 650; margin-bottom: 4px; }

.pv-follow { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
.pv-rich { font-size: 12px; color: #4a4844; }
.pv-blank { display: flex; align-items: center; justify-content: center; color: #b5b3ad; font-size: 11px; }
.pv-divider { padding: 8px 0; }
.pv-divider i { display: block; border-top-width: 1px; }
.pv-cs { text-align: center; padding: 10px; color: #2a78d6; }
</style>
