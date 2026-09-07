<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

// 后台外壳：对应原型 docs/prototype/admin/dashboard.html 的 .app-shell 结构
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 菜单直接由路由表推导，不再单独维护一份菜单配置——两份必然会不同步
const menuGroups = computed(() => {
  const layoutRoute = router.getRoutes().find((r) => r.path === '/' && r.children?.length)
  const items = router
    .getRoutes()
    .filter((r) => r.meta?.title && r.meta?.group && r.name)
    .map((r) => ({
      name: r.name,
      path: r.path,
      title: r.meta.title,
      icon: r.meta.icon,
      group: r.meta.group,
      pending: !!r.meta.pending,
    }))

  // 保持原型里的分组顺序：运营 → 系统
  const order = ['运营', '系统']
  return order
    .map((group) => ({ group, items: items.filter((i) => i.group === group) }))
    .filter((g) => g.items.length > 0)
})

const currentTitle = computed(() => route.meta?.title || '')

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
        <div class="logo">火</div>
        <div class="name">多开云商城</div>
        <div class="badge">超管</div>
      </div>
      <nav class="sidebar-nav">
        <template v-for="g in menuGroups" :key="g.group">
          <div class="nav-group-title">{{ g.group }}</div>
          <a
            v-for="item in g.items"
            :key="item.name"
            class="nav-item"
            :class="{ active: route.name === item.name, disabled: item.pending }"
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

      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>
