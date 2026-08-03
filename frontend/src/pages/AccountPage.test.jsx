import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import useAuth from '../hooks/useAuth'
import { deleteCurrentAccount } from '../services/authService'
import AccountPage from './AccountPage'

const logout = vi.fn()
vi.mock('../hooks/useAuth', () => ({ default: vi.fn() }))
vi.mock('../services/authService', () => ({ deleteCurrentAccount: vi.fn() }))

describe('AccountPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue({
      user: { fullName: 'A Reader', email: 'reader@example.com', role: 'READER' },
      logout,
    })
    vi.mocked(deleteCurrentAccount).mockReset()
    logout.mockReset()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  it('keeps deletion disabled until the explicit confirmation phrase is entered', () => {
    render(<MemoryRouter><AccountPage /></MemoryRouter>)
    const button = screen.getByRole('button', { name: 'Delete my account' })
    expect(button).toBeDisabled()
    fireEvent.change(screen.getByLabelText(/Type DELETE/i), { target: { value: 'DELETE' } })
    expect(button).toBeEnabled()
  })

  it('confirms the password with the backend before clearing the session', async () => {
    vi.mocked(deleteCurrentAccount).mockResolvedValue()
    render(<MemoryRouter><AccountPage /></MemoryRouter>)
    fireEvent.change(screen.getByLabelText(/Current password/i), { target: { value: 'secret123' } })
    fireEvent.change(screen.getByLabelText(/Type DELETE/i), { target: { value: 'DELETE' } })
    fireEvent.click(screen.getByRole('button', { name: 'Delete my account' }))
    await waitFor(() => expect(deleteCurrentAccount).toHaveBeenCalledWith('secret123'))
    expect(logout).toHaveBeenCalled()
  })
})
