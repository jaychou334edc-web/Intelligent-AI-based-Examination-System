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
import {
  createCourse,
  createTeachingClass,
  getAcademicOverview,
  getClassStudents,
  updateClassStudents,
  type AcademicOverview,
  type TeachingClass,
} from '../api/academic'
import type { UserRole } from '../api/auth'

const router = useRouter()
const route = useRoute()
const activeTab = ref('users')
const users = ref<AdminUser[]>([])
const sessions = ref<LoginSessionAudit[]>([])
const config = ref<SystemConfig | null>(null)
const academic = ref<AcademicOverview | null>(null)
const selectedUser = ref<AdminUser | null>(null)
const selectedClass = ref<TeachingClass | null>(null)
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

const courseForm = reactive({
  name: 'Java 程序设计',
  code: 'JAVA-2026',
  description: '面向软件专业的 Java 程序设计课程。',
  teacherId: undefined as number | undefined,
})

const classForm = reactive({
  courseId: undefined as number | undefined,
  name: '2023 软件 1 班',
  grade: '2023',
  major: '软件工程',
  studentIds: [] as number[],
})

const classStudentForm = reactive({
  studentIds: [] as number[],
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
    const [nextUsers, nextSessions, nextConfig, nextAcademic] = await Promise.all([
      getAdminUsers(),
      getAdminSessions(),
      getSystemConfig(),
      getAcademicOverview(),
    ])
    users.value = nextUsers
    sessions.value = nextSessions
    config.value = nextConfig
    academic.value = nextAcademic
    if (!selectedUser.value && nextUsers.length > 0) {
      selectUser(nextUsers[0])
    }
    if (!selectedClass.value && nextAcademic.classes.length > 0) {
      void selectClass(nextAcademic.classes[0])
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '管理员数据加载失败')
  } finally {
    loading.value = false
  }
}

async function selectClass(item: TeachingClass) {
  selectedClass.value = item
  try {
    const students = await getClassStudents(item.id)
    classStudentForm.studentIds = students.map((student) => student.id)
  } catch {
    classStudentForm.studentIds = []
  }
}

async function createCourseAction() {
  saving.value = true
  try {
    await createCourse({ ...courseForm })
    ElMessage.success('课程已创建')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建课程失败')
  } finally {
    saving.value = false
  }
}

async function createClassAction() {
  if (!classForm.courseId) {
    ElMessage.warning('请选择所属课程')
    return
  }
  saving.value = true
  try {
    await createTeachingClass({
      courseId: classForm.courseId,
      name: classForm.name,
      grade: classForm.grade,
      major: classForm.major,
      studentIds: classForm.studentIds,
    })
    ElMessage.success('班级已创建')
    classForm.studentIds = []
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建班级失败')
  } finally {
    saving.value = false
  }
}

async function saveClassStudents() {
  if (!selectedClass.value) {
    return
  }
  saving.value = true
  try {
    await updateClassStudents(selectedClass.value.id, classStudentForm.studentIds)
    ElMessage.success('班级学生已更新')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存班级学生失败')
  } finally {
    saving.value = false
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
  } else if (tab === '课程班级') {
    activeTab.value = 'academic'
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

      <el-tab-pane label="课程班级" name="academic">
        <section class="admin-layout">
          <article class="status-card">
            <p class="eyebrow">Course</p>
            <h2>新建课程</h2>
            <el-form label-position="top">
              <el-form-item label="课程名称">
                <el-input v-model.trim="courseForm.name" />
              </el-form-item>
              <el-form-item label="课程代码">
                <el-input v-model.trim="courseForm.code" />
              </el-form-item>
              <el-form-item label="任课教师">
                <el-select v-model="courseForm.teacherId" clearable placeholder="可选">
                  <el-option
                    v-for="teacher in users.filter((user) => user.role === 'teacher')"
                    :key="teacher.id"
                    :label="`${teacher.realName} · ${teacher.username}`"
                    :value="teacher.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="课程说明">
                <el-input v-model="courseForm.description" type="textarea" :rows="3" />
              </el-form-item>
              <el-button type="primary" :loading="saving" @click="createCourseAction">创建课程</el-button>
            </el-form>
          </article>

          <article class="status-card">
            <p class="eyebrow">Class</p>
            <h2>新建班级</h2>
            <el-form label-position="top">
              <el-form-item label="所属课程">
                <el-select v-model="classForm.courseId" placeholder="请选择课程">
                  <el-option
                    v-for="course in academic?.courses || []"
                    :key="course.id"
                    :label="course.name"
                    :value="course.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="班级名称">
                <el-input v-model.trim="classForm.name" />
              </el-form-item>
              <el-form-item label="年级">
                <el-input v-model.trim="classForm.grade" />
              </el-form-item>
              <el-form-item label="专业">
                <el-input v-model.trim="classForm.major" />
              </el-form-item>
              <el-form-item label="学生">
                <el-select v-model="classForm.studentIds" multiple filterable collapse-tags placeholder="选择学生">
                  <el-option
                    v-for="student in academic?.students || []"
                    :key="student.id"
                    :label="`${student.realName || student.username} · ${student.username}`"
                    :value="student.id"
                  />
                </el-select>
              </el-form-item>
              <el-button type="primary" :loading="saving" @click="createClassAction">创建班级</el-button>
            </el-form>
          </article>

          <article class="status-card">
            <p class="eyebrow">Overview</p>
            <h2>课程与班级</h2>
            <div class="exam-list">
              <button
                v-for="item in academic?.classes || []"
                :key="item.id"
                :class="['exam-list-item', { active: selectedClass?.id === item.id }]"
                type="button"
                @click="selectClass(item)"
              >
                <strong>{{ item.name }}</strong>
                <span>{{ item.courseName }} · {{ item.major || '未设置专业' }} · {{ item.studentCount }} 名学生</span>
              </button>
            </div>

            <el-divider />

            <h2>调整班级学生</h2>
            <el-empty v-if="!selectedClass" description="请选择班级" />
            <el-form v-else label-position="top">
              <el-form-item :label="selectedClass.name">
                <el-select v-model="classStudentForm.studentIds" multiple filterable collapse-tags placeholder="重新选择学生">
                  <el-option
                    v-for="student in academic?.students || []"
                    :key="student.id"
                    :label="`${student.realName || student.username} · ${student.username}`"
                    :value="student.id"
                  />
                </el-select>
              </el-form-item>
              <el-button plain :loading="saving" @click="saveClassStudents">保存学生名单</el-button>
            </el-form>
          </article>
        </section>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
