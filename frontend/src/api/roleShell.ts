import { apiRequest } from './client'
import type { UserRole } from './auth'

export interface RoleShell {
  role: UserRole
  title: string
}

export function getRoleShell(role: UserRole) {
  return apiRequest<RoleShell>(`/api/${role}/shell`)
}
