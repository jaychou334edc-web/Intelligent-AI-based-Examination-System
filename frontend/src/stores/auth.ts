import { defineStore } from 'pinia'
import { clearStoredToken, getStoredToken, setStoredToken } from '../api/client'
import { getCurrentUser, login, logout, type CurrentUser, type LoginRequest, type UserRole } from '../api/auth'

interface AuthState {
  token: string
  user: CurrentUser | null
  loading: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getStoredToken() ?? '',
    user: null,
    loading: false,
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    role: (state): UserRole | null => state.user?.role ?? null,
  },

  actions: {
    async login(request: LoginRequest) {
      this.loading = true
      try {
        const response = await login(request)
        this.token = response.token
        this.user = response.user
        setStoredToken(response.token)
        return response.user
      } finally {
        this.loading = false
      }
    },

    async loadCurrentUser() {
      if (!this.token) {
        return null
      }

      this.loading = true
      try {
        this.user = await getCurrentUser()
        return this.user
      } catch (error) {
        this.clearSession()
        throw error
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        if (this.token) {
          await logout()
        }
      } finally {
        this.clearSession()
      }
    },

    clearSession() {
      this.token = ''
      this.user = null
      clearStoredToken()
    },
  },
})
