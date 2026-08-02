import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getBookById } from '../services/bookService'
import {
  formatPrice,
  getAuthors,
  getAvailability,
  getBookPrice,
  getCoverTone,
  getPrimaryEdition,
  getTitleMonogram,
} from '../utils/bookPresentation'

function BookDetailsPage() {
  const { bookId } = useParams()
  const [book, setBook] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [added, setAdded] = useState(false)

  useEffect(() => {
    let active = true

    async function loadBook() {
      try {
        setLoading(true)
        setError('')
        const data = await getBookById(bookId)
        if (active) setBook(data)
      } catch (requestError) {
        console.error(requestError)
        if (active) setError('This book could not be loaded. It may have been removed or the backend may be unavailable.')
      } finally {
        if (active) setLoading(false)
      }
    }

    loadBook()
    window.scrollTo({ top: 0, behavior: 'auto' })

    return () => {
      active = false
    }
  }, [bookId])

  if (loading) {
    return (
      <section className="detail-page">
        <div className="container-xl detail-loading" aria-label="Loading book details">
          <div className="detail-cover-skeleton" />
          <div className="detail-copy-skeleton">
            <span /><span /><span /><span />
          </div>
        </div>
      </section>
    )
  }

  if (error || !book) {
    return (
      <section className="detail-page">
        <div className="container-xl">
          <div className="catalogue-message catalogue-error">
            <span><i className="bi bi-journal-x" /></span>
            <div>
              <h1>We could not find this book</h1>
              <p>{error}</p>
              <Link to="/">Return to the catalogue</Link>
            </div>
          </div>
        </div>
      </section>
    )
  }

  const primaryEdition = getPrimaryEdition(book)
  const availability = getAvailability(book)

  return (
    <section className="detail-page">
      <div className="container-xl">
        <nav className="detail-breadcrumb" aria-label="Breadcrumb">
          <Link to="/">Home</Link>
          <i className="bi bi-chevron-right" />
          <Link to="/#catalogue">Catalogue</Link>
          <i className="bi bi-chevron-right" />
          <span>{book.title}</span>
        </nav>

        <div className="detail-layout">
          <div className="detail-cover-column">
            <div className="detail-cover-frame">
              {book.coverImageUrl ? (
                <img src={book.coverImageUrl} alt={`Cover of ${book.title}`} />
              ) : (
                <div className={`fallback-book-cover detail-fallback-cover cover-tone-${getCoverTone(book)}`}>
                  <span className="fallback-book-mark">अ</span>
                  <strong>{getTitleMonogram(book.title)}</strong>
                  <small>{book.title}</small>
                </div>
              )}
            </div>
            <p className="detail-cover-note"><i className="bi bi-shield-check" /> Secure checkout · Reader-first support</p>
          </div>

          <div className="detail-content">
            <div className="detail-kicker-row">
              <span className={`availability-badge availability-${availability.tone}`}>{availability.label}</span>
              {book.languageCode && <span>{book.languageCode.toUpperCase()}</span>}
              {primaryEdition?.format && <span>{primaryEdition.format}</span>}
            </div>

            <h1>{book.title}</h1>
            {book.subtitle && <p className="detail-subtitle">{book.subtitle}</p>}
            <p className="detail-author">by <strong>{getAuthors(book)}</strong></p>

            <div className="detail-rating-row">
              <span className="stars">★★★★★</span>
              <strong>New arrival</strong>
              <span>Be the first to review</span>
            </div>

            <div className="detail-price-row">
              <strong>{formatPrice(getBookPrice(book))}</strong>
              <span>Inclusive of applicable taxes</span>
            </div>

            <div className="detail-actions">
              <button
                className={`btn btn-ink detail-cart-button${added ? ' is-added' : ''}`}
                type="button"
                disabled={availability.tone !== 'available'}
                onClick={() => setAdded((current) => !current)}
              >
                <i className={`bi ${added ? 'bi-check2' : 'bi-bag-plus'}`} />
                {added ? 'Added to cart' : 'Add to cart'}
              </button>
              <button
                className={`detail-save-button${saved ? ' is-saved' : ''}`}
                type="button"
                aria-pressed={saved}
                onClick={() => setSaved((current) => !current)}
              >
                <i className={`bi ${saved ? 'bi-heart-fill' : 'bi-heart'}`} />
                {saved ? 'Saved' : 'Save for later'}
              </button>
            </div>

            <div className="detail-description">
              <p className="eyebrow">About this book</p>
              <p>{book.description || 'A full description for this book will be added by the Akshara editorial team soon.'}</p>
            </div>

            {book.categories?.length > 0 && (
              <div className="detail-categories">
                {book.categories.map((category) => <span key={category.id}>{category.name}</span>)}
              </div>
            )}
          </div>
        </div>

        <div className="edition-section">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Choose your edition</p>
              <h2>Available formats</h2>
            </div>
          </div>

          {book.editions?.length ? (
            <div className="edition-grid">
              {book.editions.map((edition) => (
                <article className="edition-card" key={edition.id}>
                  <div>
                    <span className="edition-icon"><i className="bi bi-book" /></span>
                    <div>
                      <h3>{edition.editionName || edition.format || 'Book edition'}</h3>
                      <p>{edition.publisher?.name || 'Publisher unavailable'}</p>
                    </div>
                  </div>
                  <dl>
                    <div><dt>Format</dt><dd>{edition.format || '—'}</dd></div>
                    <div><dt>Pages</dt><dd>{edition.pageCount || '—'}</dd></div>
                    <div><dt>ISBN</dt><dd>{edition.isbn13 || edition.isbn10 || '—'}</dd></div>
                    <div><dt>Price</dt><dd>{formatPrice(edition.inventory?.price)}</dd></div>
                  </dl>
                </article>
              ))}
            </div>
          ) : (
            <p className="edition-empty">Edition information will be available soon.</p>
          )}
        </div>
      </div>
    </section>
  )
}

export default BookDetailsPage
