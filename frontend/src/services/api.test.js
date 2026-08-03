import { describe, expect, it } from 'vitest'
import { clearStoredSession, getApiErrorMessage, loadStoredSession, storeSession } from './api'

describe('API utilities', () => {
  it('stores and clears a reader session', () => {
    const session = { accessToken: 'token', user: { id: 3 } }
    storeSession(session)
    expect(loadStoredSession()).toEqual(session)
    clearStoredSession()
    expect(loadStoredSession()).toBeNull()
  })

  it('prefers field validation errors from the backend', () => {
    const error = { response: { data: { message: 'Invalid request', fieldErrors: { password: 'Password is required' } } } }
    expect(getApiErrorMessage(error)).toBe('Password is required')
  })

  it('explains timeout and network failures', () => {
    expect(getApiErrorMessage({ code: 'ECONNABORTED' })).toContain('too long')
    expect(getApiErrorMessage({ request: {} })).toContain('cannot reach')
  })
})
