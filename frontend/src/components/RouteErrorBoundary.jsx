import { Component } from 'react'
import { Link } from 'react-router-dom'

class RouteErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, details) {
    console.error('Page rendering failed', error, details)
  }

  componentDidUpdate(previousProps) {
    if (previousProps.resetKey !== this.props.resetKey && this.state.error) {
      this.setState({ error: null })
    }
  }

  render() {
    if (!this.state.error) return this.props.children

    return (
      <section className="inner-page">
        <div className="container-xl">
          <div className="catalogue-message catalogue-error">
            <span><i className="bi bi-exclamation-triangle" /></span>
            <div>
              <h1>This page could not be displayed</h1>
              <p>Akshara encountered an unexpected display error. Your account and stored data were not changed.</p>
              <button type="button" onClick={() => window.location.reload()}>Reload this page</button>
              <Link to="/">Return to the catalogue</Link>
            </div>
          </div>
        </div>
      </section>
    )
  }
}

export default RouteErrorBoundary
