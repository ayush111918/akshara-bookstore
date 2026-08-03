import { Navigate, useLocation } from 'react-router-dom'
import useAuth from '../hooks/useAuth'

function ProtectedRoute({ children }) {
  const { ready, isAuthenticated } = useAuth()
  const location = useLocation()

  if (!ready) {
    return <div className="page-loading"><span className="spinner-border" /> Checking your reader account…</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location, sessionExpired: Boolean(localStorage.getItem('akshara.session.expired')) }} replace />
  }

  return children
}

export default ProtectedRoute
