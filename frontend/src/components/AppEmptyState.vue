<script setup lang="ts">
import { DocumentChecked } from '@element-plus/icons-vue'
</script>

<template>
  <section class="app-empty-state">
    <div class="empty-visual">
      <el-icon :size="34">
        <slot name="icon">
          <DocumentChecked />
        </slot>
      </el-icon>
    </div>
    <h2>
      <slot name="title">暂无数据</slot>
    </h2>
    <p>
      <slot>当前还没有可展示的内容。</slot>
    </p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </section>
</template>
