import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getRecentReviews } from '../services/readerService'
import DiscussionThread from '../components/DiscussionThread'
import useAuth from '../hooks/useAuth'
import ReviewTrustBadges from '../components/ReviewTrustBadges'
import CommunitySidebar from '../components/CommunitySidebar'

function initials(name = 'Reader') {
  return name.split(/\s+/).filter(Boolean).map((part) => part[0]).slice(0, 2).join('').toUpperCase()
}

function reviewDate(value) {
  if (!value) return 'Recently reviewed'
  return new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

function ReviewCover({ review }) {
  const [failed, setFailed] = useState(false)
  if (!review.coverImageUrl || failed) {
    return <span className="community-book-cover community-book-cover-placeholder" aria-hidden="true"><i className="bi bi-book" /></span>
  }
  return <img className="community-book-cover" src={review.coverImageUrl} alt={`Cover of ${review.bookTitle}`} onError={() => setFailed(true)} />
}

function CommunityPage() {
  const { user } = useAuth()
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState('latest')
  const [filter, setFilter] = useState('all')

  async function loadReviews() {
    try {
      setLoading(true)
      setError('')
      setReviews(await getRecentReviews(20))
    } catch (requestError) {
      console.error(requestError)
      setError('Reader reviews could not be loaded. Please check the backend and try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getRecentReviews(20)
      .then((data) => {
        if (active) setReviews(data)
      })
      .catch((requestError) => {
        console.error(requestError)
        if (active) setError('Reader reviews could not be loaded. Please check the backend and try again.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const summary = useMemo(() => {
    const books = new Set(reviews.map((review) => review.bookId)).size
    const readers = new Set(reviews.map((review) => review.userId)).size
    const average = reviews.length
      ? reviews.reduce((total, review) => total + review.rating, 0) / reviews.length
      : 0
    return { books, readers, average }
  }, [reviews])

  const visibleReviews = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    const trusted = reviews.filter((review) => {
      if (filter === 'verified') return review.verifiedPurchase
      if (filter === 'completed') return review.completedOnAkshara
      return true
    })
    const filtered = normalizedQuery
      ? trusted.filter((review) => [review.bookTitle, review.readerName, review.headline, review.content]
        .some((value) => value?.toLowerCase().includes(normalizedQuery)))
      : trusted
    return [...filtered].sort((left, right) => {
      if (sort === 'highest') return right.rating - left.rating || new Date(right.createdAt) - new Date(left.createdAt)
      if (sort === 'oldest') return new Date(left.createdAt) - new Date(right.createdAt)
      return new Date(right.createdAt) - new Date(left.createdAt)
    })
  }, [filter, query, reviews, sort])

  return (
    <section className="inner-page community-page">
      <div className="container-xl">
        <header className="community-page-hero">
          <div className="inner-page-heading">
            <p className="eyebrow">Community reviews</p>
            <h1>See what Akshara’s readers are reading.</h1>
            <p>Discover books through honest ratings and reflections shared by the reader community.</p>
          </div>
          <div className="community-hero-actions">{user?.role === 'READER' && <Link className="btn btn-outline-dark" to="/my-reviews">Manage my reviews</Link>}<Link className="btn btn-ink" to="/#catalogue">Browse books to review <i className="bi bi-arrow-right" /></Link></div>
        </header>

        <div className="community-summary" aria-label="Community review summary">
          <div><strong>{reviews.length}</strong><span>Recent reviews</span></div>
          <div><strong>{summary.books}</strong><span>Books reviewed</span></div>
          <div><strong>{summary.readers}</strong><span>Reader voices</span></div>
          <div><strong>{reviews.length ? summary.average.toFixed(1) : '—'}</strong><span>Average rating</span></div>
        </div>

        {!loading && !error && reviews.length > 0 && (
          <div className="community-toolbar">
            <div className="community-trust-filters" role="group" aria-label="Filter community reviews">
              <span>Show</span>
              <button className={filter === 'all' ? 'is-active' : ''} type="button" onClick={() => setFilter('all')}>All voices</button>
              <button className={filter === 'verified' ? 'is-active' : ''} type="button" onClick={() => setFilter('verified')}><i className="bi bi-bag-check" /> Verified purchases</button>
              <button className={filter === 'completed' ? 'is-active' : ''} type="button" onClick={() => setFilter('completed')}><i className="bi bi-journal-check" /> Completed here</button>
            </div>
            <label>
              <i className="bi bi-search" />
              <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search books, readers, or reviews" />
            </label>
            <label className="community-sort">
              <span>Sort by</span>
              <select value={sort} onChange={(event) => setSort(event.target.value)}>
                <option value="latest">Latest reviews</option>
                <option value="highest">Highest rated</option>
                <option value="oldest">Oldest first</option>
              </select>
            </label>
          </div>
        )}

        <div className="community-content-layout">
          <div className="community-feed">
            {!loading && !error && reviews.length > 0 && <header><p className="eyebrow">Reader perspectives</p><h2>{visibleReviews.length} {visibleReviews.length === 1 ? 'conversation' : 'conversations'} to explore</h2></header>}

            {loading && <div className="community-loading"><span className="spinner-border" /><p>Loading reader reviews…</p></div>}

            {!loading && error && (
              <div className="empty-reader-page community-error-state">
                <i className="bi bi-cloud-slash" />
                <h2>The reviews are temporarily unavailable</h2>
                <p>{error}</p>
                <button type="button" className="btn btn-ink" onClick={loadReviews}>Try again</button>
              </div>
            )}

            {!loading && !error && reviews.length > 0 && visibleReviews.length > 0 && (
              <div className="community-review-grid">
                {visibleReviews.map((review) => (
                  <article className="community-review-card" key={review.id}>
                    <div className="community-review-book">
                      <ReviewCover review={review} />
                      <div><p className="eyebrow">Reader review</p><Link to={`/books/${review.bookId}`}>{review.bookTitle}</Link><span>{reviewDate(review.createdAt)}</span></div>
                    </div>
                    <div className="community-review-author">
                      <span className="reader-avatar">{initials(review.readerName)}</span>
                      <div><strong>{review.readerName}</strong><span aria-label={`${review.rating} out of 5 stars`}>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span></div>
                    </div>
                    <ReviewTrustBadges review={review} />
                    <h2>{review.headline || 'A reader’s perspective'}</h2>
                    <p>{review.content}</p>
                    <Link className="community-review-link" to={`/books/${review.bookId}#reviews`}>View book and reviews <i className="bi bi-arrow-right" /></Link>
                    <DiscussionThread reviewId={review.id} />
                  </article>
                ))}
              </div>
            )}

            {!loading && !error && reviews.length > 0 && visibleReviews.length === 0 && (
              <div className="empty-reader-page community-no-results">
                <i className="bi bi-search" />
                <h2>No reviews match these filters</h2>
                <p>Try another phrase or return to all reader voices.</p>
                <button type="button" className="btn btn-ink" onClick={() => { setQuery(''); setFilter('all') }}>Clear filters</button>
              </div>
            )}

            {!loading && !error && reviews.length === 0 && (
              <div className="empty-reader-page">
                <i className="bi bi-chat-quote" />
                <h2>Share the first reader review</h2>
                <p>Choose a book from the catalogue, open its details, and publish your rating and reflection.</p>
                <Link className="btn btn-ink" to="/#catalogue">Find a book to review</Link>
              </div>
            )}
          </div>
          <CommunitySidebar reviews={reviews} />
        </div>
      </div>
    </section>
  )
}

export default CommunityPage
