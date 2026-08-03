import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import { deleteCurrentAccount } from '../services/authService'
import { getApiErrorMessage } from '../services/api'

function AccountPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [confirmText, setConfirmText] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function deleteAccount(event) {
    event.preventDefault()
    if (!window.confirm('This permanently disables your Akshara account. Continue?')) return
    setBusy(true)
    setError('')
    try {
      await deleteCurrentAccount(password)
      logout()
      navigate('/login', { replace: true, state: { accountDeleted: true } })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your account could not be deleted.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="inner-page account-page"><div className="container-xl account-layout">
      <header><p className="eyebrow">Reader account</p><h1>Your Akshara account</h1><p>Review your sign-in identity and control your account.</p></header>
      <section className="account-card"><div className="account-avatar">{user.fullName.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</div><div><span>Full name</span><strong>{user.fullName}</strong><span>Email</span><strong>{user.email}</strong><span>Access</span><strong>{user.role}</strong></div></section>
      <section className="account-danger"><p className="eyebrow">Danger zone</p><h2>Delete account</h2><p>Your login will be disabled immediately and your name and email will be anonymized. Historical orders remain as business records. This action cannot be undone.</p>
        <form onSubmit={deleteAccount}><label>Current password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} maxLength="72" autoComplete="current-password" required /></label><label>Type DELETE to confirm<input value={confirmText} onChange={(event) => setConfirmText(event.target.value)} required /></label>{error && <p className="form-alert" role="alert">{error}</p>}<button className="btn account-delete-button" disabled={busy || confirmText !== 'DELETE'} type="submit">{busy ? 'Deleting…' : 'Delete my account'}</button></form>
      </section>
    </div></section>
  )
}

export default AccountPage
