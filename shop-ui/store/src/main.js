import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'
import './styles/index.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(Antd)

// index.html 里已内联应用过一次主题（防闪烁），这里同步 store 状态
useThemeStore(pinia).init()

app.mount('#app')
