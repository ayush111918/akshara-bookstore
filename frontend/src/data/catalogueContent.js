export const catalogueCategories = [
  { name: 'Fiction' },
  { name: 'Indian literature' },
  { name: 'Technology' },
  { name: 'History' },
  { name: 'Philosophy' },
  { name: 'Children' },
]

export const readerJourneySteps = [
  { number: '01', title: 'Discover', text: 'Find books through ideas, moods, genres, and curated shelves.', href: '#catalogue', action: 'Explore books' },
  { number: '02', title: 'Evaluate', text: 'Understand a book through details, editions, ratings, and reviews.', href: '#catalogue', action: 'Choose a book' },
  { number: '03', title: 'Organize', text: 'Shape a personal library with wishlists and reading statuses.', to: '/wishlist', adminTo: '/admin/books', action: 'Open wishlist', adminAction: 'Manage catalogue' },
  { number: '04', title: 'Connect', text: 'Exchange perspectives through discussions and reader collections.', to: '/community', action: 'Join the community' },
  { number: '05', title: 'Purchase', text: 'Choose an edition and complete the journey in one place.', to: '/cart', adminTo: '/admin/books', action: 'Open your cart', adminAction: 'Manage inventory' },
]
