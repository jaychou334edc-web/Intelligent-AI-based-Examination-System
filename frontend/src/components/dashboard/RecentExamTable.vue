<script setup lang="ts">
import type { ExamSummary } from '../../api/exams'

defineProps<{
  exams: ExamSummary[]
}>()

function statusLabel(status: string) {
  if (status === 'published') {
    return '已发布'
  }
  if (status === 'archived') {
    return '已归档'
  }
  return '草稿'
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '不限'
}
</script>

<template>
  <article class="status-card dashboard-panel recent-exam-panel">
    <div class="panel-title-row">
      <div>
        <p class="eyebrow">Recent Exams</p>
        <h2>最近考试 / 成绩概览</h2>
      </div>
      <router-link to="/teacher/exams" class="panel-link">查看全部</router-link>
    </div>
    <el-table :data="exams" stripe>
      <el-table-column prop="title" label="考试" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ statusLabel(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题数" width="80" />
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column label="时间窗口" min-width="220">
        <template #default="{ row }">
          {{ formatDateTime(row.startTime) }} - {{ formatDateTime(row.endTime) }}
        </template>
      </el-table-column>
    </el-table>
  </article>
</template>
