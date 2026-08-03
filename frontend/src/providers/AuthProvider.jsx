import { useCallback, useEffect, useMemo, useState } from 'react'
import AuthContext from '../contexts/authContext'
import { getCurrentUser, loginReader, registerReader } from '../services/authService'
import { clearStoredSession, loadStoredSession, storeSession } from '../services/api'

function AuthProvider({ children }) {
  const [initialSession] = useState(loadStoredSession)
  const [user, setUser] = useState(initialSession?.user ?? null)
  const [ready, setReady] = useState(!initialSession?.accessToken)

  useEffect(() => {
    if (!initialSession?.accessToken) return

    let active = true
    getCurrentUser()
      .then((currentUser) => {
        if (!active) return
        setUser(currentUser)
        storeSession({ ...initialSession, user: currentUser })
      })
      .catch(() => {
        if (!active) return
        clearStoredSession()
        setUser(null)
      })
      .finally(() => {
        if (active) setReady(true)
      })

    return () => { active = false }
  }, [initialSession])

  useEffect(() => {
    function expireSession() {
      clearStoredSession()
      setUser(null)
    }
    window.addEventListener('akshara:session-expired', expireSession)
    return () => window.removeEventListener('akshara:session-expired', expireSession)
  }, [])

  const acceptSession = useCallback((session) => {
    storeSession(session)
    localStorage.removeItem('akshara.session.expired')
    setUser(session.user)
    return session.user
  }, [])

  const login = useCallback(async (credentials) => {
    return acceptSession(await loginReader(credentials))
  }, [acceptSession])

  const register = useCallback(async (details) => {
    return acceptSession(await registerReader(details))
  }, [acceptSession])

  const logout = useCallback(() => {
    clearStoredSession()
    localStorage.removeItem('akshara.session.expired')
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, ready, isAuthenticated: Boolean(user), login, register, logout }),
    [user, ready, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default AuthProvider
