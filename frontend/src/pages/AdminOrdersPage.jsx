import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/api'
import { getAdminOrder, getAdminOrders, updateAdminOrderStatus } from '../services/adminOrderService'
import { formatPrice } from '../utils/bookPresentation'

const STATUS_FILTERS = ['ALL', 'PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']
const NEXT_ACTION = {
  PLACED: { status: 'CONFIRMED', label: 'Confirm order', icon: 'bi-check2-circle' },
  CONFIRMED: { status: 'PROCESSING', label: 'Start processing', icon: 'bi-box-seam' },
  PROCESSING: { status: 'SHIPPED', label: 'Mark as shipped', icon: 'bi-truck' },
  SHIPPED: { status: 'DELIVERED', label: 'Mark as delivered', icon: 'bi-house-check' },
}

function readable(value) {
  return String(value ?? '').toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase())
}

function formatDate(value, includeTime = false) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat('en-IN', includeTime
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'medium' }).format(date)
}

function AdminOrdersPage() {
  const [orders, setOrders] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadOrders() {
    setLoading(true)
    setError('')
    try {
      setOrders(await getAdminOrders())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Orders could not be loaded.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getAdminOrders()
      .then((data) => { if (active) setOrders(data) })
      .catch((requestError) => { if (active) setError(getApiErrorMessage(requestError, 'Orders could not be loaded.')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const visibleOrders = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return orders.filter((order) => {
      if (filter !== 'ALL' && order.orderStatus !== filter) return false
      if (!needle) return true
      return [order.orderId, order.customerName, order.customerEmail]
        .some((value) => String(value ?? '').toLowerCase().includes(needle))
    })
  }, [orders, filter, query])

  const summary = useMemo(() => ({
    awaiting: orders.filter((order) => order.orderStatus === 'PLACED').length,
    active: orders.filter((order) => ['CONFIRMED', 'PROCESSING', 'SHIPPED'].includes(order.orderStatus)).length,
    delivered: orders.filter((order) => order.orderStatus === 'DELIVERED').length,
    total: orders.length,
  }), [orders])

  async function selectOrder(orderId) {
    setSelectedId(orderId)
    setDetail(null)
    setDetailLoading(true)
    setError('')
    try {
      setDetail(await getAdminOrder(orderId))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Order details could not be loaded.'))
    } finally {
      setDetailLoading(false)
    }
  }

  async function changeStatus(status) {
    if (!selectedId) return
    if (status === 'CANCELLED' && !window.confirm('Cancel this order and restore its reserved inventory?')) return
    if (status === 'DELIVERED' && !window.confirm('Confirm that this order was delivered? Cash-on-delivery payment will be marked paid.')) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const updated = await updateAdminOrderStatus(selectedId, status)
      setDetail(updated)
      setOrders((current) => current.map((order) => order.orderId === selectedId
        ? { ...order, orderStatus: updated.order.orderStatus, updatedAt: new Date().toISOString() }
        : order))
      setNotice(`Order #${selectedId} is now ${readable(updated.order.orderStatus)}.`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'The order status could not be updated.'))
    } finally {
      setBusy(false)
    }
  }

  const selectedOrder = detail?.order
  const nextAction = NEXT_ACTION[selectedOrder?.orderStatus]
  const canCancel = ['PLACED', 'CONFIRMED', 'PROCESSING'].includes(selectedOrder?.orderStatus)

  return (
    <div className="admin-orders-page">
      <section className="catalogue-admin-hero admin-orders-hero">
        <div className="container-xl">
          <div><p className="eyebrow">Order operations</p><h1>Move every order forward.</h1><p>Confirm new purchases, prepare dispatches, record delivery, and keep readers informed.</p></div>
          <Link className="btn btn-light" to="/admin/books"><i className="bi bi-journals" /> Catalogue operations</Link>
        </div>
      </section>

      <section className="container-xl admin-orders-workspace">
        <div className="catalogue-admin-summary">
          <div><strong>{summary.awaiting}</strong><span>Awaiting confirmation</span></div>
          <div><strong>{summary.active}</strong><span>In fulfilment</span></div>
          <div><strong>{summary.delivered}</strong><span>Delivered</span></div>
          <div><strong>{summary.total}</strong><span>Total orders</span></div>
        </div>

        <div className="admin-orders-toolbar">
          <label><i className="bi bi-search" /><input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search order, customer, or email" /></label>
          <button type="button" onClick={loadOrders} disabled={loading}><i className="bi bi-arrow-clockwise" /> Refresh</button>
        </div>
        <div className="admin-order-filters" role="group" aria-label="Filter orders">
          {STATUS_FILTERS.map((status) => <button className={filter === status ? 'is-active' : ''} key={status} type="button" onClick={() => setFilter(status)}>{readable(status)}</button>)}
        </div>

        {error && <div className="admin-feedback admin-feedback-error" role="alert">{error}</div>}
        {notice && <div className="admin-feedback admin-feedback-success" role="status">{notice}</div>}
        {loading && <div className="page-loading"><span className="spinner-border" /> Loading orders…</div>}

        {!loading && (
          <div className="admin-order-layout">
            <div className="admin-order-list">
              {visibleOrders.map((order) => (
                <button className={`admin-order-row${selectedId === order.orderId ? ' is-selected' : ''}`} type="button" key={order.orderId} onClick={() => selectOrder(order.orderId)}>
                  <div><strong>Order #{order.orderId}</strong><span>{order.customerName}</span><small>{order.customerEmail}</small></div>
                  <div><span className={`admin-order-status status-${order.orderStatus.toLowerCase()}`}>{readable(order.orderStatus)}</span><strong>{formatPrice(order.totalAmount)}</strong><small>{formatDate(order.placedAt)}</small></div>
                  <i className="bi bi-chevron-right" />
                </button>
              ))}
              {!visibleOrders.length && <div className="catalogue-admin-empty"><i className="bi bi-receipt-cutoff" /><h2>No matching orders</h2><p>Try another status or search.</p></div>}
            </div>

            <aside className="admin-order-detail">
              {!selectedId && <div className="admin-order-placeholder"><i className="bi bi-box-seam" /><h2>Select an order</h2><p>Customer, delivery, payment, and confirmation controls will appear here.</p></div>}
              {detailLoading && <div className="page-loading"><span className="spinner-border" /> Opening order…</div>}
              {!detailLoading && selectedOrder && (
                <>
                  <header><div><p>Order #{selectedOrder.orderId}</p><h2>{detail.customerName}</h2><span>{detail.customerEmail}</span></div><span className={`admin-order-status status-${selectedOrder.orderStatus.toLowerCase()}`}>{readable(selectedOrder.orderStatus)}</span></header>
                  <div className="admin-order-primary-action">
                    {nextAction ? <button disabled={busy} type="button" onClick={() => changeStatus(nextAction.status)}><i className={`bi ${nextAction.icon}`} /> {busy ? 'Updating…' : nextAction.label}</button> : <p><i className={`bi ${selectedOrder.orderStatus === 'DELIVERED' ? 'bi-check-circle-fill' : 'bi-x-circle-fill'}`} /> This order is {readable(selectedOrder.orderStatus)}.</p>}
                    {canCancel && <button className="cancel-order-button" disabled={busy} type="button" onClick={() => changeStatus('CANCELLED')}>Cancel order</button>}
                  </div>
                  <section className="admin-order-items"><h3>Books</h3>{(selectedOrder.items ?? []).map((item) => <div key={item.orderItemId}><span>{item.coverImageUrl ? <img src={item.coverImageUrl} alt="" /> : <i className="bi bi-book" />}</span><div><strong>{item.bookTitle}</strong><small>{readable(item.bookFormat)} · Qty {item.quantity}</small></div><strong>{formatPrice(item.subtotal)}</strong></div>)}</section>
                  <div className="admin-order-facts">
                    <section><h3>Deliver to</h3><p>{selectedOrder.shippingAddress?.recipientName}<br />{selectedOrder.shippingAddress?.addressLine1}<br />{selectedOrder.shippingAddress?.addressLine2 && <>{selectedOrder.shippingAddress.addressLine2}<br /></>}{selectedOrder.shippingAddress?.city}, {selectedOrder.shippingAddress?.state} {selectedOrder.shippingAddress?.postalCode}<br />{selectedOrder.shippingAddress?.country}<br />{selectedOrder.shippingAddress?.phone}</p></section>
                    <section><h3>Payment</h3><p>{readable(selectedOrder.paymentMethod)}<br /><span className="admin-order-status">{readable(selectedOrder.paymentStatus)}</span></p><strong>Total {formatPrice(selectedOrder.totalAmount)}</strong><small>Placed {formatDate(selectedOrder.placedAt, true)}</small></section>
                  </div>
                </>
              )}
            </aside>
          </div>
        )}
      </section>
    </div>
  )
}

export default AdminOrdersPage
