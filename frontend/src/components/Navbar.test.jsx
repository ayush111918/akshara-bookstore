import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Navbar from './Navbar'

let auth = { user: null, logout: vi.fn() }

vi.mock('../hooks/useAuth', () => ({ default: () => auth }))
vi.mock('../hooks/useReaderData', () => ({
  default: () => ({ wishlist: [], cart: { totalQuantity: 0 } }),
}))

describe('Navbar account role', () => {
  beforeEach(() => {
    auth = { user: null, logout: vi.fn() }
  })

  it('clearly identifies an administrator account', () => {
    auth = { user: { fullName: 'Ayush Thakur', role: 'ADMIN' }, logout: vi.fn() }
    render(<MemoryRouter><Navbar /></MemoryRouter>)

    expect(screen.getByText('Admin')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Administrator account: Ayush Thakur' })).toHaveAttribute('href', '/admin/books')
    expect(screen.getByRole('link', { name: 'Akshara administration home' })).toHaveAttribute('href', '/admin/books')
    expect(screen.getByRole('link', { name: /storefront preview/i })).toHaveAttribute('href', '/')
    expect(screen.queryByRole('link', { name: 'Reading journey' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Community' })).not.toBeInTheDocument()
  })

  it('does not label a reader as an administrator', () => {
    auth = { user: { fullName: 'Demo Reader', role: 'READER' }, logout: vi.fn() }
    render(<MemoryRouter><Navbar /></MemoryRouter>)

    expect(screen.queryByText('Admin')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Reader account: Demo Reader' })).toHaveAttribute('href', '/account')
  })
})
