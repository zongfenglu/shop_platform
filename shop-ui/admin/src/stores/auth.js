import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import { clearToken, getToken, setToken } from '@/api/http'

const USER_KEY = 'shop_admin_user'

/**
 * 登录态。
 *
 * token 与用户信息都落 localStorage：刷新页面后不该被踢回登录页。
 * 这里不做 token 过期时间的前端判断——后端 AdminAuthFilter 会返回 401，
 * http 拦截器统一处理跳转，前端自己算过期只会产生两套不一致的判断。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.user?.realName || state.user?.username || '平台管理员',
    avatarText: (state) => (state.user?.realName || state.user?.username || '平')[0],
  },

  actions: {
    async login(username, password) {
      const data = await loginApi({ username, password })
      this.token = data.token
      this.user = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
      }
      setToken(data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      return data
    },

    logout() {
      this.token = ''
      this.user = null
      clearToken()
      localStorage.removeItem(USER_KEY)
    },
  },
})
