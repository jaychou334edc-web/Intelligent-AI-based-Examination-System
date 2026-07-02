<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTeacherExam,
  deleteTeacherExam,
  getTeacherExam,
  getTeacherExams,
  publishTeacherExam,
  updateTeacherExam,
  updateTeacherExamQuestions,
  type ExamDetail,
  type ExamSummary,
} from '../api/exams'
import { getRecentQuestions, type QuestionBankItem } from '../api/questions'
import { getAcademicClasses, type TeachingClass } from '../api/academic'

const router = useRouter()
const exams = ref<ExamSummary[]>([])
const questions = ref<QuestionBankItem[]>([])
const classes = ref<TeachingClass[]>([])
const selectedExam = ref<ExamDetail | null>(null)
const selectedQuestionIds = ref<number[]>([])
const loading = ref(false)
const saving = ref(false)
const pendingQuestionIds = ref<number[]>([])

const form = reactive({
  title: '本地测试考试',
  description: '用于机房本地测试的在线考试。',
  durationMinutes: 60,
  classId: undefined as number | undefined,
  startTime: '',
  endTime: '',
})

const questionTypeLabels: Record<string, string> = {
  single_choice: '单选',
  multiple_choice: '多选',
  true_false: '判断',
  fill_blank: '填空',
  subjective: '主观',
}

const canEditQuestions = computed(() => selectedExam.value?.status === 'draft')
const canArchive = computed(() => selectedExam.value?.status === 'published')
const canEditExamInfo = computed(() => !selectedExam.value || selectedExam.value.status === 'draft' || selectedExam.value.status === 'published')
const canEditAudience = computed(() => !selectedExam.value || selectedExam.value.status === 'draft')

async function loadAll() {
  loading.value = true
  try {
    hydratePendingQuestionIds()
    const [examList, questionList, classList] = await Promise.all([getTeacherExams(), getRecentQuestions(200), getAcademicClasses()])
    exams.value = examList
    questions.value = questionList
    classes.value = classList
    if (!selectedExam.value && examList.length > 0) {
      await selectExam(examList[0].id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试数据加载失败')
  } finally {
    loading.value = false
  }
}

function hydratePendingQuestionIds() {
  const raw = sessionStorage.getItem('aes_selected_question_ids')
  if (!raw) {
    return
  }
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      pendingQuestionIds.value = parsed.map(Number).filter((value) => Number.isFinite(value))
    }
  } catch {
    pendingQuestionIds.value = []
  }
  sessionStorage.removeItem('aes_selected_question_ids')
}

function syncForm(exam: ExamDetail) {
  form.title = exam.title
  form.description = exam.description ?? ''
  form.durationMinutes = exam.durationMinutes
  form.classId = exam.classId
  form.startTime = exam.startTime ?? ''
  form.endTime = exam.endTime ?? ''
}

function clearSelection() {
  selectedExam.value = null
  selectedQuestionIds.value = []
  form.title = '本地测试考试'
  form.description = '用于机房本地测试的在线考试。'
  form.durationMinutes = 60
  form.classId = undefined
  form.startTime = ''
  form.endTime = ''
}

function selectedClass() {
  return classes.value.find((item) => item.id === form.classId)
}

function examPayload() {
  const targetClass = selectedClass()
  return {
    title: form.title,
    description: form.description,
    durationMinutes: form.durationMinutes,
    courseId: targetClass?.courseId,
    classId: targetClass?.id,
    startTime: form.startTime || undefined,
    endTime: form.endTime || undefined,
  }
}

async function createExam() {
  saving.value = true
  try {
    selectedExam.value = await createTeacherExam(examPayload())
    selectedQuestionIds.value = pendingQuestionIds.value.length > 0 ? [...pendingQuestionIds.value] : []
    pendingQuestionIds.value = []
    ElMessage.success('考试草稿已创建')
    exams.value = await getTeacherExams()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建考试失败')
  } finally {
    saving.value = false
  }
}

async function selectExam(examId: number) {
  selectedExam.value = await getTeacherExam(examId)
  selectedQuestionIds.value = selectedExam.value.questions.map((question) => question.id)
  if (selectedExam.value.status === 'draft' && pendingQuestionIds.value.length > 0) {
    selectedQuestionIds.value = Array.from(new Set([...selectedQuestionIds.value, ...pendingQuestionIds.value]))
    pendingQuestionIds.value = []
    ElMessage.success('已带入题库页批量选择的题目，请保存组卷')
  }
  syncForm(selectedExam.value)
}

async function saveExamInfo() {
  if (!selectedExam.value) {
    return
  }
  saving.value = true
  try {
    selectedExam.value = await updateTeacherExam(selectedExam.value.id, examPayload())
    syncForm(selectedExam.value)
    ElMessage.success('考试信息已保存')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存考试信息失败')
  } finally {
    saving.value = false
  }
}

async function saveQuestions() {
  if (!selectedExam.value) {
    return
  }
  saving.value = true
  try {
    selectedExam.value = await updateTeacherExamQuestions(selectedExam.value.id, selectedQuestionIds.value)
    selectedQuestionIds.value = selectedExam.value.questions.map((question) => question.id)
    ElMessage.success('考试题目已保存')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存题目失败')
  } finally {
    saving.value = false
  }
}

async function publishExam() {
  if (!selectedExam.value) {
    return
  }
  try {
    await ElMessageBox.confirm('发布后学生即可进入考试，题目将锁定。确认发布吗？', '发布考试', {
      confirmButtonText: '发布',
      cancelButtonText: '取消',
      type: 'warning',
    })
    saving.value = true
    selectedExam.value = await publishTeacherExam(selectedExam.value.id)
    selectedQuestionIds.value = selectedExam.value.questions.map((question) => question.id)
    ElMessage.success('考试已发布')
    await loadAll()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

async function deleteOrArchiveExam() {
  if (!selectedExam.value) {
    return
  }
  const action = selectedExam.value.status === 'draft' ? '删除草稿' : '归档考试'
  try {
    await ElMessageBox.confirm(`${action}后将不再作为可编辑考试显示。确认继续吗？`, action, {
      confirmButtonText: action,
      cancelButtonText: '取消',
      type: 'warning',
    })
    saving.value = true
    await deleteTeacherExam(selectedExam.value.id)
    clearSelection()
    ElMessage.success(`${action}成功`)
    await loadAll()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

function statusLabel(status: string) {
  if (status === 'published') {
    return '已发布'
  }
  if (status === 'archived') {
    return '已归档'
  }
  return '草稿'
}

function examWindowLabel(exam: ExamSummary | ExamDetail) {
  if (!exam.startTime && !exam.endTime) {
    return '不限时间窗口'
  }
  return `${formatDateTime(exam.startTime)} - ${formatDateTime(exam.endTime)}`
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未设置'
}

function runtimeStatus(exam: ExamSummary | ExamDetail) {
  if (exam.status === 'draft') {
    return '草稿'
  }
  if (exam.status === 'archived') {
    return '已归档'
  }
  const now = Date.now()
  const start = exam.startTime ? new Date(exam.startTime).getTime() : null
  const end = exam.endTime ? new Date(exam.endTime).getTime() : null
  if (start && now < start) {
    return '未开始'
  }
  if (end && now > end) {
    return '已结束'
  }
  return '进行中'
}

function questionPreview(question: QuestionBankItem) {
  return question.stem.replace(/\[IMG:[^\]]+\]/g, '[图片]').slice(0, 90)
}

onMounted(loadAll)
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Exam Management</p>
        <h1>考试管理</h1>
        <p class="summary">创建和维护考试，草稿可调整组卷；已发布考试可继续修正名称、说明、时长和考试时间窗口。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/teacher/dashboard')">返回工作台</el-button>
        <el-button plain @click="clearSelection">新建模式</el-button>
        <el-button plain @click="loadAll">刷新</el-button>
      </div>
    </header>

    <section class="exam-layout">
      <article class="status-card exam-create-card">
        <p class="eyebrow">Exam</p>
        <h2>{{ selectedExam ? '考试信息' : '新建考试' }}</h2>
        <el-form label-position="top">
          <el-form-item label="考试名称">
            <el-input v-model="form.title" :disabled="!canEditExamInfo" />
          </el-form-item>
          <el-form-item label="考试说明">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              :disabled="!canEditExamInfo"
            />
          </el-form-item>
          <el-form-item label="时长（分钟）">
            <el-input-number
              v-model="form.durationMinutes"
              :min="1"
              :max="600"
              :disabled="!canEditExamInfo"
            />
          </el-form-item>
          <el-form-item label="目标班级">
            <el-select v-model="form.classId" clearable placeholder="不选择则发布给全部学生" :disabled="!canEditAudience">
              <el-option
                v-for="item in classes"
                :key="item.id"
                :label="`${item.courseName} · ${item.name}（${item.studentCount}人）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="可选"
              :disabled="!canEditExamInfo"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="可选"
              :disabled="!canEditExamInfo"
            />
          </el-form-item>
          <div class="action-list">
            <el-button v-if="!selectedExam" type="primary" :loading="saving" @click="createExam">创建草稿</el-button>
            <el-button v-else type="primary" :disabled="!canEditExamInfo" :loading="saving" @click="saveExamInfo">保存信息</el-button>
            <el-button v-if="selectedExam && selectedExam.status !== 'archived'" plain type="danger" :loading="saving" @click="deleteOrArchiveExam">
              {{ selectedExam.status === 'draft' ? '删除草稿' : '归档考试' }}
            </el-button>
          </div>
        </el-form>
      </article>

      <article class="status-card exam-list-card">
        <p class="eyebrow">Exams</p>
        <h2>考试列表</h2>
        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="exams.length === 0" description="暂无考试" />
        <div v-else class="exam-list">
          <button
            v-for="exam in exams"
            :key="exam.id"
            :class="['exam-list-item', { active: selectedExam?.id === exam.id }]"
            type="button"
            @click="selectExam(exam.id)"
          >
            <strong>{{ exam.title }}</strong>
            <span>{{ runtimeStatus(exam) }} · {{ exam.className || '全部学生' }} · {{ exam.questionCount }} 题 · {{ exam.totalScore }} 分</span>
            <span>{{ examWindowLabel(exam) }}</span>
          </button>
        </div>
      </article>

      <article class="status-card exam-detail-card">
        <p class="eyebrow">Paper</p>
        <h2>组卷与发布</h2>
        <el-empty v-if="!selectedExam" description="请选择或创建考试" />
        <template v-else>
          <div class="exam-detail-meta">
            <strong>{{ selectedExam.title }}</strong>
            <span>{{ runtimeStatus(selectedExam) }} · {{ selectedExam.className || '全部学生' }} · {{ selectedExam.durationMinutes }} 分钟 · {{ selectedExam.questionCount }} 题</span>
            <span>{{ examWindowLabel(selectedExam) }}</span>
          </div>

          <el-alert
            v-if="!canEditQuestions"
            title="考试已发布或归档，题目已锁定。"
            type="success"
            :closable="false"
            show-icon
          />

          <el-checkbox-group v-model="selectedQuestionIds" class="exam-question-picker" :disabled="!canEditQuestions">
            <el-checkbox v-for="question in questions" :key="question.id" :value="question.id">
              <span class="picker-question">
                <strong>#{{ question.id }} {{ questionTypeLabels[question.questionType] ?? question.questionType }}</strong>
                <span>{{ questionPreview(question) }}</span>
              </span>
            </el-checkbox>
          </el-checkbox-group>

          <div class="header-actions">
            <el-button :disabled="!canEditQuestions" :loading="saving" @click="saveQuestions">保存题目</el-button>
            <el-button type="primary" :disabled="!canEditQuestions" :loading="saving" @click="publishExam">发布考试</el-button>
            <el-button plain type="danger" :disabled="!canArchive" :loading="saving" @click="deleteOrArchiveExam">归档考试</el-button>
          </div>
        </template>
      </article>
    </section>
  </main>
</template>
