<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

// 对应原型 docs/prototype/admin/login.html
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errors = reactive({ username: '', password: '' })

function validate() {
  errors.username = form.username.trim() ? '' : '请输入管理员账号'
  errors.password = form.password ? '' : '请输入密码'
  return !errors.username && !errors.password
}

async function onSubmit() {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    message.success('登录成功')
    // redirect 来自路由守卫：从受保护页面被踢过来时能跳回原处
    await router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // http 拦截器已经统一弹过错误提示，这里不重复提示，只保持在登录页
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="login-box">
      <div class="login-logo">火</div>
      <div class="login-title">多开云商城 · 平台超管</div>
      <div class="login-sub">管理全部租户商城 · 套餐 · 域名 · 客户端授权</div>

      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label class="form-label"><span class="req">*</span>管理员账号</label>
          <input
            v-model="form.username"
            class="form-input"
            autocomplete="username"
            placeholder="请输入管理员账号"
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

        <button class="btn btn-primary btn-block" type="submit" :disabled="loading" style="height: 38px; margin-top: 6px">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <div class="form-hint" style="text-align: center; margin-top: 14px">
        平台管理员账号由系统管理员分配
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 与原型 login.html 内联样式一致 */
.login-wrap {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #12244a 0%, #1c3d6e 45%, #2a78d6 100%);
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
  background: var(--primary);
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
