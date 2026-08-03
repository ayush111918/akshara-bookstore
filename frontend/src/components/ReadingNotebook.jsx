function ReadingNotebook({ entry, draft, setDraft, editingId, busy, loading, annotations, onSave, onEdit, onDelete, onCancel }) {
  if (!entry) {
    return <aside className="reading-notebook"><div className="reading-notes-empty"><i className="bi bi-arrow-left" /><p>Select a book to open its private notebook.</p></div></aside>
  }

  return (
    <aside className="reading-notebook">
      <div className="reading-notebook-heading"><p className="eyebrow">Private notebook</p><h2>{entry.title}</h2><p>Only you can see these notes, quotations and bookmarks.</p></div>
      <form className="reading-annotation-form" onSubmit={onSave}>
        <div><label>Type<select value={draft.type} onChange={(event) => setDraft((current) => ({ ...current, type: event.target.value }))}><option value="NOTE">Note</option><option value="QUOTE">Quotation</option><option value="BOOKMARK">Bookmark</option></select></label><label>Page<input type="number" min="1" max={entry.totalPages || undefined} value={draft.pageNumber} onChange={(event) => setDraft((current) => ({ ...current, pageNumber: event.target.value }))} placeholder="Optional" /></label></div>
        <textarea minLength="1" maxLength="3000" required value={draft.content} onChange={(event) => setDraft((current) => ({ ...current, content: event.target.value }))} placeholder={draft.type === 'QUOTE' ? 'Save a passage that stayed with you…' : draft.type === 'BOOKMARK' ? 'Why are you saving this place?' : 'Write a private thought…'} />
        <div><button className="btn btn-ink" disabled={busy} type="submit">{editingId ? 'Update' : 'Save privately'}</button>{editingId && <button type="button" className="text-button" onClick={onCancel}>Cancel</button>}</div>
      </form>
      {loading && <div className="reading-notes-loading"><span className="spinner-border spinner-border-sm" /> Loading notebook…</div>}
      {!loading && annotations.length === 0 && <div className="reading-notes-empty"><i className="bi bi-journal-text" /><p>Your notebook is empty.</p></div>}
      <div className="reading-annotation-list">
        {annotations.map((annotation) => <article key={annotation.id} className={`annotation-${annotation.type.toLowerCase()}`}><div><span>{annotation.type === 'QUOTE' ? 'Quotation' : annotation.type === 'BOOKMARK' ? 'Bookmark' : 'Note'}</span>{annotation.pageNumber && <small>Page {annotation.pageNumber}</small>}</div><p>{annotation.content}</p><footer><button type="button" onClick={() => onEdit(annotation)}>Edit</button><button type="button" disabled={busy} onClick={() => onDelete(annotation)}>Delete</button></footer></article>)}
      </div>
    </aside>
  )
}

export default ReadingNotebook
