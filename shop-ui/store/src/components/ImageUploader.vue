<script setup>
import ImageField from './ImageField.vue'

/** 兼容旧入口，统一走素材库选图。 */
defineProps({
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <ImageField :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" />
</template>
