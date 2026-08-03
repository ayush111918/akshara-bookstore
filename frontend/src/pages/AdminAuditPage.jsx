import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../services/api'
import { getAuditLogs } from '../services/authService'

function AdminAuditPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)

  useEffect(() => {
    let active = true
    getAuditLogs(page).then((response) => { if (active) setData(response) })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, 'Audit activity could not be loaded.')) })
    return () => { active = false }
  }, [page])

  return <section className="inner-page audit-page"><div className="container-xl"><header><p className="eyebrow">Administration</p><h1>Security activity</h1><p>Recent registration, login and account-deletion events.</p></header>{error && <p className="form-alert">{error}</p>}{!data && !error && <div className="page-loading"><span className="spinner-border" /> Loading audit activity…</div>}{data && <><div className="audit-table-wrap"><table><thead><tr><th>When</th><th>Action</th><th>Outcome</th><th>Account</th><th>IP address</th><th>Details</th></tr></thead><tbody>{data.content.map((log) => <tr key={log.id}><td>{new Date(log.createdAt).toLocaleString('en-IN')}</td><td>{log.action.replaceAll('_', ' ')}</td><td><span className={`audit-outcome ${log.outcome.toLowerCase()}`}>{log.outcome}</span></td><td>{log.actorEmail || 'Unknown'}</td><td>{log.ipAddress || '—'}</td><td>{log.details || '—'}</td></tr>)}</tbody></table></div><div className="audit-pagination"><button disabled={data.first} onClick={() => setPage((value) => value - 1)}>Previous</button><span>Page {data.number + 1} of {Math.max(1, data.totalPages)}</span><button disabled={data.last} onClick={() => setPage((value) => value + 1)}>Next</button></div></>}</div></section>
}

export default AdminAuditPage
