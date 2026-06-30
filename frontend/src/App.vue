<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getBackendHealth, type HealthResponse } from './api/health'

const loading = ref(false)
const health = ref<HealthResponse | null>(null)
const error = ref('')

async function refreshHealth() {
  loading.value = true
  error.value = ''

  try {
    health.value = await getBackendHealth()
  } catch (caught) {
    const message = caught instanceof Error ? caught.message : 'Unknown error'
    error.value = message
  } finally {
    loading.value = false
  }
}

onMounted(refreshHealth)
</script>

<template>
  <main class="app-shell">
    <section class="hero">
      <p class="eyebrow">Phase 0 基础工程</p>
      <h1>智能化在线考试系统</h1>
      <p class="summary">
        面向学校机房部署的 Java Web 智能考试平台。
      </p>
    </section>

    <section class="status-panel">
      <div>
        <h2>后端健康检查</h2>
        <p>验证 Vue 前端是否可以访问 Spring Boot 后端服务。</p>
      </div>

      <el-button type="primary" :loading="loading" @click="refreshHealth">
        刷新
      </el-button>

      <el-alert
        v-if="error"
        type="error"
        :title="error"
        show-icon
        :closable="false"
      />

      <el-descriptions v-if="health" :column="1" border>
        <el-descriptions-item label="状态">
          <el-tag type="success">{{ health.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="服务">
          {{ health.service }}
        </el-descriptions-item>
        <el-descriptions-item label="时间">
          {{ health.timestamp }}
        </el-descriptions-item>
      </el-descriptions>
    </section>
  </main>
</template>
