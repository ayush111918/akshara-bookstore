import { Link, useNavigate } from 'react-router-dom'
import logo from '../assets/akshara-logo.png'
import useAuth from '../hooks/useAuth'
import useReaderData from '../hooks/useReaderData'

function Navbar() {
  const { user, logout } = useAuth()
  const { wishlist, cart } = useReaderData()
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN'

  function signOut() {
    logout()
    navigate('/')
  }

  return (
    <nav className="navbar navbar-expand-xl akshara-navbar sticky-top">
      <div className="container-xl">
        <Link
          className="navbar-brand akshara-brand"
          to={isAdmin ? '/admin/books' : '/'}
          aria-label={isAdmin ? 'Akshara administration home' : 'Akshara home'}
        >
          <span
            className="akshara-brand-mark"
            style={{ backgroundImage: `url(${logo})` }}
            aria-hidden="true"
          />
          <span className="akshara-brand-copy">
            <strong>AKSHARA</strong>
            <small>Discover · Read · Grow</small>
          </span>
        </Link>

        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#aksharaNavbar"
          aria-controls="aksharaNavbar"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon" />
        </button>

        <div
          className="collapse navbar-collapse"
          id="aksharaNavbar"
        >
          <ul className="navbar-nav ms-auto align-items-lg-center akshara-nav-links">
            {isAdmin ? (
              <>
                <li className="nav-item"><Link className="nav-link admin-storefront-link" to="/"><i className="bi bi-eye" /> Storefront preview</Link></li>
                <li className="nav-item"><Link className="nav-link admin-nav-link" to="/admin/orders"><i className="bi bi-box-seam" /> Orders</Link></li>
                <li className="nav-item"><Link className="nav-link admin-nav-link" to="/admin/books"><i className="bi bi-sliders" /> Catalogue</Link></li>
                <li className="nav-item"><Link className="nav-link admin-nav-link" to="/admin/audit-logs"><i className="bi bi-shield-check" /> Audit</Link></li>
              </>
            ) : (
              <>
                <li className="nav-item"><a className="nav-link" href="/#catalogue">Explore</a></li>
                <li className="nav-item">
                  {user?.role === 'READER' ? <Link className="nav-link" to="/reading-journey">Reading journey</Link> : <a className="nav-link" href="/#journey">Reading journey</a>}
                </li>
                <li className="nav-item"><Link className="nav-link" to="/community">Community</Link></li>
                {user?.role === 'READER' && (
                  <>
                    <li className="nav-item">
                      <Link className="nav-link reader-utility-link" to="/my-books"><i className="bi bi-journal-bookmark" /> My Books</Link>
                    </li>
                    <li className="nav-item">
                      <Link className="nav-link reader-utility-link" to="/orders"><i className="bi bi-receipt" /> Orders</Link>
                    </li>
                  </>
                )}
                <li className="nav-item">
                  <Link className="nav-icon-button nav-count-link" to="/wishlist" aria-label="Open wishlist">
                    <i className="bi bi-heart" />
                    {wishlist.length > 0 && <span>{wishlist.length}</span>}
                  </Link>
                </li>

                <li className="nav-item">
                  <Link className="nav-icon-button nav-count-link" to="/cart" aria-label="Open cart">
                    <i className="bi bi-bag" />
                    {cart?.totalQuantity > 0 && <span>{cart.totalQuantity}</span>}
                  </Link>
                </li>
              </>
            )}

            <li className="nav-item ms-lg-1">
              {user ? (
                <div className="nav-reader-menu">
                  <Link
                    className="nav-account-link"
                    to={isAdmin ? '/admin/books' : '/account'}
                    aria-label={isAdmin ? `Administrator account: ${user.fullName}` : `Reader account: ${user.fullName}`}
                  >
                    <i className="bi bi-person-circle" />
                    <span>{user.fullName.split(' ')[0]}</span>
                    {isAdmin && <small className="nav-role-badge">Admin</small>}
                  </Link>
                  <button type="button" onClick={signOut}>Sign out</button>
                </div>
              ) : (
                <div className="nav-auth-actions">
                  <Link className="nav-signin-link" to="/login">Sign in</Link>
                  <Link className="btn btn-ink nav-join-button" to="/register">Join Akshara</Link>
                </div>
              )}
            </li>
          </ul>
        </div>
      </div>
    </nav>
  )
}

export default Navbar
