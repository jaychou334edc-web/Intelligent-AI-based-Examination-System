<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStudentExam, saveStudentAnswer, submitStudentExam, type ExamDetail, type ExamQuestion } from '../api/exams'
import { recordAntiCheatEvent, type AntiCheatEventPayload } from '../api/monitoring'
import { loadPaperImageUrl } from '../api/papers'

const props = defineProps<{
  examId: number
}>()

const router = useRouter()
const exam = ref<ExamDetail | null>(null)
const currentIndex = ref(0)
const answers = ref<Record<number, string>>({})
const saving = ref(false)
const loading = ref(false)
const imageUrls = ref<Record<string, string>>({})
const now = ref(Date.now())
let timerId: number | undefined
let autoSaveId: number | undefined
let lastHiddenAt = 0
let eventQueue = Promise.resolve()

const currentQuestion = computed(() => exam.value?.questions[currentIndex.value] ?? null)
const isSubmitted = computed(() => exam.value?.submissionStatus === 'submitted')
const remainingSeconds = computed(() => {
  if (!exam.value?.submissionStartedAt) {
    return exam.value ? exam.value.durationMinutes * 60 : 0
  }
  const startedAt = new Date(exam.value.submissionStartedAt).getTime()
  const endsAt = startedAt + exam.value.durationMinutes * 60 * 1000
  return Math.max(0, Math.floor((endsAt - now.value) / 1000))
})
const countdownText = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
const progressText = computed(() => {
  if (!exam.value || exam.value.questions.length === 0) {
    return '0 / 0'
  }
  return `${currentIndex.value + 1} / ${exam.value.questions.length}`
})

const questionTypeLabels: Record<string, string> = {
  single_choice: '单选题',
  multiple_choice: '多选题',
  true_false: '判断题',
  fill_blank: '填空题',
  subjective: '主观/代码题',
}

async function loadExam() {
  loading.value = true
  try {
    exam.value = await getStudentExam(props.examId)
    answers.value = Object.fromEntries(exam.value.questions.map((question) => [question.id, question.savedAnswer ?? '']))
    startCountdown()
    if (!isSubmitted.value) {
      installMonitoringListeners()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '考试加载失败')
  } finally {
    loading.value = false
  }
}

function reportEvent(payload: AntiCheatEventPayload) {
  if (isSubmitted.value) {
    return
  }
  eventQueue = eventQueue
    .then(() => recordAntiCheatEvent(props.examId, payload).then(() => undefined))
    .catch(() => undefined)
}

function currentEventData(extra: Record<string, unknown> = {}) {
  return {
    questionIndex: currentIndex.value + 1,
    questionId: currentQuestion.value?.id,
    remainingSeconds: remainingSeconds.value,
    path: window.location.pathname,
    ...extra,
  }
}

function handleWindowBlur() {
  reportEvent({
    eventType: 'browser_blur',
    eventLevel: 'warning',
    eventData: currentEventData(),
  })
}

function handleVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    lastHiddenAt = Date.now()
    reportEvent({
      eventType: 'tab_hidden',
      eventLevel: 'warning',
      eventData: currentEventData({ visibilityState: document.visibilityState }),
    })
    return
  }
  if (lastHiddenAt > 0) {
    reportEvent({
      eventType: 'abnormal_disconnect',
      eventLevel: 'info',
      eventData: currentEventData({ hiddenMs: Date.now() - lastHiddenAt }),
    })
  }
}

function handleFullscreenChange() {
  if (!document.fullscreenElement) {
    reportEvent({
      eventType: 'fullscreen_exit',
      eventLevel: 'warning',
      eventData: currentEventData(),
    })
  }
}

function handleCopy() {
  reportEvent({
    eventType: 'copy_attempt',
    eventLevel: 'critical',
    eventData: currentEventData(),
  })
}

function handlePaste() {
  reportEvent({
    eventType: 'paste_attempt',
    eventLevel: 'critical',
    eventData: currentEventData(),
  })
}

function handleOffline() {
  reportEvent({
    eventType: 'network_offline',
    eventLevel: 'critical',
    eventData: currentEventData(),
  })
}

function handleOnline() {
  reportEvent({
    eventType: 'network_online',
    eventLevel: 'info',
    eventData: currentEventData(),
  })
}

function handleBeforeUnload() {
  reportEvent({
    eventType: 'page_refresh',
    eventLevel: 'warning',
    eventData: currentEventData(),
  })
}

function installMonitoringListeners() {
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('copy', handleCopy)
  document.removeEventListener('paste', handlePaste)
  window.removeEventListener('offline', handleOffline)
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('beforeunload', handleBeforeUnload)

  window.addEventListener('blur', handleWindowBlur)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('copy', handleCopy)
  document.addEventListener('paste', handlePaste)
  window.addEventListener('offline', handleOffline)
  window.addEventListener('online', handleOnline)
  window.addEventListener('beforeunload', handleBeforeUnload)
}

function removeMonitoringListeners() {
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('copy', handleCopy)
  document.removeEventListener('paste', handlePaste)
  window.removeEventListener('offline', handleOffline)
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('beforeunload', handleBeforeUnload)
}

function startCountdown() {
  if (timerId) {
    window.clearInterval(timerId)
  }
  timerId = window.setInterval(async () => {
    now.value = Date.now()
    if (exam.value && remainingSeconds.value <= 0 && !isSubmitted.value) {
      window.clearInterval(timerId)
      await submitExam(true)
    }
  }, 1000)
}

function clearImageUrls() {
  Object.values(imageUrls.value).forEach((url) => URL.revokeObjectURL(url))
  imageUrls.value = {}
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

function paperIdFromQuestion(question: ExamQuestion) {
  return question.sourcePaperId
}

async function ensureImageUrl(question: ExamQuestion, imageId: string) {
  const paperId = paperIdFromQuestion(question)
  if (!paperId) {
    return
  }
  const key = `${paperId}:${imageId}`
  if (imageUrls.value[key]) {
    return
  }
  try {
    imageUrls.value = {
      ...imageUrls.value,
      [key]: await loadPaperImageUrl(paperId, imageId),
    }
  } catch {
    imageUrls.value = {
      ...imageUrls.value,
      [key]: '',
    }
  }
}

function imageUrl(question: ExamQuestion, imageId: string) {
  const paperId = paperIdFromQuestion(question)
  if (!paperId) {
    return ''
  }
  void ensureImageUrl(question, imageId)
  return imageUrls.value[`${paperId}:${imageId}`] ?? ''
}

function setChoiceAnswer(question: ExamQuestion, key: string) {
  if (question.questionType === 'multiple_choice') {
    const current = new Set((answers.value[question.id] || '').split(',').filter(Boolean))
    if (current.has(key)) {
      current.delete(key)
    } else {
      current.add(key)
    }
    answers.value = {
      ...answers.value,
      [question.id]: Array.from(current).sort().join(','),
    }
    return
  }
  answers.value = {
    ...answers.value,
    [question.id]: key,
  }
}

function isChoiceSelected(question: ExamQuestion, key: string) {
  return (answers.value[question.id] || '').split(',').includes(key)
}

async function saveCurrentAnswer(silent = false) {
  const question = currentQuestion.value
  if (!question || isSubmitted.value) {
    return
  }
  saving.value = true
  try {
    await saveStudentAnswer(props.examId, {
      questionId: question.id,
      answer: answers.value[question.id] || '',
    })
    if (!silent) {
      ElMessage.success('答案已保存')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '答案保存失败')
  } finally {
    saving.value = false
  }
}

async function goNext() {
  await saveCurrentAnswer()
  if (exam.value && currentIndex.value < exam.value.questions.length - 1) {
    currentIndex.value += 1
  }
}

async function goPrev() {
  await saveCurrentAnswer()
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
  }
}

async function submitExam(auto = false) {
  if (!exam.value) {
    return
  }
  try {
    if (!auto) {
      await ElMessageBox.confirm('提交后不能继续修改答案。确认提交吗？', '提交考试', {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        type: 'warning',
      })
    }
    saving.value = true
    await submitStudentExam(
      props.examId,
      exam.value.questions.map((question) => ({
        questionId: question.id,
        answer: answers.value[question.id] || '',
      })),
    )
    removeMonitoringListeners()
    ElMessage.success(auto ? '考试时间到，系统已自动提交' : '考试已提交')
    await router.push('/student/results')
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

watch(
  () => currentQuestion.value ? answers.value[currentQuestion.value.id] : '',
  () => {
    if (!currentQuestion.value || isSubmitted.value) {
      return
    }
    if (autoSaveId) {
      window.clearTimeout(autoSaveId)
    }
    autoSaveId = window.setTimeout(() => {
      void saveCurrentAnswer(true)
    }, 800)
  },
)

onMounted(loadExam)
onBeforeUnmount(() => {
  removeMonitoringListeners()
  clearImageUrls()
  if (timerId) {
    window.clearInterval(timerId)
  }
  if (autoSaveId) {
    window.clearTimeout(autoSaveId)
  }
})
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Taking Exam</p>
        <h1>{{ exam?.title || '在线考试' }}</h1>
        <p class="summary">{{ exam?.description || '请按题作答，离开当前题目前会保存答案。' }}</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/student/exams')">返回列表</el-button>
        <el-button type="primary" :disabled="isSubmitted" :loading="saving" @click="submitExam">提交考试</el-button>
      </div>
    </header>

    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="!exam || exam.questions.length === 0" description="考试暂无题目" />

    <section v-else-if="currentQuestion" class="taking-layout">
      <aside class="status-card question-nav">
        <p class="eyebrow">Progress</p>
        <h2>{{ progressText }}</h2>
        <div class="countdown-box">
          <span>剩余时间</span>
          <strong>{{ countdownText }}</strong>
        </div>
        <div class="question-jump-list">
          <button
            v-for="(question, index) in exam.questions"
            :key="question.id"
            :class="['question-jump', { active: index === currentIndex, answered: answers[question.id] }]"
            type="button"
            @click="currentIndex = index"
          >
            {{ index + 1 }}
          </button>
        </div>
      </aside>

      <article class="status-card answer-card">
        <div class="question-editor-title">
          <strong>{{ questionTypeLabels[currentQuestion.questionType] ?? currentQuestion.questionType }} · {{ currentQuestion.score }} 分</strong>
          <span>第 {{ currentIndex + 1 }} 题</span>
        </div>

        <div class="stem-preview">
          <template v-for="(part, partIndex) in stemParts(currentQuestion.stem)" :key="partIndex">
            <p v-if="part.type === 'text'">{{ part.value }}</p>
            <img v-else-if="imageUrl(currentQuestion, part.value)" :src="imageUrl(currentQuestion, part.value)" :alt="part.value" />
            <span v-else class="image-loading">图片：{{ part.value }}</span>
          </template>
        </div>

        <div v-if="currentQuestion.questionType === 'single_choice' || currentQuestion.questionType === 'multiple_choice'" class="answer-choice-list">
          <button
            v-for="option in currentQuestion.options"
            :key="option.key"
            :class="['answer-choice', { active: isChoiceSelected(currentQuestion, option.key) }]"
            type="button"
            :disabled="isSubmitted"
            @click="setChoiceAnswer(currentQuestion, option.key)"
          >
            <strong>{{ option.key }}</strong>
            <span>{{ option.text }}</span>
          </button>
        </div>

        <div v-else-if="currentQuestion.questionType === 'true_false'" class="answer-choice-list compact">
          <button
            v-for="value in ['对', '错']"
            :key="value"
            :class="['answer-choice', { active: answers[currentQuestion.id] === value }]"
            type="button"
            :disabled="isSubmitted"
            @click="answers = { ...answers, [currentQuestion.id]: value }"
          >
            <strong>{{ value }}</strong>
          </button>
        </div>

        <el-input
          v-else
          v-model="answers[currentQuestion.id]"
          type="textarea"
          :rows="8"
          :disabled="isSubmitted"
          placeholder="请在此输入答案"
        />

        <div class="taking-actions">
          <el-button :disabled="currentIndex === 0" @click="goPrev">上一题</el-button>
          <el-button :loading="saving" @click="saveCurrentAnswer">保存本题</el-button>
          <el-button type="primary" :disabled="currentIndex >= exam.questions.length - 1" @click="goNext">下一题</el-button>
        </div>
      </article>
    </section>
  </main>
</template>
