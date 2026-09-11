import { createRouter, createWebHistory } from 'vue-router'
import { getToken, setUnauthorizedHandler, isTokenExpired } from '@/api/http'

/**
 * 商户后台路由。菜单结构对照原型 docs/prototype/store/dashboard.html 的侧边栏：
 * 交易（首页看板/商品/订单/售后）、运营（会员/营销/分销/门店）、
 * 店铺（装修/财务/设置/客户端/员工）、账户（我的套餐）。
 *
 * 后端已具备的接口先接真实数据（商品、订单、售后），其余按排期标 pending。
 */
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/StoreLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '首页看板', group: '交易', icon: '◨' } },
      { path: 'goods', name: 'goods', component: () => import('@/views/goods/GoodsListView.vue'), meta: { title: '商品', group: '交易', icon: '▤' } },
      { path: 'goods/new', name: 'goods-new', component: () => import('@/views/goods/GoodsPublishView.vue'), meta: { title: '发布商品' } },
      { path: 'goods/categories', name: 'goods-categories', component: () => import('@/views/goods/CategoryManageView.vue'), meta: { title: '分类管理', group: '交易', icon: '☰' } },
      { path: 'goods/:id/edit', name: 'goods-edit', component: () => import('@/views/goods/GoodsPublishView.vue'), meta: { title: '编辑商品' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '订单', group: '交易', icon: '▦' } },
      { path: 'orders/:id/ship', name: 'order-ship', component: () => import('@/views/order/OrderShipView.vue'), meta: { title: '订单发货' } },
      { path: 'orders/:id', name: 'order-detail', component: () => import('@/views/order/OrderDetailView.vue'), meta: { title: '订单详情' } },
      { path: 'after-sales', name: 'after-sales', component: () => import('@/views/afterSale/AfterSaleListView.vue'), meta: { title: '售后管理', group: '交易', icon: '↺' } },

      { path: 'members', name: 'members', component: () => import('@/views/member/MemberListView.vue'), meta: { title: '会员', group: '运营', icon: '◎' } },
      { path: 'member-grades', name: 'member-grades', component: () => import('@/views/member/MemberGradeView.vue'), meta: { title: '会员等级', group: '运营', icon: '★' } },
      { path: 'recharge-plans', name: 'recharge-plans', component: () => import('@/views/member/RechargePlanView.vue'), meta: { title: '充值方案', group: '运营', icon: '￥' } },
      { path: 'marketing', name: 'marketing', component: () => import('@/views/marketing/MarketingView.vue'), meta: { title: '营销', group: '运营', icon: '◇' } },
      { path: 'distribution', name: 'distribution', component: () => import('@/views/distribution/DistributionView.vue'), meta: { title: '分销', group: '运营', icon: '⇅' } },
      { path: 'offline-stores', name: 'offline-stores', component: () => import('@/views/offlineStore/OfflineStoreView.vue'), meta: { title: '门店', group: '运营', icon: '⌂' } },

      { path: 'diy', name: 'diy', component: () => import('@/views/diy/DiyView.vue'), meta: { title: '装修', group: '店铺', icon: '▧' } },
      { path: 'finance', name: 'finance', component: () => import('@/views/finance/FinanceView.vue'), meta: { title: '财务', group: '店铺', icon: '￥' } },
      { path: 'settings', name: 'settings', component: () => import('@/views/settings/SettingsView.vue'), meta: { title: '设置', group: '店铺', icon: '⚙' } },
      { path: 'client', name: 'client', component: () => import('@/views/client/ClientView.vue'), meta: { title: '客户端', group: '店铺', icon: '▣' } },
      { path: 'staff', name: 'staff', component: () => import('@/views/staff/StaffView.vue'), meta: { title: '员工', group: '店铺', icon: '◈' } },

      { path: 'my-package', name: 'my-package', component: () => import('@/views/package/MyPackageView.vue'), meta: { title: '我的套餐', group: '账户', icon: '◆' } },
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
  // 只看 token 是否存在不够：过期 token 会让后端 StoreTenantFilter 解析失败、租户上下文缺失，
  // 列表页第一条 SQL 就抛 IllegalStateException → HTTP 500（而非 200+10002），前端不会跳登录。
  // 这里在客户端先把过期 token 判掉并清掉，直接回登录页。
  if (!getToken() || isTokenExpired()) {
    return { name: 'login', query: to.fullPath === '/' ? undefined : { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 商户后台` : '商户管理后台'
})

setUnauthorizedHandler(() => {
  const current = router.currentRoute.value
  if (current.name !== 'login') {
    router.replace({ name: 'login', query: { redirect: current.fullPath } })
  }
})

export default router
