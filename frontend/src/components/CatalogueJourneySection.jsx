import { Link } from 'react-router-dom'
import { readerJourneySteps } from '../data/catalogueContent'

function CatalogueJourneySection({ isAdmin }) {
  return (
    <section className="journey-section" id="journey" aria-labelledby="journey-heading">
      <div className="container-xl">
        <div className="journey-intro">
          <p className="eyebrow eyebrow-light">More than a transaction</p>
          <h2 id="journey-heading">The complete reader journey,<br />in one thoughtful place.</h2>
          <p>Akshara connects the moments before and after purchase—the questions, conversations, collections, and discoveries that turn books into a reading life.</p>
        </div>
        <div className="journey-steps">
          {readerJourneySteps.map((step) => {
            const destination = isAdmin && step.adminTo ? step.adminTo : step.to
            const action = isAdmin && step.adminAction ? step.adminAction : step.action
            return (
              <article key={step.number} className="journey-step">
                <span>{step.number}</span><h3>{step.title}</h3><p>{step.text}</p>
                {step.href
                  ? <a className="journey-step-link" href={step.href}>{action} <i className="bi bi-arrow-right" /></a>
                  : <Link className="journey-step-link" to={destination}>{action} <i className="bi bi-arrow-right" /></Link>}
              </article>
            )
          })}
        </div>
      </div>
    </section>
  )
}

export default CatalogueJourneySection
