import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReadingJourneyPage from './ReadingJourneyPage'
import { getReadingAnnotations, getReadingDashboard } from '../services/readerService'

vi.mock('../services/readerService', () => ({
  createReadingAnnotation: vi.fn(), deleteReadingAnnotation: vi.fn(), deleteReadingEntry: vi.fn(),
  getReadingAnnotations: vi.fn(), getReadingDashboard: vi.fn(), updateReadingAnnotation: vi.fn(),
  updateReadingGoal: vi.fn(), updateReadingProgress: vi.fn(),
}))
vi.mock('../components/ReadingOverview', () => ({ default: () => <div>Reading overview</div> }))
vi.mock('../components/ReadingEntryCard', () => ({
  default: ({ entry, onChoose }) => <button type="button" onClick={onChoose}>Select {entry.title}</button>,
}))
vi.mock('../components/ReadingNotebook', () => ({ default: ({ entry }) => <aside>Notebook for {entry.title}</aside> }))

describe('ReadingJourneyPage notebook selection', () => {
  beforeEach(() => {
    getReadingDashboard.mockResolvedValue({
      goal: { year: 2026, targetBooks: 12 },
      entries: [
        { id: 7, title: 'A Passage to India', status: 'NOT_STARTED', currentPage: 0, totalPages: 322 },
        { id: 8, title: 'A Walk in the Woods', status: 'READING', currentPage: 45, totalPages: 328 },
      ],
    })
    getReadingAnnotations.mockResolvedValue([])
  })

  it('opens a notebook only after the reader selects its book', async () => {
    render(<MemoryRouter><ReadingJourneyPage /></MemoryRouter>)

    const select = await screen.findByRole('button', { name: 'Select A Passage to India' })
    expect(screen.queryByText('Notebook for A Passage to India')).not.toBeInTheDocument()
    fireEvent.click(select)
    expect(await screen.findByText('Notebook for A Passage to India')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Select A Walk in the Woods' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Back to all books/i }))
    expect(screen.getByRole('button', { name: 'Select A Walk in the Woods' })).toBeInTheDocument()
    expect(screen.queryByText('Notebook for A Passage to India')).not.toBeInTheDocument()
  })
})
