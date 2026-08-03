import api from './api'

export async function getAdminOrders() {
  const response = await api.get('/admin/orders')
  return Array.isArray(response.data) ? response.data : []
}

export async function getAdminOrder(orderId) {
  const response = await api.get(`/admin/orders/${orderId}`)
  return response.data
}

export async function updateAdminOrderStatus(orderId, status) {
  const response = await api.patch(`/admin/orders/${orderId}/status`, { status })
  return response.data
}
