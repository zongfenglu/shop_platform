<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'

// 商户后台外壳：对应原型 docs/prototype/store/dashboard.html
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuGroups = computed(() => {
  const items = router
    .getRoutes()
    .filter((r) => r.meta?.title && r.meta?.group && r.name)
    .map((r) => ({
      name: r.name,
      title: r.meta.title,
      icon: r.meta.icon,
      group: r.meta.group,
      pending: !!r.meta.pending,
    }))

  // 保持原型分组顺序
  const order = ['交易', '运营', '店铺', '账户']
  return order
    .map((group) => ({ group, items: items.filter((i) => i.group === group) }))
    .filter((g) => g.items.length > 0)
})

const currentTitle = computed(() => route.meta?.title || '')

function isActive(item) {
  if (item.name === 'goods') {
    return route.name === 'goods' || route.name === 'goods-new' || route.name === 'goods-edit'
  }
  return route.name === item.name
}

function go(item) {
  if (item.pending) return
  router.push({ name: item.name })
}

function onLogout() {
  Modal.confirm({
    title: '确认退出登录？',
    okText: '退出',
    cancelText: '取消',
    onOk() {
      auth.logout()
      router.replace({ name: 'login' })
    },
  })
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <!-- 品牌 Logo 刻意不随皮肤变化：它代表租户自身品牌（见 prototype.css 注释） -->
        <div class="logo" style="background: linear-gradient(135deg, #ff9143, #e34948)">店</div>
        <div class="name">{{ auth.shopCode || '商户后台' }}</div>
        <div class="badge">商户</div>
      </div>
      <nav class="sidebar-nav">
        <template v-for="g in menuGroups" :key="g.group">
          <div class="nav-group-title">{{ g.group }}</div>
          <a
            v-for="item in g.items"
            :key="item.name"
            class="nav-item"
            :class="{ active: isActive(item), disabled: item.pending }"
            :title="item.pending ? '该功能按排期在后续 Sprint 开放' : ''"
            @click="go(item)"
          >
            <span class="ico">{{ item.icon }}</span>{{ item.title }}
          </a>
        </template>
      </nav>
    </aside>

    <div class="main">
      <div class="topbar">
        <div class="breadcrumb"><b>{{ currentTitle }}</b></div>
        <div class="topbar-right">
          <ThemeSwitcher />
          <a-dropdown>
            <div class="topbar-user" style="cursor: pointer">
              <div class="avatar">{{ auth.avatarText }}</div>
              {{ auth.displayName }}
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="onLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>

      <div v-if="auth.platformImpersonation" class="impersonate-banner">
        平台代管理中 — 当前操作将记入审计日志（by_platform）
      </div>

      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.impersonate-banner {
  background: #fff7e6;
  color: #ad6800;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 16px;
  border-bottom: 1px solid #ffe7ba;
}
</style>
