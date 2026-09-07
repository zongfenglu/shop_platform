import { defineStore } from 'pinia'

/**
 * 商户后台主题（四套皮肤）。
 *
 * 与原型 docs/prototype/shared/theme.js 完全一致：同一个 localStorage key（shop_theme）、
 * 同一套主题 id，切换方式也是给 <html> 打 data-theme 属性（css 变量在 prototype.css 里已定义）。
 * 保持一致的好处是原型和真实后台可以对照着看，不会出现"原型有四套、实现只有两套"的偏差。
 *
 * 注意：品牌 Logo 刻意不随皮肤变化 —— 它代表租户自身品牌（见 prototype.css 注释）。
 */
export const THEMES = [
  { id: 'white', name: '极简白', desc: '默认外观，清爽简洁' },
  { id: 'jade', name: '翡翠绿', desc: '沉稳专业，浅色底' },
  { id: 'graphite', name: '石墨深', desc: '深色中性灰，护眼' },
  { id: 'ocean', name: '海洋深', desc: '深海蓝调，高级感' },
]

const STORAGE_KEY = 'shop_theme'

function apply(id) {
  if (id === 'white') {
    document.documentElement.removeAttribute('data-theme')
  } else {
    document.documentElement.setAttribute('data-theme', id)
  }
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    current: localStorage.getItem(STORAGE_KEY) || 'white',
  }),

  actions: {
    init() {
      apply(this.current)
    },
    set(id) {
      this.current = id
      localStorage.setItem(STORAGE_KEY, id)
      apply(id)
    },
  },
})
