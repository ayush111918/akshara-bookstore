import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import { deleteReview, getMyReviews, updateReview } from '../services/readerService'

function MyReviewsPage() {
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [draft, setDraft] = useState({ rating: 5, headline: '', content: '' })
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let active = true
    getMyReviews()
      .then((data) => { if (active) setReviews(data) })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, 'Your reviews could not be loaded.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  function startEditing(review) {
    setEditingId(review.id)
    setDraft({ rating: review.rating, headline: review.headline ?? '', content: review.content })
    setError('')
    setNotice('')
  }

  async function saveReview(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const updated = await updateReview(editingId, { ...draft, rating: Number(draft.rating) })
      setReviews((items) => items.map((review) => review.id === editingId ? updated : review))
      setEditingId(null)
      setNotice('Your review was updated.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your review could not be updated.'))
    } finally {
      setBusy(false)
    }
  }

  async function removeReview(review) {
    if (!window.confirm(`Delete your review of “${review.bookTitle}”? Its discussion replies will also be removed.`)) return
    setBusy(true)
    setError('')
    try {
      await deleteReview(review.id)
      setReviews((items) => items.filter((item) => item.id !== review.id))
      setNotice('Your review was deleted.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your review could not be deleted.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="inner-page my-reviews-page"><div className="container-xl">
      <header className="my-reviews-heading"><div><p className="eyebrow">Your reader voice</p><h1>My Reviews</h1><p>Edit or delete every review you have published from one place.</p></div><Link className="btn btn-ink" to="/community">Community discussions</Link></header>
      {error && <p className="form-alert" role="alert">{error}</p>}
      {notice && <div className="admin-feedback admin-feedback-success" role="status">{notice}</div>}
      {loading && <div className="page-loading"><span className="spinner-border" /> Loading your reviews…</div>}
      {!loading && reviews.length > 0 && <div className="my-review-list">{reviews.map((review) => <article className="my-review-item" key={review.id}>
        <div className="my-review-book">{review.coverImageUrl ? <img src={review.coverImageUrl} alt={`Cover of ${review.bookTitle}`} /> : <span><i className="bi bi-book" /></span>}<div><Link to={`/books/${review.bookId}`}>{review.bookTitle}</Link><small>Last updated {new Date(review.updatedAt).toLocaleDateString('en-IN', { dateStyle: 'medium' })}</small></div></div>
        {editingId === review.id ? <form className="my-review-edit-form" onSubmit={saveReview}><div className="rating-picker">{[1, 2, 3, 4, 5].map((rating) => <button className={rating <= draft.rating ? 'is-active' : ''} type="button" key={rating} onClick={() => setDraft((value) => ({ ...value, rating }))}>★</button>)}</div><input maxLength="120" placeholder="Headline (optional)" value={draft.headline} onChange={(event) => setDraft((value) => ({ ...value, headline: event.target.value }))} /><textarea required minLength="10" maxLength="2000" value={draft.content} onChange={(event) => setDraft((value) => ({ ...value, content: event.target.value }))} /><div><button className="btn btn-ink" disabled={busy} type="submit">Save changes</button><button className="text-button" type="button" onClick={() => setEditingId(null)}>Cancel</button></div></form> : <div className="my-review-content"><span>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span>{review.headline && <h2>{review.headline}</h2>}<p>{review.content}</p></div>}
        {editingId !== review.id && <div className="my-review-actions"><button type="button" onClick={() => startEditing(review)}><i className="bi bi-pencil" /> Edit</button><button className="danger" disabled={busy} type="button" onClick={() => removeReview(review)}><i className="bi bi-trash" /> Delete</button><Link to={`/books/${review.bookId}#reviews`}>View on book page</Link></div>}
      </article>)}</div>}
      {!loading && !reviews.length && <div className="empty-reader-page"><i className="bi bi-chat-quote" /><h2>You have not reviewed a book yet</h2><p>Open a book from your shelf or the catalogue to publish your first reflection.</p><Link className="btn btn-ink" to="/my-books">Go to My Books</Link></div>}
    </div></section>
  )
}

export default MyReviewsPage
