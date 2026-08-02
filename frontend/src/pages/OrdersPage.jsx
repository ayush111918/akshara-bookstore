import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import { getOrder, getOrders } from '../services/readerService'
import { formatPrice } from '../utils/bookPresentation'

function OrdersPage() {
  const { orderId } = useParams()
  const location = useLocation()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const request = orderId ? getOrder(orderId) : getOrders()
    request.then(setData).catch((requestError) => setError(getApiErrorMessage(requestError, 'Orders could not be loaded.')))
  }, [orderId])

  if (error) return <section className="inner-page"><div className="container-xl"><p className="form-alert">{error}</p></div></section>
  if (!data) return <div className="page-loading">Loading your orders…</div>

  if (!orderId) {
    return <section className="inner-page"><div className="container-xl"><div className="inner-page-heading"><p className="eyebrow">Your reading purchases</p><h1>Order history</h1></div>{data.length ? <div className="orders-list">{data.map((order) => <Link className="order-row" to={`/orders/${order.orderId}`} key={order.orderId}><div><strong>Order #{order.orderId}</strong><span>{new Date(order.placedAt).toLocaleDateString('en-IN', { dateStyle: 'long' })}</span></div><span className="status-pill">{order.orderStatus.replaceAll('_', ' ')}</span><strong>{formatPrice(order.totalAmount)}</strong><i className="bi bi-chevron-right" /></Link>)}</div> : <div className="empty-reader-page"><i className="bi bi-box-seam" /><h2>No orders yet</h2><p>Your completed purchases will appear here.</p></div>}</div></section>
  }

  return <section className="inner-page"><div className="container-xl narrow-page">
    {location.state?.justPlaced && <div className="success-banner"><i className="bi bi-check-circle-fill" /><div><strong>Your order is confirmed.</strong><span>Akshara has reserved the books in your cart.</span></div></div>}
    <Link className="back-link" to="/orders"><i className="bi bi-arrow-left" /> All orders</Link>
    <div className="order-detail-heading"><div><p className="eyebrow">Order #{data.orderId}</p><h1>{data.orderStatus.replaceAll('_', ' ')}</h1><p>Placed {new Date(data.placedAt).toLocaleString('en-IN', { dateStyle: 'long', timeStyle: 'short' })}</p></div><strong>{formatPrice(data.totalAmount)}</strong></div>
    <div className="order-detail-card"><h2>Books</h2>{data.items.map((item) => <div className="order-book" key={item.orderItemId}><span className="order-book-cover">{item.coverImageUrl ? <img src={item.coverImageUrl} alt="" /> : 'अ'}</span><div><strong>{item.bookTitle}</strong><span>{item.editionName || item.bookFormat} · Qty {item.quantity}</span></div><strong>{formatPrice(item.subtotal)}</strong></div>)}</div>
    <div className="order-detail-grid"><div><h2>Deliver to</h2><p>{data.shippingAddress.recipientName}<br />{data.shippingAddress.addressLine1}<br />{data.shippingAddress.addressLine2 && <>{data.shippingAddress.addressLine2}<br /></>}{data.shippingAddress.city}, {data.shippingAddress.state} {data.shippingAddress.postalCode}<br />{data.shippingAddress.country}<br />{data.shippingAddress.phone}</p></div><div><h2>Payment</h2><p>{data.paymentMethod.replaceAll('_', ' ')}<br /><span className="status-pill">{data.paymentStatus.replaceAll('_', ' ')}</span></p></div></div>
  </div></section>
}

export default OrdersPage
