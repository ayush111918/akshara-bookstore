import { Link, useParams } from 'react-router-dom'

function BookDetailsPage() {
  const { bookId } = useParams()

  return (
    <div className="container py-5">
      <Link className="text-decoration-none" to="/">
        <i className="bi bi-arrow-left me-2" />
        Back to catalogue
      </Link>

      <h1 className="mt-4">Book details</h1>
      <p>Selected book ID: {bookId}</p>
    </div>
  )
}

export default BookDetailsPage