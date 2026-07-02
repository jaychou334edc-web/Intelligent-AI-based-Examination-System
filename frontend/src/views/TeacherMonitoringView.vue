<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTeacherExams, type ExamSummary } from '../api/exams'
import {
  getTeacherExamAnalytics,
  getTeacherExamEvents,
  type AntiCheatEvent,
  type ExamAnalytics,
} from '../api/monitoring'

const router = useRouter()
const exams = ref<ExamSummary[]>([])
const currentExam = ref<ExamSummary | null>(null)
const analytics = ref<ExamAnalytics | null>(null)
const events = ref<AntiCheatEvent[]>([])
const loading = ref(false)

const eventLabels: Record<string, string> = {
  browser_blur: '窗口失焦',
  tab_hidden: '标签页隐藏',
  fullscreen_exit: '退出全屏',
  copy_attempt: '复制尝试',
  paste_attempt: '粘贴尝试',
  page_refresh: '页面刷新',
  abnormal_disconnect: '异常断开',
  network_offline: '网络离线',
  network_online: '网络恢复',
  repeated_submit: '重复提交',
}

const questionTypeLabels: Record<string, string> = {
  single_choice: '单选',
  multiple_choice: '多选',
  true_false: '判断',
  fill_blank: '填空',
  subjective: '主观',
}

async function loadExams() {
  loading.value = true
  try {
    exams.value = await getTeacherExams()
    if (!currentExam.value && exams.value.length > 0) {
      await selectExam(exams.value[0])
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '监控数据加载失败')
  } finally {
    loading.value = false
  }
}

async function selectExam(exam: ExamSummary) {
  currentExam.value = exam
  const [nextAnalytics, nextEvents] = await Promise.all([
    getTeacherExamAnalytics(exam.id),
    getTeacherExamEvents(exam.id),
  ])
  analytics.value = nextAnalytics
  events.value = nextEvents
}

function eventName(type: string) {
  return eventLabels[type] ?? type
}

function percentText(value?: number) {
  return `${(value ?? 0).toFixed(2)}%`
}

function scoreText(value?: number) {
  return `${(value ?? 0).toFixed(2)}`
}

function previewStem(stem: string) {
  return stem.replace(/\[IMG:[^\]]+\]/g, '[图片]').slice(0, 90)
}

function eventDataText(event: AntiCheatEvent) {
  const entries = Object.entries(event.eventData ?? {})
  if (entries.length === 0) {
    return '无附加数据'
  }
  return entries.map(([key, value]) => `${key}: ${String(value)}`).join('，')
}

onMounted(loadExams)
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Monitoring</p>
        <h1>监考与分析</h1>
        <p class="summary">查看考试行为时间线、成绩分布、题目得分率、难度与知识点统计。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/teacher/dashboard')">返回工作台</el-button>
        <el-button plain @click="loadExams">刷新</el-button>
      </div>
    </header>

    <section class="monitoring-layout">
      <article class="status-card grading-list-card">
        <p class="eyebrow">Exams</p>
        <h2>考试列表</h2>
        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="exams.length === 0" description="暂无考试" />
        <div v-else class="exam-list">
          <button
            v-for="exam in exams"
            :key="exam.id"
            :class="['exam-list-item', { active: currentExam?.id === exam.id }]"
            type="button"
            @click="selectExam(exam)"
          >
            <strong>{{ exam.title }}</strong>
            <span>{{ exam.status }} · {{ exam.questionCount }} 题 · {{ exam.totalScore }} 分</span>
          </button>
        </div>
      </article>

      <section class="monitoring-detail">
        <el-empty v-if="!analytics" description="请选择考试" />
        <template v-else>
          <div class="monitoring-metrics">
            <article class="status-card metric-card">
              <p class="eyebrow">Submitted</p>
              <h2>{{ analytics.submittedCount }} / {{ analytics.participantCount }}</h2>
              <span>提交人数 / 参考人数</span>
            </article>
            <article class="status-card metric-card">
              <p class="eyebrow">Average</p>
              <h2>{{ scoreText(analytics.averageScore) }}</h2>
              <span>平均分</span>
            </article>
            <article class="status-card metric-card">
              <p class="eyebrow">Pass Rate</p>
              <h2>{{ percentText(analytics.passRate) }}</h2>
              <span>及格率</span>
            </article>
            <article class="status-card metric-card">
              <p class="eyebrow">Events</p>
              <h2>{{ events.length }}</h2>
              <span>行为事件</span>
            </article>
          </div>

          <div class="monitoring-grid">
            <article class="status-card">
              <p class="eyebrow">Score</p>
              <h2>成绩分布</h2>
              <div class="stat-list">
                <div v-for="bucket in analytics.scoreDistribution" :key="bucket.label" class="stat-row">
                  <span>{{ bucket.label }}</span>
                  <strong>{{ bucket.count }} 人</strong>
                </div>
              </div>
            </article>

            <article class="status-card">
              <p class="eyebrow">Events</p>
              <h2>行为统计</h2>
              <div class="stat-list">
                <div v-for="item in analytics.eventCounts" :key="item.eventType" class="stat-row">
                  <span>{{ eventName(item.eventType) }}</span>
                  <strong>{{ item.count }} 次</strong>
                </div>
                <el-empty v-if="analytics.eventCounts.length === 0" description="暂无行为事件" />
              </div>
            </article>
          </div>

          <article class="status-card">
            <p class="eyebrow">Timeline</p>
            <h2>行为时间线</h2>
            <el-empty v-if="events.length === 0" description="暂无行为事件" />
            <div v-else class="timeline-list">
              <div v-for="event in events" :key="event.id" class="timeline-item">
                <strong>{{ event.studentName }} · {{ eventName(event.eventType) }}</strong>
                <span>{{ event.eventLevel }} · {{ event.createdAt }}</span>
                <p>{{ eventDataText(event) }}</p>
              </div>
            </div>
          </article>

          <article class="status-card">
            <p class="eyebrow">Questions</p>
            <h2>题目得分率</h2>
            <div class="accuracy-list">
              <div v-for="item in analytics.questionAccuracy" :key="item.questionId" class="accuracy-row">
                <strong>#{{ item.questionId }} {{ questionTypeLabels[item.questionType] ?? item.questionType }}</strong>
                <span>{{ previewStem(item.stem) }}</span>
                <em>{{ scoreText(item.averageScore) }} / {{ item.maxScore }} · {{ percentText(item.accuracyRate) }}</em>
              </div>
            </div>
          </article>

          <div class="monitoring-grid">
            <article class="status-card">
              <p class="eyebrow">Difficulty</p>
              <h2>难度分析</h2>
              <div class="stat-list">
                <div v-for="item in analytics.difficultyStats" :key="item.difficulty" class="stat-row">
                  <span>{{ item.difficulty }} · {{ item.questionCount }} 题</span>
                  <strong>{{ percentText(item.accuracyRate) }}</strong>
                </div>
              </div>
            </article>

            <article class="status-card">
              <p class="eyebrow">Knowledge</p>
              <h2>知识点分析</h2>
              <div class="stat-list">
                <div v-for="item in analytics.knowledgePointStats" :key="item.knowledgePoint" class="stat-row">
                  <span>{{ item.knowledgePoint }} · {{ item.questionCount }} 题</span>
                  <strong>{{ percentText(item.accuracyRate) }}</strong>
                </div>
              </div>
            </article>
          </div>
        </template>
      </section>
    </section>
  </main>
</template>
