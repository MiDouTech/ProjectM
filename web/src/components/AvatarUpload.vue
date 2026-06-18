<template>
  <!-- 头像上传：复用通用附件通道（entityType=avatar）上传图片，存附件 ID；展示走限时预签名 URL。
       v-model 绑定头像附件 ID（字符串），可空。 -->
  <div class="av">
    <div class="av__preview" :style="bgStyle">
      <span v-if="!previewUrl" class="av__placeholder">{{ initial }}</span>
    </div>
    <div class="av__ops">
      <el-upload
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="doUpload"
        accept="image/png,image/jpeg,image/gif,image/webp"
      >
        <el-button :loading="uploading" size="small">{{ modelValue ? '更换头像' : '上传头像' }}</el-button>
      </el-upload>
      <el-button v-if="modelValue" link type="danger" size="small" @click="clear">移除</el-button>
      <div class="mido-text-secondary av__hint">支持 jpg/png/gif/webp，≤ 2MB</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { attachmentApi } from '@/api/attachment'

const props = defineProps({
  modelValue: { type: [String, Number], default: null },
  // 关联实体 ID（仅作附件归属元信息，展示不依赖它）；新建用户时可缺省
  userId: { type: [String, Number], default: 0 },
  name: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const MAX_SIZE = 2 * 1024 * 1024
const uploading = ref(false)
const previewUrl = ref('')

const initial = computed(() => (props.name || '?').trim().charAt(0))
const bgStyle = computed(() => (previewUrl.value ? { backgroundImage: `url(${previewUrl.value})` } : {}))

async function loadPreview(id) {
  if (!id) {
    previewUrl.value = ''
    return
  }
  try {
    previewUrl.value = await attachmentApi.downloadUrl(id)
  } catch {
    previewUrl.value = ''
  }
}
watch(() => props.modelValue, loadPreview, { immediate: true })

function beforeUpload(file) {
  if (file.size > MAX_SIZE) {
    ElMessage.warning('图片不能超过 2MB')
    return false
  }
  return true
}

// el-upload 自定义上传：成功后回写附件 ID 并刷新预览
async function doUpload({ file }) {
  uploading.value = true
  try {
    const vo = await attachmentApi.upload('avatar', props.userId || 0, file)
    emit('update:modelValue', String(vo.id))
    await loadPreview(vo.id)
    ElMessage.success('头像已上传')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}
function clear() {
  emit('update:modelValue', null)
  previewUrl.value = ''
}
</script>

<style scoped>
.av {
  display: flex;
  align-items: center;
  gap: var(--mido-space-4);
}
.av__preview {
  width: 72px;
  height: 72px;
  border-radius: var(--mido-radius-md);
  border: var(--mido-border-width) solid var(--el-border-color);
  background-color: var(--el-fill-color-light);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}
.av__placeholder {
  font-size: var(--mido-font-size-h1);
  color: var(--el-text-color-placeholder);
}
.av__ops {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--mido-space-2);
}
.av__hint {
  font-size: var(--mido-font-size-caption);
}
</style>
