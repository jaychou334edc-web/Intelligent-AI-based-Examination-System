<script setup lang="ts">
import { Collection, DocumentAdd, EditPen, Tickets } from '@element-plus/icons-vue'

const actions = [
  { label: 'AI导入', target: '/teacher/ai-import', icon: DocumentAdd },
  { label: '题库管理', target: '/teacher/questions', icon: Collection },
  { label: '考试管理', target: '/teacher/exams', icon: Tickets },
  { label: '阅卷中心', target: '/teacher/grading', icon: EditPen },
]
</script>

<template>
  <article class="status-card dashboard-panel quick-action-panel">
    <p class="eyebrow">Quick Actions</p>
    <h2>快捷入口</h2>
    <div class="quick-action-grid">
      <router-link v-for="action in actions" :key="action.target" :to="action.target" class="quick-action">
        <el-icon><component :is="action.icon" /></el-icon>
        <span>{{ action.label }}</span>
      </router-link>
    </div>
  </article>
</template>
