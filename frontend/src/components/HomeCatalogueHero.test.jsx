import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import HomeCatalogueHero from './HomeCatalogueHero'

describe('HomeCatalogueHero curation', () => {
  it('places administrator-featured books ahead of ordinary catalogue results', () => {
    const books = [
      { id: 1, title: 'Alphabetical First', featured: false, authors: [] },
      { id: 2, title: 'Curated Choice', featured: true, authors: [] },
      { id: 3, title: 'Another Book', featured: false, authors: [] },
      { id: 4, title: 'Fourth Book', featured: false, authors: [] },
    ]

    render(
      <MemoryRouter>
        <HomeCatalogueHero books={books} search="" onSearchChange={vi.fn()} onSubmit={vi.fn()} onCategory={vi.fn()} />
      </MemoryRouter>,
    )

    expect(screen.getByText('Featured by Akshara')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open Curated Choice' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Open Fourth Book' })).not.toBeInTheDocument()
  })
})
