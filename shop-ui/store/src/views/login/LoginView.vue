<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'

// 对应原型 docs/prototype/store/login.html
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ shopCode: auth.lastShopCode, username: '', password: '' })
const loading = ref(false)
const exchanging = ref(!!route.query.ticket)
const errors = reactive({ shopCode: '', username: '', password: '' })

onMounted(async () => {
  const ticket = typeof route.query.ticket === 'string' ? route.query.ticket : ''
  if (!ticket) {
    exchanging.value = false
    return
  }
  exchanging.value = true
  try {
    await auth.impersonate(ticket)
    message.success('已进入平台代管理模式')
    await router.replace('/dashboard')
  } catch (e) {
    exchanging.value = false
  }
})

function validate() {
  errors.shopCode = form.shopCode.trim() ? '' : '请输入商城标识'
  errors.username = form.username.trim() ? '' : '请输入账号'
  errors.password = form.password ? '' : '请输入密码'
  return !errors.shopCode && !errors.username && !errors.password
}

async function onSubmit() {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    await auth.login(form.shopCode.trim(), form.username.trim(), form.password)
    message.success('登录成功')
    await router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="login-theme-switcher"><ThemeSwitcher /></div>
    <div class="login-box">
      <div class="login-logo">店</div>
      <div class="login-title">商户管理后台</div>
      <div class="login-sub">登录后管理商品 · 订单 · 会员 · 店铺装修</div>
      <div v-if="exchanging" class="login-sub">正在进入平台代管理…</div>

      <form v-else @submit.prevent="onSubmit">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>商城标识</label>
          <input
            v-model="form.shopCode"
            class="form-input"
            placeholder="如 huajianji（域名前缀）"
            @input="errors.shopCode = ''"
          />
          <div v-if="errors.shopCode" class="field-error">{{ errors.shopCode }}</div>
          <div v-else class="form-hint" style="margin-top: 4px">开店时分配的域名前缀，可在开店短信中查看</div>
        </div>

        <div class="form-item">
          <label class="form-label"><span class="req">*</span>账号</label>
          <input
            v-model="form.username"
            class="form-input"
            autocomplete="username"
            placeholder="请输入账号"
            @input="errors.username = ''"
          />
          <div v-if="errors.username" class="field-error">{{ errors.username }}</div>
        </div>

        <div class="form-item">
          <label class="form-label"><span class="req">*</span>密码</label>
          <input
            v-model="form.password"
            class="form-input"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            @input="errors.password = ''"
          />
          <div v-if="errors.password" class="field-error">{{ errors.password }}</div>
        </div>

        <button
          class="btn btn-primary btn-block"
          type="submit"
          :disabled="loading"
          style="height: 38px; margin-top: 6px; background: linear-gradient(135deg, #ff9143, #e34948); border: none"
        >
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
/* 与原型 store/login.html 一致：暖色渐变底 + 橙红品牌色 */
.login-wrap {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(160deg, #fff4ec 0%, #fbe9e5 50%, #f3ddd8 100%);
}
:global(html[data-theme='graphite']) .login-wrap,
:global(html[data-theme='ocean']) .login-wrap {
  background: linear-gradient(160deg, var(--page-bg) 0%, var(--surface-2) 100%);
}
.login-theme-switcher {
  position: absolute;
  top: 24px;
  right: 24px;
}
.login-box {
  width: 380px;
  background: var(--surface);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-modal);
  padding: 36px 32px;
}
.login-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff9143, #e34948);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}
.login-title {
  font-size: 19px;
  font-weight: 700;
  margin-bottom: 4px;
}
.login-sub {
  font-size: 12.5px;
  color: var(--text-muted);
  margin-bottom: 26px;
}
.field-error {
  font-size: 12px;
  color: var(--status-critical);
  margin-top: 4px;
}
</style>
