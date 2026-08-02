import api from './api'

export async function getWishlist() {
  const response = await api.get('/wishlist')
  return response.data
}

export async function addToWishlist(bookId) {
  const response = await api.post(`/wishlist/${bookId}`)
  return response.data
}

export async function removeFromWishlist(bookId) {
  await api.delete(`/wishlist/${bookId}`)
}

export async function getCart() {
  const response = await api.get('/cart')
  return response.data
}

export async function addCartItem(bookEditionId, quantity = 1) {
  const response = await api.post('/cart/items', { bookEditionId, quantity })
  return response.data
}

export async function updateCartItem(itemId, quantity) {
  const response = await api.patch(`/cart/items/${itemId}`, { quantity })
  return response.data
}

export async function removeCartItem(itemId) {
  const response = await api.delete(`/cart/items/${itemId}`)
  return response.data
}

export async function submitCheckout(shippingAddress) {
  const response = await api.post('/checkout', { shippingAddress })
  return response.data
}

export async function getOrders() {
  const response = await api.get('/orders')
  return response.data
}

export async function getOrder(orderId) {
  const response = await api.get(`/orders/${orderId}`)
  return response.data
}

export async function getBookReviews(bookId) {
  const response = await api.get(`/public/books/${bookId}/reviews`)
  return response.data
}

export async function getRecentReviews(limit = 6) {
  const response = await api.get('/public/reviews/recent', { params: { limit } })
  return response.data
}

export async function createReview(review) {
  const response = await api.post('/reviews', review)
  return response.data
}

export async function updateReview(reviewId, review) {
  const response = await api.put(`/reviews/${reviewId}`, review)
  return response.data
}

export async function deleteReview(reviewId) {
  await api.delete(`/reviews/${reviewId}`)
}
