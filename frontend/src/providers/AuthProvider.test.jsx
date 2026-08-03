import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import useAuth from '../hooks/useAuth'
import { getCurrentUser } from '../services/authService'
import AuthProvider from './AuthProvider'

vi.mock('../services/authService', () => ({
  getCurrentUser: vi.fn(),
  loginReader: vi.fn(),
  registerReader: vi.fn(),
}))

function SessionProbe() {
  const { ready, user } = useAuth()
  return <div>{ready ? user?.role || 'signed-out' : 'checking'}</div>
}

describe('AuthProvider session refresh', () => {
  beforeEach(() => {
    vi.mocked(getCurrentUser).mockReset()
  })

  it('keeps a session when the token role still matches the database role', async () => {
    localStorage.setItem('akshara.session', JSON.stringify({
      accessToken: 'reader-token',
      user: { id: 1, role: 'READER' },
    }))
    vi.mocked(getCurrentUser).mockResolvedValue({ id: 1, fullName: 'Reader', role: 'READER' })

    render(<AuthProvider><SessionProbe /></AuthProvider>)

    await screen.findByText('READER')
    expect(JSON.parse(localStorage.getItem('akshara.session')).user.fullName).toBe('Reader')
  })

  it('ends a stale session when the database role changed', async () => {
    localStorage.setItem('akshara.session', JSON.stringify({
      accessToken: 'old-reader-token',
      user: { id: 1, role: 'READER' },
    }))
    vi.mocked(getCurrentUser).mockResolvedValue({ id: 1, fullName: 'Admin', role: 'ADMIN' })

    render(<AuthProvider><SessionProbe /></AuthProvider>)

    await screen.findByText('signed-out')
    await waitFor(() => expect(localStorage.getItem('akshara.session')).toBeNull())
    expect(localStorage.getItem('akshara.session.roleChanged')).toBe('true')
  })
})
