import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  formatPrice,
  getAuthors,
  getAvailability,
  getBookPrice,
  getCoverTone,
  getPrimaryEdition,
  getTitleMonogram,
} from '../utils/bookPresentation'
import useAuth from '../hooks/useAuth'
import useReaderData from '../hooks/useReaderData'
import { getApiErrorMessage } from '../services/api'

function BookCard({ book }) {
  const { user } = useAuth()
  const { wishlistBookIds, toggleWishlist, addCartItem } = useReaderData()
  const navigate = useNavigate()
  const location = useLocation()
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const edition = getPrimaryEdition(book)
  const availability = getAvailability(book)
  const category = book.categories?.[0]?.name
  const saved = wishlistBookIds.has(book.id)

  function requireReader() {
    if (user) return true
    navigate('/login', { state: { from: location } })
    return false
  }

  async function handleWishlist() {
    if (!requireReader()) return
    setBusy(true)
    try {
      await toggleWishlist(book)
      setMessage(saved ? 'Removed from wishlist' : 'Saved to wishlist')
    } catch (error) {
      setMessage(getApiErrorMessage(error, 'Wishlist could not be updated.'))
    } finally {
      setBusy(false)
    }
  }

  async function handleCart() {
    if (!requireReader() || !edition?.id) return
    setBusy(true)
    try {
      await addCartItem(edition.id)
      setMessage('Added to cart')
    } catch (error) {
      setMessage(getApiErrorMessage(error, 'Book could not be added to cart.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <article className="book-card">
      <div className="book-card-cover-wrap">
        <Link className="book-card-cover-link" to={`/books/${book.id}`} aria-label={`View ${book.title}`}>
          {book.coverImageUrl ? (
            <img className="book-card-cover" src={book.coverImageUrl} alt={`Cover of ${book.title}`} />
          ) : (
            <div className={`fallback-book-cover cover-tone-${getCoverTone(book)}`}>
              <span className="fallback-book-mark">अ</span>
              <strong>{getTitleMonogram(book.title)}</strong>
              <small>{book.title}</small>
            </div>
          )}
        </Link>

        <button
          className={`save-book-button${saved ? ' is-saved' : ''}`}
          type="button"
          aria-label={saved ? `Remove ${book.title} from wishlist` : `Save ${book.title} to wishlist`}
          aria-pressed={saved}
          disabled={busy}
          onClick={handleWishlist}
        >
          <i className={`bi ${saved ? 'bi-heart-fill' : 'bi-heart'}`} />
        </button>

        <span className={`availability-badge availability-${availability.tone}`}>
          {availability.label}
        </span>
      </div>

      <div className="book-card-content">
        <div className="book-card-meta">
          <span>{category || 'General reading'}</span>
          {edition?.format && <span>{edition.format}</span>}
        </div>

        <Link className="book-card-title" to={`/books/${book.id}`}>
          {book.title}
        </Link>

        <p className="book-card-author">by {getAuthors(book)}</p>

        <div className="book-card-rating" aria-label="Reader rating preview">
          <span aria-hidden="true">★</span>
          <strong>New</strong>
          <span>Reader discovery</span>
        </div>

        <div className="book-card-footer">
          <strong className="book-card-price">{formatPrice(getBookPrice(book))}</strong>
          <div className="book-card-actions">
            <button className="book-card-arrow" disabled={busy || availability.tone !== 'available'} onClick={handleCart} aria-label={`Add ${book.title} to cart`}><i className="bi bi-bag-plus" /></button>
            <Link className="book-card-arrow" to={`/books/${book.id}`} aria-label={`View details for ${book.title}`}><i className="bi bi-arrow-up-right" /></Link>
          </div>
        </div>
        {message && <span className="book-card-message" role="status">{message}</span>}
      </div>
    </article>
  )
}

export default BookCard
