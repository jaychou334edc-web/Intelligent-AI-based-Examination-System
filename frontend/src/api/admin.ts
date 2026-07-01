import { apiRequest } from './client'
import type { UserRole } from './auth'

export type UserStatus = 'active' | 'disabled'

export interface AdminUser {
  id: number
  username: string
  role: UserRole
  status: UserStatus
  realName: string
  lastLoginAt?: string
}

export interface CreateUserPayload {
  username: string
  password: string
  role: UserRole
  realName: string
}

export interface UpdateUserPayload {
  role: UserRole
  status: UserStatus
  realName: string
}

export interface LoginSessionAudit {
  id: number
  userId: number
  username: string
  role: UserRole
  userAgent?: string
  ipAddress?: string
  expiredAt: string
  revokedAt?: string
  createdAt: string
}

export interface SystemConfig {
  schoolName: string
  uploadDir: string
  databaseUrl: string
  flywayEnabled: boolean
  aiModel: string
  aiMockEnabled: boolean
  aiFallbackEnabled: boolean
  deepSeekConfigured: boolean
  tokenTtlHours: number
  activeProfile: string
}

export function getAdminUsers() {
  return apiRequest<AdminUser[]>('/api/admin/users')
}

export function createAdminUser(payload: CreateUserPayload) {
  return apiRequest<AdminUser>('/api/admin/users', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateAdminUser(userId: number, payload: UpdateUserPayload) {
  return apiRequest<AdminUser>(`/api/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function resetAdminUserPassword(userId: number, password: string) {
  return apiRequest<AdminUser>(`/api/admin/users/${userId}/password`, {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}

export function getAdminSessions() {
  return apiRequest<LoginSessionAudit[]>('/api/admin/audit/sessions')
}

export function getSystemConfig() {
  return apiRequest<SystemConfig>('/api/admin/config')
}
