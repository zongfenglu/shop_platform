import { wechatLogin } from '@/api'
import { getLoginUser, getToken, setLoginUser, setToken } from '@/utils/request'

let loginTask = null

function requestLoginCode() {
  return new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success(result) {
        if (result.code) resolve(result.code)
        else reject(new Error('微信未返回登录凭证'))
      },
      fail: reject,
    })
  })
}

/** 微信小程序静默登录；并发页面只会发起一次 wx.login。 */
export function ensureWechatLogin(force = false) {
  // #ifdef MP-WEIXIN
  if (!force && getToken()) return Promise.resolve(getLoginUser())
  if (loginTask) return loginTask
  loginTask = (async () => {
    const code = await requestLoginCode()
    const data = await wechatLogin(code)
    if (!data?.token || !data?.userId) {
      throw new Error('微信登录服务返回异常')
    }
    setToken(data.token)
    setLoginUser({
      userId: data.userId,
      nickname: data.nickname,
      mobile: data.mobile || '',
    })
    return data
  })().finally(() => {
    loginTask = null
  })
  return loginTask
  // #endif

  // #ifndef MP-WEIXIN
  return Promise.resolve(null)
  // #endif
}
