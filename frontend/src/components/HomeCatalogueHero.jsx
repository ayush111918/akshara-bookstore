import { Link } from 'react-router-dom'
import { catalogueCategories } from '../data/catalogueContent'
import { getAuthors, getCoverTone, getTitleMonogram } from '../utils/bookPresentation'

const PLACEHOLDER_BOOKS = [
  { title: 'Stories worth finding', author: 'Curated by Akshara' },
  { title: 'Ideas that stay with you', author: 'The reader’s shelf' },
  { title: 'A new reading life', author: 'Begin with a book' },
]

function HeroBook({ book, position }) {
  const Wrapper = book.id ? Link : 'div'
  const wrapperProps = book.id ? { to: `/books/${book.id}` } : {}

  return (
    <Wrapper
      {...wrapperProps}
      className={`hero-cover-card hero-cover-card-${position}`}
      aria-label={book.id ? `Open ${book.title}` : undefined}
    >
      <span className="hero-cover-art">
        {book.coverImageUrl ? (
          <img src={book.coverImageUrl} alt="" />
        ) : (
          <span className={`hero-cover-fallback cover-tone-${getCoverTone(book)}`}>
            <small>AKSHARA</small>
            <strong>{getTitleMonogram(book.title)}</strong>
            <span>{book.title}</span>
          </span>
        )}
      </span>
      <span className="hero-cover-caption">
        <strong>{book.title}</strong>
        <small>{book.id ? getAuthors(book) : book.author}</small>
      </span>
    </Wrapper>
  )
}

function HomeCatalogueHero({ books, search, onSearchChange, onSubmit, onCategory }) {
  const featuredBooks = [
    ...books.filter((book) => book.featured),
    ...books.filter((book) => !book.featured),
  ].slice(0, 3)
  const hasCuratedBooks = featuredBooks.some((book) => book.featured)
  while (featuredBooks.length < 3) featuredBooks.push(PLACEHOLDER_BOOKS[featuredBooks.length])

  return (
    <section className="home-hero">
      <div className="hero-orb hero-orb-one" aria-hidden="true" />
      <div className="hero-orb hero-orb-two" aria-hidden="true" />

      <div className="container-xl hero-layout">
        <div className="hero-copy">
          <p className="eyebrow hero-eyebrow"><span /> A reader-first bookstore</p>
          <h1>Find a book.<br /><em>Keep the journey.</em></h1>
          <p className="hero-lead">
            Discover thoughtful books, bring them onto your shelf, and turn every
            finished chapter into progress, reflection, and conversation.
          </p>

          <form className="hero-search" onSubmit={onSubmit} role="search">
            <i className="bi bi-search" aria-hidden="true" />
            <input
              type="search"
              aria-label="Search the book catalogue"
              placeholder="Search title, author, or genre"
              value={search}
              onChange={onSearchChange}
            />
            <button type="submit">Explore books <i className="bi bi-arrow-right" /></button>
          </form>

          <div className="hero-genres" aria-label="Browse popular genres">
            <span>Browse</span>
            {catalogueCategories.map((category) => (
              <button type="button" key={category.name} onClick={() => onCategory(category.name)}>
                {category.name}
              </button>
            ))}
          </div>

          <div className="hero-proof" aria-label="Akshara platform highlights">
            <span><i className="bi bi-compass" /> Curated discovery</span>
            <span><i className="bi bi-journal-bookmark" /> Reading progress</span>
            <span><i className="bi bi-people" /> Reader community</span>
          </div>
        </div>

        <div className="hero-visual" aria-label="Books currently on Akshara’s shelf">
          <div className="hero-sun" aria-hidden="true" />
          <div className="hero-cover-stage">
            {featuredBooks.map((book, index) => (
              <HeroBook book={book} position={index + 1} key={book.id ?? book.title} />
            ))}
          </div>
          <div className="hero-shelf-note">
            <span><i className="bi bi-stars" /> {hasCuratedBooks ? 'Featured by Akshara' : 'From the Akshara shelf'}</span>
            <strong>{featuredBooks[0]?.title ?? 'Books selected for curious readers'}</strong>
            <small>Discover · Read · Grow</small>
          </div>
        </div>
      </div>
    </section>
  )
}

export default HomeCatalogueHero
