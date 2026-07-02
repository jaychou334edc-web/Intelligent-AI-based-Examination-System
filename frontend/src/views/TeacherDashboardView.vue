<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import DashboardStatCard from '../components/dashboard/DashboardStatCard.vue'
import QuickActionPanel from '../components/dashboard/QuickActionPanel.vue'
import RecentExamTable from '../components/dashboard/RecentExamTable.vue'
import TaskStreamList from '../components/dashboard/TaskStreamList.vue'
import type { DashboardTask } from '../components/dashboard/types'
import { getTeacherExams, type ExamSummary } from '../api/exams'
import { getTeacherSubmissions, type SubmissionSummary } from '../api/grading'
import { getRecentQuestions, type QuestionBankItem } from '../api/questions'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const exams = ref<ExamSummary[]>([])
const questions = ref<QuestionBankItem[]>([])
const submissions = ref<SubmissionSummary[]>([])
const loading = ref(false)

const pendingGradingCount = computed(() => submissions.value.filter((item) => item.totalScore == null).length)
const runningExams = computed(() => exams.value.filter((exam) => exam.status === 'published').length)
const draftExams = computed(() => exams.value.filter((exam) => exam.status === 'draft').length)
const recentExams = computed(() => exams.value.slice(0, 6))
const pendingAiCount = computed(() => Math.max(0, questions.value.filter((question) => !question.answer).length))

const tasks = computed<DashboardTask[]>(() => {
  const items: DashboardTask[] = []
  if (pendingAiCount.value > 0) {
    items.push({
      id: 'question-answer-check',
      type: 'question',
      title: `题库中有 ${pendingAiCount.value} 道题缺少标准答案`,
      meta: '建议发布考试前完成答案核对',
      target: '/teacher/questions',
    })
  }
  if (pendingGradingCount.value > 0) {
    items.push({
      id: 'grading',
      type: 'grading',
      title: `阅卷任务：${pendingGradingCount.value} 份答卷待处理`,
      meta: '包含主观题或缺标准答案题目',
      target: '/teacher/grading',
    })
  }
  if (runningExams.value > 0) {
    items.push({
      id: 'running-exams',
      type: 'exam',
      title: `进行中考试：${runningExams.value} 场`,
      meta: '可查看监控事件和成绩统计',
      target: '/teacher/monitoring',
    })
  }
  items.push({
    id: 'ai-import',
    type: 'ai',
    title: 'AI导入：上传 Word 或 txt 生成结构化题目',
    meta: '三阶段流程：上传、解析、审核入库',
    target: '/teacher/ai-import',
  })
  return items
})

async function loadDashboard() {
  loading.value = true
  try {
    const [examList, questionList, submissionList] = await Promise.all([
      getTeacherExams(),
      getRecentQuestions(300),
      getTeacherSubmissions(),
    ])
    exams.value = examList
    questions.value = questionList
    submissions.value = submissionList
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '教师工作台加载失败')
  } finally {
    loading.value = false
  }
}

async function handleLogout() {
  await auth.logout()
  await router.push('/login')
}

onMounted(loadDashboard)
</script>

<template>
  <main class="teacher-dashboard-page">
    <header class="teacher-dashboard-header">
      <div>
        <p class="eyebrow">Teacher Dashboard</p>
        <h1>教师工作台</h1>
        <p class="summary">按教学任务组织 AI 导入、题库维护、考试发布、阅卷与成绩分析。</p>
      </div>
      <div class="teacher-dashboard-user">
        <strong>{{ auth.user?.realName || '教师' }}</strong>
        <span>{{ auth.user?.username }} · 教师</span>
        <el-button plain :icon="SwitchButton" @click="handleLogout">退出</el-button>
      </div>
    </header>

    <section class="dashboard-stat-grid" v-loading="loading">
      <DashboardStatCard label="待处理AI导入" :value="pendingAiCount" caption="缺答案/待核对题目" tone="blue" />
      <DashboardStatCard label="待阅卷数量" :value="pendingGradingCount" caption="份答卷待确认" tone="amber" />
      <DashboardStatCard label="进行中考试" :value="runningExams" caption="场已发布考试" tone="green" />
      <DashboardStatCard label="题库总量" :value="questions.length" caption="道可用题目" tone="blue" />
    </section>

    <section class="dashboard-main-grid">
      <TaskStreamList :tasks="tasks" />
      <QuickActionPanel />
    </section>

    <RecentExamTable :exams="recentExams" />

    <section class="dashboard-main-grid compact">
      <article class="status-card dashboard-panel">
        <p class="eyebrow">Question Bank</p>
        <h2>题库维护提醒</h2>
        <div class="task-stream">
          <router-link v-for="question in questions.slice(0, 5)" :key="question.id" to="/teacher/questions" class="task-stream-item">
            <span class="task-index">#{{ question.id }}</span>
            <strong>{{ question.knowledgePoint || '未设置知识点' }}</strong>
            <em>{{ question.stem.replace(/\[IMG:[^\]]+\]/g, '[图片]').slice(0, 80) }}</em>
          </router-link>
        </div>
      </article>

      <article class="status-card dashboard-panel">
        <p class="eyebrow">Release Readiness</p>
        <h2>发布准备度</h2>
        <div class="readiness-list">
          <div>
            <span>草稿考试</span>
            <strong>{{ draftExams }}</strong>
          </div>
          <div>
            <span>已发布考试</span>
            <strong>{{ runningExams }}</strong>
          </div>
          <div>
            <span>最近提交</span>
            <strong>{{ submissions.length }}</strong>
          </div>
        </div>
      </article>
    </section>
  </main>
</template>
