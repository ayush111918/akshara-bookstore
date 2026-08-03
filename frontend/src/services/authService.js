import api from './api'

export async function loginReader(credentials) {
  const response = await api.post('/auth/login', credentials)
  return response.data
}

export async function registerReader(details) {
  const response = await api.post('/auth/register', details)
  return response.data
}

export async function getCurrentUser() {
  const response = await api.get('/users/me')
  return response.data
}

export async function deleteCurrentAccount(password) {
  await api.delete('/users/me', { data: { password } })
}

export async function getAuditLogs(page = 0, size = 25) {
  const response = await api.get('/admin/audit-logs', { params: { page, size } })
  return response.data
}
