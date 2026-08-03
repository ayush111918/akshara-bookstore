import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BookCard from './BookCard'
import BookDetailsPage from '../pages/BookDetailsPage'
import { getBookById } from '../services/bookService'

let auth = { user: { id: 1, fullName: 'Admin User', role: 'ADMIN' } }
const readerData = {
  wishlistBookIds: new Set(),
  toggleWishlist: vi.fn(),
  addCartItem: vi.fn(),
}

vi.mock('../hooks/useAuth', () => ({ default: () => auth }))
vi.mock('../hooks/useReaderData', () => ({ default: () => readerData }))
vi.mock('../services/bookService', () => ({ getBookById: vi.fn() }))
vi.mock('./ReviewSection', () => ({ default: () => <div>Reviews</div> }))

const book = {
  id: 7,
  title: '2001',
  subtitle: 'A Space Odyssey',
  description: 'A science-fiction classic.',
  featured: true,
  languageCode: 'eng',
  authors: [{ id: 1, name: 'Arthur C. Clarke' }],
  categories: [{ id: 1, name: 'Science Fiction' }],
  editions: [{
    id: 11,
    format: 'PAPERBACK',
    isbn13: '9780000000007',
    inventory: { active: true, stockQuantity: 5, availabilityStatus: 'IN_STOCK', price: 249 },
  }],
}

describe('administrator book actions', () => {
  beforeEach(() => {
    auth = { user: { id: 1, fullName: 'Admin User', role: 'ADMIN' } }
    getBookById.mockResolvedValue(book)
    window.scrollTo = vi.fn()
  })

  it('replaces reader actions on catalogue cards with catalogue management', () => {
    render(<MemoryRouter><BookCard book={book} /></MemoryRouter>)

    expect(screen.getByRole('link', { name: /manage/i })).toHaveAttribute('href', '/admin/books?book=7')
    expect(screen.getByText('Akshara pick')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /add 2001 to cart/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /save 2001 to wishlist/i })).not.toBeInTheDocument()
  })

  it('replaces reader actions on book details with admin destinations', async () => {
    render(
      <MemoryRouter initialEntries={['/books/7']}>
        <Routes><Route path="/books/:bookId" element={<BookDetailsPage />} /></Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('link', { name: /manage this book/i })).toHaveAttribute('href', '/admin/books?book=7')
    expect(screen.getByRole('link', { name: /import or add a book/i })).toHaveAttribute('href', '/admin/books/import')
    expect(screen.queryByRole('button', { name: /add to cart/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /save for later/i })).not.toBeInTheDocument()
  })
})
