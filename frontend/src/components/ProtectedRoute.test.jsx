import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import ProtectedRoute from './ProtectedRoute'

let auth = { ready: true, isAuthenticated: false }
vi.mock('../hooks/useAuth', () => ({ default: () => auth }))

describe('ProtectedRoute', () => {
  it('redirects signed-out visitors to login', () => {
    auth = { ready: true, isAuthenticated: false }
    render(<MemoryRouter initialEntries={['/private']}><Routes><Route path="/login" element={<p>Login page</p>} /><Route path="/private" element={<ProtectedRoute><p>Private page</p></ProtectedRoute>} /></Routes></MemoryRouter>)
    expect(screen.getByText('Login page')).toBeInTheDocument()
  })

  it('renders protected content for an authenticated reader', () => {
    auth = { ready: true, isAuthenticated: true }
    render(<MemoryRouter><ProtectedRoute><p>Private page</p></ProtectedRoute></MemoryRouter>)
    expect(screen.getByText('Private page')).toBeInTheDocument()
  })
})
