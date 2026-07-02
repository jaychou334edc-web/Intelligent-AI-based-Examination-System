<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ClassStudent, TeachingClass } from '../../api/academic'

const props = defineProps<{
  selectedClass: TeachingClass | null
  allStudents: ClassStudent[]
  selectedStudentIds: number[]
  saving: boolean
}>()

const emit = defineEmits<{
  save: [studentIds: number[]]
}>()

const draftStudentIds = ref<number[]>([])

watch(
  () => props.selectedStudentIds,
  (value) => {
    draftStudentIds.value = [...value]
  },
  { immediate: true }
)
</script>

<template>
  <article class="status-card education-column student-manager">
    <div class="panel-title-row">
      <div>
        <p class="eyebrow">Student</p>
        <h2>学生管理</h2>
      </div>
      <el-tag>{{ allStudents.length }} 人</el-tag>
    </div>

    <el-empty v-if="!selectedClass" description="请选择班级后分配学生" />
    <template v-else>
      <div class="selected-class-box">
        <strong>{{ selectedClass.name }}</strong>
        <span>{{ selectedClass.courseName }} · {{ selectedClass.studentCount }} 名学生</span>
      </div>
      <el-form label-position="top">
        <el-form-item label="班级学生">
          <el-select v-model="draftStudentIds" multiple filterable collapse-tags placeholder="选择学生">
            <el-option v-for="student in allStudents" :key="student.id" :label="`${student.realName || student.username} · ${student.username}`" :value="student.id" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="emit('save', draftStudentIds)">保存班级学生</el-button>
      </el-form>
    </template>

    <el-divider />
    <div class="education-list">
      <button v-for="student in allStudents" :key="student.id" type="button">
        <strong>{{ student.realName || student.username }}</strong>
        <span>{{ student.username }}{{ student.studentNumber ? ` · ${student.studentNumber}` : '' }}</span>
      </button>
    </div>
  </article>
</template>
