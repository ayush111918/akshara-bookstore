import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  formatPrice,
  getAuthors,
  getAvailability,
  getBookPrice,
  getCoverTone,
  getPrimaryEdition,
  getTitleMonogram,
} from '../utils/bookPresentation'

function BookCard({ book }) {
  const [saved, setSaved] = useState(false)
  const edition = getPrimaryEdition(book)
  const availability = getAvailability(book)
  const category = book.categories?.[0]?.name

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
          onClick={() => setSaved((current) => !current)}
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
          <Link className="book-card-arrow" to={`/books/${book.id}`} aria-label={`View details for ${book.title}`}>
            <i className="bi bi-arrow-up-right" />
          </Link>
        </div>
      </div>
    </article>
  )
}

export default BookCard
