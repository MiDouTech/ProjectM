<template>
  <div class="doc-editor" :class="{ 'doc-editor--readonly': !editable }">
    <div v-if="editable && editor" class="doc-editor__toolbar">
      <el-button-group>
        <el-button size="small" :type="editor.isActive('bold') ? 'primary' : ''"
          @click="editor.chain().focus().toggleBold().run()"><b>B</b></el-button>
        <el-button size="small" :type="editor.isActive('italic') ? 'primary' : ''"
          @click="editor.chain().focus().toggleItalic().run()"><i>I</i></el-button>
        <el-button size="small" :type="editor.isActive('strike') ? 'primary' : ''"
          @click="editor.chain().focus().toggleStrike().run()"><s>S</s></el-button>
      </el-button-group>
      <el-button-group>
        <el-button size="small" :type="editor.isActive('heading', { level: 1 }) ? 'primary' : ''"
          @click="editor.chain().focus().toggleHeading({ level: 1 }).run()">H1</el-button>
        <el-button size="small" :type="editor.isActive('heading', { level: 2 }) ? 'primary' : ''"
          @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">H2</el-button>
        <el-button size="small" :type="editor.isActive('heading', { level: 3 }) ? 'primary' : ''"
          @click="editor.chain().focus().toggleHeading({ level: 3 }).run()">H3</el-button>
      </el-button-group>
      <el-button-group>
        <el-button size="small" :type="editor.isActive('bulletList') ? 'primary' : ''"
          @click="editor.chain().focus().toggleBulletList().run()">• 列表</el-button>
        <el-button size="small" :type="editor.isActive('orderedList') ? 'primary' : ''"
          @click="editor.chain().focus().toggleOrderedList().run()">1. 列表</el-button>
        <el-button size="small" :type="editor.isActive('blockquote') ? 'primary' : ''"
          @click="editor.chain().focus().toggleBlockquote().run()">引用</el-button>
        <el-button size="small" :type="editor.isActive('codeBlock') ? 'primary' : ''"
          @click="editor.chain().focus().toggleCodeBlock().run()">代码</el-button>
      </el-button-group>
      <el-button-group>
        <el-button size="small" :icon="RefreshLeft" :disabled="!editor.can().undo()"
          @click="editor.chain().focus().undo().run()" />
        <el-button size="small" :icon="RefreshRight" :disabled="!editor.can().redo()"
          @click="editor.chain().focus().redo().run()" />
      </el-button-group>
    </div>
    <editor-content :editor="editor" class="doc-editor__body" />
  </div>
</template>

<script setup>
import { watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import { RefreshLeft, RefreshRight } from '@element-plus/icons-vue'

const props = defineProps({
  // Tiptap JSON 文档对象；空文档传 null
  modelValue: { type: Object, default: null },
  editable: { type: Boolean, default: true },
  placeholder: { type: String, default: '开始撰写文档…' },
})
const emit = defineEmits(['update:modelValue'])

const editor = useEditor({
  content: props.modelValue || '',
  editable: props.editable,
  extensions: [StarterKit, Placeholder.configure({ placeholder: props.placeholder })],
  onUpdate: ({ editor }) => emit('update:modelValue', editor.getJSON()),
})

// 外部内容变化（切换文档/回滚预览）时同步进编辑器，避免覆盖正在输入的内容
watch(() => props.modelValue, (val) => {
  if (!editor.value) return
  const current = JSON.stringify(editor.value.getJSON())
  if (JSON.stringify(val || editor.value.getJSON()) === current) return
  editor.value.commands.setContent(val || '', false)
})
watch(() => props.editable, (val) => editor.value && editor.value.setEditable(val))

onBeforeUnmount(() => editor.value && editor.value.destroy())
</script>

<style scoped>
.doc-editor {
  display: flex;
  flex-direction: column;
  border: var(--mido-border-width) solid var(--el-border-color-light);
  border-radius: var(--mido-radius-md);
  overflow: hidden;
}
.doc-editor--readonly {
  border-color: transparent;
}
.doc-editor__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mido-space-2);
  padding: var(--mido-space-2);
  border-bottom: var(--mido-border-width) solid var(--el-border-color-light);
  background-color: var(--el-fill-color-light);
}
.doc-editor__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--mido-space-4);
}
/* Tiptap 正文排版（ProseMirror 渲染节点）*/
.doc-editor__body :deep(.ProseMirror) {
  outline: none;
  min-height: 320px;
  line-height: var(--mido-line-height-body);
}
.doc-editor__body :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  color: var(--el-text-color-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}
.doc-editor__body :deep(h1) { font-size: var(--mido-font-size-h1); margin: var(--mido-space-3) 0; }
.doc-editor__body :deep(h2) { font-size: var(--mido-font-size-h2); margin: var(--mido-space-3) 0; }
.doc-editor__body :deep(h3) { font-size: var(--mido-font-size-body); font-weight: var(--mido-font-weight-bold); margin: var(--mido-space-2) 0; }
.doc-editor__body :deep(blockquote) {
  border-left: 3px solid var(--el-border-color);
  padding-left: var(--mido-space-3);
  color: var(--el-text-color-secondary);
  margin: var(--mido-space-2) 0;
}
.doc-editor__body :deep(pre) {
  background-color: var(--el-fill-color-dark);
  border-radius: var(--mido-radius-sm);
  padding: var(--mido-space-3);
  overflow-x: auto;
}
.doc-editor__body :deep(ul),
.doc-editor__body :deep(ol) {
  padding-left: var(--mido-space-5);
}
</style>
