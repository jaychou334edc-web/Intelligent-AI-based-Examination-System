<script setup lang="ts">
import type { UploadFile, UploadInstance, UploadProps } from 'element-plus'
import { DocumentAdd, UploadFilled } from '@element-plus/icons-vue'

defineProps<{
  title: string
  selectedFile: File | null
  currentPaperId?: number
  currentFileName?: string
  loading: boolean
  canParse: boolean
  beforeUpload: UploadProps['beforeUpload']
}>()

const emit = defineEmits<{
  'update:title': [value: string]
  fileChange: [file: UploadFile]
  fileRemove: []
  parse: []
}>()

const uploadRef = defineModel<UploadInstance | undefined>('uploadRef')

function handleUploadChange(file: UploadFile) {
  emit('fileChange', file)
}
</script>

<template>
  <article class="status-card flow-step-card upload-step-card">
    <div class="panel-title-row">
      <div>
        <p class="eyebrow">Step 1</p>
        <h2>文件上传</h2>
      </div>
      <el-tag :type="selectedFile || currentPaperId ? 'success' : 'info'">
        {{ selectedFile || currentPaperId ? '文件已准备' : '等待上传' }}
      </el-tag>
    </div>

    <el-form label-position="top">
      <el-form-item label="试卷标题">
        <el-input :model-value="title" @update:model-value="emit('update:title', String($event))" />
      </el-form-item>
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".docx,.txt"
        :before-upload="beforeUpload"
        :on-change="handleUploadChange"
        :on-remove="() => emit('fileRemove')"
      >
        <div class="upload-copy rich-upload-copy">
          <el-icon><UploadFilled /></el-icon>
          <strong>拖入或选择 Word / txt 题目文件</strong>
          <span>系统将保留图片占位，并在审核区显示题目结构。</span>
        </div>
      </el-upload>
      <el-button class="submit-button" type="primary" :icon="DocumentAdd" :loading="loading" :disabled="!canParse" @click="emit('parse')">
        开始解析
      </el-button>
    </el-form>

    <div v-if="selectedFile || currentPaperId" class="file-chip">
      <span>{{ selectedFile?.name || currentFileName }}</span>
      <strong>{{ currentPaperId ? `试卷 #${currentPaperId}` : '本地待上传' }}</strong>
    </div>
  </article>
</template>
