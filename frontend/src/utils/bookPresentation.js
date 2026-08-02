const coverTones = ['navy', 'forest', 'saffron', 'clay', 'plum', 'teal']

export function getPrimaryEdition(book) {
  if (!book?.editions?.length) return null

  return (
    book.editions.find(
      (edition) => edition.inventory?.active && edition.inventory?.stockQuantity > 0,
    ) ??
    book.editions.find((edition) => edition.inventory?.active) ??
    book.editions[0]
  )
}

export function getBookPrice(book) {
  return getPrimaryEdition(book)?.inventory?.price ?? null
}

export function formatPrice(value) {
  if (value == null) return 'Price unavailable'

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(Number(value))
}

export function getAuthors(book) {
  return book?.authors?.map((author) => author.name).filter(Boolean).join(', ') || 'Author unavailable'
}

export function getAvailability(book) {
  const inventory = getPrimaryEdition(book)?.inventory

  if (!inventory?.active) return { label: 'Coming soon', tone: 'muted' }
  if (inventory.stockQuantity > 0) return { label: 'In stock', tone: 'available' }
  return { label: 'Out of stock', tone: 'unavailable' }
}

export function getCoverTone(book) {
  const seed = String(book?.id ?? book?.title ?? 'akshara')
    .split('')
    .reduce((total, character) => total + character.charCodeAt(0), 0)

  return coverTones[seed % coverTones.length]
}

export function getTitleMonogram(title = 'Akshara') {
  return title
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 3)
    .map((word) => word[0])
    .join('')
    .toUpperCase()
}
