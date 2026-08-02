import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import BookCard from '../components/BookCard'
import { getBooks } from '../services/bookService'
import { getRecentReviews } from '../services/readerService'

const categories = [
  { name: 'Fiction', icon: 'bi-stars' },
  { name: 'Indian literature', icon: 'bi-feather' },
  { name: 'Technology', icon: 'bi-cpu' },
  { name: 'History', icon: 'bi-hourglass-split' },
  { name: 'Philosophy', icon: 'bi-lightbulb' },
  { name: 'Children', icon: 'bi-balloon' },
]

const journeySteps = [
  { number: '01', title: 'Discover', text: 'Find books through ideas, moods, genres, and curated shelves.' },
  { number: '02', title: 'Evaluate', text: 'Understand a book through details, editions, ratings, and reviews.' },
  { number: '03', title: 'Organize', text: 'Shape a personal library with wishlists and reading statuses.' },
  { number: '04', title: 'Connect', text: 'Exchange perspectives through discussions and reader collections.' },
  { number: '05', title: 'Purchase', text: 'Choose an edition and complete the journey in one place.' },
]

function CataloguePage() {
  const [books, setBooks] = useState([])
  const [search, setSearch] = useState('')
  const [appliedQuery, setAppliedQuery] = useState('')
  const [sort, setSort] = useState('TITLE_ASC')
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [recentReviews, setRecentReviews] = useState([])

  async function loadBooks(query = '', nextSort = 'TITLE_ASC') {
    try {
      setLoading(true)
      setError('')

      const data = await getBooks({ query, sort: nextSort })
      const content = Array.isArray(data) ? data : data.content ?? []

      setBooks(content)
      setTotal(Array.isArray(data) ? content.length : data.totalElements ?? content.length)
      setAppliedQuery(query.trim())
    } catch (requestError) {
      console.error(requestError)
      setError('The catalogue could not be loaded. Confirm that the Akshara backend is running on port 8080.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true

    getBooks()
      .then((data) => {
        if (!active) return
        const content = Array.isArray(data) ? data : data.content ?? []
        setBooks(content)
        setTotal(Array.isArray(data) ? content.length : data.totalElements ?? content.length)
      })
      .catch((requestError) => {
        console.error(requestError)
        if (active) {
          setError('The catalogue could not be loaded. Confirm that the Akshara backend is running on port 8080.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    getRecentReviews(3).then(setRecentReviews).catch((requestError) => {
      console.error('Unable to load community preview', requestError)
    })
  }, [])

  function handleSubmit(event) {
    event.preventDefault()
    loadBooks(search, sort)
    document.querySelector('#catalogue')?.scrollIntoView({ behavior: 'smooth' })
  }

  function handleCategory(category) {
    setSearch(category)
    loadBooks(category, sort)
    document.querySelector('#catalogue')?.scrollIntoView({ behavior: 'smooth' })
  }

  function handleSort(event) {
    const nextSort = event.target.value
    setSort(nextSort)
    loadBooks(appliedQuery, nextSort)
  }

  function clearSearch() {
    setSearch('')
    loadBooks('', sort)
  }

  return (
    <>
      <section className="home-hero">
        <div className="hero-orb hero-orb-one" aria-hidden="true" />
        <div className="hero-orb hero-orb-two" aria-hidden="true" />

        <div className="container-xl hero-layout">
          <div className="hero-copy">
            <p className="eyebrow hero-eyebrow">
              <span /> A reader-first bookstore
            </p>
            <h1>
              Every book begins<br />
              with an <em>अक्षर.</em>
            </h1>
            <p className="hero-lead">
              Discover books with depth, understand them through people, and build
              a reading life that keeps growing.
            </p>

            <form className="hero-search" onSubmit={handleSubmit} role="search">
              <i className="bi bi-search" aria-hidden="true" />
              <input
                type="search"
                aria-label="Search the book catalogue"
                placeholder="Search by title, author, or category"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              <button type="submit">Explore books</button>
            </form>

            <div className="hero-proof" aria-label="Akshara platform highlights">
              <span><i className="bi bi-compass" /> Curated discovery</span>
              <span><i className="bi bi-people" /> Reader community</span>
              <span><i className="bi bi-bookmark-heart" /> Personal library</span>
            </div>
          </div>

          <div className="hero-visual" aria-label="A collection of books curated for Akshara readers">
            <div className="hero-sun" aria-hidden="true" />
            <div className="hero-book hero-book-one">
              <span>AKSHARA<br />EDITIONS</span>
              <strong>The Art of<br />Curiosity</strong>
              <small>Ideas that open new doors</small>
            </div>
            <div className="hero-book hero-book-two">
              <span>READER'S PICK</span>
              <strong>Stories<br />from Home</strong>
              <small>Indian voices · Volume I</small>
            </div>
            <div className="hero-book hero-book-three">
              <span>NEW THOUGHT</span>
              <strong>The Quiet<br />Future</strong>
              <small>Technology & humanity</small>
            </div>
            <div className="hero-quote-card">
              <i className="bi bi-quote" />
              <p>A book is not only bought. It is discovered, understood, shared, and remembered.</p>
              <span>— The Akshara idea</span>
            </div>
            <div className="hero-seal"><span>अ</span><small>Begin here</small></div>
          </div>
        </div>
      </section>

      <section className="category-strip" aria-labelledby="category-heading">
        <div className="container-xl">
          <div className="category-strip-heading">
            <div>
              <p className="eyebrow">Browse by interest</p>
              <h2 id="category-heading">Where does your curiosity lead?</h2>
            </div>
            <span>Choose a shelf to begin</span>
          </div>

          <div className="category-list">
            {categories.map((category) => (
              <button type="button" key={category.name} onClick={() => handleCategory(category.name)}>
                <span><i className={`bi ${category.icon}`} /></span>
                {category.name}
                <i className="bi bi-arrow-right" />
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="catalogue-section" id="catalogue" aria-labelledby="catalogue-heading">
        <div className="container-xl">
          <div className="section-heading catalogue-heading">
            <div>
              <p className="eyebrow">The Akshara shelf</p>
              <h2 id="catalogue-heading">Books worth finding</h2>
              <p>
                {appliedQuery
                  ? `Showing matches for “${appliedQuery}”`
                  : 'Browse the latest books added to our growing catalogue.'}
              </p>
            </div>

            <div className="catalogue-tools">
              {!loading && <span className="result-count">{total} {total === 1 ? 'book' : 'books'}</span>}
              {appliedQuery && (
                <button className="clear-search-button" type="button" onClick={clearSearch}>
                  Clear search
                </button>
              )}
              <label className="sort-control">
                <span>Sort</span>
                <select value={sort} onChange={handleSort}>
                  <option value="TITLE_ASC">Title A–Z</option>
                  <option value="TITLE_DESC">Title Z–A</option>
                  <option value="NEWEST">Newest first</option>
                  <option value="OLDEST">Oldest first</option>
                </select>
              </label>
            </div>
          </div>

          {loading && (
            <div className="book-grid" aria-label="Loading catalogue">
              {[0, 1, 2, 3].map((item) => (
                <div className="book-card-skeleton" key={item}>
                  <span className="skeleton-cover" />
                  <span className="skeleton-line skeleton-line-short" />
                  <span className="skeleton-line" />
                  <span className="skeleton-line skeleton-line-medium" />
                </div>
              ))}
            </div>
          )}

          {error && (
            <div className="catalogue-message catalogue-error" role="alert">
              <span><i className="bi bi-plug" /></span>
              <div>
                <h3>The shelf is temporarily unavailable</h3>
                <p>{error}</p>
                <button type="button" onClick={() => loadBooks(appliedQuery, sort)}>Try again</button>
              </div>
            </div>
          )}

          {!loading && !error && books.length === 0 && (
            <div className="catalogue-message">
              <span><i className="bi bi-search" /></span>
              <div>
                <h3>No books found for “{appliedQuery}”</h3>
                <p>Try another title, author, or interest—or return to the full Akshara shelf.</p>
                <button type="button" onClick={clearSearch}>View all books</button>
              </div>
            </div>
          )}

          {!loading && !error && books.length > 0 && (
            <div className="book-grid">
              {books.map((book) => <BookCard book={book} key={book.id} />)}
            </div>
          )}
        </div>
      </section>

      <section className="journey-section" id="journey" aria-labelledby="journey-heading">
        <div className="container-xl">
          <div className="journey-intro">
            <p className="eyebrow eyebrow-light">More than a transaction</p>
            <h2 id="journey-heading">The complete reader journey,<br />in one thoughtful place.</h2>
            <p>
              Akshara connects the moments before and after purchase—the questions,
              conversations, collections, and discoveries that turn books into a reading life.
            </p>
          </div>

          <div className="journey-steps">
            {journeySteps.map((step) => (
              <article key={step.number}>
                <span>{step.number}</span>
                <h3>{step.title}</h3>
                <p>{step.text}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="community-section" id="community" aria-labelledby="community-heading">
        <div className="container-xl">
          <div className="section-heading community-heading">
            <div>
              <p className="eyebrow">The reading room</p>
              <h2 id="community-heading">Books become richer in conversation.</h2>
              <p>Discover the lists, reviews, and discussions taking shape across the Akshara community.</p>
            </div>
            <Link className="btn btn-ink" to="/community">Visit the community <i className="bi bi-arrow-right" /></Link>
          </div>

          <div className="community-grid">
            {recentReviews.map((review) => (
              <article className="community-card" key={review.id}>
                <div className="community-card-top">
                  <span className="reader-avatar">{review.readerName.split(/\s+/).map((part) => part[0]).slice(0, 2).join('')}</span>
                  <div><strong>{review.readerName}</strong><small>{review.rating}/5 reader review</small></div>
                  <i className="bi bi-bookmark" />
                </div>
                <h3>{review.headline || `Thoughts on ${review.bookTitle}`}</h3>
                <p>{review.content}</p>
                <Link to={`/books/${review.bookId}#reviews`} aria-label={`Read review of ${review.bookTitle}`}><i className="bi bi-arrow-up-right" /></Link>
              </article>
            ))}
            {!recentReviews.length && (
              <article className="community-card community-empty-card">
                <div className="community-card-top"><span className="reader-avatar">अ</span><div><strong>The reading room</strong><small>Ready for its first voice</small></div></div>
                <h3>Be the first reader to begin a conversation.</h3>
                <p>Open any book, add a rating and share what stayed with you.</p>
                <Link to="/community" aria-label="Visit the community"><i className="bi bi-arrow-up-right" /></Link>
              </article>
            )}
          </div>
        </div>
      </section>
    </>
  )
}

export default CataloguePage
