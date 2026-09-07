<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { pageGoods, updateGoodsStatus } from '@/api/goods'

const router = useRouter()

/**
 * 商品列表。对应原型 docs/prototype/store/goods-list.html。
 *
 * 租户隔离完全由后端负责（TenantContext + MyBatis-Plus 租户拦截器），
 * 前端不传 shopId —— 传了也会被忽略，而且传了反而是安全隐患（诱导后端信任前端入参）。
 */
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', pageNum: 1, pageSize: 20 })

const STATUS_META = {
  on: { text: '出售中', cls: 'tag-good' },
  off: { text: '仓库中', cls: 'tag-muted' },
  deleted: { text: '回收站', cls: 'tag-critical' },
}

async function load() {
  loading.value = true
  try {
    const page = await pageGoods({
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

function onPageChange(p) {
  query.pageNum = p
  load()
}

function statusOf(row) {
  return STATUS_META[row.status] || { text: row.status || '未知', cls: 'tag-muted' }
}

function firstImage(row) {
  if (!row.images) return ''
  // images 字段是 JSON 数组字符串（V5 migration: `images JSON NOT NULL COMMENT '商品主图，第一张为封面'`），
  // 不是逗号分隔——之前当成逗号分隔字符串处理是错的，会把 ["url"] 整个当 src 塞进 <img>，图完全加载不出来。
  try {
    const arr = JSON.parse(row.images)
    return Array.isArray(arr) ? arr[0] || '' : ''
  } catch (e) {
    return ''
  }
}

function fmtPrice(v) {
  if (v === null || v === undefined) return '—'
  return `¥${Number(v).toFixed(2)}`
}

async function onToggleStatus(row) {
  const next = row.status === 'on' ? 'off' : 'on'
  try {
    await updateGoodsStatus(row.id, next)
    message.success(next === 'on' ? '已上架' : '已下架')
    await load()
  } catch (e) {
    // 已由拦截器提示
  }
}
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">商品管理</div>
      <div class="page-desc">共 {{ total }} 个商品</div>
    </div>
    <div style="display: flex; gap: 8px">
      <button class="btn" @click="router.push({ name: 'goods-categories' })">分类管理</button>
      <button class="btn btn-primary" @click="router.push({ name: 'goods-new' })">＋ 发布商品</button>
    </div>
  </div>

  <div class="card card-pad" style="margin-bottom: 14px">
    <div style="display: flex; gap: 12px; align-items: center">
      <input
        v-model="query.keyword"
        class="form-input"
        style="width: 240px"
        placeholder="搜索商品名称 / 商品编码"
        @keyup.enter="onSearch"
      />
      <select v-model="query.status" class="form-select" style="width: 140px">
        <option value="">全部状态</option>
        <option value="on">出售中</option>
        <option value="off">仓库中</option>
      </select>
      <button class="btn btn-primary" @click="onSearch">查询</button>
    </div>
  </div>

  <div class="card">
    <a-spin :spinning="loading">
      <table class="table">
        <thead>
          <tr>
            <th>商品</th>
            <th>规格</th>
            <th>状态</th>
            <th class="num">库存</th>
            <th class="num">销量</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>
              <div style="display: flex; align-items: center; gap: 10px">
                <img
                  v-if="firstImage(row)"
                  :src="firstImage(row)"
                  alt=""
                  style="width: 40px; height: 40px; border-radius: 6px; object-fit: cover; flex-shrink: 0"
                />
                <div>
                  <div style="font-weight: 600">{{ row.name }}</div>
                  <div style="font-size: 12px; color: var(--text-muted)">{{ row.code || '无编码' }}</div>
                </div>
              </div>
            </td>
            <td>{{ row.specType === 'multi' ? '多规格' : '单规格' }}</td>
            <td><span class="tag tag-dot" :class="statusOf(row).cls">{{ statusOf(row).text }}</span></td>
            <td class="num">{{ row.stockTotal ?? '—' }}</td>
            <td class="num">{{ (row.salesActual ?? 0) + (row.salesInitial ?? 0) }}</td>
            <td class="num">{{ String(row.createTime || '').slice(0, 10) || '—' }}</td>
            <td>
              <button v-if="row.status !== 'deleted'" class="btn btn-sm" @click="router.push({ name: 'goods-edit', params: { id: row.id } })">编辑</button>
              <button v-if="row.status !== 'deleted'" class="btn btn-sm" @click="onToggleStatus(row)">
                {{ row.status === 'on' ? '下架' : '上架' }}
              </button>
            </td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="7" style="text-align: center; color: var(--text-muted); padding: 40px 0">
              暂无商品，去「发布商品」创建第一个吧
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
