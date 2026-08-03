# akshara-bookstore
Akshara – a full-stack online bookstore built with React, Spring Boot, JWT and MySQL.

## Current features

- Public editorial catalogue, search, book details, ratings and reviews
- JWT Reader/Admin authentication
- Wishlist, cart, shipping checkout, orders and stock management
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
npm run build
```

Health check: `GET http://localhost:8080/api/public/health`.

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

Creation returns `201`. Invalid input returns `400`, access failures return
`401`/`403`, duplicate ISBN/SKU returns `409`, and provider unavailability returns
`503`. Zero matches return `200` with an empty `results` list.

Deletion removes the book, its editions, inventory, author/category links, cart
items, wishlist entries and reviews. Historical order items keep their immutable
title, ISBN, format and price snapshots; their optional live-edition link is
detached before catalogue deletion so order history remains readable.

## Deferred scope

Seller accounts, external price comparison, AI recommendations, real payment
gateways, subscriptions and copyrighted eBook distribution are outside the MVP.
