<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 24 },
})

const source = computed(() => `/static/icons/${props.name}.svg`)
const dimensions = computed(() => {
  const value = typeof props.size === 'number' ? `${props.size}px` : props.size
  return { width: value, height: value }
})
</script>

<template>
  <image class="app-icon" :src="source" :style="dimensions" mode="aspectFit" />
</template>

<style scoped>
.app-icon {
  display: block;
  flex: 0 0 auto;
}
</style>
