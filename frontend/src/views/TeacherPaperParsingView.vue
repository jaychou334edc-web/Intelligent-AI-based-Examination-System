<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadFile, type UploadInstance, type UploadProps } from 'element-plus'
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
const currentStep = computed(() => {
  if (questions.value.length > 0) {
    return 2
  }
  if (loading.value || currentPaper.value) {
    return 1
  }
  return 0
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
const typeStats = computed(() => questionTypes.map((type) => ({
  ...type,
  count: questions.value.filter((question) => question.questionType === type.value).length,
})))
const averageConfidence = computed(() => {
  if (questions.value.length === 0) {
    return 0
  }
  const scored = questions.value.map(questionConfidence)
  return Math.round(scored.reduce((sum, value) => sum + value, 0) / scored.length)
})
const approvedCount = computed(() => questions.value.filter((question) => question.reviewStatus === 'approved').length)
const pendingCount = computed(() => Math.max(questions.value.length - approvedCount.value, 0))

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
  clearImageUrls()
}

function handleFileRemove() {
  selectedFile.value = null
  currentPaper.value = null
  questions.value = []
  rawText.value = ''
  parseProgress.value = 0
  clearImageUrls()
}

function removeQuestion(index: number) {
  questions.value.splice(index, 1)
}

function approveQuestion(question: ReviewQuestion) {
  question.reviewStatus = 'approved'
  question.reviewComment = question.reviewComment || '教师已确认'
}

function approveAll() {
  questions.value.forEach(approveQuestion)
  ElMessage.success('已将当前解析结果全部标记为通过')
}

function addOption(question: ReviewQuestion) {
  const nextKey = String.fromCharCode(65 + question.options.length)
  question.options.push({ key: nextKey, text: '' })
}

function removeOption(question: ReviewQuestion, optionIndex: number) {
  question.options.splice(optionIndex, 1)
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

function answerControlLabel(questionType: string) {
  if (questionType === 'single_choice' || questionType === 'multiple_choice') {
    return '学生端将显示选项按钮'
  }
  if (questionType === 'true_false') {
    return '学生端将显示对/错按钮'
  }
  return '学生端将显示文本作答框'
}

function questionTypeLabel(questionType: string) {
  return questionTypes.find((type) => type.value === questionType)?.label ?? questionType
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

function confidenceType(value: number) {
  if (value >= 90) {
    return 'success'
  }
  if (value >= 75) {
    return 'warning'
  }
  return 'danger'
}

function simulateProgress() {
  parseProgress.value = 8
  const checkpoints = [18, 32, 48, 63, 78, 88]
  checkpoints.forEach((value, index) => {
    window.setTimeout(() => {
      if (loading.value && parseProgress.value < value) {
        parseProgress.value = value
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
    }
    const result = await parsePaper(currentPaper.value!.id)
    clearImageUrls()
    questions.value = result.questions
    rawText.value = result.rawText
    parseProgress.value = 100
    ElMessage.success(`解析完成，共识别 ${result.questions.length} 道题`)
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(clearImageUrls)

async function submitImport() {
  if (!currentPaper.value) {
    return
  }

  importLoading.value = true
  try {
    const result = await importQuestions(currentPaper.value.id, questions.value)
    ElMessage.success(`已导入 ${result.importedCount} 道题到题库`)
    await router.push('/teacher/questions')
  } finally {
    importLoading.value = false
  }
}
</script>

<template>
  <main class="paper-page ai-import-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">AI Import</p>
        <h1>AI 题库导入</h1>
        <p class="summary">上传 Word 或 txt 题目文件，系统会提取文字和图片，调用 AI 解析为可逐题审核的结构化题库。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/teacher/questions')">查看题库</el-button>
        <el-button plain @click="router.push('/teacher/dashboard')">返回工作台</el-button>
      </div>
    </header>

    <section class="status-card ai-flow-card">
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step title="上传文件" description="docx / txt" />
        <el-step title="AI 解析" description="结构化识别" />
        <el-step title="教师审核" description="确认后入库" />
      </el-steps>
    </section>

    <section class="ai-import-layout">
      <article class="status-card ai-upload-panel">
        <div class="panel-title-row">
          <div>
            <p class="eyebrow">Step 1</p>
            <h2>上传文件</h2>
          </div>
          <el-tag v-if="selectedFile" type="success">已选择</el-tag>
          <el-tag v-else>等待文件</el-tag>
        </div>
        <el-form label-position="top">
          <el-form-item label="试卷标题">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".docx,.txt"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <div class="upload-copy">
              <strong>选择题库源文件</strong>
              <span>支持 .docx / .txt</span>
            </div>
          </el-upload>
          <el-button class="submit-button" type="primary" :loading="loading" :disabled="!canParse" @click="uploadAndParse">
            上传并启动 AI 解析
          </el-button>
        </el-form>

        <div v-if="selectedFile || currentPaper" class="file-chip">
          <span>{{ selectedFile?.name || currentPaper?.fileName }}</span>
          <strong>{{ currentPaper ? `试卷 #${currentPaper.id}` : '本地待上传' }}</strong>
        </div>
      </article>

      <article class="status-card ai-parse-panel">
        <div class="panel-title-row">
          <div>
            <p class="eyebrow">Step 2</p>
            <h2>AI 解析</h2>
          </div>
          <el-tag :type="questions.length > 0 ? 'success' : loading ? 'warning' : 'info'">
            {{ questions.length > 0 ? '解析完成' : loading ? '解析中' : '待解析' }}
          </el-tag>
        </div>

        <el-progress :percentage="parseProgress" :status="parseProgress === 100 ? 'success' : undefined" />

        <div class="ai-metrics-grid">
          <div class="metric-tile">
            <span>识别题目</span>
            <strong>{{ questions.length }}</strong>
          </div>
          <div class="metric-tile">
            <span>图片数量</span>
            <strong>{{ imageCount }}</strong>
          </div>
          <div class="metric-tile">
            <span>平均置信度</span>
            <strong>{{ averageConfidence }}%</strong>
          </div>
        </div>

        <div class="type-stat-list">
          <div v-for="type in typeStats" :key="type.value" class="type-stat-row">
            <span>{{ type.label }}</span>
            <el-progress :percentage="questions.length ? Math.round((type.count / questions.length) * 100) : 0" :show-text="false" />
            <strong>{{ type.count }}</strong>
          </div>
        </div>
      </article>

      <article class="status-card raw-card">
        <p class="eyebrow">Document</p>
        <h2>文档结构预览</h2>
        <p class="raw-hint">这里保留 Apache POI 提取出的文字与 [IMG] 图片占位，便于和 AI 结构化结果交叉检查。</p>
        <el-input v-model="rawText" type="textarea" :rows="10" readonly placeholder="解析后显示文档结构预览" />
      </article>
    </section>

    <section class="review-section">
      <div class="review-heading">
        <div>
          <p class="eyebrow">Step 3</p>
          <h2>教师审核</h2>
          <p class="review-subtitle">逐题确认题型、题干、答案和图片位置。审核通过后导入正式题库。</p>
        </div>
        <div class="review-actions">
          <el-tag type="success">已通过 {{ approvedCount }}</el-tag>
          <el-tag type="warning">待确认 {{ pendingCount }}</el-tag>
          <el-button plain :disabled="questions.length === 0" @click="approveAll">一键全部通过</el-button>
          <el-button type="success" :loading="importLoading" :disabled="!canImport" @click="submitImport">
            确认导入题库
          </el-button>
        </div>
      </div>

      <el-empty v-if="questions.length === 0" description="解析后在这里审核题目" />

      <div v-else class="question-list">
        <article v-for="(question, index) in questions" :key="question.parsedQuestionId ?? index" class="question-editor">
          <div class="question-editor-title">
            <div class="question-title-stack">
              <strong>第 {{ index + 1 }} 题 · {{ questionTypeLabel(question.questionType) }}</strong>
              <span>{{ answerControlLabel(question.questionType) }}</span>
            </div>
            <div class="question-title-actions">
              <el-tag :type="confidenceType(questionConfidence(question))">AI 置信度 {{ questionConfidence(question) }}%</el-tag>
              <el-tag v-if="question.reviewStatus === 'approved'" type="success">已通过</el-tag>
              <el-button text type="primary" @click="approveQuestion(question)">通过</el-button>
              <el-button text type="danger" @click="removeQuestion(index)">删除</el-button>
            </div>
          </div>

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

          <el-input v-model="question.stem" type="textarea" :rows="3" placeholder="题干" />

          <div class="option-list">
            <div v-for="(option, optionIndex) in question.options" :key="optionIndex" class="option-row">
              <el-input v-model="option.key" class="option-key" />
              <el-input v-model="option.text" placeholder="选项内容" />
              <el-button text type="danger" @click="removeOption(question, optionIndex)">移除</el-button>
            </div>
            <el-button plain @click="addOption(question)">添加选项</el-button>
          </div>

          <el-input v-model="question.answer" placeholder="答案" />
          <el-input v-model="question.analysis" type="textarea" :rows="2" placeholder="解析" />
          <el-input v-model="question.reviewComment" placeholder="审核备注" />
        </article>
      </div>
    </section>
  </main>
</template>
