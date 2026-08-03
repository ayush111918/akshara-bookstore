import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ReviewTrustBadges from './ReviewTrustBadges'

describe('ReviewTrustBadges', () => {
  it('shows evidence supplied by the backend', () => {
    render(<ReviewTrustBadges review={{ verifiedPurchase: true, completedOnAkshara: true }} />)
    expect(screen.getByText(/Verified purchase/i)).toBeInTheDocument()
    expect(screen.getByText(/Completed/i)).toBeInTheDocument()
  })

  it('renders no label for an unverified general review', () => {
    const { container } = render(<ReviewTrustBadges review={{}} />)
    expect(container).toBeEmptyDOMElement()
  })
})
