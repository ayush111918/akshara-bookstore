import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import useAuth from '../hooks/useAuth'
import AdminRoute from './AdminRoute'

vi.mock('../hooks/useAuth', () => ({ default: vi.fn() }))

function renderRoute() {
  return render(
    <MemoryRouter initialEntries={['/admin/books']}>
      <Routes>
        <Route path="/" element={<div>catalogue</div>} />
        <Route path="/login" element={<div>login</div>} />
        <Route path="/admin/books" element={<AdminRoute><div>admin catalogue</div></AdminRoute>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('AdminRoute', () => {
  beforeEach(() => vi.mocked(useAuth).mockReset())

  it('allows a refreshed admin session', () => {
    vi.mocked(useAuth).mockReturnValue({ ready: true, isAuthenticated: true, user: { role: 'ADMIN' } })
    renderRoute()
    expect(screen.getByText('admin catalogue')).toBeInTheDocument()
  })

  it('returns an authenticated reader to the public catalogue', () => {
    vi.mocked(useAuth).mockReturnValue({ ready: true, isAuthenticated: true, user: { role: 'READER' } })
    renderRoute()
    expect(screen.getByText('catalogue')).toBeInTheDocument()
  })

  it('sends a signed-out user to login', () => {
    vi.mocked(useAuth).mockReturnValue({ ready: true, isAuthenticated: false, user: null })
    renderRoute()
    expect(screen.getByText('login')).toBeInTheDocument()
  })
})
