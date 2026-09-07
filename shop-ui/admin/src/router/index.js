import { createRouter, createWebHistory } from 'vue-router'
import { getToken, setUnauthorizedHandler } from '@/api/http'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '数据看板', group: '运营', icon: '◨' } },
      { path: 'shops', name: 'shops', component: () => import('@/views/shop/ShopListView.vue'), meta: { title: '商城管理', group: '运营', icon: '▤' } },
      { path: 'shops/new', name: 'shop-new', component: () => import('@/views/shop/ShopWizardView.vue'), meta: { title: '新建商城' } },
      { path: 'shops/:id', name: 'shop-detail', component: () => import('@/views/shop/ShopDetailView.vue'), meta: { title: '商城详情' } },
      { path: 'packages', name: 'packages', component: () => import('@/views/package/PackageListView.vue'), meta: { title: '套餐管理', group: '运营', icon: '◩' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '订购管理', group: '运营', icon: '￥' } },
      { path: 'domains', name: 'domains', component: () => import('@/views/domain/DomainListView.vue'), meta: { title: '域名管理', group: '运营', icon: '⌘' } },
      { path: 'clients', name: 'clients', component: () => import('@/views/client/ClientManageView.vue'), meta: { title: '客户端管理', group: '运营', icon: '▣' } },
      { path: 'accounts', name: 'accounts', component: () => import('@/views/account/PlatformAccountView.vue'), meta: { title: '平台账号', group: '系统', icon: '◎' } },
      { path: 'settings', name: 'settings', component: () => import('@/views/setting/PlatformSettingView.vue'), meta: { title: '平台设置', group: '系统', icon: '⚙' } },
      { path: 'ops', name: 'ops', component: () => import('@/views/ops/OpsCenterView.vue'), meta: { title: '运维中心', group: '系统', icon: '⏱' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!getToken()) {
    return { name: 'login', query: to.fullPath === '/' ? undefined : { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 平台超管` : '多开云商城 · 平台超管'
})

setUnauthorizedHandler(() => {
  const current = router.currentRoute.value
  if (current.name !== 'login') {
    router.replace({ name: 'login', query: { redirect: current.fullPath } })
  }
})

export default router
