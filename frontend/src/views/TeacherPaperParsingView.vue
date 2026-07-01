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
  clearImageUrls()
}

function handleFileRemove() {
  selectedFile.value = null
  currentPaper.value = null
  questions.value = []
  rawText.value = ''
  clearImageUrls()
}

function removeQuestion(index: number) {
  questions.value.splice(index, 1)
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

async function uploadAndParse() {
  if (!selectedFile.value && !currentPaper.value) {
    ElMessage.warning('请先选择题库源文件')
    return
  }

  loading.value = true
  try {
    if (selectedFile.value) {
      currentPaper.value = await uploadPaper(selectedFile.value, form.title)
    }
    const result = await parsePaper(currentPaper.value!.id)
    clearImageUrls()
    questions.value = result.questions
    rawText.value = result.rawText
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
  } finally {
    importLoading.value = false
  }
}
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">AI Import</p>
        <h1>AI 题库导入</h1>
        <p class="summary">上传 txt 或 Word 题目文件，系统提取内容并生成可审核的结构化题目。</p>
      </div>
      <el-button plain @click="router.push('/teacher')">返回工作台</el-button>
    </header>

    <section class="tool-grid">
      <article class="status-card upload-card">
        <p class="eyebrow">Upload</p>
        <h2>上传题目文件</h2>
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
            上传并解析
          </el-button>
        </el-form>
      </article>

      <article class="status-card raw-card">
        <p class="eyebrow">Extraction</p>
        <h2>结构预览</h2>
        <p class="raw-hint">这里显示文档提取出的文字和图片占位。最终导入以逐题审核卡片为准。</p>
        <el-input v-model="rawText" type="textarea" :rows="9" readonly placeholder="解析后显示文档结构预览" />
      </article>
    </section>

    <section class="review-section">
      <div class="review-heading">
        <div>
          <p class="eyebrow">Review</p>
          <h2>教师审核</h2>
        </div>
        <el-button type="success" :loading="importLoading" :disabled="!canImport" @click="submitImport">
          确认导入题库
        </el-button>
      </div>

      <el-empty v-if="questions.length === 0" description="解析后在这里审核题目" />

      <div v-else class="question-list">
        <article v-for="(question, index) in questions" :key="question.parsedQuestionId ?? index" class="question-editor">
          <div class="question-editor-title">
            <strong>第 {{ index + 1 }} 题</strong>
            <el-button text type="danger" @click="removeQuestion(index)">删除</el-button>
          </div>

          <div class="question-fields">
            <el-select v-model="question.questionType" placeholder="题型">
              <el-option v-for="type in questionTypes" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
            <el-input-number v-model="question.score" :min="0.5" :step="0.5" />
            <el-input v-model="question.knowledgePoint" placeholder="知识点" />
            <el-input v-model="question.difficulty" placeholder="难度" />
          </div>

          <div class="answer-control-hint">{{ answerControlLabel(question.questionType) }}</div>

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
