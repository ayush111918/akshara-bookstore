import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthPage from './AuthPage'

const login = vi.fn()
const register = vi.fn()

vi.mock('../hooks/useAuth', () => ({
  default: () => ({ isAuthenticated: false, login, register }),
}))

describe('AuthPage', () => {
  beforeEach(() => {
    login.mockReset()
    register.mockReset()
  })

  it('submits normalized login fields through the auth provider', async () => {
    login.mockResolvedValue({})
    render(<MemoryRouter><AuthPage mode="login" /></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'reader@example.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Continue to Akshara' }))
    await waitFor(() => expect(login).toHaveBeenCalledWith({ email: 'reader@example.com', password: 'password123' }))
  })

  it('allows the password to be shown without submitting the form', () => {
    render(<MemoryRouter><AuthPage mode="login" /></MemoryRouter>)
    const password = screen.getByLabelText('Password')
    expect(password).toHaveAttribute('type', 'password')
    fireEvent.click(screen.getByRole('button', { name: 'Show password' }))
    expect(password).toHaveAttribute('type', 'text')
  })

  it('explains that academic registration does not require email verification', () => {
    render(<MemoryRouter><AuthPage mode="register" /></MemoryRouter>)
    expect(screen.getByText(/No email verification is required/i)).toBeInTheDocument()
  })
})
