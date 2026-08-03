import { Link } from 'react-router-dom'
import useAuth from '../hooks/useAuth'

function Footer() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  if (isAdmin) {
    return (
      <footer className="site-footer admin-site-footer">
        <div className="container-xl">
          <div className="footer-grid">
            <div>
              <Link className="footer-wordmark" to="/admin/books">AKSHARA ADMIN</Link>
              <p className="footer-description">Catalogue, inventory, fulfilment, and accountability tools for bookstore operations.</p>
            </div>
            <div>
              <p className="footer-heading">Operations</p>
              <Link to="/admin/books">Catalogue management</Link>
              <Link to="/admin/books/import">Import or add books</Link>
              <Link to="/admin/orders">Order fulfilment</Link>
              <Link to="/admin/audit-logs">Audit log</Link>
            </div>
            <div>
              <p className="footer-heading">Public store</p>
              <Link to="/">Preview storefront</Link>
            </div>
            <div className="footer-note">
              <span className="footer-letter"><i className="bi bi-shield-check" /></span>
              <p>Administrator workspace · Changes are recorded for accountability.</p>
            </div>
          </div>
          <div className="footer-bottom"><span>© 2026 Akshara</span><span>Administration workspace</span></div>
        </div>
      </footer>
    )
  }

  return (
    <footer className="site-footer">
      <div className="container-xl">
        <div className="footer-grid">
          <div>
            <Link className="footer-wordmark" to="/">AKSHARA</Link>
            <p className="footer-description">
              A reader-first bookstore for discovering ideas, sharing perspectives,
              and building a life around books.
            </p>
          </div>

          <div>
            <p className="footer-heading">Explore</p>
            <a href="/#catalogue">Book catalogue</a>
            <Link to="/reading-journey">Reading journey</Link>
            <Link to="/community">Community</Link>
          </div>

          <div>
            <p className="footer-heading">For readers</p>
            <Link to="/my-books">My Books</Link>
            <Link to="/reading-journey">Reading progress</Link>
            <Link to="/wishlist">Wishlist</Link>
            <Link to="/orders">Order history</Link>
            <Link to="/my-reviews">My reviews</Link>
            <Link to="/community">Reader reviews</Link>
          </div>

          <div className="footer-note">
            <span className="footer-letter">अ</span>
            <p>Every story, idea, and discovery begins with a letter.</p>
          </div>
        </div>

        <div className="footer-bottom">
          <span>© 2026 Akshara</span>
          <span>Discover · Read · Grow</span>
        </div>
      </div>
    </footer>
  )
}

export default Footer
