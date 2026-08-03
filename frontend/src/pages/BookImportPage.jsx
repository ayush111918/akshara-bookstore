import { useMemo, useState } from 'react'
import {
  AVAILABILITY_OPTIONS,
  BOOK_FORMATS,
  BOOK_SOURCES,
  createManualBook,
  importBook,
  searchExternalBooks,
} from '../services/adminBookService'
import { getApiErrorMessage } from '../services/api'

const EMPTY_FORM = {
  title: '',
  subtitle: '',
  description: '',
  authors: '',
  publisher: '',
  languageCode: 'en',
  pageCount: '',
  categories: '',
  coverImageUrl: '',
  publicationDate: '',
  editionName: '',
  isbn10: '',
  isbn13: '',
  format: 'PAPERBACK',
  price: '',
  stockQuantity: '0',
  availabilityStatus: 'IN_STOCK',
  sku: '',
  active: true,
  featured: false,
}

function cleanIsbn(value = '') {
  return String(value).replace(/[-\s]/g, '')
}

function dateInputValue(value = '') {
  const match = String(value).match(/^\d{4}-\d{2}-\d{2}/)
  return match?.[0] ?? ''
}

function commaList(value) {
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

function titleFor(result) {
  return result.subtitle ? `${result.title}: ${result.subtitle}` : result.title
}

function BookMetadataForm({ form, mode, onChange }) {
  return (
    <div className="admin-form-sections">
      <section className="admin-form-section">
        <div className="admin-section-title"><span>01</span><div><h2>Book metadata</h2><p>{mode === 'import' ? 'Review and correct the metadata before importing.' : 'Enter the bibliographic details for this title.'}</p></div></div>
        <div className="admin-field-grid">
          <label className="span-two">Title <input name="title" value={form.title} onChange={onChange} maxLength="255" required /></label>
          <label className="span-two">Subtitle <input name="subtitle" value={form.subtitle} onChange={onChange} maxLength="255" /></label>
          <label className="span-two">Description <textarea name="description" value={form.description} onChange={onChange} maxLength="20000" rows="5" /></label>
          <label>Authors <input name="authors" value={form.authors} onChange={onChange} placeholder="James Clear, …" required /></label>
          <label>Publisher <input name="publisher" value={form.publisher} onChange={onChange} /></label>
          <label className="span-two">Categories <input name="categories" value={form.categories} onChange={onChange} placeholder="Self-help, Psychology" required /></label>
          <label>Language code <input name="languageCode" value={form.languageCode} onChange={onChange} maxLength="10" placeholder="en" /></label>
          <label>Cover image URL <input name="coverImageUrl" type="url" value={form.coverImageUrl} onChange={onChange} placeholder="https://…" /></label>
        </div>
      </section>

      <section className="admin-form-section">
        <div className="admin-section-title"><span>02</span><div><h2>Edition</h2><p>ISBN identifies this exact edition and is checked for duplicates by the backend.</p></div></div>
        <div className="admin-field-grid admin-field-grid-three">
          <label>ISBN-13 <input name="isbn13" value={form.isbn13} onChange={onChange} inputMode="numeric" pattern="[0-9]{13}" maxLength="13" /></label>
          <label>ISBN-10 <input name="isbn10" value={form.isbn10} onChange={onChange} pattern="[0-9]{9}[0-9Xx]" maxLength="10" /></label>
          <label>Edition name <input name="editionName" value={form.editionName} onChange={onChange} maxLength="100" placeholder="First edition" /></label>
          <label>Publication date <input name="publicationDate" type="date" max={new Date().toISOString().slice(0, 10)} value={form.publicationDate} onChange={onChange} /></label>
          <label>Page count <input name="pageCount" type="number" min="1" value={form.pageCount} onChange={onChange} /></label>
          <label>Format <select name="format" value={form.format} onChange={onChange}>{BOOK_FORMATS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select><small>Digital editions require licensed-file delivery and are not enabled yet.</small></label>
        </div>
      </section>

      <section className="admin-form-section admin-owned-section">
        <div className="admin-section-title"><span>03</span><div><h2>Akshara catalogue details</h2><p>Pricing, inventory, visibility, and curation always belong to Akshara.</p></div></div>
        <div className="admin-field-grid admin-field-grid-three">
          <label>Selling price (₹) <input name="price" type="number" min="0" step="0.01" value={form.price} onChange={onChange} required /></label>
          <label>Stock quantity <input name="stockQuantity" type="number" min="0" step="1" value={form.stockQuantity} onChange={onChange} required /></label>
          <label>Availability <select name="availabilityStatus" value={form.availabilityStatus} onChange={onChange}>{AVAILABILITY_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
          <label className="span-two">SKU <input name="sku" value={form.sku} onChange={onChange} maxLength="100" placeholder="AKS-978…-PB" required /></label>
          <div className="admin-checks">
            <label><input name="active" type="checkbox" checked={form.active} onChange={onChange} /> Visible in catalogue</label>
            <label><input name="featured" type="checkbox" checked={form.featured} onChange={onChange} /> Featured / curated</label>
          </div>
        </div>
      </section>
    </div>
  )
}

function BookImportPage() {
  const [mode, setMode] = useState('import')
  const [query, setQuery] = useState('')
  const source = 'OPEN_LIBRARY'
  const [results, setResults] = useState([])
  const [selectedIndex, setSelectedIndex] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [searching, setSearching] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const selected = selectedIndex == null ? null : results[selectedIndex]
  const resultLabel = useMemo(() => `${results.length} edition${results.length === 1 ? '' : 's'}`, [results.length])

  function switchMode(nextMode) {
    setMode(nextMode)
    setForm(EMPTY_FORM)
    setSelectedIndex(null)
    setError('')
    setSuccess('')
  }

  function updateField(event) {
    const { name, type, checked, value } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  async function handleSearch(event) {
    event.preventDefault()
    if (!query.trim()) return
    setSearching(true)
    setSearched(false)
    setError('')
    setSuccess('')
    try {
      const nextResults = await searchExternalBooks(query, source)
      setResults(nextResults)
      setSelectedIndex(null)
      setSearched(true)
    } catch (requestError) {
      setResults([])
      setSearched(true)
      setError(getApiErrorMessage(requestError, 'The external catalogue search could not be completed.'))
    } finally {
      setSearching(false)
    }
  }

  function selectEdition(result, index) {
    setSelectedIndex(index)
    setError('')
    setSuccess('')
    setForm({
      ...EMPTY_FORM,
      title: result.title,
      subtitle: result.subtitle,
      description: result.description,
      authors: result.authors.join(', '),
      publisher: result.publisher,
      languageCode: result.languageCode || 'en',
      pageCount: result.pageCount || '',
      categories: result.categories.join(', '),
      coverImageUrl: result.coverImageUrl,
      publicationDate: dateInputValue(result.publicationDate),
      editionName: result.editionName,
      isbn10: cleanIsbn(result.isbn10),
      isbn13: cleanIsbn(result.isbn13),
      sku: `AKS-${cleanIsbn(result.isbn13 || result.isbn10)}-PB`,
    })
    requestAnimationFrame(() => document.querySelector('#catalogue-book-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
  }

  function importPayload() {
    return {
      source,
      sourceId: selected.sourceId,
      editionId: selected.editionId,
      title: form.title.trim(),
      subtitle: form.subtitle.trim() || null,
      description: form.description.trim() || null,
      authors: commaList(form.authors),
      publisher: form.publisher.trim() || null,
      languageCode: form.languageCode.trim() || null,
      pageCount: form.pageCount ? Number(form.pageCount) : null,
      categories: commaList(form.categories),
      coverImageUrl: form.coverImageUrl.trim() || null,
      publicationDate: form.publicationDate || null,
      editionName: form.editionName.trim() || null,
      isbn10: cleanIsbn(form.isbn10) || null,
      isbn13: cleanIsbn(form.isbn13) || null,
      format: form.format,
      price: Number(form.price),
      stockQuantity: Number(form.stockQuantity),
      availabilityStatus: form.availabilityStatus,
      sku: form.sku.trim(),
      active: form.active,
      featured: form.featured,
    }
  }

  function manualPayload() {
    return {
      title: form.title.trim(),
      subtitle: form.subtitle.trim() || null,
      description: form.description.trim() || null,
      coverImageUrl: form.coverImageUrl.trim() || null,
      languageCode: form.languageCode.trim() || null,
      authors: commaList(form.authors),
      publisher: form.publisher.trim() || null,
      categories: commaList(form.categories),
      pageCount: form.pageCount ? Number(form.pageCount) : null,
      publicationDate: form.publicationDate || null,
      editionName: form.editionName.trim() || null,
      isbn10: cleanIsbn(form.isbn10) || null,
      isbn13: cleanIsbn(form.isbn13) || null,
      format: form.format,
      price: Number(form.price),
      stockQuantity: Number(form.stockQuantity),
      availabilityStatus: form.availabilityStatus,
      sku: form.sku.trim(),
      active: form.active,
      featured: form.featured,
    }
  }

  async function handleSave(event) {
    event.preventDefault()
    if (mode === 'import' && !cleanIsbn(form.isbn10) && !cleanIsbn(form.isbn13)) {
      setError('Add an ISBN-10 or ISBN-13 before saving this edition.')
      return
    }
    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      const created = mode === 'import'
        ? await importBook(importPayload())
        : await createManualBook(manualPayload())
      setSuccess({
        message: `“${created?.title ?? form.title}” is now in the Akshara catalogue.`,
        bookId: created?.id,
      })
      setForm(EMPTY_FORM)
      setSelectedIndex(null)
      if (mode === 'import') setResults([])
    } catch (requestError) {
      const duplicateFallback = requestError?.response?.status === 409
        ? 'That ISBN already exists in the Akshara catalogue. Choose another edition.'
        : 'The book could not be saved.'
      setError(getApiErrorMessage(requestError, duplicateFallback))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="admin-import-page">
      <header className="admin-import-hero">
        <div className="container-xl">
          <div>
            <p className="eyebrow eyebrow-light">Catalogue administration</p>
            <h1>Bring the right books<br />onto Akshara’s shelves.</h1>
            <p>Import trusted metadata, then add the pricing and inventory details only Akshara should own.</p>
          </div>
          <div className="admin-import-principle"><i className="bi bi-shield-check" /><span><strong>Metadata travels.</strong> Commerce data stays with Akshara.</span></div>
        </div>
      </header>

      <div className="container-xl admin-import-workspace">
        <div className="admin-mode-tabs" role="tablist" aria-label="Add a book">
          <button type="button" className={mode === 'import' ? 'is-active' : ''} onClick={() => switchMode('import')}><i className="bi bi-cloud-arrow-down" /><span>Import metadata<small>Search trusted book sources</small></span></button>
          <button type="button" className={mode === 'manual' ? 'is-active' : ''} onClick={() => switchMode('manual')}><i className="bi bi-pencil-square" /><span>Manual entry<small>Create a book from scratch</small></span></button>
        </div>

        {success && <div className="admin-notice success" role="status"><i className="bi bi-check-circle-fill" /><span>{success.message}</span><a href={success.bookId ? `/books/${success.bookId}` : '/'}>View book</a></div>}
        {error && <div className="admin-notice error" role="alert"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span><button type="button" onClick={() => setError('')} aria-label="Dismiss">×</button></div>}

        {mode === 'import' && (
          <>
            <section className="admin-search-panel">
              <div><p className="eyebrow">Find a book</p><h2>Search by title, author, or ISBN</h2><p>Results are fetched through Akshara’s Spring Boot API and are not saved until you import one.</p></div>
              <form onSubmit={handleSearch} role="search">
                <div className="admin-search-input"><i className="bi bi-search" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Try “Atomic Habits” or 9780735211292" aria-label="Book search" required /><span className="admin-source-label">Open Library</span><button type="submit" disabled={searching}>{searching ? <span className="spinner-border spinner-border-sm" /> : 'Search'}</button></div>
                <small><i className="bi bi-info-circle" /> Metadata is retrieved through Akshara’s secured Open Library provider.</small>
              </form>
            </section>

            {(results.length > 0 || searched) && (
              <section className="external-results">
                <div className="external-results-heading"><div><p className="eyebrow">External results</p><h2>{results.length ? `${resultLabel} found` : 'No editions found'}</h2></div>{results.length > 0 && <span>Select the exact ISBN and cover</span>}</div>
                {results.length > 0 ? <div className="external-result-grid">{results.map((result, index) => (
                  <article className={`external-book-card ${selectedIndex === index ? 'is-selected' : ''} ${result.alreadyImported ? 'is-imported' : ''}`} key={`${result.editionId || result.isbn13 || result.isbn10}-${index}`}>
                    <div className="external-cover">{result.coverImageUrl ? <img src={result.coverImageUrl} alt={`Cover of ${result.title}`} /> : <span>अ<small>No cover</small></span>}</div>
                    <div className="external-book-copy"><div className="external-source"><span>{BOOK_SOURCES.find((item) => item.value === source)?.label}</span>{result.alreadyImported ? <strong>Already in catalogue</strong> : selectedIndex === index && <i className="bi bi-check-circle-fill" />}</div><h3>{titleFor(result)}</h3><p>{result.authors.join(', ') || 'Unknown author'}</p><dl><div><dt>ISBN</dt><dd>{result.isbn13 || result.isbn10 || 'Not supplied'}</dd></div><div><dt>Published</dt><dd>{result.publicationDate || 'Unknown'}</dd></div><div><dt>Publisher</dt><dd>{result.publisher || 'Unknown'}</dd></div><div><dt>Pages</dt><dd>{result.pageCount || '—'}</dd></div></dl><button type="button" disabled={result.alreadyImported} onClick={() => selectEdition(result, index)}>{result.alreadyImported ? 'Duplicate ISBN' : selectedIndex === index ? 'Edition selected' : 'Select this edition'} <i className="bi bi-arrow-right" /></button></div>
                  </article>
                ))}</div> : <div className="admin-empty-results"><i className="bi bi-journal-x" /><h3>No matching editions</h3><p>Try an ISBN, fewer words, or the fallback source.</p></div>}
              </section>
            )}
          </>
        )}

        {(mode === 'manual' || selected) && (
          <form id="catalogue-book-form" className="catalogue-book-form" onSubmit={handleSave}>
            <div className="catalogue-form-heading"><div><p className="eyebrow">{mode === 'import' ? 'Selected edition' : 'New catalogue record'}</p><h1>{mode === 'import' ? 'Complete the import' : 'Add a book manually'}</h1></div>{form.coverImageUrl && <img src={form.coverImageUrl} alt="Selected book cover" />}</div>
            <BookMetadataForm form={form} mode={mode} onChange={updateField} />
            <div className="admin-submit-bar"><div><i className="bi bi-database-check" /><span><strong>Saved permanently to MySQL</strong><small>The public catalogue will use this Akshara-owned record.</small></span></div><button className="btn btn-ink" type="submit" disabled={submitting}>{submitting ? <><span className="spinner-border spinner-border-sm" /> Saving…</> : <>{mode === 'import' ? 'Import into catalogue' : 'Create book'} <i className="bi bi-arrow-right" /></>}</button></div>
          </form>
        )}
      </div>
    </section>
  )
}

export default BookImportPage
