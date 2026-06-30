<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTeacherExam,
  getTeacherExam,
  getTeacherExams,
  publishTeacherExam,
  updateTeacherExamQuestions,
  type ExamDetail,
  type ExamSummary,
} from '../api/exams'
import { getRecentQuestions, type QuestionBankItem } from '../api/questions'

const router = useRouter()
const exams = ref<ExamSummary[]>([])
const questions = ref<QuestionBankItem[]>([])
const selectedExam = ref<ExamDetail | null>(null)
const selectedQuestionIds = ref<number[]>([])
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  title: '本地测试考试',
  description: '用于机房本地测试的在线考试。',
  durationMinutes: 60,
})

const questionTypeLabels: Record<string, string> = {
  single_choice: '单选',
  multiple_choice: '多选',
  true_false: '判断',
  fill_blank: '填空',
  subjective: '主观',
}

const canEditQuestions = computed(() => selectedExam.value?.status === 'draft')

async function loadAll() {
  loading.value = true
  try {
    const [examList, questionList] = await Promise.all([getTeacherExams(), getRecentQuestions(200)])
    exams.value = examList
    questions.value = questionList
    if (!selectedExam.value && examList.length > 0) {
      await selectExam(examList[0].id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试数据加载失败')
  } finally {
    loading.value = false
  }
}

async function createExam() {
  saving.value = true
  try {
    selectedExam.value = await createTeacherExam({ ...form })
    selectedQuestionIds.value = []
    ElMessage.success('考试草稿已创建')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建考试失败')
  } finally {
    saving.value = false
  }
}

async function selectExam(examId: number) {
  selectedExam.value = await getTeacherExam(examId)
  selectedQuestionIds.value = selectedExam.value.questions.map((question) => question.id)
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

function statusLabel(status: string) {
  return status === 'published' ? '已发布' : '草稿'
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
        <p class="summary">创建考试草稿，从题库选题并发布给学生。Phase 3 重点先完成考试发布与在线作答闭环。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/teacher')">返回工作台</el-button>
        <el-button plain @click="loadAll">刷新</el-button>
      </div>
    </header>

    <section class="exam-layout">
      <article class="status-card exam-create-card">
        <p class="eyebrow">Create</p>
        <h2>新建考试</h2>
        <el-form label-position="top">
          <el-form-item label="考试名称">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="考试说明">
            <el-input v-model="form.description" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="时长（分钟）">
            <el-input-number v-model="form.durationMinutes" :min="1" :max="600" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="createExam">创建草稿</el-button>
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
            <span>{{ statusLabel(exam.status) }} · {{ exam.questionCount }} 题 · {{ exam.totalScore }} 分</span>
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
            <span>{{ statusLabel(selectedExam.status) }} · {{ selectedExam.durationMinutes }} 分钟 · {{ selectedExam.questionCount }} 题</span>
          </div>

          <el-alert
            v-if="!canEditQuestions"
            title="考试已发布，题目已锁定。"
            type="success"
            :closable="false"
            show-icon
          />

          <el-checkbox-group v-model="selectedQuestionIds" class="exam-question-picker" :disabled="!canEditQuestions">
            <el-checkbox v-for="question in questions" :key="question.id" :label="question.id">
              <span class="picker-question">
                <strong>#{{ question.id }} {{ questionTypeLabels[question.questionType] ?? question.questionType }}</strong>
                <span>{{ questionPreview(question) }}</span>
              </span>
            </el-checkbox>
          </el-checkbox-group>

          <div class="header-actions">
            <el-button :disabled="!canEditQuestions" :loading="saving" @click="saveQuestions">保存题目</el-button>
            <el-button type="primary" :disabled="!canEditQuestions" :loading="saving" @click="publishExam">发布考试</el-button>
          </div>
        </template>
      </article>
    </section>
  </main>
</template>
