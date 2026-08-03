import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import Footer from './Footer'

let auth = { user: null }
vi.mock('../hooks/useAuth', () => ({ default: () => auth }))

describe('Footer role navigation', () => {
  it('shows operations rather than reader journeys to an administrator', () => {
    auth = { user: { role: 'ADMIN' } }
    render(<MemoryRouter><Footer /></MemoryRouter>)

    expect(screen.getByRole('link', { name: 'Catalogue management' })).toHaveAttribute('href', '/admin/books')
    expect(screen.getByRole('link', { name: 'Order fulfilment' })).toHaveAttribute('href', '/admin/orders')
    expect(screen.queryByRole('link', { name: 'Reading journey' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Wishlist' })).not.toBeInTheDocument()
  })
})
