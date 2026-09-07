<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStoreDashboard, getStoreDaily } from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'

/**
 * 商户首页看板。对应原型 docs/prototype/store/dashboard.html。
 * 交易数字来自 /store/dashboard/overview，按已支付订单汇总，不使用模拟数据。
 */
const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const data = ref({
  todayGmv: 0,
  todayOrderCount: 0,
  unpaidCount: 0,
  pendingShipCount: 0,
  afterSaleOpenCount: 0,
  goodsTotal: 0,
  goodsOnSale: 0,
  last7Days: [],
})
const yesterday = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    data.value = (await getStoreDashboard()) || data.value
    yesterday.value = await getStoreDaily().catch(() => null)
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

const maxGmv = computed(() => Math.max(1, ...((data.value.last7Days || []).map((d) => Number(d.gmv) || 0))))

function money(v) {
  return Number(v || 0).toFixed(2)
}

function go(name) {
  router.push({ name })
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">首页看板</div>
      <div class="page-desc">
        欢迎回来，{{ auth.displayName }}
        <span v-if="yesterday" style="margin-left: 12px; color: var(--text-muted)">
          昨日成交 ¥{{ money(yesterday.payAmount) }} / {{ yesterday.payCount || 0 }} 单
        </span>
      </div>
    </div>
  </div>

  <a-spin :spinning="loading">
    <div class="grid grid-4" style="margin-bottom: 16px">
      <div class="card kpi">
        <div class="kpi-label">今日成交额</div>
        <div class="kpi-value">¥{{ money(data.todayGmv) }}</div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">今日已支付订单</div>
        <div class="kpi-value">{{ data.todayOrderCount }}</div>
      </div>
      <div class="card kpi" style="cursor: pointer" @click="go('orders')">
        <div class="kpi-label">待发货</div>
        <div class="kpi-value">{{ data.pendingShipCount }}</div>
        <div class="kpi-foot"><span class="tag tag-warning tag-dot">需处理</span></div>
      </div>
      <div class="card kpi" style="cursor: pointer" @click="go('after-sales')">
        <div class="kpi-label">进行中售后</div>
        <div class="kpi-value">{{ data.afterSaleOpenCount }}</div>
      </div>
    </div>

    <div class="grid grid-4" style="margin-bottom: 16px">
      <div class="card kpi">
        <div class="kpi-label">商品总数</div>
        <div class="kpi-value">{{ data.goodsTotal }}</div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">出售中</div>
        <div class="kpi-value">{{ data.goodsOnSale }}</div>
        <div class="kpi-foot"><span class="tag tag-good tag-dot">在售</span></div>
      </div>
      <div class="card kpi" style="cursor: pointer" @click="go('orders')">
        <div class="kpi-label">待付款订单</div>
        <div class="kpi-value">{{ data.unpaidCount }}</div>
      </div>
      <div class="card kpi">
        <div class="kpi-label">近 7 日成交额</div>
        <div class="kpi-value">¥{{ money((data.last7Days || []).reduce((s, d) => s + Number(d.gmv || 0), 0)) }}</div>
      </div>
    </div>

    <div class="card card-pad">
      <p class="card-title">近 7 日成交额</p>
      <p class="card-sub">按支付成功时间汇总 payPrice，单位元</p>
      <div v-if="!(data.last7Days || []).length" style="color: var(--text-muted); font-size: 13px">暂无数据</div>
      <div v-for="d in data.last7Days" :key="d.date" class="bar-row">
        <div class="bar-label">{{ d.date.slice(5) }}</div>
        <div class="bar-track">
          <div class="bar-fill" :style="{ width: `${(Number(d.gmv) / maxGmv) * 100}%` }"></div>
        </div>
        <div class="bar-value">¥{{ money(d.gmv) }} / {{ d.orderCount }}</div>
      </div>
    </div>
  </a-spin>
</template>

<style scoped>
.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.bar-row:last-child { margin-bottom: 0; }
.bar-label {
  width: 56px;
  font-size: 12.5px;
  color: var(--text-secondary);
  flex-shrink: 0;
}
.bar-track { flex: 1; height: 20px; }
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
  width: 120px;
  text-align: right;
  flex-shrink: 0;
}
</style>
