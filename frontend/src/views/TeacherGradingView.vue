<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTeacherSubmission,
  getTeacherSubmissions,
  gradeTeacherAnswer,
  suggestAiGrade,
  type GradingAnswer,
  type SubmissionGrading,
  type SubmissionSummary,
} from '../api/grading'

const router = useRouter()
const submissions = ref<SubmissionSummary[]>([])
const current = ref<SubmissionGrading | null>(null)
const loading = ref(false)
const grading = ref(false)
const suggesting = ref<Record<number, boolean>>({})
const draftScores = reactive<Record<number, number>>({})
const draftComments = reactive<Record<number, string>>({})

const typeLabels: Record<string, string> = {
  single_choice: '单选题',
  multiple_choice: '多选题',
  true_false: '判断题',
  fill_blank: '填空题',
  subjective: '主观/代码题',
}

async function loadSubmissions() {
  loading.value = true
  try {
    submissions.value = await getTeacherSubmissions()
    if (!current.value && submissions.value.length > 0) {
      await selectSubmission(submissions.value[0].submissionId)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '阅卷列表加载失败')
  } finally {
    loading.value = false
  }
}

async function selectSubmission(submissionId: number) {
  current.value = await getTeacherSubmission(submissionId)
  Object.keys(draftScores).forEach((key) => delete draftScores[Number(key)])
  Object.keys(draftComments).forEach((key) => delete draftComments[Number(key)])
  current.value.answers.forEach((answer) => {
    draftScores[answer.questionId] = answer.manualScore ?? answer.finalScore ?? 0
    draftComments[answer.questionId] = answer.teacherComment ?? ''
  })
}

function needsManual(answer: GradingAnswer) {
  return answer.gradingStatus === 'pending' || answer.questionType === 'fill_blank' || answer.questionType === 'subjective'
}

function canRequestAiSuggestion(answer: GradingAnswer) {
  return answer.gradingStatus === 'pending' || answer.questionType === 'fill_blank' || answer.questionType === 'subjective'
}

function gradingStatusLabel(answer: GradingAnswer) {
  if (answer.gradingStatus === 'auto_graded') {
    return answer.isCorrect == null ? '系统自动评分' : answer.isCorrect ? '系统判定正确' : '系统判定错误'
  }
  if (answer.gradingStatus === 'manual_graded') {
    return '教师已评分'
  }
  return '待人工复核'
}

function correctAnswerText(answer: GradingAnswer) {
  if (answer.correctAnswer) {
    return answer.correctAnswer
  }
  if (answer.gradingStatus === 'pending') {
    return '缺少标准答案，需教师人工复核'
  }
  return '人工判断'
}

async function saveGrade(answer: GradingAnswer) {
  if (!current.value) {
    return
  }
  grading.value = true
  try {
    current.value = await gradeTeacherAnswer(
      current.value.submissionId,
      answer.questionId,
      draftScores[answer.questionId] ?? 0,
      draftComments[answer.questionId] ?? '',
    )
    ElMessage.success('评分已保存')
    await loadSubmissions()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评分失败')
  } finally {
    grading.value = false
  }
}

async function requestAiSuggestion(answer: GradingAnswer) {
  if (!current.value) {
    return
  }
  suggesting.value = {
    ...suggesting.value,
    [answer.questionId]: true,
  }
  try {
    current.value = await suggestAiGrade(current.value.submissionId, answer.questionId)
    const updated = current.value.answers.find((item) => item.questionId === answer.questionId)
    if (updated?.aiSuggestionScore != null) {
      draftScores[answer.questionId] = updated.aiSuggestionScore
      draftComments[answer.questionId] = updated.aiComment || draftComments[answer.questionId] || ''
    }
    ElMessage.success('AI 评分建议已生成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 评分失败')
  } finally {
    suggesting.value = {
      ...suggesting.value,
      [answer.questionId]: false,
    }
  }
}

function applyAiSuggestion(answer: GradingAnswer) {
  if (answer.aiSuggestionScore == null) {
    ElMessage.warning('请先生成 AI 建议')
    return
  }
  draftScores[answer.questionId] = answer.aiSuggestionScore
  draftComments[answer.questionId] = answer.aiComment || ''
}

function scoreText(value?: number) {
  return value == null ? '未评分' : `${value} 分`
}

onMounted(loadSubmissions)
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Grading</p>
        <h1>阅卷管理</h1>
        <p class="summary">查看学生提交记录，客观题优先自动评分；缺少标准答案或无法判定时转入人工复核，教师可手动评分。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/teacher/dashboard')">返回工作台</el-button>
        <el-button plain @click="loadSubmissions">刷新</el-button>
      </div>
    </header>

    <section class="grading-layout">
      <article class="status-card grading-list-card">
        <p class="eyebrow">Submissions</p>
        <h2>提交记录</h2>
        <el-skeleton v-if="loading" :rows="5" animated />
        <el-empty v-else-if="submissions.length === 0" description="暂无提交记录" />
        <div v-else class="exam-list">
          <button
            v-for="submission in submissions"
            :key="submission.submissionId"
            :class="['exam-list-item', { active: current?.submissionId === submission.submissionId }]"
            type="button"
            @click="selectSubmission(submission.submissionId)"
          >
            <strong>{{ submission.examTitle }}</strong>
            <span>{{ submission.studentName }} · {{ scoreText(submission.totalScore) }} / {{ submission.maxScore }} 分</span>
          </button>
        </div>
      </article>

      <article class="status-card grading-detail-card">
        <p class="eyebrow">Review</p>
        <h2>答卷详情</h2>
        <el-empty v-if="!current" description="请选择提交记录" />
        <template v-else>
          <div class="exam-detail-meta">
            <strong>{{ current.examTitle }} · {{ current.studentName }}</strong>
            <span>总分：{{ scoreText(current.totalScore) }} / {{ current.maxScore }} 分</span>
          </div>

          <div class="grading-answer-list">
            <section v-for="answer in current.answers" :key="answer.questionId" class="grading-answer">
              <div class="question-editor-title">
                <strong>#{{ answer.questionId }} {{ typeLabels[answer.questionType] ?? answer.questionType }}</strong>
                <span>{{ scoreText(answer.finalScore) }} / {{ answer.maxScore }} 分</span>
              </div>
              <p class="grading-stem">{{ answer.stem }}</p>
              <div v-if="answer.options.length > 0" class="option-list">
                <div v-for="option in answer.options" :key="option.key" class="bank-option-row">
                  <strong>{{ option.key }}</strong>
                  <span>{{ option.text }}</span>
                </div>
              </div>
              <div class="bank-meta">
                <span>学生答案：{{ answer.studentAnswer || '未作答' }}</span>
                <span>标准答案：{{ correctAnswerText(answer) }}</span>
                <span>状态：{{ gradingStatusLabel(answer) }}</span>
              </div>

              <div v-if="needsManual(answer)" class="manual-grade-row">
                <el-input-number v-model="draftScores[answer.questionId]" :min="0" :max="answer.maxScore" :step="0.5" />
                <el-input v-model="draftComments[answer.questionId]" placeholder="教师评语" />
                <el-button type="primary" :loading="grading" @click="saveGrade(answer)">保存评分</el-button>
              </div>

              <div v-if="canRequestAiSuggestion(answer)" class="ai-grade-panel">
                <div class="ai-grade-heading">
                  <div>
                    <p class="eyebrow">AI Suggestion</p>
                    <h2>{{ answer.gradingStatus === 'pending' ? 'AI 辅助复核' : '主观题 AI 辅助评分' }}</h2>
                  </div>
                  <div class="header-actions">
                    <el-button plain :loading="suggesting[answer.questionId]" @click="requestAiSuggestion(answer)">
                      {{ answer.aiSuggestionScore == null ? '生成建议' : '重新评分' }}
                    </el-button>
                    <el-button type="success" plain :disabled="answer.aiSuggestionScore == null" @click="applyAiSuggestion(answer)">
                      采用建议
                    </el-button>
                  </div>
                </div>
                <el-empty v-if="answer.aiSuggestionScore == null" description="点击生成建议，AI 会给出参考分和理由" />
                <div v-else class="ai-grade-result">
                  <strong>{{ answer.aiSuggestionScore }} / {{ answer.maxScore }} 分</strong>
                  <p>{{ answer.aiComment }}</p>
                </div>
              </div>
            </section>
          </div>
        </template>
      </article>
    </section>
  </main>
</template>
