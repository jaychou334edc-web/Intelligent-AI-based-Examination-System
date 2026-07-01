<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteQuestion, getRecentQuestions, updateQuestion, type QuestionBankItem } from '../api/questions'
import { loadPaperImageUrl } from '../api/papers'
import type { ReviewQuestion } from '../api/paperParsing'

const router = useRouter()
const questions = ref<QuestionBankItem[]>([])
const loading = ref(false)
const saving = ref(false)
const imageUrls = ref<Record<string, string | null>>({})
const selectedIds = ref<number[]>([])
const previewQuestion = ref<QuestionBankItem | null>(null)
const previewVisible = computed({
  get: () => previewQuestion.value !== null,
  set: (value: boolean) => {
    if (!value) {
      previewQuestion.value = null
    }
  },
})
const editingQuestion = ref<QuestionBankItem | null>(null)
const editDialogVisible = ref(false)

const filters = reactive({
  keyword: '',
  types: [] as string[],
  knowledgePoint: '',
  sourcePaperId: '',
})

const editForm = reactive<ReviewQuestion>({
  questionType: 'single_choice',
  stem: '',
  options: [],
  answer: '',
  analysis: '',
  score: 2,
  knowledgePoint: '',
  difficulty: 'normal',
  reviewComment: '',
})

const questionTypes = [
  { label: '单选题', value: 'single_choice' },
  { label: '多选题', value: 'multiple_choice' },
  { label: '判断题', value: 'true_false' },
  { label: '填空题', value: 'fill_blank' },
  { label: '主观/代码题', value: 'subjective' },
]

const questionTypeLabels = Object.fromEntries(questionTypes.map((type) => [type.value, type.label]))

const knowledgePoints = computed(() => {
  const values = new Set<string>()
  questions.value.forEach((question) => {
    if (question.knowledgePoint) {
      values.add(question.knowledgePoint)
    }
  })
  return Array.from(values)
})

const sourcePaperIds = computed(() => {
  const values = new Set<number>()
  questions.value.forEach((question) => {
    if (question.sourcePaperId) {
      values.add(question.sourcePaperId)
    }
  })
  return Array.from(values).sort((a, b) => b - a)
})

const filteredQuestions = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  const sourcePaperId = filters.sourcePaperId ? Number(filters.sourcePaperId) : null
  return questions.value.filter((question) => {
    const matchesKeyword = !keyword
      || question.stem.toLowerCase().includes(keyword)
      || question.answer?.toLowerCase().includes(keyword)
      || question.analysis?.toLowerCase().includes(keyword)
    const matchesType = filters.types.length === 0 || filters.types.includes(question.questionType)
    const matchesKnowledge = !filters.knowledgePoint || question.knowledgePoint === filters.knowledgePoint
    const matchesSource = !sourcePaperId || question.sourcePaperId === sourcePaperId
    return matchesKeyword && matchesType && matchesKnowledge && matchesSource
  })
})

const bankStats = computed(() => ({
  total: questions.value.length,
  filtered: filteredQuestions.value.length,
  selected: selectedIds.value.length,
  images: questions.value.reduce((sum, question) => sum + imageIds(question.stem).length, 0),
}))

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

function imageIds(text: string) {
  return Array.from(text.matchAll(/\[IMG:([^\]]+)\]/g)).map((match) => match[1])
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

function formatDate(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function clearFilters() {
  filters.keyword = ''
  filters.types = []
  filters.knowledgePoint = ''
  filters.sourcePaperId = ''
}

function toggleSelection(questionId: number, checked: boolean) {
  if (checked && !selectedIds.value.includes(questionId)) {
    selectedIds.value = [...selectedIds.value, questionId]
  }
  if (!checked) {
    selectedIds.value = selectedIds.value.filter((id) => id !== questionId)
  }
}

function openPreview(question: QuestionBankItem) {
  previewQuestion.value = question
}

function openEdit(question: QuestionBankItem) {
  editingQuestion.value = question
  editForm.parsedQuestionId = undefined
  editForm.questionType = question.questionType
  editForm.stem = question.stem
  editForm.options = question.options.map((option) => ({ ...option }))
  editForm.answer = question.answer || ''
  editForm.analysis = question.analysis || ''
  editForm.score = Number(question.score)
  editForm.knowledgePoint = question.knowledgePoint || ''
  editForm.difficulty = question.difficulty || 'normal'
  editForm.reviewComment = '题库管理页编辑'
  editDialogVisible.value = true
}

function addOption() {
  const nextKey = String.fromCharCode(65 + editForm.options.length)
  editForm.options.push({ key: nextKey, text: '' })
}

function removeOption(index: number) {
  editForm.options.splice(index, 1)
}

async function saveEdit() {
  if (!editingQuestion.value) {
    return
  }
  saving.value = true
  try {
    await updateQuestion(editingQuestion.value.id, editForm)
    ElMessage.success('题目已更新')
    editDialogVisible.value = false
    await loadQuestions()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '题目保存失败')
  } finally {
    saving.value = false
  }
}

async function removeBankQuestion(question: QuestionBankItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除题目 #${question.id}？已被考试引用的题目会被系统阻止删除。`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteQuestion(question.id)
    ElMessage.success('题目已删除')
    await loadQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

async function goCreateExamWithSelection() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要加入考试的题目')
    return
  }
  sessionStorage.setItem('aes_selected_question_ids', JSON.stringify(selectedIds.value))
  ElMessage.success('已保存选题，进入考试管理后可继续组卷')
  await router.push('/teacher/exams')
}

async function loadQuestions() {
  loading.value = true
  try {
    clearImageUrls()
    questions.value = await getRecentQuestions(200)
    selectedIds.value = selectedIds.value.filter((id) => questions.value.some((question) => question.id === id))
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
  <main class="paper-page question-bank-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Question Bank</p>
        <h1>题库管理</h1>
        <p class="summary">按题型、知识点和来源试卷快速筛选题目，支持预览、编辑、删除和批量选题。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/teacher/papers')">AI 导入</el-button>
        <el-button plain @click="loadQuestions">刷新</el-button>
        <el-button plain @click="router.push('/teacher')">返回工作台</el-button>
      </div>
    </header>

    <section class="bank-stat-grid">
      <article class="status-card bank-stat-card">
        <span>题库总量</span>
        <strong>{{ bankStats.total }}</strong>
      </article>
      <article class="status-card bank-stat-card">
        <span>当前筛选</span>
        <strong>{{ bankStats.filtered }}</strong>
      </article>
      <article class="status-card bank-stat-card">
        <span>已选题目</span>
        <strong>{{ bankStats.selected }}</strong>
      </article>
      <article class="status-card bank-stat-card">
        <span>图片占位</span>
        <strong>{{ bankStats.images }}</strong>
      </article>
    </section>

    <section class="question-bank-layout">
      <aside class="status-card bank-filter-panel">
        <p class="eyebrow">Filters</p>
        <h2>筛选条件</h2>
        <el-form label-position="top">
          <el-form-item label="关键字">
            <el-input v-model="filters.keyword" placeholder="搜索题干、答案、解析" clearable />
          </el-form-item>
          <el-form-item label="题型">
            <el-checkbox-group v-model="filters.types" class="filter-check-list">
              <el-checkbox v-for="type in questionTypes" :key="type.value" :value="type.value">
                {{ type.label }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="知识点">
            <el-select v-model="filters.knowledgePoint" clearable placeholder="全部知识点">
              <el-option v-for="point in knowledgePoints" :key="point" :label="point" :value="point" />
            </el-select>
          </el-form-item>
          <el-form-item label="来源试卷">
            <el-select v-model="filters.sourcePaperId" clearable placeholder="全部来源">
              <el-option v-for="paperId in sourcePaperIds" :key="paperId" :label="`试卷 #${paperId}`" :value="String(paperId)" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="filter-actions">
          <el-button plain @click="clearFilters">清空筛选</el-button>
          <el-button type="primary" :disabled="selectedIds.length === 0" @click="goCreateExamWithSelection">批量加入考试</el-button>
        </div>
      </aside>

      <section class="status-card bank-table-panel">
        <div class="panel-title-row">
          <div>
            <p class="eyebrow">Questions</p>
            <h2>题目列表</h2>
          </div>
          <el-tag>{{ filteredQuestions.length }} 条</el-tag>
        </div>

        <el-table v-loading="loading" :data="filteredQuestions" empty-text="暂无题库数据" row-key="id">
          <el-table-column width="52">
            <template #default="{ row }">
              <el-checkbox :model-value="selectedIds.includes(row.id)" @change="(checked: boolean) => toggleSelection(row.id, checked)" />
            </template>
          </el-table-column>
          <el-table-column prop="id" label="ID" width="82" />
          <el-table-column label="题型" width="116">
            <template #default="{ row }">
              <el-tag size="small">{{ questionTypeLabels[row.questionType] ?? row.questionType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题干">
            <template #default="{ row }">
              <span class="table-stem">{{ row.stem }}</span>
            </template>
          </el-table-column>
          <el-table-column label="知识点" width="130">
            <template #default="{ row }">{{ row.knowledgePoint || '未设置' }}</template>
          </el-table-column>
          <el-table-column prop="score" label="分值" width="86" />
          <el-table-column label="来源" width="100">
            <template #default="{ row }">{{ row.sourcePaperId ? `#${row.sourcePaperId}` : '-' }}</template>
          </el-table-column>
          <el-table-column label="创建时间" width="150">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="178" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openPreview(row)">预览</el-button>
              <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button text type="danger" @click="removeBankQuestion(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </section>

    <el-drawer v-model="previewVisible" size="46%" title="题目预览">
      <template v-if="previewQuestion">
        <div class="preview-meta">
          <el-tag>{{ questionTypeLabels[previewQuestion.questionType] ?? previewQuestion.questionType }}</el-tag>
          <span>{{ previewQuestion.score }} 分</span>
          <span>知识点：{{ previewQuestion.knowledgePoint || '未设置' }}</span>
        </div>

        <div class="stem-preview">
          <template v-for="(part, partIndex) in stemParts(previewQuestion.stem)" :key="partIndex">
            <p v-if="part.type === 'text'">{{ part.value }}</p>
            <el-image
              v-else-if="imageUrl(previewQuestion.sourcePaperId, part.value)"
              :src="imageUrl(previewQuestion.sourcePaperId, part.value)"
              :preview-src-list="[imageUrl(previewQuestion.sourcePaperId, part.value)]"
              fit="contain"
            />
            <span v-else-if="!previewQuestion.sourcePaperId" class="image-loading">图片缺少试卷来源：{{ part.value }}</span>
            <span v-else-if="imageFailed(previewQuestion.sourcePaperId, part.value)" class="image-loading">图片加载失败：{{ part.value }}</span>
            <span v-else class="image-loading">图片加载中：{{ part.value }}</span>
          </template>
        </div>

        <div v-if="previewQuestion.options.length > 0" class="option-list">
          <div v-for="option in previewQuestion.options" :key="option.key" class="bank-option-row">
            <strong>{{ option.key }}</strong>
            <span>{{ option.text }}</span>
          </div>
        </div>

        <div class="bank-meta">
          <span>答案：{{ previewQuestion.answer || '未设置' }}</span>
          <span>难度：{{ previewQuestion.difficulty || 'normal' }}</span>
          <span>来源试卷：{{ previewQuestion.sourcePaperId ? `#${previewQuestion.sourcePaperId}` : '无' }}</span>
        </div>

        <p v-if="previewQuestion.analysis" class="bank-analysis">解析：{{ previewQuestion.analysis }}</p>
      </template>
    </el-drawer>

    <el-dialog v-model="editDialogVisible" title="编辑题目" width="760px">
      <el-form label-position="top">
        <div class="question-fields">
          <el-form-item label="题型">
            <el-select v-model="editForm.questionType">
              <el-option v-for="type in questionTypes" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="分值">
            <el-input-number v-model="editForm.score" :min="0.5" :step="0.5" />
          </el-form-item>
          <el-form-item label="知识点">
            <el-input v-model="editForm.knowledgePoint" />
          </el-form-item>
          <el-form-item label="难度">
            <el-input v-model="editForm.difficulty" />
          </el-form-item>
        </div>

        <el-form-item label="题干">
          <el-input v-model="editForm.stem" type="textarea" :rows="5" />
        </el-form-item>

        <el-form-item label="选项">
          <div class="option-list full-width">
            <div v-for="(option, optionIndex) in editForm.options" :key="optionIndex" class="option-row">
              <el-input v-model="option.key" class="option-key" />
              <el-input v-model="option.text" placeholder="选项内容" />
              <el-button text type="danger" @click="removeOption(optionIndex)">移除</el-button>
            </div>
            <el-button plain @click="addOption">添加选项</el-button>
          </div>
        </el-form-item>

        <el-form-item label="答案">
          <el-input v-model="editForm.answer" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="editForm.analysis" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>
