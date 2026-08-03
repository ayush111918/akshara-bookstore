import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'
import {
  createReview,
  deleteReview,
  getBookReviews,
  updateReview,
} from '../services/readerService'
import ReviewTrustBadges from './ReviewTrustBadges'

function ReviewSection({ bookId }) {
  const { user } = useAuth()
  const [data, setData] = useState({ reviews: [], reviewCount: 0, averageRating: 0 })
  const [form, setForm] = useState({ rating: 5, headline: '', content: '' })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const ownReview = useMemo(
    () => data.reviews.find((review) => Number(review.userId) === Number(user?.id)),
    [data.reviews, user],
  )

  async function loadReviews() {
    try {
      const nextData = await getBookReviews(bookId)
      setData(nextData)
      const currentReview = nextData.reviews.find((review) => Number(review.userId) === Number(user?.id))
      if (currentReview) {
        setForm({
          rating: currentReview.rating,
          headline: currentReview.headline ?? '',
          content: currentReview.content,
        })
      }
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Reviews could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getBookReviews(bookId)
      .then((nextData) => {
        if (!active) return
        setData(nextData)
        const currentReview = nextData.reviews.find((review) => Number(review.userId) === Number(user?.id))
        if (currentReview) {
          setForm({ rating: currentReview.rating, headline: currentReview.headline ?? '', content: currentReview.content })
        }
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, 'Reviews could not be loaded.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [bookId, user])

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const payload = { rating: Number(form.rating), headline: form.headline, content: form.content }
      if (ownReview) {
        await updateReview(ownReview.id, payload)
      } else {
        await createReview({ ...payload, bookId: Number(bookId) })
      }
      await loadReviews()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your review could not be saved.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!ownReview || !window.confirm('Delete your review?')) return
    setSaving(true)
    try {
      await deleteReview(ownReview.id)
      setForm({ rating: 5, headline: '', content: '' })
      await loadReviews()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your review could not be deleted.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="reviews-section" id="reviews">
      <div className="section-heading review-heading">
        <div>
          <p className="eyebrow">Reader perspectives</p>
          <h2>What the community thinks</h2>
        </div>
        <div className="review-summary">
          <strong>{data.reviewCount ? data.averageRating.toFixed(1) : 'New'}</strong>
          <span>{data.reviewCount} {data.reviewCount === 1 ? 'review' : 'reviews'}</span>
        </div>
      </div>

      {user?.role === 'READER' ? (
        <form className="review-form" onSubmit={handleSubmit}>
          {ownReview && <div className="review-editing-notice"><i className="bi bi-pencil-square" /> You are editing your published review. You can update or delete it here, or from <Link to="/my-reviews">My Reviews</Link>.</div>}
          <div className="rating-picker" aria-label="Rating">
            {[1, 2, 3, 4, 5].map((rating) => (
              <button
                type="button"
                key={rating}
                className={rating <= form.rating ? 'is-active' : ''}
                onClick={() => setForm((current) => ({ ...current, rating }))}
                aria-label={`${rating} stars`}
              >★</button>
            ))}
          </div>
          <input
            value={form.headline}
            onChange={(event) => setForm((current) => ({ ...current, headline: event.target.value }))}
            placeholder="A short headline (optional)"
            maxLength="120"
          />
          <textarea
            value={form.content}
            onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))}
            placeholder="Share what stayed with you about this book…"
            minLength="10"
            maxLength="2000"
            required
          />
          <div>
            <button className="btn btn-ink" disabled={saving} type="submit">
              {saving ? 'Saving…' : ownReview ? 'Update my review' : 'Publish review'}
            </button>
            {ownReview && <button className="text-button danger" disabled={saving} type="button" onClick={handleDelete}>Delete</button>}
          </div>
        </form>
      ) : user?.role === 'ADMIN' ? (
        <div className="review-signin">Reviews are published from reader accounts. Administrators can still read the community's reviews here.</div>
      ) : (
        <div className="review-signin">Already read it? <Link to="/login">Sign in to write a review.</Link></div>
      )}

      {error && <p className="form-alert" role="alert">{error}</p>}
      {loading ? (
        <p className="muted-message">Loading reader reviews…</p>
      ) : data.reviews.length ? (
        <div className="review-list">
          {data.reviews.map((review) => (
            <article className="review-card" key={review.id}>
              <div className="reader-avatar">{review.readerName.split(/\s+/).map((part) => part[0]).slice(0, 2).join('')}</div>
              <div>
                <div className="review-card-meta">
                  <strong>{review.readerName}</strong>
                  <span>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span>
                </div>
                <ReviewTrustBadges review={review} />
                {review.headline && <h3>{review.headline}</h3>}
                <p>{review.content}</p>
                <small>{new Date(review.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' })}</small>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <p className="muted-message">No reviews yet. Begin the conversation for this book.</p>
      )}
    </section>
  )
}

export default ReviewSection
