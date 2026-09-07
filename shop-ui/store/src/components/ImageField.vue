<script setup>
import { computed, ref } from 'vue'
import MaterialPicker from './MaterialPicker.vue'

/**
 * 统一选图入口：缩略图 + 打开素材库。单图绑定字符串，多图绑定字符串数组。
 */
const props = defineProps({
  modelValue: { type: [String, Array], default: '' },
  multiple: { type: Boolean, default: false },
  max: { type: Number, default: 9 },
  size: { type: String, default: 'md' },
})

const emit = defineEmits(['update:modelValue'])

const open = ref(false)

function isImgUrl(value) {
  return typeof value === 'string' && /^(https?:\/\/|\/)/.test(value)
}

const urls = computed(() => {
  if (props.multiple) {
    return (Array.isArray(props.modelValue) ? props.modelValue : []).filter(isImgUrl)
  }
  return isImgUrl(props.modelValue) ? [props.modelValue] : []
})

const canAdd = computed(() => urls.value.length < (props.multiple ? props.max : 1))

function openPicker() {
  open.value = true
}

function onSelect(picked) {
  const incoming = Array.isArray(picked) ? picked : [picked]
  if (props.multiple) {
    const merged = []
    for (const url of [...urls.value, ...incoming]) {
      if (url && !merged.includes(url)) merged.push(url)
    }
    emit('update:modelValue', merged.slice(0, props.max))
    return
  }
  emit('update:modelValue', incoming[0] || '')
}

function removeAt(index) {
  if (props.multiple) {
    emit('update:modelValue', urls.value.filter((_, i) => i !== index))
    return
  }
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="img-field" :class="'is-' + size">
    <div class="img-field-list">
      <div v-for="(url, i) in urls" :key="url + '-' + i" class="img-field-tile">
        <img :src="url" alt="" />
        <span v-if="multiple && i === 0" class="img-field-badge">主图</span>
        <button type="button" class="img-field-remove" @click="removeAt(i)">移除</button>
      </div>
      <button v-if="canAdd" type="button" class="img-field-add" @click="openPicker">
        <span class="img-field-plus">＋</span>
        <span>{{ urls.length ? '继续添加' : '从素材库选择' }}</span>
      </button>
    </div>
    <div class="form-hint">
      {{ multiple ? `第一张为主图，最多 ${max} 张` : '点击从素材库选择或上传' }}
    </div>
    <MaterialPicker v-model:open="open" :multiple="multiple" :max="multiple ? max - urls.length : 1" @select="onSelect" />
  </div>
</template>

<style scoped>
.img-field-list { display: flex; flex-wrap: wrap; gap: 10px; }
.img-field-tile,
.img-field-add {
  width: 88px;
  height: 88px;
  border-radius: var(--r-sm);
  position: relative;
  overflow: hidden;
}
.is-sm .img-field-tile,
.is-sm .img-field-add {
  width: 56px;
  height: 56px;
}
.img-field-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border: 1px solid var(--border);
  border-radius: var(--r-sm);
}
.img-field-badge {
  position: absolute;
  left: 4px;
  bottom: 4px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
}
.img-field-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  border: 0;
  border-radius: 999px;
  background: var(--status-critical);
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  cursor: pointer;
}
.img-field-add {
  border: 1px dashed var(--border-strong);
  background: var(--surface-2);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
}
.is-sm .img-field-add { font-size: 11px; }
.img-field-plus { font-size: 18px; line-height: 1; color: var(--primary); }
</style>
