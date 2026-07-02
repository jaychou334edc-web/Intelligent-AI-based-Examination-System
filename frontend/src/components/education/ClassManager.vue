<script setup lang="ts">
import { reactive } from 'vue'
import type { ClassStudent, Course, TeachingClass } from '../../api/academic'

defineProps<{
  courses: Course[]
  classes: TeachingClass[]
  students: ClassStudent[]
  saving: boolean
}>()

const emit = defineEmits<{
  create: [payload: { courseId: number; name: string; grade?: string; major?: string; studentIds: number[] }]
  select: [item: TeachingClass]
}>()

const form = reactive({
  courseId: undefined as number | undefined,
  name: '',
  grade: '',
  major: '',
  studentIds: [] as number[],
})

function submit() {
  if (!form.courseId) {
    return
  }
  emit('create', {
    courseId: form.courseId,
    name: form.name,
    grade: form.grade,
    major: form.major,
    studentIds: form.studentIds,
  })
  form.name = ''
  form.grade = ''
  form.major = ''
  form.studentIds = []
}
</script>

<template>
  <article class="status-card education-column">
    <div class="panel-title-row">
      <div>
        <p class="eyebrow">Class</p>
        <h2>班级管理</h2>
      </div>
      <el-tag>{{ classes.length }} 个</el-tag>
    </div>

    <el-form label-position="top" class="education-form">
      <el-form-item label="所属课程">
        <el-select v-model="form.courseId" placeholder="选择课程">
          <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="班级名称">
        <el-input v-model.trim="form.name" />
      </el-form-item>
      <el-form-item label="年级 / 专业">
        <div class="inline-form-row">
          <el-input v-model.trim="form.grade" placeholder="年级" />
          <el-input v-model.trim="form.major" placeholder="专业" />
        </div>
      </el-form-item>
      <el-form-item label="初始学生">
        <el-select v-model="form.studentIds" multiple filterable collapse-tags placeholder="可选">
          <el-option v-for="student in students" :key="student.id" :label="`${student.realName || student.username} · ${student.username}`" :value="student.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" :loading="saving" :disabled="!form.courseId || !form.name" @click="submit">新建班级</el-button>
    </el-form>

    <div class="education-list">
      <button v-for="item in classes" :key="item.id" type="button" @click="emit('select', item)">
        <strong>{{ item.name }}</strong>
        <span>{{ item.courseName }} · {{ item.major || '未设置专业' }} · {{ item.studentCount }} 名学生</span>
      </button>
    </div>
  </article>
</template>
