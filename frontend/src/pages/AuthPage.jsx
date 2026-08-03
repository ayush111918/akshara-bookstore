import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'

function AuthPage({ mode }) {
  const isRegister = mode === 'register'
  const { isAuthenticated, login, register } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  if (isAuthenticated) return <Navigate to="/" replace />

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      if (isRegister) await register(form)
      else await login({ email: form.email, password: form.password })
      navigate(location.state?.from?.pathname || '/', { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, isRegister ? 'Registration failed.' : 'Login failed.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-panel auth-story">
        <span className="auth-akshar">अ</span>
        <p className="eyebrow eyebrow-light">Your reading life, remembered</p>
        <h1>{isRegister ? 'Begin your Akshara journey.' : 'Welcome back, reader.'}</h1>
        <p>Save discoveries, build your cart, place orders, and add your voice to the reading room.</p>
      </div>
      <div className="auth-panel auth-form-panel">
        <form className="auth-form" onSubmit={handleSubmit}>
          <p className="eyebrow">Reader account</p>
          <h2>{isRegister ? 'Join Akshara' : 'Sign in'}</h2>
          {!isRegister && location.state?.sessionExpired && <p className="form-alert" role="status">Your session expired. Sign in again to continue safely.</p>}
          {!isRegister && location.state?.accountDeleted && <p className="auth-success-note" role="status">Your account was deleted and sign-in access was disabled.</p>}
          {isRegister && (
            <label>Full name<input name="fullName" value={form.fullName} onChange={updateField} maxLength="100" required /></label>
          )}
          <label>Email<input name="email" type="email" value={form.email} onChange={updateField} maxLength="254" required /></label>
          <label>Password<span className="password-field"><input name="password" type={showPassword ? 'text' : 'password'} value={form.password} onChange={updateField} minLength={isRegister ? 8 : undefined} maxLength="72" autoComplete={isRegister ? 'new-password' : 'current-password'} required /><button type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}><i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`} /></button></span></label>
          {isRegister && <p className="auth-privacy-note"><i className="bi bi-shield-check" /> No email verification is required for this academic version. Use a password you do not use elsewhere.</p>}
          {error && <p className="form-alert" role="alert">{error}</p>}
          <button className="btn btn-ink" type="submit" disabled={submitting}>
            {submitting ? 'Please wait…' : isRegister ? 'Create reader account' : 'Continue to Akshara'}
          </button>
          <p className="auth-switch">
            {isRegister ? 'Already a reader?' : 'New to Akshara?'}{' '}
            <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Sign in' : 'Create an account'}</Link>
          </p>
        </form>
      </div>
    </section>
  )
}

export default AuthPage
