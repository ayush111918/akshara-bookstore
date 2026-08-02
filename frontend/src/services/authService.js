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
