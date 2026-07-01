<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRecentQuestions, type QuestionBankItem } from '../api/questions'
import { loadPaperImageUrl } from '../api/papers'

const router = useRouter()
const questions = ref<QuestionBankItem[]>([])
const loading = ref(false)
const imageUrls = ref<Record<string, string | null>>({})

const questionTypeLabels: Record<string, string> = {
  single_choice: '单选题',
  multiple_choice: '多选题',
  true_false: '判断题',
  fill_blank: '填空题',
  subjective: '主观/代码题',
}

function clearImageUrls() {
  Object.values(imageUrls.value).forEach((url) => {
    if (url) {
      URL.revokeObjectURL(url)
    }
  })
  imageUrls.value = {}
}

function imageKey(paperId: number, imageId: string) {
  return `${paperId}:${imageId}`
}

async function ensureImageUrl(paperId: number | undefined, imageId: string) {
  if (!paperId) {
    return
  }
  const key = imageKey(paperId, imageId)
  if (Object.prototype.hasOwnProperty.call(imageUrls.value, key)) {
    return
  }
  imageUrls.value = {
    ...imageUrls.value,
    [key]: '',
  }
  try {
    imageUrls.value = {
      ...imageUrls.value,
      [key]: await loadPaperImageUrl(paperId, imageId),
    }
  } catch {
    imageUrls.value = {
      ...imageUrls.value,
      [key]: null,
    }
  }
}

function imageUrl(paperId: number | undefined, imageId: string) {
  if (!paperId) {
    return ''
  }
  void ensureImageUrl(paperId, imageId)
  return imageUrls.value[imageKey(paperId, imageId)] || ''
}

function imageFailed(paperId: number | undefined, imageId: string) {
  if (!paperId) {
    return false
  }
  return imageUrls.value[imageKey(paperId, imageId)] === null
}

function stemParts(text: string) {
  const parts: Array<{ type: 'text' | 'image'; value: string }> = []
  const pattern = /\[IMG:([^\]]+)\]/g
  let lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: 'text', value: text.slice(lastIndex, match.index) })
    }
    parts.push({ type: 'image', value: match[1] })
    lastIndex = pattern.lastIndex
  }
  if (lastIndex < text.length) {
    parts.push({ type: 'text', value: text.slice(lastIndex) })
  }
  return parts
}

async function loadQuestions() {
  loading.value = true
  try {
    clearImageUrls()
    questions.value = await getRecentQuestions()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题库加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadQuestions)
onBeforeUnmount(clearImageUrls)
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Question Bank</p>
        <h1>题库查看</h1>
        <p class="summary">查看最近导入的题目，确认题型、选项、答案和图片是否保留。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="loadQuestions">刷新</el-button>
        <el-button plain @click="router.push('/teacher')">返回工作台</el-button>
      </div>
    </header>

    <el-empty v-if="!loading && questions.length === 0" description="暂无题库数据" />

    <section v-else class="question-list">
      <article v-for="question in questions" :key="question.id" class="question-editor">
        <div class="question-editor-title">
          <strong>#{{ question.id }} {{ questionTypeLabels[question.questionType] ?? question.questionType }}</strong>
          <span>{{ question.score }} 分</span>
        </div>

        <div class="stem-preview">
          <template v-for="(part, partIndex) in stemParts(question.stem)" :key="partIndex">
            <p v-if="part.type === 'text'">{{ part.value }}</p>
            <img v-else-if="imageUrl(question.sourcePaperId, part.value)" :src="imageUrl(question.sourcePaperId, part.value)" :alt="part.value" />
            <span v-else-if="!question.sourcePaperId" class="image-loading">图片缺少试卷来源：{{ part.value }}</span>
            <span v-else-if="imageFailed(question.sourcePaperId, part.value)" class="image-loading">图片加载失败：{{ part.value }}</span>
            <span v-else class="image-loading">图片加载中：{{ part.value }}</span>
          </template>
        </div>

        <div v-if="question.options.length > 0" class="option-list">
          <div v-for="option in question.options" :key="option.key" class="bank-option-row">
            <strong>{{ option.key }}</strong>
            <span>{{ option.text }}</span>
          </div>
        </div>

        <div class="bank-meta">
          <span>答案：{{ question.answer || '未设置' }}</span>
          <span>难度：{{ question.difficulty || 'normal' }}</span>
          <span>知识点：{{ question.knowledgePoint || '未设置' }}</span>
        </div>

        <p v-if="question.analysis" class="bank-analysis">解析：{{ question.analysis }}</p>
      </article>
    </section>
  </main>
</template>
