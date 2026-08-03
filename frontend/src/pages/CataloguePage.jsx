import { useEffect, useState } from 'react'
import BookCard from '../components/BookCard'
import { getBooks } from '../services/bookService'
import { getRecentReviews } from '../services/readerService'
import useAuth from '../hooks/useAuth'
import CatalogueJourneySection from '../components/CatalogueJourneySection'
import CatalogueCommunityPreview from '../components/CatalogueCommunityPreview'
import HomeCatalogueHero from '../components/HomeCatalogueHero'

function CataloguePage() {
  const { user } = useAuth()
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
      <HomeCatalogueHero
        books={books}
        search={search}
        onSearchChange={(event) => setSearch(event.target.value)}
        onSubmit={handleSubmit}
        onCategory={handleCategory}
      />

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

      <CatalogueJourneySection isAdmin={user?.role === 'ADMIN'} />
      <CatalogueCommunityPreview reviews={recentReviews} />
    </>
  )
}

export default CataloguePage
