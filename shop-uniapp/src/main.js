import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// uni-app 用 createSSRApp（跨端要求），不是 createApp
export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  return { app }
}
