<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Refresh, Tickets } from '@element-plus/icons-vue'
import { getStudentExams, type ExamSummary } from '../api/exams'
import AppEmptyState from '../components/AppEmptyState.vue'

const router = useRouter()
const exams = ref<ExamSummary[]>([])
const loading = ref(false)

async function loadExams() {
  loading.value = true
  try {
    exams.value = await getStudentExams()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试列表加载失败')
  } finally {
    loading.value = false
  }
}

function statusLabel(status?: string) {
  if (status === 'submitted') {
    return '已提交'
  }
  if (status === 'in_progress') {
    return '答题中'
  }
  return '未开始'
}

function runtimeStatus(exam: ExamSummary) {
  if (exam.submissionStatus === 'submitted') {
    return '已提交'
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
  return statusLabel(exam.submissionStatus)
}

function actionLabel(status?: string) {
  if (status === 'submitted') {
    return '查看考试'
  }
  if (status === 'in_progress') {
    return '继续答题'
  }
  return '进入考试'
}

function canEnter(exam: ExamSummary) {
  if (exam.submissionStatus === 'submitted') {
    return true
  }
  const now = Date.now()
  const start = exam.startTime ? new Date(exam.startTime).getTime() : null
  const end = exam.endTime ? new Date(exam.endTime).getTime() : null
  return (!start || now >= start) && (!end || now <= end)
}

function windowText(exam: ExamSummary) {
  if (!exam.startTime && !exam.endTime) {
    return '不限考试窗口'
  }
  return `${formatDate(exam.startTime)} - ${formatDate(exam.endTime)}`
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未设置'
}

onMounted(loadExams)
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">My Exams</p>
        <h1>考试列表</h1>
        <p class="summary">查看教师已发布的考试，进入后按题作答。选择题使用按钮作答，填空和主观题使用文本输入。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Back" @click="router.push('/student')">返回工作台</el-button>
        <el-button plain :icon="Refresh" @click="loadExams">刷新</el-button>
      </div>
    </header>

    <el-skeleton v-if="loading" :rows="5" animated />
    <AppEmptyState v-else-if="exams.length === 0">
      <template #icon>
        <Tickets />
      </template>
      <template #title>暂无已发布考试</template>
      教师发布考试并分配到你的班级后，会出现在这里。
    </AppEmptyState>

    <section v-else class="student-exam-grid">
      <article v-for="exam in exams" :key="exam.id" class="status-card student-exam-card">
        <div>
          <p class="eyebrow">{{ runtimeStatus(exam) }}</p>
          <h2>{{ exam.title }}</h2>
          <p class="summary">{{ exam.description || '无考试说明' }}</p>
        </div>
        <div class="bank-meta">
          <span>{{ exam.durationMinutes }} 分钟</span>
          <span>{{ exam.questionCount }} 题</span>
          <span>{{ exam.totalScore }} 分</span>
          <span>{{ exam.className || '全部学生' }}</span>
        </div>
        <p class="raw-hint">{{ windowText(exam) }}</p>
        <el-button type="primary" :icon="Tickets" :disabled="!canEnter(exam)" @click="router.push(`/student/exams/${exam.id}`)">
          {{ actionLabel(exam.submissionStatus) }}
        </el-button>
      </article>
    </section>
  </main>
</template>
