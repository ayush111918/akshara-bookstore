import { Navigate, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import useAuth from '../hooks/useAuth'
import useReaderData from '../hooks/useReaderData'
import { getApiErrorMessage } from '../services/api'
import { submitCheckout } from '../services/readerService'
import { formatPrice } from '../utils/bookPresentation'

function CheckoutPage() {
  const { user } = useAuth()
  const { cart, refresh } = useReaderData()
  const navigate = useNavigate()
  const [form, setForm] = useState({ recipientName: user?.fullName ?? '', phone: '', addressLine1: '', addressLine2: '', city: '', state: '', postalCode: '', country: 'India' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  if (cart && !cart.items.length) return <Navigate to="/cart" replace />

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const order = await submitCheckout(form)
      await refresh()
      navigate(`/orders/${order.orderId}`, { state: { justPlaced: true } })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Checkout could not be completed.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="inner-page"><div className="container-xl">
      <div className="inner-page-heading"><p className="eyebrow">Secure checkout</p><h1>Where should we send your books?</h1></div>
      <div className="checkout-layout">
        <form className="checkout-form" onSubmit={handleSubmit}>
          <h2>Shipping address</h2>
          <div className="form-grid">
            <label>Recipient name<input name="recipientName" value={form.recipientName} onChange={updateField} required /></label>
            <label>Phone<input name="phone" value={form.phone} onChange={updateField} placeholder="9876543210" required /></label>
            <label className="span-two">Address line 1<input name="addressLine1" value={form.addressLine1} onChange={updateField} required /></label>
            <label className="span-two">Address line 2 <small>Optional</small><input name="addressLine2" value={form.addressLine2} onChange={updateField} /></label>
            <label>City<input name="city" value={form.city} onChange={updateField} required /></label>
            <label>State<input name="state" value={form.state} onChange={updateField} required /></label>
            <label>PIN / postal code<input name="postalCode" value={form.postalCode} onChange={updateField} required /></label>
            <label>Country<input name="country" value={form.country} onChange={updateField} required /></label>
          </div>
          {error && <p className="form-alert">{error}</p>}
          <button className="btn btn-ink" disabled={submitting || !cart?.items?.length} type="submit">{submitting ? 'Placing order…' : 'Place order · Cash on delivery'}</button>
        </form>
        <aside className="order-summary"><p className="eyebrow">Your order</p>{cart?.items?.map((item) => <div key={item.itemId}><span>{item.title} × {item.quantity}</span><strong>{formatPrice(item.subtotal)}</strong></div>)}<hr /><div><span>Subtotal</span><strong>{formatPrice(cart?.totalAmount)}</strong></div><small>Final shipping fee and total are confirmed by the backend at placement.</small></aside>
      </div>
    </div></section>
  )
}

export default CheckoutPage
