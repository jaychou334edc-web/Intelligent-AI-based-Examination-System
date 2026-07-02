<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CourseManager from '../components/education/CourseManager.vue'
import ClassManager from '../components/education/ClassManager.vue'
import StudentManager from '../components/education/StudentManager.vue'
import { getAdminUsers, type AdminUser } from '../api/admin'
import {
  createCourse,
  createTeachingClass,
  getAcademicOverview,
  getClassStudents,
  updateClassStudents,
  type AcademicOverview,
  type Course,
  type TeachingClass,
} from '../api/academic'

const router = useRouter()
const users = ref<AdminUser[]>([])
const academic = ref<AcademicOverview | null>(null)
const selectedCourse = ref<Course | null>(null)
const selectedClass = ref<TeachingClass | null>(null)
const selectedStudentIds = ref<number[]>([])
const loading = ref(false)
const saving = ref(false)

const teachers = computed(() => users.value.filter((user) => user.role === 'teacher'))
const courses = computed(() => academic.value?.courses ?? [])
const classes = computed(() => academic.value?.classes ?? [])
const students = computed(() => academic.value?.students ?? [])
const filteredClasses = computed(() => {
  if (!selectedCourse.value) {
    return classes.value
  }
  return classes.value.filter((item) => item.courseId === selectedCourse.value?.id)
})

async function loadEducation() {
  loading.value = true
  try {
    const [nextUsers, nextAcademic] = await Promise.all([getAdminUsers(), getAcademicOverview()])
    users.value = nextUsers
    academic.value = nextAcademic
    if (!selectedCourse.value && nextAcademic.courses.length > 0) {
      selectedCourse.value = nextAcademic.courses[0]
    }
    if (!selectedClass.value && nextAcademic.classes.length > 0) {
      await selectClass(nextAcademic.classes[0])
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '教学组织数据加载失败')
  } finally {
    loading.value = false
  }
}

async function createCourseAction(payload: { name: string; code?: string; description?: string; teacherId?: number }) {
  saving.value = true
  try {
    selectedCourse.value = await createCourse(payload)
    ElMessage.success('课程已创建')
    await loadEducation()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建课程失败')
  } finally {
    saving.value = false
  }
}

async function createClassAction(payload: { courseId: number; name: string; grade?: string; major?: string; studentIds: number[] }) {
  saving.value = true
  try {
    selectedClass.value = await createTeachingClass(payload)
    ElMessage.success('班级已创建')
    await loadEducation()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建班级失败')
  } finally {
    saving.value = false
  }
}

function selectCourse(course: Course) {
  selectedCourse.value = course
}

async function selectClass(item: TeachingClass) {
  selectedClass.value = item
  try {
    const classStudents = await getClassStudents(item.id)
    selectedStudentIds.value = classStudents.map((student) => student.id)
  } catch {
    selectedStudentIds.value = []
  }
}

async function saveClassStudents(studentIds: number[]) {
  if (!selectedClass.value) {
    return
  }
  saving.value = true
  try {
    await updateClassStudents(selectedClass.value.id, studentIds)
    ElMessage.success('班级学生已更新')
    selectedStudentIds.value = [...studentIds]
    await loadEducation()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存班级学生失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadEducation)
</script>

<template>
  <main class="paper-page education-page" v-loading="loading">
    <header class="page-header">
      <div>
        <p class="eyebrow">Class & Course System</p>
        <h1>班级 / 课程管理体系</h1>
        <p class="summary">按“课程 → 班级 → 学生 → 考试”的教学组织结构管理基础数据，供教师发布考试时选择目标班级。</p>
      </div>
      <div class="header-actions">
        <el-button plain @click="router.push('/admin')">返回工作台</el-button>
        <el-button plain @click="loadEducation">刷新</el-button>
      </div>
    </header>

    <section class="education-flow-strip">
      <span>课程</span>
      <i></i>
      <span>班级</span>
      <i></i>
      <span>学生</span>
      <i></i>
      <span>考试</span>
    </section>

    <section class="education-layout">
      <CourseManager :courses="courses" :teachers="teachers" :saving="saving" @create="createCourseAction" @select="selectCourse" />
      <ClassManager :courses="courses" :classes="filteredClasses" :students="students" :saving="saving" @create="createClassAction" @select="selectClass" />
      <StudentManager :selected-class="selectedClass" :all-students="students" :selected-student-ids="selectedStudentIds" :saving="saving" @save="saveClassStudents" />
    </section>
  </main>
</template>
