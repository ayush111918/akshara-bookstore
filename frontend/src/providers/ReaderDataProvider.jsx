import { useCallback, useEffect, useMemo, useState } from 'react'
import ReaderDataContext from '../contexts/readerDataContext'
import useAuth from '../hooks/useAuth'
import {
  addCartItem as addCartItemRequest,
  addToWishlist,
  getCart,
  getWishlist,
  removeCartItem as removeCartItemRequest,
  removeFromWishlist,
  updateCartItem as updateCartItemRequest,
} from '../services/readerService'

function ReaderDataProvider({ children }) {
  const { user } = useAuth()
  const [wishlist, setWishlist] = useState([])
  const [cart, setCart] = useState(null)
  const isReader = user?.role === 'READER'
  const [loading, setLoading] = useState(Boolean(isReader))

  const refresh = useCallback(async () => {
    if (!isReader) return
    try {
      const [nextWishlist, nextCart] = await Promise.all([getWishlist(), getCart()])
      setWishlist(nextWishlist)
      setCart(nextCart)
    } finally {
      setLoading(false)
    }
  }, [isReader])

  useEffect(() => {
    let active = true

    if (!isReader) {
      Promise.resolve().then(() => {
        if (!active) return
        setWishlist([])
        setCart(null)
        setLoading(false)
      })
    } else {
      Promise.all([getWishlist(), getCart()])
        .then(([nextWishlist, nextCart]) => {
          if (!active) return
          setWishlist(nextWishlist)
          setCart(nextCart)
        })
        .catch((error) => console.error('Unable to load reader data', error))
        .finally(() => {
          if (active) setLoading(false)
        })
    }

    return () => { active = false }
  }, [isReader])

  const toggleWishlist = useCallback(async (book) => {
    const existing = wishlist.find((item) => item.book.id === book.id)
    if (existing) {
      await removeFromWishlist(book.id)
      setWishlist((items) => items.filter((item) => item.book.id !== book.id))
      return false
    }

    const item = await addToWishlist(book.id)
    setWishlist((items) => [item, ...items])
    return true
  }, [wishlist])

  const addCartItem = useCallback(async (bookEditionId, quantity = 1) => {
    const nextCart = await addCartItemRequest(bookEditionId, quantity)
    setCart(nextCart)
    return nextCart
  }, [])

  const updateCartItem = useCallback(async (itemId, quantity) => {
    const nextCart = await updateCartItemRequest(itemId, quantity)
    setCart(nextCart)
    return nextCart
  }, [])

  const removeCartItem = useCallback(async (itemId) => {
    const nextCart = await removeCartItemRequest(itemId)
    setCart(nextCart)
    return nextCart
  }, [])

  const value = useMemo(() => ({
    wishlist,
    wishlistBookIds: new Set(wishlist.map((item) => item.book.id)),
    cart,
    loading,
    refresh,
    toggleWishlist,
    addCartItem,
    updateCartItem,
    removeCartItem,
    clearCartState: () => setCart(null),
  }), [wishlist, cart, loading, refresh, toggleWishlist, addCartItem, updateCartItem, removeCartItem])

  return <ReaderDataContext.Provider value={value}>{children}</ReaderDataContext.Provider>
}

export default ReaderDataProvider
