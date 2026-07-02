<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  caption: string
  tone?: 'blue' | 'green' | 'amber' | 'red'
}>()
</script>

<template>
  <article :class="['status-card dashboard-stat-card', tone || 'blue']">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <p>{{ caption }}</p>
  </article>
</template>
