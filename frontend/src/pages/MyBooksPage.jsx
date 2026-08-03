import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import {
  deletePersonalBook,
  getMyBooks,
  getPersonalBookFile,
  getPersonalBooks,
  getReadingDashboard,
  uploadPersonalBook,
  createReadingEntry,
} from '../services/readerService'
import { getCoverTone, getTitleMonogram } from '../utils/bookPresentation'

const FILTERS = [
  { value: 'ALL', label: 'All books' },
  { value: 'IN_TRANSIT', label: 'On the way' },
  { value: 'DELIVERED', label: 'On my shelf' },
  { value: 'UPLOADED', label: 'My uploads' },
]

function statusCopy(status) {
  if (status === 'DELIVERED') return { label: 'On my shelf', detail: 'Delivered', tone: 'owned' }
  if (status === 'SHIPPED') return { label: 'On the way', detail: 'Shipped', tone: 'transit' }
  if (status === 'PROCESSING') return { label: 'Preparing your book', detail: 'Processing', tone: 'transit' }
  if (status === 'CONFIRMED') return { label: 'Order confirmed', detail: 'Confirmed', tone: 'transit' }
  return { label: 'Order placed', detail: 'Awaiting confirmation', tone: 'transit' }
}

function formatFileSize(bytes) {
  if (!Number.isFinite(Number(bytes))) return ''
  return `${(Number(bytes) / 1024 / 1024).toFixed(1)} MB`
}

function readingAction(entry, emptyLabel = 'Start reading') {
  if (!entry) return { label: emptyLabel, icon: 'bi-book-half' }
  if (entry.status === 'READING') return { label: 'Continue reading', icon: 'bi-book-open' }
  if (entry.status === 'PAUSED') return { label: 'Resume reading', icon: 'bi-play-circle' }
  if (entry.status === 'COMPLETED') return { label: 'Review journey', icon: 'bi-check-circle' }
  return { label: 'Begin reading', icon: 'bi-book-half' }
}

function MyBookCover({ item }) {
  if (item.coverImageUrl) return <img src={item.coverImageUrl} alt={`Cover of ${item.title}`} />
  return (
    <div className={`my-book-fallback cover-tone-${getCoverTone({ id: item.bookId ?? item.id, title: item.title })}`}>
      <span>अ</span><strong>{getTitleMonogram(item.title)}</strong>
    </div>
  )
}

function MyBooksPage() {
  const navigate = useNavigate()
  const [books, setBooks] = useState([])
  const [uploads, setUploads] = useState([])
  const [readingEntries, setReadingEntries] = useState([])
  const [filter, setFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showUpload, setShowUpload] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadMessage, setUploadMessage] = useState('')
  const [busyBookId, setBusyBookId] = useState(null)
  const [trackingKey, setTrackingKey] = useState(null)

  async function loadBooks() {
    setLoading(true)
    setError('')
    try {
      const [purchased, personal, reading] = await Promise.all([getMyBooks(), getPersonalBooks(), getReadingDashboard()])
      setBooks(purchased)
      setUploads(personal)
      setReadingEntries(reading.entries || [])
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your books could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    Promise.all([getMyBooks(), getPersonalBooks(), getReadingDashboard()])
      .then(([purchased, personal, reading]) => {
        if (!active) return
        setBooks(purchased)
        setUploads(personal)
        setReadingEntries(reading.entries || [])
      })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, 'Your books could not be loaded.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const visiblePurchased = useMemo(() => books.filter((item) => {
    if (filter === 'DELIVERED') return item.orderStatus === 'DELIVERED'
    if (filter === 'IN_TRANSIT') return item.orderStatus !== 'DELIVERED'
    if (filter === 'UPLOADED') return false
    return true
  }), [books, filter])
  const visibleUploads = filter === 'IN_TRANSIT' || filter === 'DELIVERED' ? [] : uploads
  const visibleCount = visiblePurchased.length + visibleUploads.length
  const readingEntryBySource = useMemo(() => new Map(
    readingEntries.map((entry) => [`${entry.sourceType}-${entry.sourceId}`, entry]),
  ), [readingEntries])

  async function handleUpload(event) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const file = form.get('personalFile')
    setUploading(true)
    setUploadMessage('')
    try {
      const uploaded = await uploadPersonalBook({
        title: form.get('title'),
        author: form.get('author'),
        file,
      })
      setUploads((items) => [uploaded, ...items])
      setFilter('UPLOADED')
      setUploadMessage(`${uploaded.title} was added to My Books.`)
      formElement.reset()
    } catch (requestError) {
      setUploadMessage(getApiErrorMessage(requestError, 'The file could not be uploaded.'))
    } finally {
      setUploading(false)
    }
  }

  async function openOrDownload(item, download) {
    const previewWindow = !download ? window.open('', '_blank') : null
    if (previewWindow) previewWindow.opener = null
    setBusyBookId(item.id)
    setUploadMessage('')
    try {
      const blob = await getPersonalBookFile(item.id, download)
      const url = URL.createObjectURL(blob)
      if (previewWindow) {
        previewWindow.location = url
      } else {
        const anchor = document.createElement('a')
        anchor.href = url
        anchor.download = item.originalFilename
        document.body.appendChild(anchor)
        anchor.click()
        anchor.remove()
      }
      window.setTimeout(() => URL.revokeObjectURL(url), 60000)
    } catch (requestError) {
      previewWindow?.close()
      setUploadMessage(getApiErrorMessage(requestError, 'The personal book could not be opened.'))
    } finally {
      setBusyBookId(null)
    }
  }

  async function removeUpload(item) {
    if (!window.confirm(`Remove “${item.title}” and its file from My Books?`)) return
    setBusyBookId(item.id)
    setUploadMessage('')
    try {
      await deletePersonalBook(item.id)
      setUploads((items) => items.filter((book) => book.id !== item.id))
      setUploadMessage(`${item.title} was removed.`)
    } catch (requestError) {
      setUploadMessage(getApiErrorMessage(requestError, 'The personal book could not be removed.'))
    } finally {
      setBusyBookId(null)
    }
  }

  async function openReadingJourney(sourceType, sourceId, key, existingEntry) {
    if (existingEntry) {
      navigate(`/reading-journey?entry=${existingEntry.id}`)
      return
    }
    setTrackingKey(key)
    setUploadMessage('')
    try {
      const entry = await createReadingEntry(sourceType, sourceId)
      navigate(`/reading-journey?entry=${entry.id}`)
    } catch (requestError) {
      setUploadMessage(getApiErrorMessage(requestError, 'This book could not be added to your reading journey.'))
    } finally {
      setTrackingKey(null)
    }
  }

  return (
    <section className="inner-page my-books-page">
      <div className="container-xl">
        <header className="my-books-hero">
          <div><p className="eyebrow">Your personal library</p><h1>My Books</h1><p>Keep purchased books and your own private reading files together in one thoughtful shelf.</p></div>
          <div className="my-books-stats"><strong>{books.length + uploads.length}</strong><span>Total titles</span><strong>{uploads.length}</strong><span>Private uploads</span></div>
        </header>

        <section className={`personal-upload-panel${showUpload ? ' is-open' : ''}`}>
          <button className="personal-upload-toggle" type="button" onClick={() => setShowUpload((value) => !value)} aria-expanded={showUpload}>
            <span><i className="bi bi-cloud-arrow-up" /><span><strong>Add your own book</strong><small>Private PDF or EPUB · maximum 25 MB</small></span></span>
            <i className={`bi ${showUpload ? 'bi-chevron-up' : 'bi-chevron-down'}`} />
          </button>
          {showUpload && (
            <form className="personal-upload-form" onSubmit={handleUpload}>
              <label><span>Book title</span><input name="title" maxLength="255" required placeholder="The title shown on your shelf" /></label>
              <label><span>Author <small>optional</small></span><input name="author" maxLength="180" placeholder="Author or creator" /></label>
              <label className="personal-file-field"><span>Book file</span><input name="personalFile" type="file" accept=".pdf,.epub,application/pdf,application/epub+zip" required /><small>Only upload files you own or are allowed to use.</small></label>
              <button className="btn btn-ink" disabled={uploading} type="submit">{uploading ? <><span className="spinner-border spinner-border-sm" /> Uploading…</> : 'Add to My Books'}</button>
            </form>
          )}
          {uploadMessage && <p className="personal-upload-message" role="status">{uploadMessage}</p>}
        </section>

        <div className="my-books-toolbar" role="group" aria-label="Filter your books">
          {FILTERS.map((item) => <button className={filter === item.value ? 'is-active' : ''} key={item.value} type="button" onClick={() => setFilter(item.value)}>{item.label}</button>)}
          <span className="my-books-toolbar-links"><Link to="/my-reviews"><i className="bi bi-chat-quote" /> My reviews</Link><Link to="/orders"><i className="bi bi-receipt" /> Order history</Link></span>
        </div>

        {loading && <div className="page-loading"><span className="spinner-border" /> Loading your books…</div>}
        {!loading && error && <div className="catalogue-message catalogue-error"><span><i className="bi bi-exclamation-circle" /></span><div><h2>We could not open your library</h2><p>{error}</p><button className="btn btn-ink" type="button" onClick={loadBooks}>Try again</button></div></div>}
        {!loading && !error && visibleCount > 0 && (
          <div className="my-books-grid">
            {visibleUploads.map((item) => {
              const readingEntry = readingEntryBySource.get(`PERSONAL_UPLOAD-${item.id}`)
              const action = readingAction(readingEntry, 'Track reading')
              return <article className="my-book-card personal-book-card" key={`upload-${item.id}`}>
                <div className="my-book-cover"><MyBookCover item={item} /><span className="my-book-status status-uploaded">Private upload</span></div>
                <div className="my-book-copy">
                  <p>{item.format} · {formatFileSize(item.fileSize)}</p>
                  <h2>{item.title}</h2>
                  <span>{item.author || item.originalFilename}</span>
                  <div className="my-book-progress"><i className="bi bi-shield-lock-fill" /><div><strong>Only visible to you</strong><small>Stored in your Akshara library</small></div></div>
                  <div className="my-book-actions personal-book-actions">
                    <button disabled={trackingKey === `upload-${item.id}`} type="button" onClick={() => openReadingJourney('PERSONAL_UPLOAD', item.id, `upload-${item.id}`, readingEntry)}><i className={`bi ${action.icon}`} /> {trackingKey === `upload-${item.id}` ? 'Adding…' : action.label}</button>
                    <button disabled={busyBookId === item.id} type="button" onClick={() => openOrDownload(item, item.format !== 'PDF')}>{item.format === 'PDF' ? 'Open book' : 'Download EPUB'}</button>
                    <button disabled={busyBookId === item.id} type="button" onClick={() => openOrDownload(item, true)}>Download</button>
                    <button className="danger" disabled={busyBookId === item.id} type="button" onClick={() => removeUpload(item)} aria-label={`Remove ${item.title}`}><i className="bi bi-trash" /></button>
                  </div>
                </div>
              </article>
            })}
            {visiblePurchased.map((item) => {
              const status = statusCopy(item.orderStatus)
              const readingEntry = readingEntryBySource.get(`PURCHASED_BOOK-${item.bookId}`)
              const action = readingAction(readingEntry)
              return (
                <article className="my-book-card" key={`order-${item.orderItemId}`}>
                  <div className="my-book-cover"><MyBookCover item={item} /><span className={`my-book-status status-${status.tone}`}>{status.label}</span></div>
                  <div className="my-book-copy">
                    <p>{item.format}{item.quantity > 1 ? ` · ${item.quantity} copies` : ''}</p>
                    <h2>{item.title}</h2>
                    <span>{item.publisherName || item.editionName || item.isbn || 'Akshara edition'}</span>
                    <div className="my-book-progress"><i className={`bi ${item.orderStatus === 'DELIVERED' ? 'bi-check-circle-fill' : 'bi-box-seam'}`} /><div><strong>{status.detail}</strong><small>Order #{item.orderId}</small></div></div>
                    <div className="my-book-actions">
                      {item.orderStatus === 'DELIVERED' && item.bookId && <button className="my-book-primary-action" disabled={trackingKey === `book-${item.bookId}`} type="button" onClick={() => openReadingJourney('PURCHASED_BOOK', item.bookId, `book-${item.bookId}`, readingEntry)}><i className={`bi ${action.icon}`} /> {trackingKey === `book-${item.bookId}` ? 'Adding…' : action.label}</button>}
                      {item.bookId && <Link className="my-book-secondary-action" to={`/books/${item.bookId}`}>{item.orderStatus === 'DELIVERED' ? 'Book details' : 'View book'} <i className="bi bi-arrow-up-right" /></Link>}
                      <Link className="my-book-secondary-action" to={`/orders/${item.orderId}`}><i className="bi bi-box-seam" /> Track order</Link>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        )}
        {!loading && !error && visibleCount === 0 && <div className="empty-reader-page"><i className="bi bi-journal-bookmark" /><h2>{books.length + uploads.length ? 'No books in this view' : 'Your library is waiting'}</h2><p>{books.length + uploads.length ? 'Choose another shelf to see your books.' : 'Place an order or upload a private PDF or EPUB to begin.'}</p>{filter === 'UPLOADED' ? <button className="btn btn-ink" type="button" onClick={() => setShowUpload(true)}>Upload a book</button> : <Link className="btn btn-ink" to="/#catalogue">Explore books</Link>}</div>}
      </div>
    </section>
  )
}

export default MyBooksPage
