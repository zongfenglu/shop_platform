<script setup>
import { ref } from 'vue'
import { login } from '@/api'
import { setLoginUser, setToken } from '@/utils/request'

/**
 * 消费者登录。对应后端 ConsumerAuthController#login（loginOrRegister：手机号即注册即登录）。
 *
 * 注意后端目前的实现只校验手机号、不校验验证码（见 Sprint 5 记录：user 表提前建的最小实现），
 * 短信验证码按排期属于会员体系 Sprint，这里先按现有接口对接，不在前端假装有验证码流程。
 */
const mobile = ref('')
const loading = ref(false)

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
    setTimeout(() => uni.navigateBack({ delta: 1 }), 600)
  } catch (e) {
    // 错误提示已由 request.js 统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="page">
    <view class="title">登录</view>
    <view class="desc">未注册的手机号将自动创建账号</view>
    <input v-model="mobile" class="input" type="number" maxlength="11" placeholder="请输入手机号" />
    <button class="btn" :disabled="loading" @click="onLogin">
      {{ loading ? '登录中…' : '登 录' }}
    </button>
  </view>
</template>

<style scoped>
.page {
  padding: 64rpx 40rpx;
}
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
