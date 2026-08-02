import BookCard from '../components/BookCard'
import useReaderData from '../hooks/useReaderData'

function WishlistPage() {
  const { wishlist, loading } = useReaderData()

  return (
    <section className="inner-page">
      <div className="container-xl">
        <div className="inner-page-heading">
          <p className="eyebrow">Your saved shelf</p>
          <h1>Wishlist</h1>
          <p>Books you want to return to, all in one place.</p>
        </div>
        {loading ? <p className="muted-message">Loading your wishlist…</p> : wishlist.length ? (
          <div className="book-grid">{wishlist.map((item) => <BookCard key={item.wishlistItemId} book={item.book} />)}</div>
        ) : <div className="empty-reader-page"><i className="bi bi-heart" /><h2>Your wishlist is waiting</h2><p>Save a book from the catalogue and it will appear here.</p></div>}
      </div>
    </section>
  )
}

export default WishlistPage
