import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  AVAILABILITY_OPTIONS,
  deleteCatalogueBook,
  getAdminBooks,
  restockEdition,
  seedCatalogue,
  updateBookFeatured,
  updateEditionInventory,
} from '../services/adminBookService'
import { getApiErrorMessage } from '../services/api'

function inventoryDraft(inventory) {
  return {
    price: String(inventory?.price ?? ''),
    stockQuantity: String(inventory?.stockQuantity ?? 0),
    availabilityStatus: inventory?.availabilityStatus ?? 'OUT_OF_STOCK',
    active: Boolean(inventory?.active),
    restockQuantity: '',
  }
}

function CatalogueAdminPage() {
  const [searchParams] = useSearchParams()
  const focusedBookId = searchParams.get('book')
  const [books, setBooks] = useState([])
  const [drafts, setDrafts] = useState({})
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [seeding, setSeeding] = useState(false)

  async function loadCatalogue() {
    try {
      setLoading(true)
      setError('')
      const data = await getAdminBooks()
      setBooks(data)
      setDrafts(Object.fromEntries(data.flatMap((book) =>
        (book.editions ?? []).map((edition) => [edition.id, inventoryDraft(edition.inventory)]),
      )))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'The catalogue administration data could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true

    getAdminBooks()
      .then((data) => {
        if (!active) return
        setBooks(data)
        setDrafts(Object.fromEntries(data.flatMap((book) =>
          (book.editions ?? []).map((edition) => [edition.id, inventoryDraft(edition.inventory)]),
        )))
      })
      .catch((requestError) => {
        if (active) {
          setError(getApiErrorMessage(requestError, 'The catalogue administration data could not be loaded.'))
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const visibleBooks = useMemo(() => {
    const needle = query.trim().toLowerCase()
    if (!needle) return books
    return books.filter((book) => [
      book.title,
      ...(book.authors ?? []).map((author) => author.name),
      ...(book.editions ?? []).flatMap((edition) => [edition.sku, edition.isbn13, edition.isbn10]),
    ].some((value) => String(value ?? '').toLowerCase().includes(needle)))
  }, [books, query])

  useEffect(() => {
    if (loading || !focusedBookId) return
    const target = document.getElementById(`admin-book-${focusedBookId}`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [focusedBookId, loading, visibleBooks.length])

  const summary = useMemo(() => {
    const editions = books.flatMap((book) => book.editions ?? [])
    return {
      books: books.length,
      editions: editions.length,
      units: editions.reduce((total, edition) => total + (edition.inventory?.stockQuantity ?? 0), 0),
      attention: editions.filter((edition) => !edition.inventory?.active || edition.inventory?.stockQuantity === 0).length,
    }
  }, [books])

  function updateDraft(editionId, field, value) {
    setDrafts((current) => ({
      ...current,
      [editionId]: { ...current[editionId], [field]: value },
    }))
  }

  function replaceInventory(editionId, inventory) {
    setBooks((current) => current.map((book) => ({
      ...book,
      editions: (book.editions ?? []).map((edition) =>
        edition.id === editionId ? { ...edition, inventory } : edition),
    })))
    setDrafts((current) => ({
      ...current,
      [editionId]: { ...inventoryDraft(inventory), restockQuantity: '' },
    }))
  }

  async function saveInventory(edition) {
    const draft = drafts[edition.id]
    try {
      setBusy(`save-${edition.id}`)
      setError('')
      setNotice('')
      const inventory = await updateEditionInventory(edition.id, {
        price: Number(draft.price),
        stockQuantity: Number(draft.stockQuantity),
        availabilityStatus: draft.availabilityStatus,
        active: draft.active,
      })
      replaceInventory(edition.id, inventory)
      setNotice(`${edition.sku} inventory was updated.`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Inventory could not be updated.'))
    } finally {
      setBusy('')
    }
  }

  async function restock(edition) {
    const quantity = Number(drafts[edition.id]?.restockQuantity)
    if (!Number.isInteger(quantity) || quantity <= 0) {
      setError('Enter a positive whole-number restock quantity.')
      return
    }
    try {
      setBusy(`restock-${edition.id}`)
      setError('')
      setNotice('')
      const inventory = await restockEdition(edition.id, quantity)
      replaceInventory(edition.id, inventory)
      setNotice(`${quantity} units were added to ${edition.sku}.`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'The edition could not be restocked.'))
    } finally {
      setBusy('')
    }
  }

  async function toggleFeatured(book) {
    try {
      setBusy(`feature-${book.id}`)
      setError('')
      const updated = await updateBookFeatured(book.id, !book.featured)
      setBooks((current) => current.map((item) => item.id === book.id ? updated : item))
      setNotice(`${book.title} was ${updated.featured ? 'added to' : 'removed from'} the featured shelf.`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Featured status could not be updated.'))
    } finally {
      setBusy('')
    }
  }

  async function deleteBook(book) {
    const confirmed = window.confirm(
      `Permanently delete “${book.title}” and all of its editions and inventory? This cannot be undone.`,
    )
    if (!confirmed) return

    try {
      setBusy(`delete-${book.id}`)
      setError('')
      setNotice('')
      await deleteCatalogueBook(book.id)
      setBooks((current) => current.filter((item) => item.id !== book.id))
      setDrafts((current) => {
        const next = { ...current }
        for (const edition of book.editions ?? []) delete next[edition.id]
        return next
      })
      setNotice(`${book.title} was permanently deleted from the catalogue.`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'The book could not be deleted.'))
    } finally {
      setBusy('')
    }
  }

  async function seedStarterCatalogue() {
    const confirmed = window.confirm(
      'Import 50 curated books across classics, science, history, philosophy, business, biography, Indian literature, and children’s books? This can take about a minute.',
    )
    if (!confirmed) return
    try {
      setSeeding(true)
      setError('')
      setNotice('Importing the curated starter catalogue…')
      const result = await seedCatalogue(50)
      await loadCatalogue()
      setNotice(`Imported ${result.imported} books. The catalogue now contains ${result.catalogueSizeAfter} books.`)
    } catch (requestError) {
      setNotice('')
      setError(getApiErrorMessage(requestError, 'The starter catalogue could not be populated.'))
    } finally {
      setSeeding(false)
    }
  }

  return (
    <div className="catalogue-admin-page">
      <section className="catalogue-admin-hero">
        <div className="container-xl">
          <div>
            <p className="eyebrow">Catalogue operations</p>
            <h1>Manage Akshara’s shelves.</h1>
            <p>Restock editions, adjust price and availability, pause selling, and curate featured books.</p>
          </div>
          <div className="catalogue-admin-hero-actions">
            <Link className="btn btn-light" to="/admin/orders"><i className="bi bi-box-seam" /> Manage orders</Link>
            <button type="button" className="btn btn-light" onClick={seedStarterCatalogue} disabled={seeding}><i className="bi bi-collection" /> {seeding ? 'Importing 50 books…' : 'Populate 50 books'}</button>
            <Link className="btn btn-light" to="/admin/books/import"><i className="bi bi-cloud-arrow-down" /> Import or add a book</Link>
          </div>
        </div>
      </section>

      <section className="container-xl catalogue-admin-workspace">
        <div className="catalogue-admin-summary" aria-label="Catalogue summary">
          <div><strong>{summary.books}</strong><span>Books</span></div>
          <div><strong>{summary.editions}</strong><span>Sellable editions</span></div>
          <div><strong>{summary.units}</strong><span>Units in stock</span></div>
          <div><strong>{summary.attention}</strong><span>Need attention</span></div>
        </div>

        <div className="catalogue-admin-toolbar">
          <label><i className="bi bi-search" /><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search title, author, ISBN, or SKU" /></label>
          <button type="button" onClick={loadCatalogue} disabled={loading}><i className="bi bi-arrow-clockwise" /> Refresh</button>
        </div>

        {error && <div className="admin-feedback admin-feedback-error" role="alert">{error}</div>}
        {notice && <div className="admin-feedback admin-feedback-success" role="status">{notice}</div>}
        {loading && <div className="page-loading"><span className="spinner-border" /> Loading catalogue…</div>}

        {!loading && visibleBooks.length === 0 && (
          <div className="catalogue-admin-empty"><i className="bi bi-journal-x" /><h2>No matching catalogue records</h2><p>Try another search or import a new book.</p></div>
        )}

        <div className="catalogue-admin-list">
          {visibleBooks.map((book) => (
            <article
              id={`admin-book-${book.id}`}
              className={`catalogue-admin-book${String(book.id) === focusedBookId ? ' is-targeted' : ''}`}
              key={book.id}
              aria-label={`Manage ${book.title}`}
            >
              <header>
                <div className="catalogue-admin-cover">{book.coverImageUrl ? <img src={book.coverImageUrl} alt="" /> : <i className="bi bi-book" />}</div>
                <div><p>{(book.authors ?? []).map((author) => author.name).join(', ') || 'Unknown author'}</p><h2>{book.title}</h2><span>{book.editions?.length ?? 0} {(book.editions?.length ?? 0) === 1 ? 'edition' : 'editions'}</span></div>
                <div className="catalogue-admin-book-actions">
                  <button type="button" className={book.featured ? 'is-featured' : ''} onClick={() => toggleFeatured(book)} disabled={Boolean(busy)}><i className={`bi ${book.featured ? 'bi-star-fill' : 'bi-star'}`} /> {book.featured ? 'Featured' : 'Feature'}</button>
                  <Link to={`/books/${book.id}`}>Public page <i className="bi bi-arrow-up-right" /></Link>
                  <button type="button" className="catalogue-delete-button" onClick={() => deleteBook(book)} disabled={Boolean(busy)}><i className="bi bi-trash3" /> {busy === `delete-${book.id}` ? 'Deleting…' : 'Delete'}</button>
                </div>
              </header>

              <div className="catalogue-admin-editions">
                {(book.editions ?? []).map((edition) => {
                  const draft = drafts[edition.id] ?? inventoryDraft(edition.inventory)
                  return (
                    <section className="catalogue-admin-edition" key={edition.id}>
                      <div className="edition-identity">
                        <strong>{edition.format}</strong><span>SKU {edition.sku}</span><span>{edition.isbn13 ? `ISBN ${edition.isbn13}` : 'No ISBN'}</span>
                      </div>
                      <div className="edition-inventory-fields">
                        <label>Price (₹)<input type="number" min="0.01" step="0.01" value={draft.price} onChange={(event) => updateDraft(edition.id, 'price', event.target.value)} /></label>
                        <label>Total stock<input type="number" min="0" step="1" value={draft.stockQuantity} onChange={(event) => updateDraft(edition.id, 'stockQuantity', event.target.value)} /></label>
                        <label>Availability<select value={draft.availabilityStatus} onChange={(event) => updateDraft(edition.id, 'availabilityStatus', event.target.value)}>{AVAILABILITY_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
                        <label className="edition-active-toggle"><input type="checkbox" checked={draft.active} onChange={(event) => updateDraft(edition.id, 'active', event.target.checked)} /><span>Active for sale</span></label>
                        <button type="button" className="edition-save-button" onClick={() => saveInventory(edition)} disabled={Boolean(busy)}>{busy === `save-${edition.id}` ? 'Saving…' : 'Save changes'}</button>
                      </div>
                      <div className="edition-restock">
                        <label><span>Quick restock</span><input type="number" min="1" step="1" placeholder="Units to add" value={draft.restockQuantity} onChange={(event) => updateDraft(edition.id, 'restockQuantity', event.target.value)} /></label>
                        <button type="button" onClick={() => restock(edition)} disabled={Boolean(busy)}><i className="bi bi-box-arrow-in-down" /> {busy === `restock-${edition.id}` ? 'Adding…' : 'Add stock'}</button>
                      </div>
                    </section>
                  )
                })}
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}

export default CatalogueAdminPage
