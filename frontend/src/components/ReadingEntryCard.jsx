import { useState } from 'react'
import { Link } from 'react-router-dom'
import { getCoverTone, getTitleMonogram } from '../utils/bookPresentation'
import { READING_STATUS_OPTIONS, readingDate, readingStatusLabel } from '../utils/readingJourney'

function ReadingCover({ entry }) {
  const [failed, setFailed] = useState(false)
  if (entry.coverImageUrl && !failed) {
    return <img src={entry.coverImageUrl} alt={`Cover of ${entry.title}`} onError={() => setFailed(true)} />
  }
  return <div className={`reading-cover-fallback cover-tone-${getCoverTone({ id: entry.id, title: entry.title })}`}><span>अ</span><strong>{getTitleMonogram(entry.title)}</strong></div>
}

function ReadingEntryCard({ entry, draft, selected, busy, onChoose, onDraftChange, onSave, onRemove }) {
  const currentStatus = draft.status || entry.status

  function changeStatus(value) {
    onDraftChange('status', value)
    if (value === 'NOT_STARTED') onDraftChange('currentPage', '0')
  }

  function changeCurrentPage(value) {
    onDraftChange('currentPage', value)
    if (Number(value) > 0 && currentStatus === 'NOT_STARTED') {
      onDraftChange('status', 'READING')
    }
  }

  return (
    <article className={`reading-entry-card status-${entry.status.toLowerCase()}${selected ? ' is-selected' : ''}`}>
      <button className="reading-entry-select" type="button" onClick={onChoose} aria-label={`${selected ? 'Close' : 'Open'} private notebook for ${entry.title}`}>
        <span className="reading-entry-cover"><ReadingCover entry={entry} /></span>
        <span className="reading-entry-copy"><small>{entry.sourceType === 'PERSONAL_UPLOAD' ? 'Private upload' : 'From My Books'}</small><strong>{entry.title}</strong><span>{entry.author || readingStatusLabel(entry.status)}</span></span>
        <span className={`reading-status status-${entry.status.toLowerCase()}`}>{readingStatusLabel(entry.status)}</span>
      </button>
      <div className="reading-progress-track" aria-label={`${entry.progressPercentage}% complete`}><span style={{ width: `${entry.progressPercentage}%` }} /></div>
      <div className="reading-progress-summary"><span>{entry.currentPage}{entry.totalPages ? ` / ${entry.totalPages} pages` : ' pages logged'}</span><strong>{entry.progressPercentage}%</strong></div>
      <form className="reading-progress-form" onSubmit={onSave}>
        <label>Status<select value={currentStatus} onChange={(event) => changeStatus(event.target.value)}>{READING_STATUS_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
        <label>Current page<input type="number" min="0" value={draft.currentPage ?? entry.currentPage} onChange={(event) => changeCurrentPage(event.target.value)} required /></label>
        <label>Total pages<input type="number" min="1" value={draft.totalPages ?? ''} onChange={(event) => onDraftChange('totalPages', event.target.value)} placeholder="Optional" /></label>
        <button className="btn btn-ink" disabled={busy} type="submit">{busy ? 'Saving…' : 'Save progress'}</button>
      </form>
      <div className="reading-entry-meta"><span><i className="bi bi-calendar3" /> {entry.startedOn ? `Started ${readingDate(entry.startedOn)}` : 'Not started yet'}</span><span><i className="bi bi-sticky" /> {entry.annotationCount} private items</span>{entry.reviewAvailable && <Link to={`/books/${entry.bookId}#reviews`}>Write your review <i className="bi bi-arrow-right" /></Link>}<button type="button" disabled={busy} onClick={onRemove}>Remove</button></div>
    </article>
  )
}

export default ReadingEntryCard
