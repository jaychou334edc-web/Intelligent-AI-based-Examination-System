<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadInstance, type UploadProps } from 'element-plus'
import { importQuestions, parsePaper, type ReviewQuestion } from '../api/paperParsing'
import { uploadPaper, type Paper } from '../api/papers'

const router = useRouter()
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const currentPaper = ref<Paper | null>(null)
const questions = ref<ReviewQuestion[]>([])
const rawText = ref('')
const loading = ref(false)
const importLoading = ref(false)

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

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const lowerName = file.name.toLowerCase()
  if (!lowerName.endsWith('.docx') && !lowerName.endsWith('.txt')) {
    ElMessage.error('请上传 .docx 或 .txt 文件')
    return false
  }
  selectedFile.value = file
  return false
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
    questions.value = result.questions
    rawText.value = result.rawText
    ElMessage.success(`解析完成，共识别 ${result.questions.length} 道题`)
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
          <el-upload ref="uploadRef" drag :auto-upload="false" :limit="1" accept=".docx,.txt" :before-upload="beforeUpload">
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
        <h2>提取文本</h2>
        <el-input v-model="rawText" type="textarea" :rows="11" readonly placeholder="解析后显示文档提取结果" />
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
