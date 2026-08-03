import { describe, expect, it } from 'vitest'
import { readingDate, readingStatusLabel } from './readingJourney'

describe('reading journey presentation', () => {
  it('uses reader-friendly shelf labels', () => {
    expect(readingStatusLabel('NOT_STARTED')).toBe('Want to read')
    expect(readingStatusLabel('COMPLETED')).toBe('Completed')
  })

  it('does not invent a missing activity date', () => {
    expect(readingDate(null)).toBe('Not recorded')
  })
})
