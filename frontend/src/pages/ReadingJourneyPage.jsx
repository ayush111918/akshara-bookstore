import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import {
  createReadingAnnotation,
  deleteReadingAnnotation,
  deleteReadingEntry,
  getReadingAnnotations,
  getReadingDashboard,
  updateReadingAnnotation,
  updateReadingGoal,
  updateReadingProgress,
} from '../services/readerService'
import { getCoverTone, getTitleMonogram } from '../utils/bookPresentation'

const STATUS_OPTIONS = [
  { value: 'NOT_STARTED', label: 'Want to read' },
  { value: 'READING', label: 'Reading' },
  { value: 'PAUSED', label: 'Paused' },
  { value: 'COMPLETED', label: 'Completed' },
]

const FILTERS = [
  { value: 'ALL', label: 'All' },
  ...STATUS_OPTIONS,
]

const EMPTY_ANNOTATION = { type: 'NOTE', content: '', pageNumber: '' }

function statusLabel(status) {
  return STATUS_OPTIONS.find((option) => option.value === status)?.label || status
}

function ReadingCover({ entry }) {
  const [failed, setFailed] = useState(false)
  if (entry.coverImageUrl && !failed) {
    return <img src={entry.coverImageUrl} alt={`Cover of ${entry.title}`} onError={() => setFailed(true)} />
  }
  return <div className={`reading-cover-fallback cover-tone-${getCoverTone({ id: entry.id, title: entry.title })}`}><span>अ</span><strong>{getTitleMonogram(entry.title)}</strong></div>
}

function formatDate(value) {
  if (!value) return 'Not recorded'
  return new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

function ReadingJourneyPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [drafts, setDrafts] = useState({})
  const [busyId, setBusyId] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [annotations, setAnnotations] = useState([])
  const [annotationsLoading, setAnnotationsLoading] = useState(false)
  const [annotationDraft, setAnnotationDraft] = useState(EMPTY_ANNOTATION)
  const [editingAnnotationId, setEditingAnnotationId] = useState(null)
  const [annotationBusy, setAnnotationBusy] = useState(false)
  const [goalTarget, setGoalTarget] = useState('')

  function applyDashboard(data) {
    setDashboard(data)
    setGoalTarget(String(data.goal.targetBooks))
    setDrafts(Object.fromEntries(data.entries.map((entry) => [entry.id, {
      status: entry.status,
      currentPage: String(entry.currentPage ?? 0),
      totalPages: entry.totalPages ? String(entry.totalPages) : '',
    }])))
    const requestedId = Number(searchParams.get('entry'))
    const requested = data.entries.find((entry) => entry.id === requestedId)
    setSelectedId((current) => requested?.id || (data.entries.some((entry) => entry.id === current) ? current : data.entries[0]?.id || null))
  }

  async function loadDashboard() {
    setLoading(true)
    setError('')
    try {
      applyDashboard(await getReadingDashboard())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your reading journey could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getReadingDashboard()
      .then((data) => { if (active) applyDashboard(data) })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, 'Your reading journey could not be loaded.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
    // The initial entry query is deliberately consumed only during first load.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!selectedId) {
      return undefined
    }
    let active = true
    getReadingAnnotations(selectedId)
      .then((data) => { if (active) setAnnotations(data) })
      .catch((requestError) => { if (active) setMessage(getApiErrorMessage(requestError, 'Your private notes could not be loaded.')) })
      .finally(() => { if (active) setAnnotationsLoading(false) })
    return () => { active = false }
  }, [selectedId])

  const visibleEntries = useMemo(() => {
    if (!dashboard) return []
    return filter === 'ALL'
      ? dashboard.entries
      : dashboard.entries.filter((entry) => entry.status === filter)
  }, [dashboard, filter])

  const selectedEntry = dashboard?.entries.find((entry) => entry.id === selectedId)

  function updateDraft(entryId, field, value) {
    setDrafts((current) => ({
      ...current,
      [entryId]: { ...current[entryId], [field]: value },
    }))
  }

  async function saveProgress(event, entry) {
    event.preventDefault()
    const draft = drafts[entry.id]
    setBusyId(entry.id)
    setMessage('')
    try {
      await updateReadingProgress(entry.id, {
        status: draft.status,
        currentPage: Number(draft.currentPage || 0),
        totalPages: draft.totalPages ? Number(draft.totalPages) : null,
      })
      setMessage(`${entry.title} was updated.`)
      await loadDashboard()
    } catch (requestError) {
      setMessage(getApiErrorMessage(requestError, 'Progress could not be saved.'))
    } finally {
      setBusyId(null)
    }
  }

  async function removeEntry(entry) {
    if (!window.confirm(`Remove “${entry.title}” from your reading journey? Your private notes for it will also be removed.`)) return
    setBusyId(entry.id)
    setMessage('')
    try {
      await deleteReadingEntry(entry.id)
      if (selectedId === entry.id) setSelectedId(null)
      setMessage(`${entry.title} was removed from the journey.`)
      await loadDashboard()
    } catch (requestError) {
      setMessage(getApiErrorMessage(requestError, 'The reading entry could not be removed.'))
    } finally {
      setBusyId(null)
    }
  }

  function chooseEntry(entryId) {
    setAnnotations([])
    setAnnotationsLoading(true)
    setSelectedId(entryId)
    setSearchParams({ entry: String(entryId) })
    setAnnotationDraft(EMPTY_ANNOTATION)
    setEditingAnnotationId(null)
  }

  async function saveGoal(event) {
    event.preventDefault()
    const year = dashboard.goal.year
    setMessage('')
    try {
      const goal = await updateReadingGoal(year, Number(goalTarget))
      setDashboard((current) => ({ ...current, goal }))
      setMessage(`${year} goal updated to ${goal.targetBooks} books.`)
    } catch (requestError) {
      setMessage(getApiErrorMessage(requestError, 'The reading goal could not be updated.'))
    }
  }

  async function saveAnnotation(event) {
    event.preventDefault()
    if (!selectedEntry) return
    setAnnotationBusy(true)
    setMessage('')
    const payload = {
      type: annotationDraft.type,
      content: annotationDraft.content.trim(),
      pageNumber: annotationDraft.pageNumber ? Number(annotationDraft.pageNumber) : null,
    }
    try {
      const saved = editingAnnotationId
        ? await updateReadingAnnotation(editingAnnotationId, payload)
        : await createReadingAnnotation(selectedEntry.id, payload)
      setAnnotations((items) => editingAnnotationId
        ? items.map((item) => (item.id === saved.id ? saved : item))
        : [saved, ...items])
      setAnnotationDraft(EMPTY_ANNOTATION)
      setEditingAnnotationId(null)
      setMessage(editingAnnotationId ? 'Private annotation updated.' : 'Private annotation saved.')
      setDashboard((current) => ({
        ...current,
        entries: current.entries.map((entry) => (entry.id === selectedEntry.id
          ? { ...entry, annotationCount: editingAnnotationId ? entry.annotationCount : entry.annotationCount + 1 }
          : entry)),
      }))
    } catch (requestError) {
      setMessage(getApiErrorMessage(requestError, 'The private annotation could not be saved.'))
    } finally {
      setAnnotationBusy(false)
    }
  }

  function editAnnotation(annotation) {
    setEditingAnnotationId(annotation.id)
    setAnnotationDraft({
      type: annotation.type,
      content: annotation.content,
      pageNumber: annotation.pageNumber ? String(annotation.pageNumber) : '',
    })
  }

  async function removeAnnotation(annotation) {
    if (!window.confirm('Delete this private annotation?')) return
    setAnnotationBusy(true)
    setMessage('')
    try {
      await deleteReadingAnnotation(annotation.id)
      setAnnotations((items) => items.filter((item) => item.id !== annotation.id))
      setDashboard((current) => ({
        ...current,
        entries: current.entries.map((entry) => (entry.id === selectedEntry.id
          ? { ...entry, annotationCount: Math.max(0, entry.annotationCount - 1) }
          : entry)),
      }))
      setMessage('Private annotation deleted.')
    } catch (requestError) {
      setMessage(getApiErrorMessage(requestError, 'The annotation could not be deleted.'))
    } finally {
      setAnnotationBusy(false)
    }
  }

  return (
    <section className="inner-page reading-journey-page">
      <div className="container-xl">
        <header className="reading-journey-hero">
          <div><p className="eyebrow">Your reading life</p><h1>Turn every book into a journey.</h1><p>Track progress, keep quotations and private thoughts, build a reading rhythm, and reflect when a book is complete.</p></div>
          <Link className="btn reading-books-link" to="/my-books"><i className="bi bi-journal-richtext" /> Add from My Books</Link>
        </header>

        {loading && <div className="page-loading"><span className="spinner-border" /> Opening your reading journey…</div>}
        {!loading && error && <div className="catalogue-message catalogue-error"><span><i className="bi bi-exclamation-circle" /></span><div><h2>Your journey could not be opened</h2><p>{error}</p><button className="btn btn-ink" type="button" onClick={loadDashboard}>Try again</button></div></div>}

        {!loading && !error && dashboard && (
          <>
            <section className="reading-stat-grid" aria-label="Reading statistics">
              <div><span>Current streak</span><strong>{dashboard.statistics.currentStreak}</strong><small>days</small></div>
              <div><span>Reading now</span><strong>{dashboard.statistics.currentlyReading}</strong><small>books</small></div>
              <div><span>Completed</span><strong>{dashboard.statistics.completedThisYear}</strong><small>this year</small></div>
              <div><span>Pages logged</span><strong>{dashboard.statistics.pagesReadThisYear}</strong><small>this year</small></div>
              <div><span>Longest streak</span><strong>{dashboard.statistics.longestStreak}</strong><small>days</small></div>
            </section>

            <section className="reading-goal-card">
              <div><p className="eyebrow">{dashboard.goal.year} reading goal</p><h2>{dashboard.goal.completedBooks} of {dashboard.goal.targetBooks} books completed</h2><div className="reading-goal-track"><span style={{ width: `${dashboard.goal.progressPercentage}%` }} /></div></div>
              <form onSubmit={saveGoal}><label>Target<input type="number" min="1" max="1000" value={goalTarget} onChange={(event) => setGoalTarget(event.target.value)} required /></label><button className="btn btn-ink" type="submit">Update goal</button></form>
            </section>

            {message && <p className="reading-journey-message" role="status">{message}</p>}

            {dashboard.entries.length === 0 ? (
              <div className="empty-reader-page reading-empty"><i className="bi bi-bookmark-star" /><h2>Your first reading journey starts in My Books</h2><p>Choose a delivered book or a private upload, then select Start reading.</p><Link className="btn btn-ink" to="/my-books">Open My Books</Link></div>
            ) : (
              <>
                <div className="reading-filter-bar" role="group" aria-label="Filter reading journey">
                  {FILTERS.map((item) => <button type="button" className={filter === item.value ? 'is-active' : ''} onClick={() => setFilter(item.value)} key={item.value}>{item.label}</button>)}
                </div>

                <div className="reading-workspace">
                  <div className="reading-entry-list">
                    {visibleEntries.map((entry) => {
                      const draft = drafts[entry.id] || {}
                      return (
                        <article className={`reading-entry-card${selectedId === entry.id ? ' is-selected' : ''}`} key={entry.id}>
                          <button className="reading-entry-select" type="button" onClick={() => chooseEntry(entry.id)}>
                            <span className="reading-entry-cover"><ReadingCover entry={entry} /></span>
                            <span className="reading-entry-copy"><small>{entry.sourceType === 'PERSONAL_UPLOAD' ? 'Private upload' : 'From My Books'}</small><strong>{entry.title}</strong><span>{entry.author || statusLabel(entry.status)}</span></span>
                            <span className={`reading-status status-${entry.status.toLowerCase()}`}>{statusLabel(entry.status)}</span>
                          </button>
                          <div className="reading-progress-track" aria-label={`${entry.progressPercentage}% complete`}><span style={{ width: `${entry.progressPercentage}%` }} /></div>
                          <div className="reading-progress-summary"><span>{entry.currentPage}{entry.totalPages ? ` / ${entry.totalPages} pages` : ' pages logged'}</span><strong>{entry.progressPercentage}%</strong></div>
                          <form className="reading-progress-form" onSubmit={(event) => saveProgress(event, entry)}>
                            <label>Status<select value={draft.status || entry.status} onChange={(event) => updateDraft(entry.id, 'status', event.target.value)}>{STATUS_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
                            <label>Current page<input type="number" min="0" value={draft.currentPage ?? entry.currentPage} onChange={(event) => updateDraft(entry.id, 'currentPage', event.target.value)} required /></label>
                            <label>Total pages<input type="number" min="1" value={draft.totalPages ?? ''} onChange={(event) => updateDraft(entry.id, 'totalPages', event.target.value)} placeholder="Optional" /></label>
                            <button className="btn btn-ink" disabled={busyId === entry.id} type="submit">{busyId === entry.id ? 'Saving…' : 'Save progress'}</button>
                          </form>
                          <div className="reading-entry-meta"><span><i className="bi bi-calendar3" /> Started {formatDate(entry.startedOn)}</span><span><i className="bi bi-sticky" /> {entry.annotationCount} private items</span>{entry.reviewAvailable && <Link to={`/books/${entry.bookId}#reviews`}>Write your review <i className="bi bi-arrow-right" /></Link>}<button type="button" disabled={busyId === entry.id} onClick={() => removeEntry(entry)}>Remove</button></div>
                        </article>
                      )
                    })}
                    {visibleEntries.length === 0 && <div className="reading-filter-empty"><h2>No books in this shelf</h2><p>Choose another reading status.</p></div>}
                  </div>

                  <aside className="reading-notebook">
                    {selectedEntry ? (
                      <>
                        <div className="reading-notebook-heading"><p className="eyebrow">Private notebook</p><h2>{selectedEntry.title}</h2><p>Only you can see these notes, quotations and bookmarks.</p></div>
                        <form className="reading-annotation-form" onSubmit={saveAnnotation}>
                          <div><label>Type<select value={annotationDraft.type} onChange={(event) => setAnnotationDraft((current) => ({ ...current, type: event.target.value }))}><option value="NOTE">Note</option><option value="QUOTE">Quotation</option><option value="BOOKMARK">Bookmark</option></select></label><label>Page<input type="number" min="1" max={selectedEntry.totalPages || undefined} value={annotationDraft.pageNumber} onChange={(event) => setAnnotationDraft((current) => ({ ...current, pageNumber: event.target.value }))} placeholder="Optional" /></label></div>
                          <textarea minLength="1" maxLength="3000" required value={annotationDraft.content} onChange={(event) => setAnnotationDraft((current) => ({ ...current, content: event.target.value }))} placeholder={annotationDraft.type === 'QUOTE' ? 'Save a passage that stayed with you…' : annotationDraft.type === 'BOOKMARK' ? 'Why are you saving this place?' : 'Write a private thought…'} />
                          <div><button className="btn btn-ink" disabled={annotationBusy} type="submit">{editingAnnotationId ? 'Update' : 'Save privately'}</button>{editingAnnotationId && <button type="button" className="text-button" onClick={() => { setEditingAnnotationId(null); setAnnotationDraft(EMPTY_ANNOTATION) }}>Cancel</button>}</div>
                        </form>
                        {annotationsLoading && <div className="reading-notes-loading"><span className="spinner-border spinner-border-sm" /> Loading notebook…</div>}
                        {!annotationsLoading && annotations.length === 0 && <div className="reading-notes-empty"><i className="bi bi-journal-text" /><p>Your notebook is empty.</p></div>}
                        <div className="reading-annotation-list">
                          {annotations.map((annotation) => <article key={annotation.id} className={`annotation-${annotation.type.toLowerCase()}`}><div><span>{annotation.type === 'QUOTE' ? 'Quotation' : annotation.type === 'BOOKMARK' ? 'Bookmark' : 'Note'}</span>{annotation.pageNumber && <small>Page {annotation.pageNumber}</small>}</div><p>{annotation.content}</p><footer><button type="button" onClick={() => editAnnotation(annotation)}>Edit</button><button type="button" disabled={annotationBusy} onClick={() => removeAnnotation(annotation)}>Delete</button></footer></article>)}
                        </div>
                      </>
                    ) : <div className="reading-notes-empty"><i className="bi bi-arrow-left" /><p>Select a book to open its private notebook.</p></div>}
                  </aside>
                </div>
              </>
            )}
          </>
        )}
      </div>
    </section>
  )
}

export default ReadingJourneyPage
