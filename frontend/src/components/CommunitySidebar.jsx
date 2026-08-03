import { Link } from 'react-router-dom'

function conversationBooks(reviews) {
  const byBook = new Map()

  for (const review of reviews) {
    const current = byBook.get(review.bookId) ?? {
      id: review.bookId,
      title: review.bookTitle,
      coverImageUrl: review.coverImageUrl,
      reviews: 0,
      ratingTotal: 0,
    }
    current.reviews += 1
    current.ratingTotal += review.rating
    byBook.set(review.bookId, current)
  }

  return [...byBook.values()]
    .sort((left, right) => right.reviews - left.reviews || right.ratingTotal - left.ratingTotal)
    .slice(0, 4)
}

function CommunitySidebar({ reviews }) {
  const books = conversationBooks(reviews)

  return (
    <aside className="community-sidebar" aria-label="About the Akshara community">
      <section className="community-side-card community-guide-card">
        <p className="eyebrow">The reading room</p>
        <h2>A review starts the conversation.</h2>
        <ol>
          <li><span>1</span><p><strong>Finish or explore a book</strong><small>Build a perspective worth sharing.</small></p></li>
          <li><span>2</span><p><strong>Publish a review</strong><small>Rate the book and explain what stayed with you.</small></p></li>
          <li><span>3</span><p><strong>Open a discussion</strong><small>Readers can question, respond, and add another interpretation.</small></p></li>
        </ol>
      </section>

      {books.length > 0 && (
        <section className="community-side-card">
          <p className="eyebrow">In conversation</p>
          <h2>Books readers are discussing</h2>
          <div className="community-conversation-books">
            {books.map((book) => (
              <Link to={`/books/${book.id}#reviews`} key={book.id}>
                {book.coverImageUrl
                  ? <img src={book.coverImageUrl} alt="" />
                  : <span><i className="bi bi-book" /></span>}
                <p><strong>{book.title}</strong><small>{book.reviews} {book.reviews === 1 ? 'review' : 'reviews'} · {(book.ratingTotal / book.reviews).toFixed(1)} ★</small></p>
                <i className="bi bi-arrow-up-right" />
              </Link>
            ))}
          </div>
        </section>
      )}

      <section className="community-privacy-note">
        <i className="bi bi-shield-check" />
        <p><strong>Your public voice, your private notebook.</strong><span>Reviews and replies are shared. Reading notes, quotations, and bookmarks remain private.</span></p>
      </section>
    </aside>
  )
}

export default CommunitySidebar
