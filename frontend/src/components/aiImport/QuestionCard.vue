<script setup lang="ts">
import type { ReviewQuestion } from '../../api/paperParsing'

defineProps<{
  question: ReviewQuestion
  index: number
  active: boolean
  confidence: number
}>()

defineEmits<{
  select: []
}>()
</script>

<template>
  <button :class="['question-review-card', { active }]" type="button" @click="$emit('select')">
    <strong>第 {{ index + 1 }} 题</strong>
    <span>{{ question.questionType }} · {{ question.score }} 分 · {{ confidence }}%</span>
    <p>{{ question.stem.replace(/\[IMG:[^\]]+\]/g, '[图片]').slice(0, 72) }}</p>
  </button>
</template>
