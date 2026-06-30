<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRoleShell, type RoleShell } from '../api/roleShell'
import { useAuthStore } from '../stores/auth'
import type { UserRole } from '../api/auth'

const props = defineProps<{
  role: UserRole
}>()

const router = useRouter()
const auth = useAuthStore()
const shell = ref<RoleShell | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const roleMeta: Record<UserRole, { title: string; subtitle: string; tone: string; label: string; actions: string[] }> = {
  admin: {
    title: '管理员工作台',
    subtitle: '统一管理账号权限、基础资料与学校考试运行配置。',
    tone: 'blue',
    label: '管理员',
    actions: ['用户管理', '权限审计', '系统配置'],
  },
  teacher: {
    title: '教师工作台',
    subtitle: '集中处理题库建设、试卷管理、考试发布与成绩分析。',
    tone: 'green',
    label: '教师',
    actions: ['题库维护', '组卷审核', '成绩分析'],
  },
  student: {
    title: '学生考试入口',
    subtitle: '查看考试安排，进入在线答题，并查询个人成绩。',
    tone: 'amber',
    label: '学生',
    actions: ['考试列表', '在线答题', '成绩查询'],
  },
}

const currentMeta = computed(() => roleMeta[props.role])
const displayTitle = computed(() => shell.value?.title ?? currentMeta.value.title)

async function loadShell() {
  loading.value = true
  errorMessage.value = ''
  try {
    shell.value = await getRoleShell(props.role)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '工作台加载失败'
  } finally {
    loading.value = false
  }
}

async function handleLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  await router.push('/login')
}

onMounted(loadShell)
</script>

<template>
  <main class="workspace-page">
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
          <el-button v-for="action in currentMeta.actions" :key="action" plain disabled>
            {{ action }}
          </el-button>
        </div>
      </article>
    </section>
  </main>
</template>
