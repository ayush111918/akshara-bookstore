import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import ReadingEntryCard from './ReadingEntryCard'

const entry = {
  id: 7,
  sourceType: 'ORDER',
  title: 'A Book',
  author: 'An Author',
  status: 'READING',
  currentPage: 30,
  totalPages: 300,
  progressPercentage: 10,
  annotationCount: 2,
  startedOn: '2026-08-01',
  reviewAvailable: false,
}

describe('ReadingEntryCard', () => {
  it('reports progress edits and submits through its page workflow callbacks', () => {
    const onDraftChange = vi.fn()
    const onSave = vi.fn((event) => event.preventDefault())
    render(
      <MemoryRouter>
        <ReadingEntryCard entry={entry} draft={{ ...entry, currentPage: '30', totalPages: '300' }} selected={false} busy={false} onChoose={vi.fn()} onDraftChange={onDraftChange} onSave={onSave} onRemove={vi.fn()} />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('Current page'), { target: { value: '45' } })
    expect(onDraftChange).toHaveBeenCalledWith('currentPage', '45')
    fireEvent.click(screen.getByRole('button', { name: 'Save progress' }))
    expect(onSave).toHaveBeenCalledOnce()
  })

  it('moves a not-started book to Reading when page progress is entered', () => {
    const onDraftChange = vi.fn()
    render(
      <MemoryRouter>
        <ReadingEntryCard entry={{ ...entry, status: 'NOT_STARTED', currentPage: 0, startedOn: null }} draft={{ status: 'NOT_STARTED', currentPage: '0', totalPages: '300' }} selected={false} busy={false} onChoose={vi.fn()} onDraftChange={onDraftChange} onSave={vi.fn()} onRemove={vi.fn()} />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('Current page'), { target: { value: '8' } })
    expect(onDraftChange).toHaveBeenCalledWith('currentPage', '8')
    expect(onDraftChange).toHaveBeenCalledWith('status', 'READING')
    expect(screen.getByText('Not started yet')).toBeInTheDocument()
  })
})
