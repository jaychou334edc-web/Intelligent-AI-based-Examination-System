<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Collection,
  DataAnalysis,
  DocumentAdd,
  EditPen,
  Histogram,
  Monitor,
  Odometer,
  Reading,
  SwitchButton,
  Tickets,
} from '@element-plus/icons-vue'
import { getRoleShell, type RoleShell } from '../api/roleShell'
import { useAuthStore } from '../stores/auth'
import type { UserRole } from '../api/auth'
import { getTeacherExams, type ExamSummary } from '../api/exams'
import { getRecentQuestions, type QuestionBankItem } from '../api/questions'

const props = defineProps<{
  role: UserRole
}>()

const router = useRouter()
const auth = useAuthStore()
const shell = ref<RoleShell | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const teacherExams = ref<ExamSummary[]>([])
const teacherQuestions = ref<QuestionBankItem[]>([])

const roleMeta: Record<UserRole, { title: string; subtitle: string; tone: string; label: string; actions: string[] }> = {
  admin: {
    title: '管理员工作台',
    subtitle: '统一管理账号权限、基础资料与学校考试运行配置。',
    tone: 'blue',
    label: '管理员',
    actions: ['用户管理', '课程班级', '权限审计', '系统配置'],
  },
  teacher: {
    title: '教师工作台',
    subtitle: '集中处理题库建设、试卷管理、考试发布与成绩分析。',
    tone: 'green',
    label: '教师',
    actions: ['AI 题库导入', '题库维护', '考试管理', '阅卷管理', '监考分析', '成绩统计'],
  },
  student: {
    title: '学生考试入口',
    subtitle: '查看考试安排，进入在线答题，并查询个人成绩。',
    tone: 'amber',
    label: '学生',
    actions: ['考试列表', '继续考试', '成绩查询'],
  },
}

const currentMeta = computed(() => roleMeta[props.role])
const displayTitle = computed(() => shell.value?.title ?? currentMeta.value.title)
const teacherStats = computed(() => {
  const published = teacherExams.value.filter((exam) => exam.status === 'published').length
  const drafts = teacherExams.value.filter((exam) => exam.status === 'draft').length
  return {
    grading: teacherExams.value.reduce((sum, exam) => sum + (exam.submissionStatus === 'submitted' ? 1 : 0), 0),
    running: published,
    questions: teacherQuestions.value.length,
    drafts,
  }
})
const recentTeacherExams = computed(() => teacherExams.value.slice(0, 4))
const recentTeacherQuestions = computed(() => teacherQuestions.value.slice(0, 5))

function actionTarget(action: string) {
  if (props.role === 'admin' && action === '课程班级') {
    return '/admin/education'
  }
  if (props.role === 'admin' && ['用户管理', '权限审计', '系统配置'].includes(action)) {
    return `/admin/manage?tab=${encodeURIComponent(action)}`
  }
  if (props.role === 'teacher' && action === 'AI 题库导入') {
    return '/teacher/ai-import'
  }
  if (props.role === 'teacher' && action === '题库维护') {
    return '/teacher/questions'
  }
  if (props.role === 'teacher' && action === '考试管理') {
    return '/teacher/exams'
  }
  if (props.role === 'teacher' && action === '阅卷管理') {
    return '/teacher/grading'
  }
  if (props.role === 'teacher' && action === '监考分析') {
    return '/teacher/monitoring'
  }
  if (props.role === 'teacher' && action === '成绩统计') {
    return '/teacher/analytics'
  }
  if (props.role === 'student' && (action === '考试列表' || action === '继续考试')) {
    return '/student/exams'
  }
  if (props.role === 'student' && action === '成绩查询') {
    return '/student/results'
  }
  return ''
}

async function openAction(action: string) {
  const target = actionTarget(action)
  if (target) {
    await router.push(target)
  }
}

async function loadShell() {
  loading.value = true
  errorMessage.value = ''
  try {
    shell.value = await getRoleShell(props.role)
    if (props.role === 'teacher') {
      const [exams, questions] = await Promise.all([getTeacherExams(), getRecentQuestions(200)])
      teacherExams.value = exams
      teacherQuestions.value = questions
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '工作台加载失败'
  } finally {
    loading.value = false
  }
}

function examStatusLabel(status: string) {
  if (status === 'published') {
    return '已发布'
  }
  if (status === 'archived') {
    return '已归档'
  }
  return '草稿'
}

async function handleLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}

onMounted(loadShell)
</script>

<template>
  <main v-if="props.role === 'teacher'" class="teacher-workbench-page">
    <aside class="teacher-sidebar">
      <div class="sidebar-brand">
        <strong>AI Exam</strong>
        <span>教师工作台</span>
      </div>
      <nav class="teacher-nav">
        <button class="active" type="button" @click="router.push('/teacher/dashboard')"><el-icon><Odometer /></el-icon>工作台</button>
        <button type="button" @click="router.push('/teacher/ai-import')"><el-icon><DocumentAdd /></el-icon>AI 导入</button>
        <button type="button" @click="router.push('/teacher/questions')"><el-icon><Collection /></el-icon>题库管理</button>
        <button type="button" @click="router.push('/teacher/exams')"><el-icon><Tickets /></el-icon>考试管理</button>
        <button type="button" @click="router.push('/teacher/grading')"><el-icon><EditPen /></el-icon>阅卷中心</button>
        <button type="button" @click="router.push('/teacher/monitoring')"><el-icon><Monitor /></el-icon>监考分析</button>
        <button type="button" @click="router.push('/teacher/analytics')"><el-icon><Histogram /></el-icon>成绩统计</button>
      </nav>
      <div class="sidebar-user">
        <strong>{{ auth.user?.realName }}</strong>
        <span>{{ auth.user?.username }} · 教师</span>
        <el-button plain size="small" :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
      </div>
    </aside>

    <section class="teacher-workbench-main">
      <header class="teacher-workbench-header">
        <div>
          <p class="eyebrow">Teaching Console</p>
          <h1>{{ auth.user?.realName || '教师' }}，欢迎回来</h1>
          <p class="summary">集中处理 AI 导题、题库维护、考试发布、阅卷和监考分析。</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="DocumentAdd" @click="router.push('/teacher/ai-import')">AI 导入题目</el-button>
          <el-button plain :icon="Tickets" @click="router.push('/teacher/exams')">创建考试</el-button>
        </div>
      </header>

      <section class="teacher-metric-grid">
        <article class="status-card teacher-metric-card">
          <span>待阅卷</span>
          <strong>{{ teacherStats.grading }}</strong>
          <p>份答卷</p>
        </article>
        <article class="status-card teacher-metric-card">
          <span>进行中考试</span>
          <strong>{{ teacherStats.running }}</strong>
          <p>场考试</p>
        </article>
        <article class="status-card teacher-metric-card">
          <span>题库总量</span>
          <strong>{{ teacherStats.questions }}</strong>
          <p>道题</p>
        </article>
        <article class="status-card teacher-metric-card">
          <span>草稿考试</span>
          <strong>{{ teacherStats.drafts }}</strong>
          <p>待发布</p>
        </article>
      </section>

      <section class="teacher-workbench-grid">
        <article class="status-card teacher-task-card">
          <div class="panel-title-row">
            <div>
              <p class="eyebrow">Recent Exams</p>
              <h2>最近考试</h2>
            </div>
            <el-button text type="primary" :icon="Tickets" @click="router.push('/teacher/exams')">查看全部</el-button>
          </div>
          <el-skeleton v-if="loading" :rows="4" animated />
          <el-empty v-else-if="recentTeacherExams.length === 0" description="暂无考试" />
          <div v-else class="teacher-list">
            <button v-for="exam in recentTeacherExams" :key="exam.id" type="button" @click="router.push('/teacher/exams')">
              <strong>{{ exam.title }}</strong>
              <span>{{ examStatusLabel(exam.status) }} · {{ exam.questionCount }} 题 · {{ exam.totalScore }} 分</span>
            </button>
          </div>
        </article>

        <article class="status-card teacher-task-card">
          <div class="panel-title-row">
            <div>
              <p class="eyebrow">AI Import</p>
              <h2>最近导入题目</h2>
            </div>
            <el-button text type="primary" :icon="Collection" @click="router.push('/teacher/questions')">进入题库</el-button>
          </div>
          <el-skeleton v-if="loading" :rows="4" animated />
          <el-empty v-else-if="recentTeacherQuestions.length === 0" description="暂无题目" />
          <div v-else class="teacher-list">
            <button v-for="question in recentTeacherQuestions" :key="question.id" type="button" @click="router.push('/teacher/questions')">
              <strong>#{{ question.id }} {{ question.knowledgePoint || '未设置知识点' }}</strong>
              <span>{{ question.stem.replace(/\[IMG:[^\]]+\]/g, '[图片]').slice(0, 64) }}</span>
            </button>
          </div>
        </article>

        <article class="status-card teacher-task-card">
          <p class="eyebrow">Tasks</p>
          <h2>待处理任务</h2>
          <div class="task-stack">
            <button type="button" @click="router.push('/teacher/ai-import')">导入新试卷并审核 AI 解析结果</button>
            <button type="button" @click="router.push('/teacher/questions')">维护题库知识点、分值和答案</button>
            <button type="button" @click="router.push('/teacher/grading')">处理主观题人工阅卷</button>
            <button type="button" @click="router.push('/teacher/analytics')">查看成绩统计和知识点分析</button>
          </div>
        </article>
      </section>
    </section>
  </main>

  <main v-else class="workspace-page">
    <header :class="['workspace-hero', currentMeta.tone]">
      <div>
        <p class="eyebrow">Dashboard</p>
        <h1>{{ displayTitle }}</h1>
        <p class="summary">{{ currentMeta.subtitle }}</p>
      </div>

      <div class="user-box">
        <span class="user-name">{{ auth.user?.realName }}</span>
        <span class="user-role">{{ auth.user?.username }} · {{ currentMeta.label }}</span>
        <el-button plain @click="handleLogout">退出</el-button>
      </div>
    </header>

    <section class="workspace-grid">
      <article class="status-card">
        <p class="eyebrow">认证状态</p>
        <h2>当前用户</h2>
        <dl class="profile-list">
          <div>
            <dt>姓名</dt>
            <dd>{{ auth.user?.realName }}</dd>
          </div>
          <div>
            <dt>账号</dt>
            <dd>{{ auth.user?.username }}</dd>
          </div>
          <div>
            <dt>角色</dt>
            <dd>{{ props.role }}</dd>
          </div>
        </dl>
      </article>

      <article class="status-card">
        <p class="eyebrow">接口校验</p>
        <h2>角色入口</h2>
        <el-skeleton v-if="loading" :rows="3" animated />
        <el-alert v-else-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
        <div v-else class="shell-result">
          <span class="result-dot"></span>
          <span>{{ shell?.title }}</span>
        </div>
      </article>

      <article class="status-card action-card">
        <p class="eyebrow">Navigation</p>
        <h2>功能入口</h2>
        <div class="action-list">
          <el-button
            v-for="action in currentMeta.actions"
            :key="action"
            plain
            :icon="props.role === 'student' && action === '成绩查询' ? Reading : DataAnalysis"
            :disabled="!actionTarget(action)"
            @click="openAction(action)"
          >
            {{ action }}
          </el-button>
        </div>
      </article>
    </section>
  </main>
</template>
