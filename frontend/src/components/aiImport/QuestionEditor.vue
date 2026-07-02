<script setup lang="ts">
import type { ReviewQuestion } from '../../api/paperParsing'

defineProps<{
  question: ReviewQuestion
  questionTypes: Array<{ label: string; value: string }>
  imageUrl: (imageId: string) => string
  imageFailed: (imageId: string) => boolean
  stemParts: (text: string) => Array<{ type: 'text' | 'image'; value: string }>
}>()

const emit = defineEmits<{
  addOption: []
  removeOption: [index: number]
  approve: []
}>()
</script>

<template>
  <div class="question-editor flow-question-editor">
    <div class="question-fields">
      <el-select v-model="question.questionType" placeholder="题型">
        <el-option v-for="type in questionTypes" :key="type.value" :label="type.label" :value="type.value" />
      </el-select>
      <el-input-number v-model="question.score" :min="0.5" :step="0.5" />
      <el-input v-model="question.knowledgePoint" placeholder="知识点" />
      <el-input v-model="question.difficulty" placeholder="难度" />
    </div>

    <div class="stem-preview">
      <template v-for="(part, partIndex) in stemParts(question.stem)" :key="partIndex">
        <p v-if="part.type === 'text'">{{ part.value }}</p>
        <img v-else-if="imageUrl(part.value)" :src="imageUrl(part.value)" :alt="part.value" />
        <span v-else-if="imageFailed(part.value)" class="image-loading">图片加载失败：{{ part.value }}</span>
        <span v-else class="image-loading">图片加载中：{{ part.value }}</span>
      </template>
    </div>

    <el-input v-model="question.stem" type="textarea" :rows="5" placeholder="题干" />

    <div class="option-list">
      <div v-for="(option, optionIndex) in question.options" :key="optionIndex" class="option-row">
        <el-input v-model="option.key" class="option-key" />
        <el-input v-model="option.text" placeholder="选项内容" />
        <el-button text type="danger" @click="emit('removeOption', optionIndex)">移除</el-button>
      </div>
      <el-button plain @click="emit('addOption')">添加选项</el-button>
    </div>

    <el-input v-model="question.answer" placeholder="答案" />
    <el-input v-model="question.analysis" type="textarea" :rows="3" placeholder="解析" />
    <el-input v-model="question.reviewComment" placeholder="审核备注" />
    <el-button type="success" @click="emit('approve')">保存当前题并标记通过</el-button>
  </div>
</template>
