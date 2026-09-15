<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { login } from '@/api'
import { setLoginUser, setToken } from '@/utils/request'
import { ensureWechatLogin } from '@/utils/wechatAuth'

/**
 * 消费者登录。对应后端 ConsumerAuthController#login（loginOrRegister：手机号即注册即登录）。
 *
 * 注意后端目前的实现只校验手机号、不校验验证码（见 Sprint 5 记录：user 表提前建的最小实现），
 * 短信验证码按排期属于会员体系 Sprint，这里先按现有接口对接，不在前端假装有验证码流程。
 */
const mobile = ref('')
const loading = ref(false)
const wechatError = ref('')

// #ifdef MP-WEIXIN
onLoad(() => onWechatLogin())
// #endif

async function onWechatLogin(force = false) {
  loading.value = true
  wechatError.value = ''
  try {
    await ensureWechatLogin(force)
    uni.showToast({ title: '登录成功', icon: 'success' })
    finishLogin()
  } catch (e) {
    wechatError.value = e?.message || '微信登录失败，请重试'
  } finally {
    loading.value = false
  }
}

function finishLogin() {
  setTimeout(() => {
    const pages = getCurrentPages()
    if (pages.length > 1) uni.navigateBack({ delta: 1 })
    else uni.switchTab({ url: '/pages/my/index' })
  }, 400)
}

async function onLogin() {
  const m = String(mobile.value || '').trim()
  if (!/^1[3-9]\d{9}$/.test(m)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await login(m)
    setToken(data.token)
    // 登录响应只有 userId/nickname（见 ConsumerLoginResponse），没有 profile 接口可查，
    // 存到本地供"我的"页展示，避免每次都要重新登录才能看到昵称
    setLoginUser({ userId: data.userId, nickname: data.nickname })
    uni.showToast({ title: '登录成功', icon: 'success' })
    finishLogin()
  } catch (e) {
    // 错误提示已由 request.js 统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <!-- #ifdef MP-WEIXIN -->
    <view class="wechat-login">
      <view class="wechat-mark">微信</view>
      <view class="title">微信登录</view>
      <view class="desc">正在确认微信身份，无需输入手机号</view>
      <view v-if="loading" class="wechat-status">登录中...</view>
      <button v-else-if="wechatError" class="btn" @click="onWechatLogin(true)">重新登录</button>
      <view v-if="wechatError" class="error">{{ wechatError }}</view>
    </view>
    <!-- #endif -->

    <!-- #ifndef MP-WEIXIN -->
    <view class="title">登录</view>
    <view class="desc">未注册的手机号将自动创建账号</view>
    <input v-model="mobile" class="input" type="number" maxlength="11" placeholder="请输入手机号" />
    <button class="btn" :disabled="loading" @click="onLogin">
      {{ loading ? '登录中…' : '登 录' }}
    </button>
    <!-- #endif -->
  </view>
</template>

<style scoped>
.page {
  padding: 64rpx 40rpx;
}
.wechat-login { min-height: 600rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.wechat-mark { width: 112rpx; height: 112rpx; margin-bottom: 28rpx; display: flex; align-items: center; justify-content: center; border-radius: 24rpx; background: #07c160; color: #fff; font-size: 28rpx; font-weight: 700; }
.wechat-status { color: #777; font-size: 26rpx; }
.error { margin-top: 20rpx; color: #b8403a; font-size: 24rpx; text-align: center; }
.title {
  font-size: 40rpx;
  font-weight: 700;
}
.desc {
  font-size: 24rpx;
  color: #898781;
  margin: 12rpx 0 40rpx;
}
.input {
  border: 2rpx solid rgba(11, 11, 11, 0.16);
  border-radius: 12rpx;
  height: 88rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  margin-bottom: 32rpx;
}
.btn {
  background: #2a78d6;
  color: #fff;
  border-radius: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
}
</style>
