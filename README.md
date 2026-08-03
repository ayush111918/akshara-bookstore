# akshara-bookstore
Akshara – a full-stack online bookstore built with React, Spring Boot, JWT and MySQL.

## Current features

- Public editorial catalogue, search, book details, ratings and reviews
- JWT Reader/Admin authentication
- Wishlist, cart, shipping checkout, orders and stock management
- My Books for delivered purchases and private PDF/EPUB uploads
- Reading Journey with shelves, page progress, dates, private notes, quotations,
  bookmarks, reading streaks, yearly goals and a completion-to-review path
- Reader account controls with password-confirmed deletion/anonymization
- Administrator security audit log for registration, login and account-deletion events
- Admin-only Open Library metadata search and transactional catalogue import
- Admin-only manual entry for rare or missing books
- Admin catalogue dashboard for restocking, pricing, availability and curation
- ISBN and SKU duplicate protection

External services supply descriptive metadata only. Akshara owns price, stock,
availability, SKU, reviews, wishlist, cart, users and orders.

## Local configuration

Run MySQL 8.4 with database `akshara`, then provide secrets through the process
environment:

```bash
export DB_URL='jdbc:mysql://localhost:3306/akshara'
export DB_USERNAME='akshara_app'
export DB_PASSWORD='<local-password>'
export JWT_SECRET='<long-random-local-secret>'
export BOOK_IMPORT_CONTACT='your-contact@example.com'
```

`BOOK_IMPORT_CONTACT` is only Akshara's Open Library contact identifier. It is not
an administrator login. No Google Books key is required for the current flow.

## Run and verify

```bash
cd backend
./mvnw spring-boot:run
```

```bash
cd frontend
npm ci
npm run dev
```

Verification:

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm run lint
npm test
npm run build
```

Health check: `GET http://localhost:8080/api/public/health`.

The frontend tests use Vitest, React Testing Library and jsdom. They cover API
error/session utilities, authentication form behavior and protected routing.
Backend tests cover business services, controllers, validation, authentication,
account deletion and Reading Journey ownership rules.

## Authentication lifecycle

Registration creates a Reader immediately; email verification is intentionally
outside this academic project's scope. Passwords are BCrypt hashes and are never
returned by the API. The login UI supports password visibility, preserves the
requested protected destination and clearly reports expired sessions. A global
API interceptor clears invalid sessions after a `401` response.

Readers can open `/account` and delete their account after confirming both their
current password and the word `DELETE`. Akshara removes private uploads and their
files, Reading Journey data, private notes, goals, cart and wishlist; then it
disables the account, replaces the password, and anonymizes the reader's name and
email. Historical orders and public contributions are retained so shared and
business records remain internally consistent.

Admins can inspect paginated security events at `/admin/audit-logs`. Audit rows
store an actor snapshot rather than a user foreign key, so registration, login
success/failure and account deletion remain traceable after anonymization.

## Administrator account

Public registration always creates a `READER`. For local development, register a
normal account and deliberately promote it in MySQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';
```

Log out and back in afterward so the new JWT contains the Admin role. Never commit
a default administrator password.

## Hybrid catalogue workflow

1. Log in as Admin and open `/admin/books/import`.
2. Search Open Library by title, author or ISBN.
3. Select the correct edition and review the normalized metadata.
4. Enter Akshara format, SKU, price, stock, availability and active state.
5. Import and follow the success link to `/books/{bookId}`.
6. Use Manual Entry when an external record is unsuitable or missing.

Akshara never contacts Open Library during public catalogue requests or startup.

## Catalogue operations

Open `/admin/books` as an administrator to manage existing catalogue records. The
dashboard supports searching by title, author, ISBN or SKU; reviewing stock
totals; adding stock; changing the exact stock quantity and price; changing
availability; activating or pausing an edition; and curating the featured shelf.
Unreferenced books can also be permanently deleted after confirmation.

Quick restocking adds units under a database inventory lock and does not replace
the current quantity. Adding stock to an `OUT_OF_STOCK` edition automatically
returns it to `IN_STOCK`; an explicitly `UNAVAILABLE` edition stays unavailable.
Setting total stock to zero while requesting `IN_STOCK` is normalized to
`OUT_OF_STOCK`.

## Order operations

1. Sign in with an `ADMIN` account and open `/admin/orders`.
2. Select a newly placed order and choose **Confirm order**.
3. Advance fulfilment in order: `CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED`.
4. Orders may be cancelled before shipping; cancellation restores reserved inventory.
5. Marking a cash-on-delivery order delivered marks its payment as paid.

Readers see these changes in Order History and My Books. Status transitions are
validated by Spring Boot and cannot be skipped or reversed.

## Reading Journey

Readers open `/my-books` and choose **Start reading** on a delivered purchase or
**Track reading** on a private upload. `/reading-journey` then provides four
personal shelves: Want to Read, Reading, Paused and Completed. Progress updates
record pages and activity dates, which drive current/longest streaks and yearly
statistics. Notes, quotations and bookmarks are private to their owner. A
completed catalogue book links back to its public page so the reader can publish
a review and continue into the community discussion.

Reading records use title, author and cover snapshots. Removing a catalogue item
or personal file therefore does not silently erase the reader's progress. Removing
the Reading Journey entry itself also removes its private annotations and activity
history after confirmation.

## Reader API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/reading-journey` | Load entries, statistics and the current yearly goal |
| `POST` | `/api/reading-journey` | Track a delivered purchase or owned private upload |
| `PUT` | `/api/reading-journey/{id}/progress` | Change shelf, page progress and total pages |
| `DELETE` | `/api/reading-journey/{id}` | Remove a journey and its private data |
| `GET` | `/api/reading-journey/{id}/annotations` | List the reader's private notebook items |
| `POST` | `/api/reading-journey/{id}/annotations` | Add a note, quotation or bookmark |
| `PUT` | `/api/reading-journey/annotations/{id}` | Edit an owned notebook item |
| `DELETE` | `/api/reading-journey/annotations/{id}` | Delete an owned notebook item |
| `PUT` | `/api/reading-journey/goal/{year}` | Set the yearly completed-book target |
| `GET` | `/api/users/me` | Return the current enabled account |
| `DELETE` | `/api/users/me` | Password-confirm and anonymize/disable the account |

## Admin API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/admin/book-import/search?query=...&source=OPEN_LIBRARY&page=1&size=10` | Search normalized metadata |
| `POST` | `/api/admin/book-import` | Import reviewed metadata and store data |
| `POST` | `/api/admin/books` | Create a manual catalogue book |
| `GET` | `/api/admin/books` | List catalogue records |
| `PUT` | `/api/admin/books/{id}` | Update a catalogue record |
| `PATCH` | `/api/admin/books/{id}/featured` | Curate featured status |
| `PUT` | `/api/admin/editions/{editionId}/inventory` | Update price, total stock, availability and active state |
| `POST` | `/api/admin/editions/{editionId}/inventory/restock` | Add units safely to current stock |
| `DELETE` | `/api/admin/books/{id}` | Delete a catalogue record |
| `GET` | `/api/admin/orders` | List customer orders for fulfilment |
| `GET` | `/api/admin/orders/{id}` | View customer, delivery, payment and book details |
| `PATCH` | `/api/admin/orders/{id}/status` | Advance or cancel an order using validated transitions |
| `GET` | `/api/admin/audit-logs?page=0&size=25` | Inspect paginated security activity |

Creation returns `201`. Invalid input returns `400`, access failures return
`401`/`403`, duplicate ISBN/SKU returns `409`, and provider unavailability returns
`503`. Zero matches return `200` with an empty `results` list.

Deletion removes the book, its editions, inventory, author/category links, cart
items, wishlist entries and reviews. Historical order items keep their immutable
title, ISBN, format and price snapshots; their optional live-edition link is
detached before catalogue deletion so order history remains readable.

## Container deployment

The repository includes production-oriented Dockerfiles, an Nginx single-page
application configuration and Docker Compose for React, Spring Boot and MySQL.
Hibernate `ddl-auto=update` is deliberately retained in both local and production
profiles for this project.

```bash
cp .env.example .env
# Replace every placeholder secret in .env
docker compose up --build
```

Open `http://localhost:8088` (or the `AKSHARA_PORT` selected in `.env`). MySQL,
private uploads and backend logs use named persistent volumes. Nginx serves the
React build, falls back to `index.html` for client routes, proxies `/api`, passes
forwarded client headers and accepts the same 26 MB upload limit as Spring Boot.

Do not commit `.env`; it is ignored by Git. For a public internet deployment,
place an HTTPS reverse proxy or managed TLS service in front of this stack.

## Digital reader scope

Private PDFs currently open inline in the browser and EPUBs download. The next
safe increment is an embedded PDF.js reader that reports the visible page to the
Reading Journey API. A complete EPUB reader is a separate, larger feature because
EPUB content reflows: progress must use chapters/locations rather than stable PDF
page numbers. It should not be combined with the first PDF-reader increment.

## Deferred scope

Email verification, password-reset email delivery, seller accounts, external
price comparison, AI recommendations, real payment gateways, subscriptions and
copyrighted eBook distribution are outside the MVP.
