function ReviewTrustBadges({ review }) {
  if (!review.verifiedPurchase && !review.completedOnAkshara) return null

  return (
    <span className="review-trust-badges" aria-label="Reader context">
      {review.verifiedPurchase && (
        <span title="This reader received the book through an Akshara order">
          <i className="bi bi-patch-check" /> Verified purchase
        </span>
      )}
      {review.completedOnAkshara && (
        <span title="This reader marked the book completed in Reading Journey">
          <i className="bi bi-bookmark-check" /> Completed
        </span>
      )}
    </span>
  )
}

export default ReviewTrustBadges
