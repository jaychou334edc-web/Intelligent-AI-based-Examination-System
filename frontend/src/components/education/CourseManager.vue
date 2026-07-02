<script setup lang="ts">
import { reactive } from 'vue'
import type { AdminUser } from '../../api/admin'
import type { Course } from '../../api/academic'

defineProps<{
  courses: Course[]
  teachers: AdminUser[]
  saving: boolean
}>()

const emit = defineEmits<{
  create: [payload: { name: string; code?: string; description?: string; teacherId?: number }]
  select: [course: Course]
}>()

const form = reactive({
  name: '',
  code: '',
  description: '',
  teacherId: undefined as number | undefined,
})

function submit() {
  emit('create', { ...form })
  form.name = ''
  form.code = ''
  form.description = ''
  form.teacherId = undefined
}
</script>

<template>
  <article class="status-card education-column">
    <div class="panel-title-row">
      <div>
        <p class="eyebrow">Course</p>
        <h2>课程管理</h2>
      </div>
      <el-tag>{{ courses.length }} 门</el-tag>
    </div>

    <el-form label-position="top" class="education-form">
      <el-form-item label="课程名称">
        <el-input v-model.trim="form.name" />
      </el-form-item>
      <el-form-item label="课程代码">
        <el-input v-model.trim="form.code" />
      </el-form-item>
      <el-form-item label="教师">
        <el-select v-model="form.teacherId" clearable placeholder="选择任课教师">
          <el-option v-for="teacher in teachers" :key="teacher.id" :label="`${teacher.realName} · ${teacher.username}`" :value="teacher.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-button type="primary" :loading="saving" :disabled="!form.name" @click="submit">新建课程</el-button>
    </el-form>

    <div class="education-list">
      <button v-for="course in courses" :key="course.id" type="button" @click="emit('select', course)">
        <strong>{{ course.name }}</strong>
        <span>{{ course.teacherName || '未分配教师' }} · {{ course.classCount }} 个班 · {{ course.studentCount }} 名学生</span>
      </button>
    </div>
  </article>
</template>
