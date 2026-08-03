import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from './api'
import { searchExternalBooks } from './adminBookService'

vi.mock('./api', () => ({ default: { get: vi.fn() } }))

describe('admin book metadata service', () => {
  beforeEach(() => vi.mocked(api.get).mockReset())

  it('sends the selected provider only through Akshara backend', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: { results: [] } })

    await searchExternalBooks(' Atomic Habits ', 'GOOGLE_BOOKS')

    expect(api.get).toHaveBeenCalledWith('/admin/book-import/search', {
      params: { query: 'Atomic Habits', source: 'GOOGLE_BOOKS' },
      timeout: 30000,
    })
  })

  it('preserves provider identity while normalizing external metadata', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: { results: [{
      source: 'GOOGLE_BOOKS', sourceId: 'volume-1', title: 'A Book',
      authors: ['An Author'], isbn13: '9780000000001',
    }] } })

    const results = await searchExternalBooks('A Book', 'GOOGLE_BOOKS')

    expect(results[0]).toMatchObject({
      source: 'GOOGLE_BOOKS', sourceId: 'volume-1', isbn13: '9780000000001',
    })
  })
})
