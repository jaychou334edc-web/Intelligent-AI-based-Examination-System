<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { ReviewQuestion } from '../../api/paperParsing'
import QuestionCard from './QuestionCard.vue'
import QuestionEditor from './QuestionEditor.vue'

const props = defineProps<{
  questions: ReviewQuestion[]
  questionTypes: Array<{ label: string; value: string }>
  importLoading: boolean
  canImport: boolean
  imageUrl: (imageId: string) => string
  imageFailed: (imageId: string) => boolean
  stemParts: (text: string) => Array<{ type: 'text' | 'image'; value: string }>
  questionConfidence: (question: ReviewQuestion) => number
}>()

const emit = defineEmits<{
  approve: [question: ReviewQuestion]
  approveAll: []
  remove: [index: number]
  submitImport: []
}>()

const activeIndex = ref(0)
const activeQuestion = computed(() => props.questions[activeIndex.value])
const approvedCount = computed(() => props.questions.filter((question) => question.reviewStatus === 'approved').length)
const pendingCount = computed(() => Math.max(props.questions.length - approvedCount.value, 0))

watch(
  () => props.questions.length,
  (length) => {
    if (activeIndex.value >= length) {
      activeIndex.value = Math.max(0, length - 1)
    }
  }
)

function addOption(question: ReviewQuestion) {
  const nextKey = String.fromCharCode(65 + question.options.length)
  question.options.push({ key: nextKey, text: '' })
}

function removeOption(question: ReviewQuestion, optionIndex: number) {
  question.options.splice(optionIndex, 1)
}

function goNext() {
  if (activeIndex.value < props.questions.length - 1) {
    activeIndex.value += 1
  }
}
</script>

<template>
  <section class="status-card flow-review-panel">
    <div class="review-heading">
      <div>
        <p class="eyebrow">Step 3</p>
        <h2>题目审核</h2>
        <p class="review-subtitle">左侧选择题目，右侧编辑题干、选项、答案、分值、知识点、难度和图片预览。</p>
      </div>
      <div class="review-actions">
        <el-tag type="success">已通过 {{ approvedCount }}</el-tag>
        <el-tag type="warning">待确认 {{ pendingCount }}</el-tag>
        <el-button plain :disabled="questions.length === 0" @click="emit('approveAll')">批量通过</el-button>
        <el-button type="success" :loading="importLoading" :disabled="!canImport" @click="emit('submitImport')">提交入库</el-button>
      </div>
    </div>

    <el-empty v-if="questions.length === 0" description="解析完成后在这里审核题目" />

    <div v-else class="flow-review-layout">
      <aside class="question-card-list">
        <QuestionCard
          v-for="(question, index) in questions"
          :key="question.parsedQuestionId ?? index"
          :question="question"
          :index="index"
          :active="activeIndex === index"
          :confidence="questionConfidence(question)"
          @select="activeIndex = index"
        />
      </aside>

      <section v-if="activeQuestion" class="question-detail-editor">
        <div class="question-editor-title">
          <strong>第 {{ activeIndex + 1 }} 题</strong>
          <div class="question-title-actions">
            <el-button plain :disabled="activeIndex === 0" @click="activeIndex -= 1">上一题</el-button>
            <el-button plain :disabled="activeIndex >= questions.length - 1" @click="goNext">下一题</el-button>
            <el-button text type="danger" @click="emit('remove', activeIndex)">删除</el-button>
          </div>
        </div>
        <QuestionEditor
          :question="activeQuestion"
          :question-types="questionTypes"
          :image-url="imageUrl"
          :image-failed="imageFailed"
          :stem-parts="stemParts"
          @add-option="addOption(activeQuestion)"
          @remove-option="removeOption(activeQuestion, $event)"
          @approve="emit('approve', activeQuestion)"
        />
      </section>
    </div>
  </section>
</template>
