<script setup>
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { message } from 'ant-design-vue'
import { uploadImage } from '@/api/upload'
import MaterialPicker from './MaterialPicker.vue'

/**
 * 商品详情富文本。工具栏可直接上传图片，也可从素材库插入。
 * HTML 存 goods.content，C 端用 rich-text 渲染。
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入商品详情，支持标题、段落和图片' },
})
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const materialOpen = ref(false)
const mode = 'default'

const toolbarConfig = {
  excludeKeys: [
    'group-video',
    'insertVideo',
    'uploadVideo',
    'codeBlock',
    'todo',
    'fullScreen',
    'emotion',
  ],
}

const editorConfig = computed(() => ({
  placeholder: props.placeholder,
  autoFocus: false,
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 5 * 1024 * 1024,
      allowedFileTypes: ['image/*'],
      customUpload: async (file, insertFn) => {
        try {
          const material = await uploadImage(file)
          if (material?.url) {
            insertFn(material.url, material.name || '', material.url)
          }
        } catch (e) {
          message.error('图片上传失败')
        }
      },
    },
  },
}))

const html = computed({
  get: () => props.modelValue || '',
  set: (value) => emit('update:modelValue', normalizeHtml(value)),
})

function handleCreated(editor) {
  editorRef.value = editor
}

function normalizeHtml(value) {
  if (!value) return ''
  const hasImage = /<img[\s>]/i.test(value)
  const text = value.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
  if (!text && !hasImage) return ''
  return value
}

watch(
  () => props.modelValue,
  (value) => {
    const editor = editorRef.value
    if (!editor) return
    const next = value || ''
    if (editor.getHtml() !== next) {
      editor.setHtml(next)
    }
  },
)

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
    editorRef.value = null
  }
})

function onMaterialSelect(picked) {
  const urls = (Array.isArray(picked) ? picked : [picked]).filter(Boolean)
  const editor = editorRef.value
  if (!editor || !urls.length) return
  urls.forEach((url) => {
    editor.dangerouslyInsertHtml(`<p><img src="${escapeAttr(url)}" alt="" style="max-width:100%;height:auto;" /></p>`)
  })
}

function escapeAttr(url) {
  return String(url).replace(/"/g, '&quot;')
}
</script>

<template>
  <div class="rich-editor">
    <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" :mode="mode" class="rich-editor-toolbar" />
    <Editor
      v-model="html"
      :defaultConfig="editorConfig"
      :mode="mode"
      class="rich-editor-body"
      @onCreated="handleCreated"
    />
    <div class="rich-editor-foot">
      <button type="button" class="btn btn-sm" @click="materialOpen = true">从素材库插入图片</button>
      <span class="form-hint" style="margin: 0">工具栏也可直接上传图片，单张不超过 5MB</span>
    </div>
    <MaterialPicker v-model:open="materialOpen" multiple :max="9" @select="onMaterialSelect" />
  </div>
</template>

<style scoped>
.rich-editor {
  border: 1px solid var(--border-strong);
  border-radius: var(--r-sm);
  overflow: hidden;
  background: var(--surface);
}
.rich-editor-toolbar {
  border-bottom: 1px solid var(--gridline);
}
.rich-editor-body {
  height: 360px;
  overflow-y: hidden;
}
.rich-editor-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-top: 1px solid var(--gridline);
  background: var(--surface-2);
}
.rich-editor :deep(.w-e-toolbar) {
  background: var(--surface-2);
  border: 0;
}
.rich-editor :deep(.w-e-bar) {
  background: var(--surface-2);
}
.rich-editor :deep(.w-e-text-container) {
  background: var(--surface);
}
.rich-editor :deep(.w-e-text-placeholder) {
  color: var(--text-muted);
  font-style: normal;
}
.rich-editor :deep(.w-e-text-container [data-slate-editor]) {
  padding: 12px 16px;
  color: var(--text-primary);
}
.rich-editor :deep(.w-e-text-container img) {
  max-width: 100%;
  height: auto;
}
html[data-theme='graphite'] .rich-editor :deep(.w-e-bar-item button),
html[data-theme='ocean'] .rich-editor :deep(.w-e-bar-item button) {
  color: var(--text-secondary);
}
</style>
