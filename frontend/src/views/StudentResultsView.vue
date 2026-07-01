<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Reading, Refresh } from '@element-plus/icons-vue'
import { getStudentResult, getStudentResults, type SubmissionGrading, type SubmissionSummary } from '../api/grading'
import AppEmptyState from '../components/AppEmptyState.vue'

const router = useRouter()
const results = ref<SubmissionSummary[]>([])
const current = ref<SubmissionGrading | null>(null)
const loading = ref(false)

async function loadResults() {
  loading.value = true
  try {
    results.value = await getStudentResults()
    if (!current.value && results.value.length > 0) {
      await selectResult(results.value[0].submissionId)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '成绩加载失败')
  } finally {
    loading.value = false
  }
}

async function selectResult(submissionId: number) {
  current.value = await getStudentResult(submissionId)
}

function scoreText(value?: number) {
  return value == null ? '待评分' : `${value} 分`
}

onMounted(loadResults)
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Results</p>
        <h1>成绩查询</h1>
        <p class="summary">查看已提交考试的总分和每题得分。主观题需要教师阅卷后才会计入最终成绩。</p>
      </div>
      <div class="header-actions">
        <el-button plain :icon="Back" @click="router.push('/student')">返回工作台</el-button>
        <el-button plain :icon="Refresh" @click="loadResults">刷新</el-button>
      </div>
    </header>

    <section class="grading-layout">
      <article class="status-card grading-list-card">
        <p class="eyebrow">My Scores</p>
        <h2>成绩列表</h2>
        <el-skeleton v-if="loading" :rows="5" animated />
        <AppEmptyState v-else-if="results.length === 0">
          <template #icon>
            <Reading />
          </template>
          <template #title>暂无成绩</template>
          提交考试并完成阅卷后，成绩会展示在这里。
        </AppEmptyState>
        <div v-else class="exam-list">
          <button
            v-for="result in results"
            :key="result.submissionId"
            :class="['exam-list-item', { active: current?.submissionId === result.submissionId }]"
            type="button"
            @click="selectResult(result.submissionId)"
          >
            <strong>{{ result.examTitle }}</strong>
            <span>{{ scoreText(result.totalScore) }} / {{ result.maxScore }} 分</span>
          </button>
        </div>
      </article>

      <article class="status-card grading-detail-card">
        <p class="eyebrow">Detail</p>
        <h2>成绩详情</h2>
        <AppEmptyState v-if="!current">
          <template #icon>
            <Reading />
          </template>
          <template #title>请选择成绩记录</template>
          从左侧列表选择一场考试，查看每题得分和教师评语。
        </AppEmptyState>
        <template v-else>
          <div class="exam-detail-meta">
            <strong>{{ current.examTitle }}</strong>
            <span>总分：{{ scoreText(current.totalScore) }} / {{ current.maxScore }} 分</span>
          </div>
          <div class="grading-answer-list">
            <section v-for="answer in current.answers" :key="answer.questionId" class="grading-answer">
              <div class="question-editor-title">
                <strong>#{{ answer.questionId }}</strong>
                <span>{{ scoreText(answer.finalScore) }} / {{ answer.maxScore }} 分</span>
              </div>
              <p class="grading-stem">{{ answer.stem }}</p>
              <div class="bank-meta">
                <span>我的答案：{{ answer.studentAnswer || '未作答' }}</span>
                <span>标准答案：{{ answer.correctAnswer || '教师评阅' }}</span>
                <span>状态：{{ answer.gradingStatus }}</span>
              </div>
              <p v-if="answer.teacherComment" class="bank-analysis">教师评语：{{ answer.teacherComment }}</p>
            </section>
          </div>
        </template>
      </article>
    </section>
  </main>
</template>
