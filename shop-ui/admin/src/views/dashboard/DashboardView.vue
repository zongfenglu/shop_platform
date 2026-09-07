<script setup>
import { computed, onMounted, ref } from 'vue'
import { pageShops } from '@/api/shop'
import { getAdminDashboard } from '@/api/dashboard'

/**
 * 数据看板。对应原型 docs/prototype/admin/dashboard.html。
 * 商城分布来自商城列表；GMV / 近 7 日成交来自 /admin/dashboard/overview（已支付消费者订单）。
 */
const loading = ref(false)
const shops = ref([])
const total = ref(0)
const trade = ref({
  todayGmv: 0,
  todayOrderCount: 0,
  pendingShopOrders: 0,
  last7DaysGmv: 0,
  last7Days: [],
})

onMounted(async () => {
  loading.value = true
  try {
    const [page, overview] = await Promise.all([
      pageShops({ pageNum: 1, pageSize: 100 }),
      getAdminDashboard().catch(() => null),
    ])
    shops.value = page.records || []
    total.value = page.total || 0
    if (overview) trade.value = overview
  } catch (e) {
    shops.value = []
  } finally {
    loading.value = false
  }
})

const byStatus = computed(() => {
  const map = { trial: 0, normal: 0, expired: 0, disabled: 0, archived: 0 }
  for (const s of shops.value) {
    if (s.status in map) map[s.status] += 1
  }
  return map
})

const industryTop = computed(() => {
  const counter = new Map()
  for (const s of shops.value) {
    const key = s.industry || '未设置'
    counter.set(key, (counter.get(key) || 0) + 1)
  }
  return [...counter.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6)
})

const maxIndustry = computed(() => Math.max(1, ...industryTop.value.map(([, n]) => n)))
const maxGmv = computed(() => Math.max(1, ...((trade.value.last7Days || []).map((d) => Number(d.gmv) || 0))))

function money(v) {
  return Number(v || 0).toFixed(2)
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">数据看板</div>
      <div class="page-desc">全平台 {{ total }} 家商城运营概况</div>
    </div>
  </div>

  <a-spin :spinning="loading">
    <div class="grid grid-4" style="margin-bottom: 16px">
      <div class="card kpi">
        <div class="kpi-label">商城总数</div>
        <div class="kpi-value">{{ total }}</div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">试用中</div>
        <div class="kpi-value">{{ byStatus.trial }}</div>
        <div class="kpi-foot"><span class="tag tag-primary">待转化</span></div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">正常运营</div>
        <div class="kpi-value">{{ byStatus.normal }}</div>
        <div class="kpi-foot"><span class="tag tag-good tag-dot">健康</span></div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">已过期 / 已停用</div>
        <div class="kpi-value">
          {{ byStatus.expired }}
          <span style="font-size: 14px; color: var(--text-muted); font-weight: 500">/ {{ byStatus.disabled }}</span>
        </div>
        <div class="kpi-foot"><span class="tag tag-critical tag-dot">待处理</span></div>
      </div>
    </div>

    <div class="grid grid-4" style="margin-bottom: 16px">
      <div class="card kpi">
        <div class="kpi-label">今日平台 GMV</div>
        <div class="kpi-value">¥{{ money(trade.todayGmv) }}</div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">今日已支付订单</div>
        <div class="kpi-value">{{ trade.todayOrderCount }}</div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">待确认套餐订单</div>
        <div class="kpi-value">{{ trade.pendingShopOrders }}</div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">近 7 日 GMV</div>
        <div class="kpi-value">¥{{ money(trade.last7DaysGmv) }}</div>
      </div>
    </div>

    <div class="grid grid-2">
      <div class="card card-pad">
        <p class="card-title">行业分布</p>
        <p class="card-sub">按已开通商城的行业归类（Top 6）</p>
        <div v-if="industryTop.length === 0" style="color: var(--text-muted); font-size: 13px">暂无数据</div>
        <div v-for="[name, count] in industryTop" :key="name" class="bar-row">
          <div class="bar-label">{{ name }}</div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: `${(count / maxIndustry) * 100}%` }"></div>
          </div>
          <div class="bar-value">{{ count }}</div>
        </div>
      </div>

      <div class="card card-pad">
        <p class="card-title">近 7 日平台成交额</p>
        <p class="card-sub">已支付消费者订单 payPrice 汇总</p>
        <div v-if="!(trade.last7Days || []).length" style="color: var(--text-muted); font-size: 13px">暂无成交</div>
        <div v-for="d in trade.last7Days" :key="d.date" class="bar-row">
          <div class="bar-label">{{ d.date.slice(5) }}</div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: `${(Number(d.gmv) / maxGmv) * 100}%` }"></div>
          </div>
          <div class="bar-value">¥{{ money(d.gmv) }}</div>
        </div>
      </div>
    </div>
  </a-spin>
</template>

<style scoped>
/* 与原型 dashboard.html 内联的条形图样式一致 */
.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.bar-row:last-child {
  margin-bottom: 0;
}
.bar-label {
  width: 120px;
  font-size: 12.5px;
  color: var(--text-secondary);
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bar-track {
  flex: 1;
  position: relative;
  height: 20px;
}
.bar-fill {
  height: 20px;
  border-radius: 0 4px 4px 0;
  background: var(--series-1);
  min-width: 2px;
}
.bar-value {
  font-size: 12px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
  width: 96px;
  text-align: right;
  flex-shrink: 0;
}
</style>
