import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyBooksPage from './MyBooksPage'
import { createReadingEntry, getMyBooks, getPersonalBooks, getReadingDashboard } from '../services/readerService'

vi.mock('../services/readerService', () => ({
  createReadingEntry: vi.fn(),
  deletePersonalBook: vi.fn(),
  getMyBooks: vi.fn(),
  getPersonalBookFile: vi.fn(),
  getPersonalBooks: vi.fn(),
  getReadingDashboard: vi.fn(),
  uploadPersonalBook: vi.fn(),
}))

function CurrentLocation() {
  return <span>{useLocation().pathname}{useLocation().search}</span>
}

describe('MyBooksPage reading actions', () => {
  beforeEach(() => {
    getMyBooks.mockResolvedValue([{
      orderItemId: 21,
      orderId: 4,
      orderStatus: 'DELIVERED',
      bookId: 11,
      title: 'A Walk in the Woods',
      format: 'PAPERBACK',
      quantity: 1,
    }])
    getPersonalBooks.mockResolvedValue([])
    getReadingDashboard.mockResolvedValue({
      entries: [{ id: 7, sourceType: 'PURCHASED_BOOK', sourceId: 11, status: 'READING' }],
    })
  })

  it('continues an existing journey instead of creating it again', async () => {
    render(
      <MemoryRouter initialEntries={['/my-books']}>
        <Routes>
          <Route path="*" element={<><MyBooksPage /><CurrentLocation /></>} />
        </Routes>
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('button', { name: /Continue reading/i }))

    expect(await screen.findByText('/reading-journey?entry=7')).toBeInTheDocument()
    expect(createReadingEntry).not.toHaveBeenCalled()
  })
})
