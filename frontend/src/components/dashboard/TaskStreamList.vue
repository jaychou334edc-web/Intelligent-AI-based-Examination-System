<script setup lang="ts">
import { Connection, EditPen, MagicStick, Tickets } from '@element-plus/icons-vue'
import type { DashboardTask } from './types'

defineProps<{
  tasks: DashboardTask[]
}>()

const icons = {
  ai: MagicStick,
  grading: EditPen,
  exam: Tickets,
  question: Connection,
}
</script>

<template>
  <article class="status-card dashboard-panel">
    <div class="panel-title-row">
      <div>
        <p class="eyebrow">Task Stream</p>
        <h2>待办任务流</h2>
      </div>
    </div>
    <el-empty v-if="tasks.length === 0" description="暂无待处理任务" />
    <div v-else class="task-stream">
      <router-link v-for="task in tasks" :key="task.id" :to="task.target" class="task-stream-item">
        <span class="task-icon"><el-icon><component :is="icons[task.type]" /></el-icon></span>
        <strong>{{ task.title }}</strong>
        <em>{{ task.meta }}</em>
      </router-link>
    </div>
  </article>
</template>
