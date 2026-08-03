import { Link } from 'react-router-dom'
import useReaderData from '../hooks/useReaderData'
import { getApiErrorMessage } from '../services/api'
import { formatPrice } from '../utils/bookPresentation'
import { useState } from 'react'

function CartPage() {
  const { cart, loading, updateCartItem, removeCartItem } = useReaderData()
  const [busyItem, setBusyItem] = useState(null)
  const [error, setError] = useState('')

  async function changeQuantity(item, quantity) {
    if (quantity < 1 || quantity > item.availableStock) return
    setBusyItem(item.itemId)
    setError('')
    try {
      await updateCartItem(item.itemId, quantity)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Quantity could not be updated.'))
    } finally {
      setBusyItem(null)
    }
  }

  async function remove(itemId) {
    setBusyItem(itemId)
    try {
      await removeCartItem(itemId)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Item could not be removed.'))
    } finally {
      setBusyItem(null)
    }
  }

  if (loading && !cart) return <div className="page-loading">Loading your cart…</div>

  return (
    <section className="inner-page">
      <div className="container-xl">
        <div className="inner-page-heading"><p className="eyebrow">Ready when you are</p><h1>Your cart</h1><p>Review editions and quantities before checkout.</p></div>
        {error && <p className="form-alert">{error}</p>}
        {!cart?.items?.length ? (
          <div className="empty-reader-page"><i className="bi bi-bag" /><h2>Your cart is empty</h2><p>Explore the catalogue to find your next read.</p><Link className="btn btn-ink" to="/#catalogue">Explore books</Link></div>
        ) : (
          <div className="cart-layout">
            <div className="cart-items">
              {cart.items.map((item) => (
                <article className="cart-item" key={item.itemId}>
                  <Link className="cart-thumb" to={`/books/${item.bookId}`}>
                    {item.coverImageUrl ? <img src={item.coverImageUrl} alt="" /> : <span>अ</span>}
                  </Link>
                  <div className="cart-item-copy">
                    <Link to={`/books/${item.bookId}`}><h2>{item.title}</h2></Link>
                    <p>{item.editionName || item.format} · {item.publisherName || 'Akshara edition'}</p>
                    <strong>{formatPrice(item.unitPrice)}</strong>
                  </div>
                  <div className="quantity-control" aria-label={`Quantity for ${item.title}`}>
                    <button disabled={busyItem === item.itemId || item.quantity <= 1} onClick={() => changeQuantity(item, item.quantity - 1)}>−</button>
                    <span>{item.quantity}</span>
                    <button disabled={busyItem === item.itemId || item.quantity >= item.availableStock} onClick={() => changeQuantity(item, item.quantity + 1)}>+</button>
                  </div>
                  <div className="cart-item-total"><strong>{formatPrice(item.subtotal)}</strong><button disabled={busyItem === item.itemId} onClick={() => remove(item.itemId)}>Remove</button></div>
                </article>
              ))}
            </div>
            <aside className="order-summary">
              <p className="eyebrow">Order summary</p><h2>{cart.totalQuantity} {cart.totalQuantity === 1 ? 'book' : 'books'}</h2>
              <div><span>Subtotal</span><strong>{formatPrice(cart.totalAmount)}</strong></div>
              <div><span>Shipping</span><strong>Calculated at checkout</strong></div>
              <Link className="btn btn-ink" to="/checkout">Continue to checkout <i className="bi bi-arrow-right" /></Link>
            </aside>
          </div>
        )}
      </div>
    </section>
  )
}

export default CartPage
