<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import type { LoginRequest, UserRole } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const errorMessage = ref('')

const form = reactive<LoginRequest>({
  username: 'admin',
  password: 'Admin@123456',
})

const rules: FormRules<LoginRequest> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const roleEntrances: Array<{ role: UserRole; label: string; tone: string }> = [
  { role: 'admin', label: '管理员', tone: 'blue' },
  { role: 'teacher', label: '教师', tone: 'green' },
  { role: 'student', label: '学生', tone: 'amber' },
]

const loadingText = computed(() => (auth.loading ? '登录中' : '登录'))

async function submitLogin() {
  errorMessage.value = ''

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  try {
    const user = await auth.login(form)
    ElMessage.success(`欢迎，${user.realName}`)
    await router.push(`/${user.role}`)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-brand">
      <p class="eyebrow">Intelligent AI-based Examination System</p>
      <h1>智能在线考试系统</h1>
      <p class="summary">
        面向学校机房环境的统一考试管理入口，管理员、教师、学生使用各自账号进入对应工作台。
      </p>
      <div class="role-strip" aria-label="系统角色">
        <span v-for="entrance in roleEntrances" :key="entrance.role" :class="['role-chip', entrance.tone]">
          {{ entrance.label }}
        </span>
      </div>
    </section>

    <section class="login-panel" aria-labelledby="login-title">
      <div class="panel-heading">
        <p class="eyebrow">Secure Access</p>
        <h2 id="login-title">账号登录</h2>
      </div>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        class="form-alert"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submitLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" autocomplete="username" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            size="large"
            show-password
            type="password"
          />
        </el-form-item>

        <el-button class="submit-button" type="primary" size="large" :loading="auth.loading" @click="submitLogin">
          {{ loadingText }}
        </el-button>
      </el-form>
    </section>
  </main>
</template>
