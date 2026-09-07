<script setup>
import { onShow } from '@dcloudio/uni-app'
import { computed, onUnmounted, ref } from 'vue'
import { getSeckillSessions, listSeckillActives, getSeckillActive } from '@/api/index'

/**
 * 秒杀专场。对应原型 docs/prototype/h5/seckill.html。
 * 秒杀（time_ids 非空）：场次制 + 限量 + 限购，Redis 预扣在下单时拦截；
 * 限时折扣（time_ids 空）：全天仅换价，走普通库存。
 * "立即抢购"跳结算页，透传 activityType=seckill / activityId / goodsId / skuId。
 */
const sessions = ref([])
const actives = ref([]) // [{ ...active, goods: [] }]
const loading = ref(true)
const now = ref(Date.now())
let timer = null

onShow(() => load())

onUnmounted(() => { if (timer) clearInterval(timer) })

async function load() {
  loading.value = true
  try {
    const [s, acts] = await Promise.all([getSeckillSessions(), listSeckillActives()])
    sessions.value = s || []
    const activeList = acts || []
    const enriched = await Promise.all(activeList.map((a) => getSeckillActive(a.id).catch(() => null)))
    actives.value = activeList.map((a, i) => ({ ...a, goods: (enriched[i]?.goods) || [] }))
    startClock()
  } finally {
    loading.value = false
  }
}

function startClock() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => { now.value = Date.now() }, 1000)
}

const ongoingSession = computed(() => sessions.value.find((s) => s.status === 'ongoing'))

function countdownEnd(session) {
  if (!session?.endTime) return null
  const [h, m, sec] = session.endTime.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m, sec || 0, 0)
  return d.getTime()
}

const remainingMs = computed(() => {
  const end = countdownEnd(ongoingSession.value)
  if (!end) return null
  return Math.max(0, end - now.value)
})

function fmtMs(ms) {
  if (ms == null) return '--:--:--'
  const s = Math.floor(ms / 1000)
  const h = String(Math.floor(s / 3600)).padStart(2, '0')
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
  const sec = String(s % 60).padStart(2, '0')
  return `${h}:${m}:${sec}`
}

function sessionStatusText(s) {
  return { upcoming: '即将开始', ongoing: '抢购中', ended: '已结束' }[s] || s
}
function sessionStatusClass(s) {
  return { upcoming: 'badge-upcoming', ongoing: 'badge-ongoing', ended: 'badge-ended' }[s] || ''
}

function activeSlotStatus(a) {
  // 秒杀：取其场次的当前状态；限时折扣：始终 ongoing
  if (!a.isSeckill) return 'ongoing'
  const times = a.times || []
  if (!times.length) return 'ongoing'
  if (times.some((t) => t.status === 'ongoing')) return 'ongoing'
  if (times.some((t) => t.status === 'upcoming')) return 'upcoming'
  return 'ended'
}

function canBuy(a, g) {
  if (activeSlotStatus(a) === 'ended') return false
  if (a.isSeckill && g.seckillNum > 0 && (g.remaining ?? 0) <= 0) return false
  return true
}

function buyBtnText(a, g) {
  if (activeSlotStatus(a) === 'upcoming') return '即将开始'
  if (activeSlotStatus(a) === 'ended') return '已结束'
  if (a.isSeckill && g.seckillNum > 0 && (g.remaining ?? 0) <= 0) return '已抢光'
  return a.isSeckill ? '立即抢购' : '立即购买'
}

function buy(a, g) {
  if (!canBuy(a, g)) return
  uni.navigateTo({
    url: `/pages/order/checkout?skuId=${g.skuId}&goodsId=${g.goodsId}&quantity=1&activityType=seckill&activityId=${a.id}`,
  })
}

function progress(g) {
  if (!g.seckillNum) return 0
  return Math.min(100, Math.round(((g.sold || 0) / g.seckillNum) * 100))
}
function fmtPrice(v) { return Number(v ?? 0).toFixed(2) }
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中…</view>
    <view v-else>
      <!-- Hero -->
      <view class="hero">
        <view class="hero-title">秒杀专场</view>
        <view class="hero-sub">
          <view v-if="ongoingSession" class="hero-sub-row">
            <text class="hero-session">{{ ongoingSession.name }} 抢购中</text>
            <text class="hero-cd">距结束 {{ fmtMs(remainingMs) }}</text>
          </view>
          <view v-else-if="sessions.length" class="hero-sub-row">
            <text class="hero-session">今日场次</text>
            <text class="hero-cd">敬请期待</text>
          </view>
          <text v-else class="hero-session">限时折扣</text>
        </view>
      </view>

      <!-- 场次标签 -->
      <scroll-view v-if="sessions.length" scroll-x class="sessions">
        <view
          v-for="s in sessions"
          :key="s.id"
          class="session-chip"
          :class="sessionStatusClass(s.status)"
        >
          <text class="session-name">{{ s.name }}</text>
          <text class="session-status">{{ sessionStatusText(s.status) }}</text>
        </view>
      </scroll-view>

      <!-- 活动区 -->
      <view v-if="!actives.length" class="empty">暂无进行中的秒杀/限时折扣活动</view>
      <view v-for="a in actives" :key="a.id" class="active">
        <view class="active-hd">
          <text class="active-name">{{ a.name }}</text>
          <text class="active-tag" :class="a.isSeckill ? 'tag-seckill' : 'tag-discount'">
            {{ a.isSeckill ? '秒杀' : '限时折扣' }}
          </text>
          <text class="active-date">{{ a.startDate }} ~ {{ a.endDate }}</text>
        </view>

        <view v-for="g in a.goods" :key="g.id" class="goods">
          <image v-if="g.image" class="goods-img" :src="g.image" mode="aspectFill" />
          <view v-else class="goods-img goods-img-ph">无图</view>
          <view class="goods-info">
            <view class="goods-name">{{ g.goodsName || ('商品' + g.goodsId) }}</view>
            <view class="price-row">
              <text class="seckill-price">¥{{ fmtPrice(g.seckillPrice) }}</text>
              <text v-if="g.originalPrice" class="origin-price">¥{{ fmtPrice(g.originalPrice) }}</text>
            </view>
            <view v-if="a.isSeckill && g.seckillNum > 0" class="progress">
              <view class="progress-track"><view class="progress-fill" :style="{ width: progress(g) + '%' }" /></view>
              <text class="progress-text">已抢 {{ g.sold || 0 }}/{{ g.seckillNum }} · 剩余 {{ g.remaining ?? 0 }}</text>
            </view>
            <view v-if="a.isSeckill && g.limitPerUser > 0" class="limit-tip">每人限购 {{ g.limitPerUser }} 件</view>
            <button class="buy-btn" :disabled="!canBuy(a, g)" @click="buy(a, g)">
              {{ buyBtnText(a, g) }}
            </button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { min-height: 100vh; background: #f3f1ec; padding-bottom: 40rpx; }
.empty { text-align: center; color: #898781; padding: 120rpx 0; font-size: 26rpx; }

.hero {
  background: linear-gradient(135deg, #e34948, #ff6b35);
  color: #fff; padding: 36rpx 28rpx 30rpx;
}
.hero-title { font-size: 40rpx; font-weight: 800; letter-spacing: 2rpx; }
.hero-sub { margin-top: 14rpx; display: flex; align-items: baseline; gap: 18rpx; font-size: 26rpx; }
.hero-sub-row { display: flex; align-items: baseline; gap: 18rpx; }
.hero-cd { font-size: 30rpx; font-weight: 700; font-variant-numeric: tabular-nums; }

.sessions { white-space: nowrap; padding: 20rpx 24rpx; }
.session-chip {
  display: inline-flex; flex-direction: column; align-items: center;
  background: #fff; border-radius: 14rpx; padding: 14rpx 26rpx; margin-right: 16rpx;
  min-width: 150rpx;
}
.session-name { font-size: 26rpx; font-weight: 600; color: #2a2a2a; }
.session-status { font-size: 20rpx; margin-top: 6rpx; }
.badge-upcoming { border: 2rpx solid #2a78d6; }
.badge-upcoming .session-status { color: #2a78d6; }
.badge-ongoing { border: 2rpx solid #e34948; background: #fff5f0; }
.badge-ongoing .session-status { color: #e34948; font-weight: 600; }
.badge-ended .session-status { color: #b5b3ad; }

.active { background: #fff; margin: 0 24rpx 24rpx; border-radius: 18rpx; padding: 24rpx; }
.active-hd { display: flex; align-items: center; gap: 14rpx; margin-bottom: 20rpx; }
.active-name { font-size: 30rpx; font-weight: 700; }
.active-tag { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx; }
.tag-seckill { background: #e34948; color: #fff; }
.tag-discount { background: #ff8a3d; color: #fff; }
.active-date { margin-left: auto; font-size: 22rpx; color: #898781; }

.goods { display: flex; gap: 18rpx; padding: 18rpx 0; border-top: 2rpx solid #f5f4f1; }
.goods:first-of-type { border-top: 0; }
.goods-img { width: 160rpx; height: 160rpx; border-radius: 12rpx; background: #f0efec; flex-shrink: 0; }
.goods-img-ph { display: flex; align-items: center; justify-content: center; color: #b5b3ad; font-size: 22rpx; }
.goods-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.goods-name { font-size: 27rpx; line-height: 1.4; }
.price-row { margin-top: 10rpx; display: flex; align-items: baseline; gap: 12rpx; }
.seckill-price { color: #e34948; font-size: 34rpx; font-weight: 800; }
.origin-price { color: #b5b3ad; font-size: 22rpx; text-decoration: line-through; }
.progress { margin-top: 12rpx; }
.progress-track { height: 16rpx; border-radius: 8rpx; background: #f0efec; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #ff6b35, #e34948); }
.progress-text { font-size: 20rpx; color: #898781; margin-top: 6rpx; }
.limit-tip { font-size: 20rpx; color: #2a78d6; margin-top: 6rpx; }
.buy-btn {
  margin-top: auto; align-self: flex-end; background: #e34948; color: #fff;
  border: 0; border-radius: 30rpx; font-size: 25rpx; padding: 0 32rpx; height: 60rpx; line-height: 60rpx;
}
.buy-btn[disabled] { background: #c9c7c1; color: #fff; }
</style>
