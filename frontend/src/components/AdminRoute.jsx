import { Navigate, useLocation } from 'react-router-dom'
import useAuth from '../hooks/useAuth'

function AdminRoute({ children }) {
  const { ready, isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!ready) {
    return <div className="page-loading"><span className="spinner-border" /> Checking admin access…</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{
      from: location,
      roleChanged: Boolean(localStorage.getItem('akshara.session.roleChanged')),
    }} replace />
  }

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return children
}

export default AdminRoute
