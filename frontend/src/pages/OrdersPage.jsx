import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import { getOrder, getOrders } from '../services/readerService'
import { formatPrice } from '../utils/bookPresentation'

const DELIVERY_STEPS = ['PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED']

function readableStatus(value = 'PLACED') {
  return String(value).replaceAll('_', ' ').toLowerCase().replace(/^./, (letter) => letter.toUpperCase())
}

function safeDate(value, includeTime = false) {
  if (!value) return 'Date unavailable'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return date.toLocaleString('en-IN', includeTime
    ? { dateStyle: 'long', timeStyle: 'short' }
    : { dateStyle: 'long' })
}

function DeliveryTimeline({ status }) {
  const normalizedStatus = DELIVERY_STEPS.includes(status) ? status : 'PLACED'
  const currentIndex = DELIVERY_STEPS.indexOf(normalizedStatus)

  if (status === 'CANCELLED') {
    return (
      <div className="order-cancelled-message">
        <i className="bi bi-x-circle" />
        <div><strong>This order was cancelled</strong><span>No further delivery steps will take place.</span></div>
      </div>
    )
  }

  return (
    <ol className="delivery-timeline" aria-label="Delivery progress">
      {DELIVERY_STEPS.map((step, index) => (
        <li className={index < currentIndex ? 'is-complete' : index === currentIndex ? 'is-current' : ''} key={step}>
          <span>{index < currentIndex ? <i className="bi bi-check" /> : index + 1}</span>
          <div><strong>{readableStatus(step)}</strong><small>{step === 'PLACED' ? 'Order received' : step === 'DELIVERED' ? 'Books delivered' : 'Pending update'}</small></div>
        </li>
      ))}
    </ol>
  )
}

function OrderBook({ item, delivered }) {
  const isDigital = item.bookFormat === 'PDF' || item.bookFormat === 'EPUB'
  const bookDestination = item.bookId ? `/books/${item.bookId}` : null

  return (
    <div className="order-book">
      <span className="order-book-cover">{item.coverImageUrl ? <img src={item.coverImageUrl} alt={`Cover of ${item.bookTitle ?? 'ordered book'}`} /> : 'अ'}</span>
      <div>
        {bookDestination ? <Link className="order-book-title" to={bookDestination}>{item.bookTitle ?? 'Ordered book'}</Link> : <strong>{item.bookTitle ?? 'Ordered book'}</strong>}
        <span>{item.editionName || readableStatus(item.bookFormat || 'Edition')} · Qty {item.quantity ?? 1}</span>
        {isDigital && <small className="order-access-warning"><i className="bi bi-info-circle" /> Digital access is unavailable for this historical item.</small>}
        {!isDigital && delivered && bookDestination && <Link className="order-reflection-link" to={`${bookDestination}#reviews`}>Review and reflect on this book <i className="bi bi-arrow-right" /></Link>}
        {!isDigital && !delivered && bookDestination && <Link className="order-reflection-link" to={bookDestination}>View book details <i className="bi bi-arrow-right" /></Link>}
      </div>
      <strong>{formatPrice(item.subtotal)}</strong>
    </div>
  )
}

function OrdersPage() {
  const { orderId } = useParams()
  const location = useLocation()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loadedKey, setLoadedKey] = useState('')
  const [errorKey, setErrorKey] = useState('')
  const requestKey = orderId || 'order-list'

  useEffect(() => {
    let active = true
    const request = orderId ? getOrder(orderId) : getOrders()
    request
      .then((response) => {
        if (active) {
          setData(response)
          setError('')
          setLoadedKey(orderId || 'order-list')
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(getApiErrorMessage(requestError, 'Orders could not be loaded.'))
          setErrorKey(orderId || 'order-list')
        }
      })
    return () => {
      active = false
    }
  }, [orderId])

  const orderItems = useMemo(() => Array.isArray(data?.items) ? data.items : [], [data])

  if (error && errorKey === requestKey) {
    return <section className="inner-page"><div className="container-xl"><div className="catalogue-message catalogue-error"><span><i className="bi bi-receipt-cutoff" /></span><div><h1>We could not open this order</h1><p>{error}</p><Link to="/orders">Return to order history</Link></div></div></div></section>
  }
  if (!data || loadedKey !== requestKey) return <div className="page-loading"><span className="spinner-border" /> Loading your orders…</div>

  if (!orderId) {
    const orders = Array.isArray(data) ? data : []
    return (
      <section className="inner-page"><div className="container-xl">
        <div className="inner-page-heading"><p className="eyebrow">Your reading purchases</p><h1>Order history</h1><p>Track physical deliveries and return to books you have purchased.</p></div>
        {orders.length ? <div className="orders-list">{orders.map((order) => <Link className="order-row" to={`/orders/${order.orderId}`} key={order.orderId}><div><strong>Order #{order.orderId}</strong><span>{safeDate(order.placedAt)}</span></div><span className="status-pill">{readableStatus(order.orderStatus)}</span><strong>{formatPrice(order.totalAmount)}</strong><i className="bi bi-chevron-right" /></Link>)}</div> : <div className="empty-reader-page"><i className="bi bi-box-seam" /><h2>No orders yet</h2><p>Your completed purchases will appear here.</p><Link className="btn btn-ink" to="/#catalogue">Explore books</Link></div>}
      </div></section>
    )
  }

  const status = data.orderStatus || 'PLACED'
  const delivered = status === 'DELIVERED'
  const address = data.shippingAddress
  const hasOnlyPhysicalBooks = orderItems.every((item) => item.bookFormat !== 'PDF' && item.bookFormat !== 'EPUB')

  return (
    <section className="inner-page"><div className="container-xl narrow-page">
      {location.state?.justPlaced && <div className="success-banner"><i className="bi bi-check-circle-fill" /><div><strong>Your order is confirmed.</strong><span>Akshara has reserved the physical books in your cart.</span></div></div>}
      <div className="order-detail-navigation"><Link className="back-link" to="/orders"><i className="bi bi-arrow-left" /> All orders</Link><Link className="back-link" to="/my-books"><i className="bi bi-journal-bookmark" /> Go to My Books</Link></div>
      <div className="order-detail-heading"><div><p className="eyebrow">Order #{data.orderId ?? orderId}</p><h1>{readableStatus(status)}</h1><p>Placed {safeDate(data.placedAt, true)}</p></div><strong>{formatPrice(data.totalAmount)}</strong></div>

      <section className="order-journey-card">
        <div className="order-journey-heading"><div><p className="eyebrow">Delivery journey</p><h2>{delivered ? 'Your books have arrived.' : 'Your physical books are on their way.'}</h2></div><span className="status-pill">{readableStatus(status)}</span></div>
        <DeliveryTimeline status={status} />
        {hasOnlyPhysicalBooks && <p className="order-access-note"><i className="bi bi-box-seam" /> This purchase is for physical editions. It is now saved in <Link to="/my-books">My Books</Link>, where you can follow it from delivery to your reading shelf.</p>}
      </section>

      <div className="order-detail-card"><h2>Books in this order</h2>{orderItems.length ? orderItems.map((item, index) => <OrderBook item={item} delivered={delivered} key={item.orderItemId ?? `${item.bookTitle}-${index}`} />) : <p className="muted-message">No item details are available for this order.</p>}</div>

      <div className="order-detail-grid">
        <div><h2>Deliver to</h2>{address ? <p>{address.recipientName}<br />{address.addressLine1}<br />{address.addressLine2 && <>{address.addressLine2}<br /></>}{address.city}, {address.state} {address.postalCode}<br />{address.country}<br />{address.phone}</p> : <p>Shipping details are unavailable.</p>}</div>
        <div><h2>Payment</h2><p>{readableStatus(data.paymentMethod || 'Payment method unavailable')}<br /><span className="status-pill">{readableStatus(data.paymentStatus || 'Pending')}</span></p></div>
      </div>
    </div></section>
  )
}

export default OrdersPage
