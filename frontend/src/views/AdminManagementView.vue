<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminUser,
  getAdminSessions,
  getAdminUsers,
  getSystemConfig,
  resetAdminUserPassword,
  updateAdminUser,
  type AdminUser,
  type LoginSessionAudit,
  type SystemConfig,
} from '../api/admin'
import type { UserRole } from '../api/auth'

const router = useRouter()
const route = useRoute()
const activeTab = ref('users')
const users = ref<AdminUser[]>([])
const sessions = ref<LoginSessionAudit[]>([])
const config = ref<SystemConfig | null>(null)
const selectedUser = ref<AdminUser | null>(null)
const loading = ref(false)
const saving = ref(false)

const createForm = reactive({
  username: '',
  password: '',
  role: 'student' as UserRole,
  realName: '',
})

const editForm = reactive({
  role: 'student' as UserRole,
  status: 'active' as 'active' | 'disabled',
  realName: '',
  password: '',
})

const roleLabels: Record<string, string> = {
  admin: '管理员',
  teacher: '教师',
  student: '学生',
}

const statusLabels: Record<string, string> = {
  active: '启用',
  disabled: '停用',
}

async function loadAll() {
  loading.value = true
  try {
    const [nextUsers, nextSessions, nextConfig] = await Promise.all([
      getAdminUsers(),
      getAdminSessions(),
      getSystemConfig(),
    ])
    users.value = nextUsers
    sessions.value = nextSessions
    config.value = nextConfig
    if (!selectedUser.value && nextUsers.length > 0) {
      selectUser(nextUsers[0])
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '管理员数据加载失败')
  } finally {
    loading.value = false
  }
}

function selectUser(user: AdminUser) {
  selectedUser.value = user
  editForm.role = user.role
  editForm.status = user.status
  editForm.realName = user.realName
  editForm.password = ''
}

async function createUser() {
  saving.value = true
  try {
    const user = await createAdminUser({ ...createForm })
    ElMessage.success('用户已创建')
    createForm.username = ''
    createForm.password = ''
    createForm.realName = ''
    await loadAll()
    selectUser(user)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建用户失败')
  } finally {
    saving.value = false
  }
}

async function saveUser() {
  if (!selectedUser.value) {
    return
  }
  saving.value = true
  try {
    const user = await updateAdminUser(selectedUser.value.id, {
      role: editForm.role,
      status: editForm.status,
      realName: editForm.realName,
    })
    ElMessage.success('用户信息已保存')
    await loadAll()
    selectUser(user)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存用户失败')
  } finally {
    saving.value = false
  }
}

async function resetPassword() {
  if (!selectedUser.value || !editForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  try {
    await ElMessageBox.confirm('确认重置该用户密码吗？', '重置密码', {
      confirmButtonText: '重置',
      cancelButtonText: '取消',
      type: 'warning',
    })
    saving.value = true
    await resetAdminUserPassword(selectedUser.value.id, editForm.password)
    editForm.password = ''
    ElMessage.success('密码已重置')
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const tab = route.query.tab
  if (tab === '权限审计') {
    activeTab.value = 'audit'
  } else if (tab === '系统配置') {
    activeTab.value = 'config'
  }
  void loadAll()
})
</script>

<template>
  <main class="paper-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Administration</p>
        <h1>系统管理</h1>
        <p class="summary">管理系统账号、查看登录审计，并核对本地部署配置。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/admin')">返回工作台</el-button>
        <el-button plain @click="loadAll">刷新</el-button>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="用户管理" name="users">
        <section class="admin-layout">
          <article class="status-card">
            <p class="eyebrow">Create</p>
            <h2>新建用户</h2>
            <el-form label-position="top">
              <el-form-item label="用户名">
                <el-input v-model.trim="createForm.username" />
              </el-form-item>
              <el-form-item label="初始密码">
                <el-input v-model="createForm.password" type="password" show-password />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="createForm.role">
                  <el-option label="管理员" value="admin" />
                  <el-option label="教师" value="teacher" />
                  <el-option label="学生" value="student" />
                </el-select>
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model.trim="createForm.realName" />
              </el-form-item>
              <el-button type="primary" :loading="saving" @click="createUser">创建用户</el-button>
            </el-form>
          </article>

          <article class="status-card">
            <p class="eyebrow">Users</p>
            <h2>账号列表</h2>
            <el-skeleton v-if="loading" :rows="5" animated />
            <div v-else class="exam-list">
              <button
                v-for="user in users"
                :key="user.id"
                :class="['exam-list-item', { active: selectedUser?.id === user.id }]"
                type="button"
                @click="selectUser(user)"
              >
                <strong>{{ user.realName }} · {{ user.username }}</strong>
                <span>{{ roleLabels[user.role] }} · {{ statusLabels[user.status] }} · 最近登录 {{ user.lastLoginAt || '无' }}</span>
              </button>
            </div>
          </article>

          <article class="status-card">
            <p class="eyebrow">Edit</p>
            <h2>账号维护</h2>
            <el-empty v-if="!selectedUser" description="请选择用户" />
            <el-form v-else label-position="top">
              <el-form-item label="姓名">
                <el-input v-model.trim="editForm.realName" />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="editForm.role">
                  <el-option label="管理员" value="admin" />
                  <el-option label="教师" value="teacher" />
                  <el-option label="学生" value="student" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="editForm.status">
                  <el-option label="启用" value="active" />
                  <el-option label="停用" value="disabled" />
                </el-select>
              </el-form-item>
              <div class="header-actions">
                <el-button type="primary" :loading="saving" @click="saveUser">保存用户</el-button>
              </div>
              <el-divider />
              <el-form-item label="新密码">
                <el-input v-model="editForm.password" type="password" show-password />
              </el-form-item>
              <el-button plain :loading="saving" @click="resetPassword">重置密码</el-button>
            </el-form>
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="权限审计" name="audit">
        <article class="status-card">
          <p class="eyebrow">Audit</p>
          <h2>最近登录会话</h2>
          <el-table :data="sessions" stripe>
            <el-table-column prop="username" label="账号" width="140" />
            <el-table-column label="角色" width="110">
              <template #default="{ row }">{{ roleLabels[row.role] }}</template>
            </el-table-column>
            <el-table-column prop="ipAddress" label="IP" width="150" />
            <el-table-column prop="createdAt" label="登录时间" width="190" />
            <el-table-column prop="expiredAt" label="过期时间" width="190" />
            <el-table-column prop="revokedAt" label="退出时间" width="190" />
            <el-table-column prop="userAgent" label="客户端" min-width="220" />
          </el-table>
        </article>
      </el-tab-pane>

      <el-tab-pane label="系统配置" name="config">
        <article class="status-card">
          <p class="eyebrow">Configuration</p>
          <h2>部署配置摘要</h2>
          <dl v-if="config" class="profile-list config-list">
            <div><dt>学校名称</dt><dd>{{ config.schoolName }}</dd></div>
            <div><dt>运行环境</dt><dd>{{ config.activeProfile }}</dd></div>
            <div><dt>数据库</dt><dd>{{ config.databaseUrl }}</dd></div>
            <div><dt>Flyway</dt><dd>{{ config.flywayEnabled ? '启用' : '停用' }}</dd></div>
            <div><dt>上传目录</dt><dd>{{ config.uploadDir }}</dd></div>
            <div><dt>AI 模型</dt><dd>{{ config.aiModel }}</dd></div>
            <div><dt>DeepSeek Key</dt><dd>{{ config.deepSeekConfigured ? '已配置' : '未配置' }}</dd></div>
            <div><dt>AI Mock</dt><dd>{{ config.aiMockEnabled ? '启用' : '停用' }}</dd></div>
            <div><dt>AI 规则回退</dt><dd>{{ config.aiFallbackEnabled ? '启用' : '停用' }}</dd></div>
            <div><dt>登录有效期</dt><dd>{{ config.tokenTtlHours }} 小时</dd></div>
          </dl>
        </article>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
