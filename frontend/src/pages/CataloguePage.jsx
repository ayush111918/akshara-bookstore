import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getBooks } from '../services/bookService'

function CataloguePage() {
  const [books, setBooks] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadBooks(searchValue = '') {
    try {
      setLoading(true)
      setError('')

      const data = await getBooks(searchValue)

      // Supports both paginated and plain-array responses.
      setBooks(Array.isArray(data) ? data : data.content ?? [])
    } catch (requestError) {
      console.error(requestError)
      setError(
        'The catalogue could not be loaded. Confirm that the backend is running.',
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadBooks()
  }, [])

  function handleSubmit(event) {
    event.preventDefault()
    loadBooks(search)
  }

  return (
    <>
      <section className="hero-section">
        <div className="container py-5 text-center">
          <p className="hero-label mb-2">DISCOVER · READ · GROW</p>
          <h1 className="display-5 fw-bold">
            Find your next meaningful read
          </h1>
          <p className="lead text-secondary mx-auto hero-description">
            Explore books across authors, categories, languages and formats.
          </p>

          <form
            className="search-form mx-auto mt-4"
            onSubmit={handleSubmit}
          >
            <div className="input-group input-group-lg shadow-sm">
              <span className="input-group-text bg-white border-end-0">
                <i className="bi bi-search" />
              </span>

              <input
                className="form-control border-start-0"
                type="search"
                placeholder="Search by title, author or category"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />

              <button className="btn btn-akshara px-4" type="submit">
                Search
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="container py-5">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <div>
            <p className="section-label mb-1">BOOK CATALOGUE</p>
            <h2 className="h3 mb-0">Explore books</h2>
          </div>

          {!loading && (
            <span className="text-secondary">
              {books.length} {books.length === 1 ? 'book' : 'books'}
            </span>
          )}
        </div>

        {loading && (
          <div className="text-center py-5">
            <div className="spinner-border text-success" role="status" />
            <p className="mt-3 text-secondary">Loading catalogue...</p>
          </div>
        )}

        {error && (
          <div className="alert alert-danger">
            <i className="bi bi-exclamation-circle me-2" />
            {error}
          </div>
        )}

        {!loading && !error && books.length === 0 && (
          <div className="empty-state text-center">
            <i className="bi bi-book display-4" />
            <h3 className="h5 mt-3">No books found</h3>
            <p className="text-secondary">
              Try another title, author or category.
            </p>
          </div>
        )}

        <div className="row g-4">
          {books.map((book) => (
            <div
              className="col-12 col-sm-6 col-lg-4 col-xl-3"
              key={book.id}
            >
              <article className="card book-card h-100 border-0 shadow-sm">
                <div className="book-cover">
                  {book.coverImageUrl ? (
                    <img
                      src={book.coverImageUrl}
                      alt={`Cover of ${book.title}`}
                    />
                  ) : (
                    <div className="fallback-cover">
                      <i className="bi bi-book" />
                      <span>{book.title}</span>
                    </div>
                  )}
                </div>

                <div className="card-body d-flex flex-column">
                  <h3 className="h5 card-title">{book.title}</h3>

                  <p className="text-secondary small mb-2">
                    {book.authors?.map((author) => author.name).join(', ') ||
                      'Author unavailable'}
                  </p>

                  <p className="book-description">
                    {book.description || 'Description will be added soon.'}
                  </p>

                  <div className="mt-auto d-flex justify-content-between align-items-center">
                    <strong className="book-price">
                      {book.price != null
                        ? `₹${Number(book.price).toFixed(2)}`
                        : 'Price unavailable'}
                    </strong>

                    <Link
                      className="btn btn-outline-success"
                      to={`/books/${book.id}`}
                    >
                      View details
                    </Link>
                  </div>
                </div>
              </article>
            </div>
          ))}
        </div>
      </section>
    </>
  )
}

export default CataloguePage