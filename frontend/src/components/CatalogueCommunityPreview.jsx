import { Link } from 'react-router-dom'
import ReviewTrustBadges from './ReviewTrustBadges'

function initials(name) {
  return name.split(/\s+/).map((part) => part[0]).slice(0, 2).join('')
}

function CatalogueCommunityPreview({ reviews }) {
  return (
    <section className="community-section" id="community" aria-labelledby="community-heading">
      <div className="container-xl">
        <div className="section-heading community-heading">
          <div><p className="eyebrow">The reading room</p><h2 id="community-heading">Books become richer in conversation.</h2><p>Discover the lists, reviews, and discussions taking shape across the Akshara community.</p></div>
          <Link className="btn btn-ink" to="/community">Visit the community <i className="bi bi-arrow-right" /></Link>
        </div>
        <div className="community-grid">
          {reviews.map((review) => (
            <article className="community-card" key={review.id}>
              <div className="community-card-top"><span className="reader-avatar">{initials(review.readerName)}</span><div><strong>{review.readerName}</strong><small>{review.rating}/5 reader review</small></div><i className="bi bi-bookmark" /></div>
              <ReviewTrustBadges review={review} />
              <h3>{review.headline || `Thoughts on ${review.bookTitle}`}</h3><p>{review.content}</p>
              <Link to={`/books/${review.bookId}#reviews`} aria-label={`Read review of ${review.bookTitle}`}><i className="bi bi-arrow-up-right" /></Link>
            </article>
          ))}
          {!reviews.length && <article className="community-card community-empty-card"><div className="community-card-top"><span className="reader-avatar">अ</span><div><strong>The reading room</strong><small>Ready for its first voice</small></div></div><h3>Be the first reader to begin a conversation.</h3><p>Open any book, add a rating and share what stayed with you.</p><Link to="/community" aria-label="Visit the community"><i className="bi bi-arrow-up-right" /></Link></article>}
        </div>
      </div>
    </section>
  )
}

export default CatalogueCommunityPreview
