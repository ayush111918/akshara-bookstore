import api from './api'

export const BOOK_SOURCES = [
  { value: 'OPEN_LIBRARY', label: 'Open Library' },
  { value: 'GOOGLE_BOOKS', label: 'Google Books (requires server key)' },
]

export const BOOK_FORMATS = [
  { value: 'PAPERBACK', label: 'Paperback' },
  { value: 'HARDCOVER', label: 'Hardcover' },
]

export const AVAILABILITY_OPTIONS = [
  { value: 'IN_STOCK', label: 'Available' },
  { value: 'OUT_OF_STOCK', label: 'Out of stock' },
  { value: 'UNAVAILABLE', label: 'Unavailable' },
]

function unwrapResults(data) {
  if (Array.isArray(data)) return data
  return data?.results ?? data?.items ?? data?.content ?? []
}

function toList(value) {
  if (Array.isArray(value)) {
    return value.map((item) => typeof item === 'string' ? item : item?.name).filter(Boolean)
  }
  if (typeof value === 'string') return value.split(',').map((item) => item.trim()).filter(Boolean)
  return []
}

function normalizeEdition(item, parent = {}) {
  const isbn = item.isbn ?? item.isbn13 ?? item.isbn10 ?? ''
  return {
    source: item.source ?? parent.source ?? '',
    sourceId: item.sourceId ?? item.externalId ?? item.key ?? parent.sourceId ?? parent.externalId ?? parent.key ?? '',
    editionId: item.editionId ?? item.externalEditionId ?? item.key ?? '',
    title: item.title ?? parent.title ?? '',
    subtitle: item.subtitle ?? parent.subtitle ?? '',
    description: item.description ?? parent.description ?? '',
    authors: toList(item.authors ?? item.authorNames ?? parent.authors ?? parent.authorNames),
    publisher: item.publisher ?? item.publisherName ?? parent.publisher ?? parent.publisherName ?? '',
    languageCode: item.languageCode ?? item.language ?? parent.languageCode ?? parent.language ?? '',
    pageCount: item.pageCount ?? item.numberOfPages ?? parent.pageCount ?? parent.numberOfPages ?? '',
    categories: toList(item.categories ?? item.subjects ?? parent.categories ?? parent.subjects),
    coverImageUrl: item.coverImageUrl ?? item.coverUrl ?? item.thumbnailUrl ?? parent.coverImageUrl ?? parent.coverUrl ?? parent.thumbnailUrl ?? '',
    publicationDate: item.publicationDate ?? item.publishedDate ?? item.publishDate ?? parent.publicationDate ?? parent.publishedDate ?? parent.publishDate ?? '',
    isbn10: item.isbn10 ?? (String(isbn).replace(/[-\s]/g, '').length === 10 ? isbn : ''),
    isbn13: item.isbn13 ?? (String(isbn).replace(/[-\s]/g, '').length === 13 ? isbn : ''),
    editionName: item.editionName ?? item.name ?? '',
    alreadyImported: Boolean(item.alreadyImported ?? item.imported ?? item.existsInCatalogue ?? parent.alreadyImported ?? parent.imported),
  }
}

export async function searchExternalBooks(query, source = 'OPEN_LIBRARY') {
  const response = await api.get('/admin/book-import/search', {
    params: { query: query.trim(), source },
    timeout: 30000,
  })
  return unwrapResults(response.data).flatMap((item) => {
    const editions = item.editions ?? item.variants
    return Array.isArray(editions) && editions.length
      ? editions.map((edition) => normalizeEdition(edition, item))
      : [normalizeEdition(item)]
  })
}

export async function importBook(payload) {
  const response = await api.post('/admin/book-import', payload)
  return response.data
}

export async function createManualBook(payload) {
  const response = await api.post('/admin/books', payload)
  return response.data
}

export async function getAdminBooks() {
  const response = await api.get('/admin/books')
  return Array.isArray(response.data) ? response.data : []
}

export async function updateEditionInventory(editionId, payload) {
  const response = await api.put(`/admin/editions/${editionId}/inventory`, payload)
  return response.data
}

export async function restockEdition(editionId, quantity) {
  const response = await api.post(`/admin/editions/${editionId}/inventory/restock`, { quantity })
  return response.data
}

export async function updateBookFeatured(bookId, featured) {
  const response = await api.patch(`/admin/books/${bookId}/featured`, { featured })
  return response.data
}

export async function deleteCatalogueBook(bookId) {
  await api.delete(`/admin/books/${bookId}`)
}

export async function seedCatalogue(count = 50) {
  const response = await api.post('/admin/book-import/seed', null, {
    params: { count },
    timeout: 180000,
  })
  return response.data
}
