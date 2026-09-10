<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageShops } from '@/api/shop'

const router = useRouter()

/**
 * 商城管理列表。对应原型 docs/prototype/admin/shop-list.html。
 *
 * 与原型的差异（有意为之）：
 * - 「商品数 / 累计GMV」两列原型里是静态假数据，后端 Shop 实体没有这两个字段，
 *   聚合统计按排期属于后续 Sprint（运营看板），这里先不显示假数字。
 */
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', pageNum: 1, pageSize: 20 })

const STATUS_META = {
  trial: { text: '试用中', cls: 'tag-good' },
  normal: { text: '正常', cls: 'tag-good' },
  expired: { text: '已过期', cls: 'tag-critical' },
  disabled: { text: '已停用', cls: 'tag-critical' },
  archived: { text: '已归档', cls: 'tag-muted' },
}

async function load() {
  loading.value = true
  try {
    const page = await pageShops({
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    rows.value = page.records || []
    total.value = page.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onSearch() {
  query.pageNum = 1
  load()
}

function onPageChange(page) {
  query.pageNum = page
  load()
}

const stats = computed(() => {
  const list = rows.value
  const now = Date.now()
  const sevenDays = 7 * 24 * 3600 * 1000
  return {
    total: total.value,
    running: list.filter((s) => s.status === 'normal' || s.status === 'trial').length,
    expiringSoon: list.filter((s) => {
      if (!s.expireTime) return false
      const t = new Date(s.expireTime).getTime() - now
      return t > 0 && t <= sevenDays
    }).length,
    stopped: list.filter((s) => s.status === 'expired' || s.status === 'disabled').length,
  }
})

function statusOf(row) {
  return STATUS_META[row.status] || { text: row.status || '未知', cls: 'tag-muted' }
}

function fmtDate(v) {
  if (!v) return '—'
  // 后端返回 LocalDateTime 的 ISO 串（2026-08-04T11:19:39），只取日期部分
  return String(v).slice(0, 10)
}

function maskMobile(m) {
  if (!m || m.length < 7) return m || '—'
  return `${m.slice(0, 3)}****${m.slice(-4)}`
}

function previewH5(shop) {
  const h5Url = 'http://localhost:5175'
  const previewUrl = `${h5Url}/?_shopId=${shop.id}`
  const preview = window.open(previewUrl, '_blank')
  if (preview) {
    preview.addEventListener('load', () => {
      preview.postMessage({ type: 'setShopId', shopId: shop.id }, h5Url)
    })
  }
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">商城管理</div>
      <div class="page-desc">管理平台下全部租户商城 · 当前共 {{ total }} 家商城</div>
    </div>
    <button class="btn btn-primary" @click="router.push({ name: 'shop-new' })">＋ 新建商城</button>
  </div>

  <div class="grid grid-4" style="margin-bottom: 18px">
    <div class="card kpi">
      <div class="kpi-label">商城总数</div>
      <div class="kpi-value">{{ stats.total }}</div>
    </div>
    <div class="card kpi">
      <div class="kpi-label">正常运营（本页）</div>
      <div class="kpi-value">{{ stats.running }}</div>
      <div class="kpi-foot"><span class="tag tag-good tag-dot">健康</span></div>
    </div>
    <div class="card kpi">
      <div class="kpi-label">7日内到期（本页）</div>
      <div class="kpi-value">{{ stats.expiringSoon }}</div>
      <div class="kpi-foot"><span class="tag tag-warning tag-dot">需跟进续费</span></div>
    </div>
    <div class="card kpi">
      <div class="kpi-label">已过期 / 已停用（本页）</div>
      <div class="kpi-value">{{ stats.stopped }}</div>
      <div class="kpi-foot"><span class="tag tag-critical tag-dot">待处理</span></div>
    </div>
  </div>

  <div class="card card-pad" style="margin-bottom: 14px">
    <div style="display: flex; gap: 12px; align-items: center">
      <input
        v-model="query.keyword"
        class="form-input"
        style="width: 240px"
        placeholder="搜索商城名称 / 域名前缀 / 手机号"
        @keyup.enter="onSearch"
      />
      <select v-model="query.status" class="form-select" style="width: 140px">
        <option value="">全部状态</option>
        <option value="trial">试用中</option>
        <option value="normal">正常</option>
        <option value="expired">已过期</option>
        <option value="disabled">已停用</option>
      </select>
      <button class="btn btn-primary" @click="onSearch">查询</button>
    </div>
  </div>

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead>
          <tr>
            <th>商城</th>
            <th>联系人</th>
            <th>状态</th>
            <th>套餐</th>
            <th>到期时间</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>
              <div style="font-weight: 600">{{ row.name }}</div>
              <div style="font-size: 12px; color: var(--text-muted)">
                {{ row.code }} · {{ row.industry || '未设置行业' }}
              </div>
            </td>
            <td>{{ row.contact || '—' }} {{ maskMobile(row.mobile) }}</td>
            <td><span class="tag tag-dot" :class="statusOf(row).cls">{{ statusOf(row).text }}</span></td>
            <td>{{ row.packageName || '—' }}</td>
            <td class="num">{{ fmtDate(row.expireTime) }}</td>
            <td class="num">{{ fmtDate(row.createTime) }}</td>
            <td>
              <button class="btn btn-sm" @click="router.push({ name: 'shop-detail', params: { id: row.id } })">查看 / 编辑</button>
              <button class="btn btn-sm" style="margin-left: 8px" @click="previewH5(row)">预览 H5</button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="7" style="text-align: center; color: var(--text-muted); padding: 40px 0">
              暂无商城数据
            </td>
          </tr>
        </tbody>
      </table>
    </a-spin>

    <div v-if="total > query.pageSize" style="padding: 14px; display: flex; justify-content: flex-end">
      <a-pagination
        :current="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        :show-size-changer="false"
        @change="onPageChange"
      />
    </div>
  </div>
</template>
