import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getRecentReviews } from '../services/readerService'

function CommunityPage() {
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getRecentReviews(12).then(setReviews).catch(console.error).finally(() => setLoading(false))
  }, [])

  return <section className="inner-page community-page"><div className="container-xl"><div className="inner-page-heading"><p className="eyebrow">The reading room</p><h1>Books become richer in conversation.</h1><p>Recent ratings and reflections from Akshara readers.</p></div>{loading ? <p className="muted-message">Opening the reading room…</p> : reviews.length ? <div className="community-review-grid">{reviews.map((review) => <article className="community-review-card" key={review.id}><div><span className="reader-avatar">{review.readerName.split(/\s+/).map((part) => part[0]).slice(0, 2).join('')}</span><div><strong>{review.readerName}</strong><span>{'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}</span></div></div><p className="eyebrow">Review of {review.bookTitle}</p><h2>{review.headline || 'A reader’s perspective'}</h2><p>{review.content}</p><Link to={`/books/${review.bookId}#reviews`}>Continue the conversation <i className="bi bi-arrow-right" /></Link></article>)}</div> : <div className="empty-reader-page"><i className="bi bi-chat-quote" /><h2>The reading room is ready</h2><p>Be the first to review a book and begin a conversation.</p><Link className="btn btn-ink" to="/#catalogue">Find a book</Link></div>}</div></section>
}

export default CommunityPage
