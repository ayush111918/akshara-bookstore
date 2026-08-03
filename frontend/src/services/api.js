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
  if (firstFieldError || data?.message) return firstFieldError || data.message
  if (error?.code === 'ECONNABORTED') return 'The server took too long to respond. Please try again.'
  if (!error?.response && error?.request) return 'Akshara cannot reach the server. Check your connection and try again.'
  return fallback
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

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const session = loadStoredSession()
    const requestUrl = error?.config?.url || ''
    if (error?.response?.status === 401 && session?.accessToken && !requestUrl.startsWith('/auth/')) {
      clearStoredSession()
      localStorage.setItem('akshara.session.expired', 'true')
      window.dispatchEvent(new CustomEvent('akshara:session-expired'))
    }
    return Promise.reject(error)
  },
)

export default api
