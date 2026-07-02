<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Back, Refresh, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppEmptyState from '../components/AppEmptyState.vue'
import { getTeacherExams, type ExamSummary } from '../api/exams'
import { getTeacherExamAnalytics, type ExamAnalytics } from '../api/monitoring'

const router = useRouter()
const exams = ref<ExamSummary[]>([])
const currentExam = ref<ExamSummary | null>(null)
const analytics = ref<ExamAnalytics | null>(null)
const loading = ref(false)
const analyticsLoading = ref(false)

const publishedExams = computed(() => exams.value.filter((exam) => exam.status !== 'draft'))
const topQuestionAccuracy = computed(() => analytics.value?.questionAccuracy.slice(0, 8) ?? [])
const passRateLevel = computed(() => {
  const passRate = analytics.value?.passRate ?? 0
  if (passRate >= 80) {
    return '优秀'
  }
  if (passRate >= 60) {
    return '正常'
  }
  return '需关注'
})

async function loadExams() {
  loading.value = true
  try {
    exams.value = await getTeacherExams()
    const first = publishedExams.value[0] ?? exams.value[0]
    if (!currentExam.value && first) {
      await selectExam(first)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '成绩统计加载失败')
  } finally {
    loading.value = false
  }
}

async function selectExam(exam: ExamSummary) {
  currentExam.value = exam
  analyticsLoading.value = true
  try {
    analytics.value = await getTeacherExamAnalytics(exam.id)
  } catch (error) {
    analytics.value = null
    ElMessage.error(error instanceof Error ? error.message : '考试分析加载失败')
  } finally {
    analyticsLoading.value = false
  }
}

function scoreText(value?: number) {
  return (value ?? 0).toFixed(1)
}

function percentText(value?: number) {
  return `${(value ?? 0).toFixed(1)}%`
}

function barWidth(value?: number) {
  return `${Math.max(4, Math.min(value ?? 0, 100))}%`
}

function previewStem(stem: string) {
  return stem.replace(/\[IMG:[^\]]+\]/g, '[图片]').slice(0, 76)
}

onMounted(loadExams)
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Analytics</p>
        <h1>成绩统计</h1>
        <p class="summary">按考试查看提交情况、成绩分布、及格率、知识点掌握情况和题目得分率。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Back" @click="router.push('/teacher/dashboard')">返回工作台</el-button>
        <el-button plain :icon="Refresh" @click="loadExams">刷新</el-button>
      </div>
    </header>

    <section class="analytics-layout">
      <article class="status-card grading-list-card">
        <p class="eyebrow">Exams</p>
        <h2>考试范围</h2>
        <el-skeleton v-if="loading" :rows="5" animated />
        <AppEmptyState v-else-if="exams.length === 0">
          <template #icon>
            <TrendCharts />
          </template>
          <template #title>暂无考试数据</template>
          创建并发布考试后，这里会展示班级成绩与题目分析。
          <template #action>
            <el-button type="primary" @click="router.push('/teacher/exams')">创建考试</el-button>
          </template>
        </AppEmptyState>
        <div v-else class="exam-list">
          <button
            v-for="exam in exams"
            :key="exam.id"
            :class="['exam-list-item', { active: currentExam?.id === exam.id }]"
            type="button"
            @click="selectExam(exam)"
          >
            <strong>{{ exam.title }}</strong>
            <span>{{ exam.className || '全部学生' }} · {{ exam.questionCount }} 题 · {{ exam.totalScore }} 分</span>
          </button>
        </div>
      </article>

      <section class="analytics-detail">
        <el-skeleton v-if="analyticsLoading" :rows="8" animated />
        <AppEmptyState v-else-if="!analytics">
          <template #icon>
            <TrendCharts />
          </template>
          <template #title>请选择考试</template>
          选择左侧考试后查看成绩统计和知识点分析。
        </AppEmptyState>
        <template v-else>
          <section class="analytics-metric-grid">
            <article class="status-card metric-card">
              <p class="eyebrow">Participants</p>
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
              <span>{{ passRateLevel }}</span>
            </article>
            <article class="status-card metric-card">
              <p class="eyebrow">Range</p>
              <h2>{{ scoreText(analytics.minScore) }} - {{ scoreText(analytics.maxScore) }}</h2>
              <span>最低分 - 最高分</span>
            </article>
          </section>

          <section class="analytics-grid">
            <article class="status-card">
              <div class="panel-title-row">
                <div>
                  <p class="eyebrow">Distribution</p>
                  <h2>成绩分布</h2>
                </div>
                <span class="soft-badge">{{ analytics.examTitle }}</span>
              </div>
              <div class="bar-list">
                <div v-for="bucket in analytics.scoreDistribution" :key="bucket.label" class="bar-row">
                  <span>{{ bucket.label }}</span>
                  <div class="bar-track">
                    <i :style="{ width: barWidth(analytics.participantCount ? (bucket.count / analytics.participantCount) * 100 : 0) }" />
                  </div>
                  <strong>{{ bucket.count }} 人</strong>
                </div>
              </div>
            </article>

            <article class="status-card">
              <p class="eyebrow">Knowledge</p>
              <h2>知识点掌握</h2>
              <div class="bar-list">
                <div v-for="item in analytics.knowledgePointStats" :key="item.knowledgePoint" class="bar-row">
                  <span>{{ item.knowledgePoint }}</span>
                  <div class="bar-track success">
                    <i :style="{ width: barWidth(item.accuracyRate) }" />
                  </div>
                  <strong>{{ percentText(item.accuracyRate) }}</strong>
                </div>
                <AppEmptyState v-if="analytics.knowledgePointStats.length === 0">
                  <template #title>暂无知识点数据</template>
                  为题目维护知识点后，这里会展示掌握情况。
                </AppEmptyState>
              </div>
            </article>
          </section>

          <article class="status-card">
            <div class="panel-title-row">
              <div>
                <p class="eyebrow">Questions</p>
                <h2>题目得分率</h2>
              </div>
              <span class="soft-badge">前 {{ topQuestionAccuracy.length }} 题</span>
            </div>
            <div class="accuracy-table">
              <div v-for="item in topQuestionAccuracy" :key="item.questionId" class="accuracy-table-row">
                <strong>#{{ item.questionId }}</strong>
                <span>{{ previewStem(item.stem) }}</span>
                <em>{{ scoreText(item.averageScore) }} / {{ item.maxScore }}</em>
                <div class="bar-track compact">
                  <i :style="{ width: barWidth(item.accuracyRate) }" />
                </div>
                <b>{{ percentText(item.accuracyRate) }}</b>
              </div>
            </div>
          </article>
        </template>
      </section>
    </section>
  </main>
</template>
