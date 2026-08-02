import { Link } from 'react-router-dom'
import logo from '../assets/akshara-logo.png'

function Navbar() {
  return (
    <nav className="navbar navbar-expand-lg akshara-navbar sticky-top">
      <div className="container-xl">
        <Link className="navbar-brand akshara-brand" to="/" aria-label="Akshara home">
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
            <li className="nav-item">
              <a className="nav-link" href="/#catalogue">Explore</a>
            </li>

            <li className="nav-item">
              <a className="nav-link" href="/#journey">Reading journey</a>
            </li>

            <li className="nav-item">
              <a className="nav-link" href="/#community">Community</a>
            </li>

            <li className="nav-item">
              <button className="nav-icon-button" type="button" aria-label="Open wishlist" title="Wishlist preview">
                <i className="bi bi-heart" />
              </button>
            </li>

            <li className="nav-item">
              <button className="nav-icon-button" type="button" aria-label="Open cart" title="Cart preview">
                <i className="bi bi-bag" />
              </button>
            </li>

            <li className="nav-item ms-lg-1">
              <button className="btn btn-ink nav-join-button" type="button">
                Join Akshara
              </button>
            </li>
          </ul>
        </div>
      </div>
    </nav>
  )
}

export default Navbar
