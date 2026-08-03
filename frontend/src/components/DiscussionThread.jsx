import { useState } from 'react'
import { Link } from 'react-router-dom'
import useAuth from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'
import {
  createReviewReply,
  deleteReviewReply,
  getReviewReplies,
  updateReviewReply,
} from '../services/readerService'

function DiscussionThread({ reviewId }) {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const [loading, setLoading] = useState(false)
  const [replies, setReplies] = useState([])
  const [draft, setDraft] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [editingText, setEditingText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function toggleThread() {
    const nextOpen = !open
    setOpen(nextOpen)
    if (!nextOpen || loaded) return
    setLoading(true)
    setError('')
    try {
      setReplies(await getReviewReplies(reviewId))
      setLoaded(true)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'The discussion could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }

  async function submitReply(event) {
    event.preventDefault()
    if (draft.trim().length < 2) return
    setBusy(true)
    setError('')
    try {
      const created = await createReviewReply(reviewId, draft.trim())
      setReplies((items) => [...items, created])
      setDraft('')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your reply could not be posted.'))
    } finally {
      setBusy(false)
    }
  }

  async function saveEdit(replyId) {
    if (editingText.trim().length < 2) return
    setBusy(true)
    setError('')
    try {
      const updated = await updateReviewReply(replyId, editingText.trim())
      setReplies((items) => items.map((reply) => reply.id === replyId ? updated : reply))
      setEditingId(null)
      setEditingText('')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your reply could not be updated.'))
    } finally {
      setBusy(false)
    }
  }

  async function removeReply(reply) {
    if (!window.confirm('Delete this discussion reply?')) return
    setBusy(true)
    setError('')
    try {
      await deleteReviewReply(reply.id)
      setReplies((items) => items.filter((item) => item.id !== reply.id))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Your reply could not be deleted.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className={`discussion-thread${open ? ' is-open' : ''}`}>
      <button className="discussion-toggle" type="button" onClick={toggleThread} aria-expanded={open}>
        <span><i className="bi bi-chat-left-text" /> {open ? 'Close discussion' : 'Open discussion'}</span>
        {loaded && <small>{replies.length} {replies.length === 1 ? 'reply' : 'replies'}</small>}
      </button>
      {open && (
        <div className="discussion-body">
          <p className="discussion-explainer"><strong>Discussion is different from a review.</strong> Reply to the reader’s perspective, ask a question, or add another interpretation.</p>
          {loading && <p className="muted-message">Loading discussion…</p>}
          {!loading && replies.map((reply) => {
            const mine = Number(reply.userId) === Number(user?.id)
            return (
              <article className="discussion-reply" key={reply.id}>
                <span className="reader-avatar">{reply.readerName?.split(/\s+/).map((part) => part[0]).slice(0, 2).join('')}</span>
                <div>
                  <header><strong>{reply.readerName}</strong><small>{new Date(reply.createdAt).toLocaleDateString('en-IN', { dateStyle: 'medium' })}</small></header>
                  {editingId === reply.id ? <textarea maxLength="1000" minLength="2" value={editingText} onChange={(event) => setEditingText(event.target.value)} /> : <p>{reply.content}</p>}
                  {mine && <div className="discussion-owner-actions">{editingId === reply.id ? <><button disabled={busy} type="button" onClick={() => saveEdit(reply.id)}>Save</button><button type="button" onClick={() => setEditingId(null)}>Cancel</button></> : <><button type="button" onClick={() => { setEditingId(reply.id); setEditingText(reply.content) }}>Edit</button><button className="danger" disabled={busy} type="button" onClick={() => removeReply(reply)}>Delete</button></>}</div>}
                </div>
              </article>
            )
          })}
          {!loading && loaded && !replies.length && <p className="muted-message">No replies yet. Start a focused conversation about this review.</p>}
          {user?.role === 'READER' ? <form className="discussion-reply-form" onSubmit={submitReply}><textarea value={draft} minLength="2" maxLength="1000" required placeholder="Reply thoughtfully to this reader…" onChange={(event) => setDraft(event.target.value)} /><button disabled={busy} type="submit">{busy ? 'Posting…' : 'Post reply'}</button></form> : <p className="discussion-signin"><Link to="/login">Sign in as a reader</Link> to join this discussion.</p>}
          {error && <p className="form-alert" role="alert">{error}</p>}
        </div>
      )}
    </section>
  )
}

export default DiscussionThread
