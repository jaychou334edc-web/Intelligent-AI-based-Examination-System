import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RoleShellView from '../views/RoleShellView.vue'
import TeacherPaperParsingView from '../views/TeacherPaperParsingView.vue'
import TeacherQuestionBankView from '../views/TeacherQuestionBankView.vue'
import { useAuthStore } from '../stores/auth'
import type { UserRole } from '../api/auth'

function roleHome(role: UserRole) {
  return `/${role}`
}

function requiredRole(route: RouteLocationNormalized) {
  return route.meta.role as UserRole | undefined
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: () => {
        const auth = useAuthStore()
        return auth.user ? roleHome(auth.user.role) : '/login'
      },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/admin',
      name: 'admin',
      component: RoleShellView,
      meta: { requiresAuth: true, role: 'admin' },
      props: { role: 'admin' },
    },
    {
      path: '/teacher',
      name: 'teacher',
      component: RoleShellView,
      meta: { requiresAuth: true, role: 'teacher' },
      props: { role: 'teacher' },
    },
    {
      path: '/teacher/papers',
      name: 'teacher-papers',
      component: TeacherPaperParsingView,
      meta: { requiresAuth: true, role: 'teacher' },
    },
    {
      path: '/teacher/questions',
      name: 'teacher-questions',
      component: TeacherQuestionBankView,
      meta: { requiresAuth: true, role: 'teacher' },
    },
    {
      path: '/student',
      name: 'student',
      component: RoleShellView,
      meta: { requiresAuth: true, role: 'student' },
      props: { role: 'student' },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.name === 'login' && auth.token) {
    if (!auth.user) {
      try {
        await auth.loadCurrentUser()
      } catch {
        return true
      }
    }
    return auth.user ? roleHome(auth.user.role) : true
  }

  if (!to.meta.requiresAuth) {
    return true
  }

  if (!auth.token) {
    return '/login'
  }

  if (!auth.user) {
    try {
      await auth.loadCurrentUser()
    } catch {
      return '/login'
    }
  }

  const role = requiredRole(to)
  if (role && auth.user?.role !== role) {
    return auth.user ? roleHome(auth.user.role) : '/login'
  }

  return true
})

export default router
