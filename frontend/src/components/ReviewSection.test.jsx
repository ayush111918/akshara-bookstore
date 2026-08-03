import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReviewSection from './ReviewSection'
import { getBookReviews } from '../services/readerService'

let auth = { user: null }

vi.mock('../hooks/useAuth', () => ({ default: () => auth }))
vi.mock('../services/readerService', () => ({
  createReview: vi.fn(),
  deleteReview: vi.fn(),
  getBookReviews: vi.fn(),
  updateReview: vi.fn(),
}))

describe('ReviewSection permissions', () => {
  beforeEach(() => {
    getBookReviews.mockResolvedValue({ reviews: [], reviewCount: 0, averageRating: 0 })
  })

  it('does not offer the review form to an administrator', async () => {
    auth = { user: { id: 1, role: 'ADMIN' } }
    render(<ReviewSection bookId={7} />)

    expect(screen.getByText(/reviews are published from reader accounts/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /publish review/i })).not.toBeInTheDocument()
    expect(await screen.findByText(/no reviews yet/i)).toBeInTheDocument()
  })
})
