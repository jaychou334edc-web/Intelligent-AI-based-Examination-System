<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadFile, type UploadInstance, type UploadProps } from 'element-plus'
import ParsingStep from '../components/aiImport/ParsingStep.vue'
import QuestionReviewPanel from '../components/aiImport/QuestionReviewPanel.vue'
import UploadStep from '../components/aiImport/UploadStep.vue'
import { importQuestions, parsePaper, type ReviewQuestion } from '../api/paperParsing'
import { loadPaperImageUrl, uploadPaper, type Paper } from '../api/papers'

const router = useRouter()
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const currentPaper = ref<Paper | null>(null)
const questions = ref<ReviewQuestion[]>([])
const rawText = ref('')
const loading = ref(false)
const importLoading = ref(false)
const imageUrls = ref<Record<string, string | null>>({})
const parseProgress = ref(0)
const parseLogs = ref<string[]>([])

const form = reactive({
  title: '本地测试试卷',
})

const questionTypes = [
  { label: '单选题', value: 'single_choice' },
  { label: '多选题', value: 'multiple_choice' },
  { label: '判断题', value: 'true_false' },
  { label: '填空题', value: 'fill_blank' },
  { label: '主观/代码题', value: 'subjective' },
]

const canParse = computed(() => Boolean(selectedFile.value || currentPaper.value))
const canImport = computed(() => currentPaper.value && questions.value.length > 0)
const activeStep = computed(() => {
  if (questions.value.length > 0) {
    return 3
  }
  if (loading.value || currentPaper.value) {
    return 2
  }
  return 1
})
const imageCount = computed(() => {
  const imageIds = new Set<string>()
  for (const question of questions.value) {
    for (const match of question.stem.matchAll(/\[IMG:([^\]]+)\]/g)) {
      imageIds.add(match[1])
    }
  }
  return imageIds.size
})
const averageConfidence = computed(() => {
  if (questions.value.length === 0) {
    return 0
  }
  const scored = questions.value.map(questionConfidence)
  return Math.round(scored.reduce((sum, value) => sum + value, 0) / scored.length)
})

function isSupportedFile(fileName: string) {
  const lowerName = fileName.toLowerCase()
  return lowerName.endsWith('.docx') || lowerName.endsWith('.txt')
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (!isSupportedFile(file.name)) {
    ElMessage.error('请上传 .docx 或 .txt 文件')
    return false
  }
  return false
}

function handleFileChange(file: UploadFile) {
  if (!file.raw) {
    return
  }
  if (!isSupportedFile(file.name)) {
    ElMessage.error('请上传 .docx 或 .txt 文件')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    return
  }
  selectedFile.value = file.raw
  currentPaper.value = null
  questions.value = []
  rawText.value = ''
  parseProgress.value = 0
  parseLogs.value = ['文件已选择，等待开始解析。']
  clearImageUrls()
}

function handleFileRemove() {
  selectedFile.value = null
  currentPaper.value = null
  questions.value = []
  rawText.value = ''
  parseProgress.value = 0
  parseLogs.value = []
  clearImageUrls()
}

function approveQuestion(question: ReviewQuestion) {
  question.reviewStatus = 'approved'
  question.reviewComment = question.reviewComment || '教师已确认'
  ElMessage.success('当前题已标记通过')
}

function approveAll() {
  questions.value.forEach((question) => {
    question.reviewStatus = 'approved'
    question.reviewComment = question.reviewComment || '教师已确认'
  })
  ElMessage.success('已将当前解析结果全部标记为通过')
}

function removeQuestion(index: number) {
  questions.value.splice(index, 1)
}

function clearImageUrls() {
  Object.values(imageUrls.value).forEach((url) => {
    if (url) {
      URL.revokeObjectURL(url)
    }
  })
  imageUrls.value = {}
}

async function ensureImageUrl(imageId: string) {
  if (!currentPaper.value || Object.prototype.hasOwnProperty.call(imageUrls.value, imageId)) {
    return
  }
  imageUrls.value = {
    ...imageUrls.value,
    [imageId]: '',
  }
  try {
    imageUrls.value = {
      ...imageUrls.value,
      [imageId]: await loadPaperImageUrl(currentPaper.value.id, imageId),
    }
  } catch {
    imageUrls.value = {
      ...imageUrls.value,
      [imageId]: null,
    }
  }
}

function imageUrl(imageId: string) {
  void ensureImageUrl(imageId)
  return imageUrls.value[imageId] || ''
}

function imageFailed(imageId: string) {
  return imageUrls.value[imageId] === null
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

function questionConfidence(question: ReviewQuestion) {
  let score = 65
  if (question.stem.trim()) {
    score += 10
  }
  if (question.answer.trim()) {
    score += 8
  }
  if (question.analysis.trim()) {
    score += 5
  }
  if (['single_choice', 'multiple_choice'].includes(question.questionType) && question.options.length >= 2) {
    score += 10
  }
  if (!['single_choice', 'multiple_choice'].includes(question.questionType)) {
    score += 6
  }
  if (question.stem.includes('[IMG:')) {
    score -= 3
  }
  return Math.max(50, Math.min(score, 98))
}

function pushLog(message: string) {
  parseLogs.value = [...parseLogs.value, message]
}

function simulateProgress() {
  parseProgress.value = 8
  parseLogs.value = ['AI正在解析试卷...']
  const checkpoints = [
    { value: 18, log: '已上传文件并建立解析任务' },
    { value: 32, log: '已提取 Word / txt 文本结构' },
    { value: 48, log: '已识别图片占位与题目段落' },
    { value: 63, log: '正在请求 DeepSeek 生成 JSON 结构' },
    { value: 78, log: '正在校验题型、选项、答案和分值' },
    { value: 88, log: '正在整理教师审核视图' },
  ]
  checkpoints.forEach((checkpoint, index) => {
    window.setTimeout(() => {
      if (loading.value && parseProgress.value < checkpoint.value) {
        parseProgress.value = checkpoint.value
        pushLog(`✔ ${checkpoint.log}`)
      }
    }, 350 + index * 420)
  })
}

async function uploadAndParse() {
  if (!selectedFile.value && !currentPaper.value) {
    ElMessage.warning('请先选择题库源文件')
    return
  }

  loading.value = true
  simulateProgress()
  try {
    if (selectedFile.value) {
      currentPaper.value = await uploadPaper(selectedFile.value, form.title)
      pushLog('✔ 文件上传成功')
    }
    const result = await parsePaper(currentPaper.value!.id)
    clearImageUrls()
    questions.value = result.questions
    rawText.value = result.rawText
    parseProgress.value = 100
    pushLog(`✔ 解析完成，共识别 ${result.questions.length} 道题`)
    ElMessage.success(`解析完成，共识别 ${result.questions.length} 道题`)
  } catch (error) {
    pushLog('解析失败，请检查文件内容或 AI 配置。')
    ElMessage.error(error instanceof Error ? error.message : '解析失败')
  } finally {
    loading.value = false
  }
}

async function submitImport() {
  if (!currentPaper.value) {
    return
  }

  importLoading.value = true
  try {
    const result = await importQuestions(currentPaper.value.id, questions.value)
    ElMessage.success(`已导入 ${result.importedCount} 道题到题库`)
    await router.push('/teacher/questions')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入题库失败')
  } finally {
    importLoading.value = false
  }
}

onBeforeUnmount(clearImageUrls)
</script>

<template>
  <main class="paper-page ai-flow-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">AI Import Flow</p>
        <h1>AI题库导入流程系统</h1>
        <p class="summary">以“上传文件、AI解析、教师审核”三阶段组织题库导入，保留图片占位并支持逐题编辑入库。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/teacher/dashboard')">返回工作台</el-button>
        <el-button plain @click="router.push('/teacher/questions')">查看题库</el-button>
      </div>
    </header>

    <section class="status-card ai-flow-card">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="文件上传" description="Word / txt" />
        <el-step title="AI解析中" description="过程日志" />
        <el-step title="题目审核" description="编辑确认入库" />
      </el-steps>
    </section>

    <section class="ai-flow-top-grid">
      <UploadStep
        v-model:upload-ref="uploadRef"
        v-model:title="form.title"
        :selected-file="selectedFile"
        :current-paper-id="currentPaper?.id"
        :current-file-name="currentPaper?.fileName"
        :loading="loading"
        :can-parse="canParse"
        :before-upload="beforeUpload"
        @file-change="handleFileChange"
        @file-remove="handleFileRemove"
        @parse="uploadAndParse"
      />

      <ParsingStep
        :loading="loading"
        :progress="parseProgress"
        :question-count="questions.length"
        :image-count="imageCount"
        :average-confidence="averageConfidence"
        :logs="parseLogs"
      />

      <article class="status-card flow-step-card raw-card">
        <p class="eyebrow">Document Preview</p>
        <h2>文档结构预览</h2>
        <p class="raw-hint">保留 Apache POI 提取文字与 [IMG] 图片占位，便于和 AI 结构化结果交叉检查。</p>
        <el-input v-model="rawText" type="textarea" :rows="12" readonly placeholder="解析后显示文档结构预览" />
      </article>
    </section>

    <QuestionReviewPanel
      :questions="questions"
      :question-types="questionTypes"
      :import-loading="importLoading"
      :can-import="Boolean(canImport)"
      :image-url="imageUrl"
      :image-failed="imageFailed"
      :stem-parts="stemParts"
      :question-confidence="questionConfidence"
      @approve="approveQuestion"
      @approve-all="approveAll"
      @remove="removeQuestion"
      @submit-import="submitImport"
    />
  </main>
</template>
