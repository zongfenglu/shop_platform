import { defineStore } from 'pinia'
import { login as loginApi, impersonate as impersonateApi } from '@/api/auth'
import { clearToken, getToken, setToken } from '@/api/http'

const USER_KEY = 'shop_store_user'
const SHOP_CODE_KEY = 'shop_store_last_code'

/**
 * 商户后台登录态。
 *
 * 与平台端的差异：商户登录需要 shopCode（商城标识）定位租户，
 * 且 token 里带 shopId —— 后端靠它建立 TenantContext，所有查询自动按租户隔离。
 * 这里把上次用的 shopCode 记住，同一台电脑的店主不用每次重复输入。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
    lastShopCode: localStorage.getItem(SHOP_CODE_KEY) || '',
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.user?.realName || state.user?.username || '商户账号',
    avatarText: (state) => (state.user?.realName || state.user?.username || '店')[0],
    shopCode: (state) => state.user?.shopCode || state.lastShopCode,
    platformImpersonation: (state) => !!state.user?.platformImpersonation,
  },

  actions: {
    async login(shopCode, username, password) {
      const data = await loginApi({ shopCode, username, password })
      this.applySession(data, shopCode || data.shopCode)
      return data
    },

    async impersonate(ticket) {
      const data = await impersonateApi(ticket)
      this.applySession(data, data.shopCode)
      return data
    },

    applySession(data, shopCode) {
      this.token = data.token
      this.user = {
        shopId: data.shopId,
        shopCode: shopCode || data.shopCode,
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        platformImpersonation: !!data.platformImpersonation,
      }
      setToken(data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      if (shopCode) {
        localStorage.setItem(SHOP_CODE_KEY, shopCode)
        this.lastShopCode = shopCode
      }
    },

    logout() {
      this.token = ''
      this.user = null
      clearToken()
      localStorage.removeItem(USER_KEY)
      // 刻意保留 lastShopCode：退出登录不该让店主重新记自己的商城标识
    },
  },
})
