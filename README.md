# Akshara Bookstore

Akshara is a reader-first online bookstore that connects discovery and commerce
with the reader's journey after purchase.

`Discover -> Purchase or upload -> My Books -> Track reading -> Review -> Discuss`

The application is an academic full-stack project built with React, Spring Boot,
JWT authentication, and MySQL.

## What Akshara provides

### Reader features

- Searchable catalogue with book details, editions, ratings, and reviews
- Wishlist, cart, address-based checkout, and order tracking
- My Books for delivered purchases and private PDF/EPUB uploads
- Reading shelves, progress, dates, goals, streaks, notes, quotations, and bookmarks
- Review editing/deletion and book discussions
- Password-confirmed account deletion and anonymization

### Administrator features

- Open Library metadata search with an optional Google Books fallback
- Reviewed catalogue import and manual book entry
- ISBN and SKU duplicate protection
- Price, stock, availability, active-sale, and featured-book management
- Validated order fulfilment and inventory restoration on cancellation
- Security audit records for important authentication and account events

External providers supply descriptive metadata only. Akshara owns its catalogue
selection, prices, stock, availability, users, orders, reviews, and reading data.

## Architecture

```text
React + Vite
     |
     | JSON REST API / JWT
     v
Spring Boot controllers -> services -> JPA repositories -> MySQL
                              |
                              +-> Open Library / optional Google Books
```

Private uploads are stored by the backend and are accessible only to their owner.
The Nginx Docker service serves React and proxies `/api` requests to Spring Boot.

## Technology

| Layer | Technology |
| --- | --- |
| Frontend | React, Vite, React Router, Axios, Bootstrap Icons |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA |
| Authentication | JWT and BCrypt |
| Database | MySQL 8.4 |
| Testing | JUnit, Mockito, Vitest, React Testing Library |
| Deployment | Docker Compose and Nginx |

## Run with Docker

```bash
cp .env.example .env
# Replace every placeholder in .env with a local secret.
docker compose up --build -d
docker compose ps
```

Open `http://localhost:8088`. The port can be changed through `AKSHARA_PORT` in
`.env`. Do not commit `.env`.

Health check:

```bash
curl http://localhost:8088/api/public/health
```

MySQL data, private uploads, and backend logs use named Docker volumes.

## Run locally

Start MySQL with an `akshara` database and configure the environment:

```bash
export DB_URL='jdbc:mysql://localhost:3306/akshara'
export DB_USERNAME='akshara_app'
export DB_PASSWORD='<local-password>'
export JWT_SECRET='<at-least-32-random-characters>'
export BOOK_IMPORT_CONTACT='your-contact@example.com'
# Optional: export GOOGLE_BOOKS_API_KEY='<google-books-api-key>'
```

Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

In another terminal, start the frontend:

```bash
cd frontend
npm ci
npm run dev
```

The local backend health endpoint is
`http://localhost:8080/api/public/health`.

## Create an administrator

Registration always creates a `READER`. Register normally, then deliberately
promote the account in MySQL:

```sql
USE akshara;
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';
```

For Docker, open the database console with:

```bash
docker compose exec database mysql -u root -p
```

Sign out and sign in again after promotion so the new JWT contains the Admin
role. The project intentionally has no committed default Admin password.

## Main workflows

### Catalogue import

1. Sign in as Admin and open `/admin/books/import`.
2. Search by title, author, or ISBN.
3. Select and review an external edition.
4. Add Akshara's format, SKU, price, stock, availability, and active state.
5. Import the selection or use Manual Entry when metadata is unavailable.

The frontend never calls external book providers directly. Spring Boot
normalizes, validates, enriches, deduplicates, and stores the selected result.

### Order fulfilment

Readers check out using cash on delivery. An Admin opens `/admin/orders` and
advances an order through:

`PLACED -> CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED`

Invalid state changes are rejected. Eligible cancellation restores reserved
inventory. Delivered purchases appear in the Reader's My Books shelf.

### Reading Journey

A Reader can track a delivered physical book or a private upload from
`/my-books`. The `/reading-journey` dashboard manages reading status, page
progress, dates, private annotations, streaks, yearly goals, and completion.
Completing a catalogue book leads to its review and community discussion.

Purchasing a physical edition does not unlock a copyrighted digital file.

## Verification

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm run lint
npm test -- --run
npm run build
```

The backend tests cover services, controllers, authentication, validation,
ownership rules, and data lifecycle behavior. Frontend tests cover session and
route handling, forms, Reading Journey behavior, review trust indicators, and
service utilities.

## Project boundaries

- Cash on delivery is simulated; no real payment gateway is claimed.
- Email verification and password-reset email delivery are outside the MVP.
- Private PDFs open through the browser; EPUB files download rather than render
  in an integrated reader.
- Akshara does not distribute copyrighted eBooks with physical purchases.
- Seeded catalogue prices and quantities are demonstration data, not market data.
- Hibernate `ddl-auto=update` is retained as a documented academic shortcut.
- Seller accounts, subscriptions, and AI recommendations are deferred.
