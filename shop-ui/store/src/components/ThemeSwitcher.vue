<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { THEMES, useThemeStore } from '@/stores/theme'

// 对应原型 docs/prototype/shared/theme.js 的切换器 UI，样式类名沿用 prototype.css
const theme = useThemeStore()
const open = ref(false)
const root = ref(null)

function toggle(e) {
  e.stopPropagation()
  open.value = !open.value
}

function pick(id) {
  theme.set(id)
  open.value = false
}

function onDocClick(e) {
  if (root.value && !root.value.contains(e.target)) open.value = false
}

onMounted(() => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))
</script>

<template>
  <div ref="root" class="theme-switcher" :class="{ open }">
    <div class="theme-switcher-btn" @click="toggle">
      <span class="dot"></span><span class="label">主题</span>
    </div>
    <div class="theme-switcher-panel">
      <div class="theme-switcher-title">选择后台外观</div>
      <div
        v-for="t in THEMES"
        :key="t.id"
        class="theme-opt"
        :class="{ active: theme.current === t.id }"
        @click="pick(t.id)"
      >
        <div class="swatch" :class="`theme-swatch-${t.id}`"></div>
        <div class="info">
          <div class="name">{{ t.name }}</div>
          <div class="desc">{{ t.desc }}</div>
        </div>
        <div class="check">✓</div>
      </div>
    </div>
  </div>
</template>
