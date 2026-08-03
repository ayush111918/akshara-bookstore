import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import ReadingOverview from './ReadingOverview'

const dashboard = {
  statistics: {
    currentStreak: 4,
    currentlyReading: 2,
    completedThisYear: 3,
    pagesReadThisYear: 640,
    longestStreak: 9,
  },
  goal: {
    year: 2026,
    targetBooks: 12,
    completedBooks: 3,
    progressPercentage: 25,
  },
}

describe('ReadingOverview', () => {
  it('presents reading statistics and submits a changed goal', () => {
    const onGoalChange = vi.fn()
    const onGoalSubmit = vi.fn((event) => event.preventDefault())

    render(<ReadingOverview dashboard={dashboard} goalTarget="12" onGoalChange={onGoalChange} onGoalSubmit={onGoalSubmit} />)

    expect(screen.getByText('640')).toBeInTheDocument()
    expect(screen.getByLabelText('25% of reading goal complete')).toBeInTheDocument()
    expect(screen.getByText('9 books left in this year’s goal.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Target'), { target: { value: '15' } })
    expect(onGoalChange).toHaveBeenCalledOnce()
    fireEvent.click(screen.getByRole('button', { name: 'Update goal' }))
    expect(onGoalSubmit).toHaveBeenCalledOnce()
  })
})
