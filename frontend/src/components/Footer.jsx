import { Link } from 'react-router-dom'

function Footer() {
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
            <a href="/#journey">Reading journey</a>
            <a href="/#community">Community</a>
          </div>

          <div>
            <p className="footer-heading">For readers</p>
            <span>Wishlist</span>
            <span>Personal library</span>
            <span>Curated collections</span>
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
