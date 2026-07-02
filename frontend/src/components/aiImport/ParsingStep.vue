<script setup lang="ts">
defineProps<{
  loading: boolean
  progress: number
  questionCount: number
  imageCount: number
  averageConfidence: number
  logs: string[]
}>()
</script>

<template>
  <article class="status-card flow-step-card parsing-step-card">
    <div class="panel-title-row">
      <div>
        <p class="eyebrow">Step 2</p>
        <h2>AI解析中</h2>
      </div>
      <el-tag :type="questionCount > 0 ? 'success' : loading ? 'warning' : 'info'">
        {{ questionCount > 0 ? '解析完成' : loading ? '解析中' : '待解析' }}
      </el-tag>
    </div>

    <el-progress :percentage="progress" :status="progress === 100 ? 'success' : undefined" />

    <div class="ai-metrics-grid">
      <div class="metric-tile">
        <span>识别题目</span>
        <strong>{{ questionCount }}</strong>
      </div>
      <div class="metric-tile">
        <span>图片占位</span>
        <strong>{{ imageCount }}</strong>
      </div>
      <div class="metric-tile">
        <span>平均置信度</span>
        <strong>{{ averageConfidence }}%</strong>
      </div>
    </div>

    <div class="parse-log">
      <p v-for="item in logs" :key="item">{{ item }}</p>
      <p v-if="logs.length === 0">等待上传文件后开始解析。</p>
    </div>
  </article>
</template>
