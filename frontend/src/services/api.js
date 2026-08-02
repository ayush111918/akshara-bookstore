import axios from 'axios'

const SESSION_KEY = 'akshara.session'

export function loadStoredSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY))
  } catch {
    return null
  }
}

export function storeSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearStoredSession() {
  localStorage.removeItem(SESSION_KEY)
}

export function getApiErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data
  const firstFieldError = data?.fieldErrors && Object.values(data.fieldErrors)[0]
  return firstFieldError || data?.message || fallback
}

const api = axios.create({
  baseURL: '/api',
  timeout: 12000,
})

api.interceptors.request.use((config) => {
  const session = loadStoredSession()
  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  return config
})

export default api
