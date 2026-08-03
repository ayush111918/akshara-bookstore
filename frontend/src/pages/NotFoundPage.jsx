import { Link } from 'react-router-dom'

function NotFoundPage() {
  return <section className="inner-page not-found-page"><div><span>404</span><p className="eyebrow">Page not found</p><h1>This page has wandered off the shelf.</h1><p>The address may be incorrect, or the page may have moved.</p><Link className="btn btn-ink" to="/">Return to the catalogue</Link></div></section>
}

export default NotFoundPage
