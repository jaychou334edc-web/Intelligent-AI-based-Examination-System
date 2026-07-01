<script setup lang="ts">
import { useRouter } from 'vue-router'
import { HomeFilled, RefreshLeft } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import type { UserRole } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()

function roleHome(role?: UserRole) {
  return role ? `/${role}` : '/login'
}
</script>

<template>
  <main class="error-page">
    <section class="error-panel">
      <p class="eyebrow">404</p>
      <h1>页面不存在</h1>
      <p class="summary">当前地址没有匹配的系统页面，请返回工作台继续操作。</p>
      <div class="header-actions">
        <el-button type="primary" :icon="HomeFilled" @click="router.push(roleHome(auth.user?.role))">返回工作台</el-button>
        <el-button plain :icon="RefreshLeft" @click="router.back()">返回上一页</el-button>
      </div>
    </section>
  </main>
</template>
