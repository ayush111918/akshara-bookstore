import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CatalogueAdminPage from './CatalogueAdminPage'
import { getAdminBooks } from '../services/adminBookService'

vi.mock('../services/adminBookService', () => ({
  AVAILABILITY_OPTIONS: [],
  deleteCatalogueBook: vi.fn(),
  getAdminBooks: vi.fn(),
  restockEdition: vi.fn(),
  seedCatalogue: vi.fn(),
  updateBookFeatured: vi.fn(),
  updateEditionInventory: vi.fn(),
}))

describe('CatalogueAdminPage focused book', () => {
  beforeEach(() => {
    HTMLElement.prototype.scrollIntoView = vi.fn()
    getAdminBooks.mockResolvedValue([{
      id: 7,
      title: '2001',
      authors: [{ name: 'Arthur C. Clarke' }],
      editions: [],
    }])
  })

  it('highlights and scrolls to the book named in the URL', async () => {
    render(<MemoryRouter initialEntries={['/admin/books?book=7']}><CatalogueAdminPage /></MemoryRouter>)

    const record = await screen.findByRole('article', { name: 'Manage 2001' })
    expect(record).toHaveClass('is-targeted')
    await waitFor(() => expect(record.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' }))
  })
})
