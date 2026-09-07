<script setup>
import { useRoute } from 'vue-router'

// 菜单已按原型定稿，但页面按排期在后续 Sprint 实现。
// 这里给一个明确的占位提示，而不是空白页或死链——原型侧对应的静态页已就绪，实现时直接对照。
const route = useRoute()
</script>

<template>
  <div class="page-header">
    <div>
      <div class="page-title">{{ route.meta.title }}</div>
      <div class="page-desc">该模块按 Sprint 排期尚未实现</div>
    </div>
  </div>
  <div class="card card-pad">
    <p class="card-title">待开发</p>
    <p class="card-sub" style="margin-bottom: 0">
      「{{ route.meta.title }}」的高保真原型已就绪，实现时对照 <code>docs/prototype/admin/</code> 下对应页面复刻即可，
      开发排期见 <code>docs/04-开发计划-Sprint排期.md</code>。
    </p>
  </div>
</template>
