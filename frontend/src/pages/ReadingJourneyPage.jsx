import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import ReadingEntryCard from '../components/ReadingEntryCard'
import ReadingNotebook from '../components/ReadingNotebook'
import ReadingOverview from '../components/ReadingOverview'
import { READING_FILTERS } from '../utils/readingJourney'
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
const EMPTY_ANNOTATION = { type: 'NOTE', content: '', pageNumber: '' }

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
            <ReadingOverview dashboard={dashboard} goalTarget={goalTarget} onGoalChange={(event) => setGoalTarget(event.target.value)} onGoalSubmit={saveGoal} />

            {message && <p className="reading-journey-message" role="status">{message}</p>}

            {dashboard.entries.length === 0 ? (
              <div className="empty-reader-page reading-empty"><i className="bi bi-bookmark-star" /><h2>Your first reading journey starts in My Books</h2><p>Choose a delivered book or a private upload, then select Start reading.</p><Link className="btn btn-ink" to="/my-books">Open My Books</Link></div>
            ) : (
              <>
                <div className="reading-filter-bar" role="group" aria-label="Filter reading journey">
                  {READING_FILTERS.map((item) => {
                    const count = item.value === 'ALL' ? dashboard.entries.length : dashboard.entries.filter((entry) => entry.status === item.value).length
                    return <button type="button" className={`reading-filter-${item.value.toLowerCase()}${filter === item.value ? ' is-active' : ''}`} onClick={() => setFilter(item.value)} key={item.value}>{item.label}<span>{count}</span></button>
                  })}
                </div>

                <div className="reading-workspace">
                  <div className="reading-entry-list">
                    {visibleEntries.map((entry) => {
                      const draft = drafts[entry.id] || {}
                      return <ReadingEntryCard key={entry.id} entry={entry} draft={draft} selected={selectedId === entry.id} busy={busyId === entry.id} onChoose={() => chooseEntry(entry.id)} onDraftChange={(field, value) => updateDraft(entry.id, field, value)} onSave={(event) => saveProgress(event, entry)} onRemove={() => removeEntry(entry)} />
                    })}
                    {visibleEntries.length === 0 && <div className="reading-filter-empty"><h2>No books in this shelf</h2><p>Choose another reading status.</p></div>}
                  </div>

                  <ReadingNotebook entry={selectedEntry} draft={annotationDraft} setDraft={setAnnotationDraft} editingId={editingAnnotationId} busy={annotationBusy} loading={annotationsLoading} annotations={annotations} onSave={saveAnnotation} onEdit={editAnnotation} onDelete={removeAnnotation} onCancel={() => { setEditingAnnotationId(null); setAnnotationDraft(EMPTY_ANNOTATION) }} />
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
