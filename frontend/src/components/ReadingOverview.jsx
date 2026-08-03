const READING_STATS = [
  { key: 'currentStreak', label: 'Current streak', unit: 'days', icon: 'bi-fire', tone: 'saffron' },
  { key: 'currentlyReading', label: 'Reading now', unit: 'books', icon: 'bi-book-half', tone: 'green' },
  { key: 'completedThisYear', label: 'Completed', unit: 'this year', icon: 'bi-check2-circle', tone: 'gold' },
  { key: 'pagesReadThisYear', label: 'Pages logged', unit: 'this year', icon: 'bi-file-text', tone: 'blue' },
  { key: 'longestStreak', label: 'Longest streak', unit: 'days', icon: 'bi-trophy', tone: 'clay' },
]

function ReadingOverview({ dashboard, goalTarget, onGoalChange, onGoalSubmit }) {
  const goal = dashboard.goal
  const percentage = Math.min(100, Math.max(0, goal.progressPercentage ?? 0))

  return (
    <>
      <section className="reading-stat-grid" aria-label="Reading statistics">
        {READING_STATS.map((stat) => (
          <div className={`reading-stat-${stat.tone}`} key={stat.key}>
            <i className={`bi ${stat.icon}`} aria-hidden="true" />
            <span>{stat.label}</span>
            <strong>{dashboard.statistics[stat.key]} <small>{stat.unit}</small></strong>
          </div>
        ))}
      </section>

      <section className="reading-goal-card">
        <div className="reading-goal-ring" style={{ '--goal-progress': `${percentage}%` }} aria-label={`${percentage}% of reading goal complete`}>
          <span><strong>{Math.round(percentage)}%</strong><small>complete</small></span>
        </div>
        <div className="reading-goal-copy">
          <p className="eyebrow">{goal.year} reading goal</p>
          <h2>{goal.completedBooks} of {goal.targetBooks} books completed</h2>
          <div className="reading-goal-track"><span style={{ width: `${percentage}%` }} /></div>
          <small>{goal.completedBooks >= goal.targetBooks ? 'Goal reached—keep the rhythm going.' : `${Math.max(0, goal.targetBooks - goal.completedBooks)} books left in this year’s goal.`}</small>
        </div>
        <form onSubmit={onGoalSubmit}>
          <label>Target<input type="number" min="1" max="1000" value={goalTarget} onChange={onGoalChange} required /></label>
          <button className="btn btn-ink" type="submit">Update goal</button>
        </form>
      </section>
    </>
  )
}

export default ReadingOverview
