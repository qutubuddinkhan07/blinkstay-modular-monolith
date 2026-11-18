# 5. API reference

Base URL: `http://localhost:8080` locally, `/api` through the Netlify proxy in production. Every response is `{ success, message, data }`.

**Conventions**

- Login is a cookie (`BLINKSTAY_TOKEN`). Every `POST`, `PUT`, `PATCH`, `DELETE` needs the `X-XSRF-TOKEN` header (copy of the `XSRF-TOKEN` cookie), except the public auth calls and the webhook.
- Version prefixes are by area, not by API version: `v1` auth, `v2` users / admin users / notifications, `v3` listings / rooms / favorites, `v5` bookings / payments / reviews / dashboards.
- Lists take `page` and `size` (size capped at 50 on most lists).
- **†** = the exact path was not in the design notes. Check the controller, or Swagger UI in the dev profile (`/swagger-ui.html`), and remove the mark.

## Auth (`/api/v1/auth`, `AuthController`, `PasswordResetController`, `CsrfConstroller`)

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/api/v1/auth/csrf` | public | returns the CSRF token, sets `XSRF-TOKEN` |
| POST | `/api/v1/auth/login` | public | `{ username: email, password }` → sets the cookie, `data: null` |
| POST | `/api/v1/auth/logout` | public | revokes the token, clears the cookie (works with a dead token) |
| POST | `/api/v1/auth/forgot-password` | public | `{ username, email }` → always 200 |
| POST | `/api/v1/auth/reset-password` | public | `{ email, otp, newPassword }` |

## User (`/api/v2/user`, `UserController`)

| Method | Path | Who | Notes |
|---|---|---|---|
| POST | `/api/v2/user/register-init` | public | details + optional photo (multipart) → OTP emailed; 429 inside 60 s |
| POST | `/api/v2/user/verify-otp` | public | `{ email, otp }` → 201, account created |
| GET / PUT / DELETE | `/api/v2/user/me` † | logged in | profile, update (any fields sent), delete account |
| photo, change password | † | logged in | change password re-issues this device's cookie |

## Host requests and host profile (`HostRequestController`, `AdminHostRequestController`, `HostProfileController`)

| Method | Path | Who | Notes |
|---|---|---|---|
| POST | `/api/v2/host-requests` | user | one pending at a time (409) |
| GET | `/api/v2/host-requests/me` | user | my latest request and status |
| DELETE | `/api/v2/host-requests/me` | user | cancel a pending request |
| GET | `/api/v2/admin/host-requests?status=` | admin | queue |
| GET | pending count † | admin | navbar badge |
| POST | `/api/v2/admin/host-requests/{id}/approve` | admin | role + profile |
| POST | `/api/v2/admin/host-requests/{id}/reject` | admin | `{ reason }` required |
| GET / PUT | host business profile (`hosts/me`) † | host | |

## Admin users and caches (`AdminUserController`, `CacheAdminController`)

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/api/v2/admin/users?q=&role=&blocked=&deleted=&page=&size=` | admin | `AdminUserDto` rows |
| POST | `/api/v2/admin/users/{id}/block` | admin | `{ reason }` (max 500); 409 if active bookings |
| POST | `/api/v2/admin/users/{id}/unblock` | admin | |
| POST | `/api/v2/admin/users/{id}/restore` | admin | deleted account back |
| GET | cache list † | admin | |
| DELETE | `/api/v2/admin/cache/{cacheName}` | admin | |

## Notifications (`/api/v2/notifications`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v2/notifications?page=&size=&unreadOnly=` | own rows only |
| GET | `/api/v2/notifications/unread-count` | number |
| PATCH | `/api/v2/notifications/{id}/read` | idempotent |
| PATCH | `/api/v2/notifications/read-all` | rows updated |
| DELETE | `/api/v2/notifications/{id}` | 404 if not yours |
| DELETE | `/api/v2/notifications?scope=read\|all` | default `read` |

## Listings, rooms, favorites (`/api/v3`)

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/api/v3/listings/all?page=&size=&sortBy=&direction=&location=&name=&category=` † (query names) | public | published feed, cached |
| GET | `/api/v3/listings/{id}` | public | 404 unless published, owner or admin |
| GET | `/api/v3/listings/my-listings` | host | |
| POST | `/api/v3/listings/create` | host | 201 |
| PUT / DELETE, images | `/api/v3/listings/{id}...` † | host (owner) | delete refused with active bookings |
| POST | `/api/v3/listings/{id}/publish` · `/pause` · `/resume` | host (owner) | publish/resume need a room (409) |
| GET | `/api/v3/listings/manager/{managerId}` | admin | a host's listings |
| POST | `/api/v3/admin/listings/{id}/suspend` | admin | `{ reason }`; 409 with active bookings |
| POST | `/api/v3/admin/listings/{id}/unsuspend` | admin | 409 if the owner is blocked |
| CRUD | rooms of a listing (`RoomController`) † | host (owner) | edits take the room lock |
| GET / POST / DELETE | `/api/v3/favorites...` † | logged in | |

## Availability and bookings (`/api/v5`)

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/api/v5/availability/...` † | public | free rooms and price per room type for dates; also "free tonight" |
| POST | `/api/v5/bookings` | guest | 201, `PENDING_PAYMENT`, `holdExpiresAt` |
| GET | `/api/v5/bookings/{id}` | guest (owner) | |
| GET | my bookings † | guest | `PageResult` |
| GET | `/api/v5/bookings/{id}/cancellation-preview` | guest | refund amount, deadline, message |
| POST | `/api/v5/bookings/{id}/cancel` | guest | `{ reason? }` |
| GET | `/api/v5/host/bookings?status=&listingId=&from=&to=&sort=&direction=&page=&size=` | host | paid bookings only, guest name only |
| POST | `/api/v5/host/bookings/{id}/cancel` | host | `{ reason }` required, full refund |
| GET | `/api/v5/host/calendar/{listingId}?month=YYYY-MM` | host (owner) | per room type per day |
| GET | `/api/v5/admin/bookings?q=&guestId=&hostId=&...` | admin | search by reference |
| POST | `/api/v5/admin/bookings/{id}/cancel` | admin | `{ reason }` required |

## Payments (`/api/v5`)

| Method | Path | Who | Exists when |
|---|---|---|---|
| POST | `/api/v5/bookings/{bookingId}/pay` | guest | always; reuses an open order |
| POST | `/api/v5/payments/razorpay/{orderId}/verify` | guest | `provider=razorpay` |
| POST | `/api/v5/payments/webhook/razorpay` | Razorpay (signed, no CSRF) | `provider=razorpay` |
| POST | `/api/v5/payments/mock/{orderId}/complete` | guest, `{ outcome: SUCCESS\|FAILURE }` | `provider=mock` |
| GET | `/api/v5/admin/payments/attention` | admin | always |
| POST | `/api/v5/admin/payments/{paymentId}/retry-refund` | admin | 409 unless `REFUND_FAILED` |
| POST | `/api/v5/admin/payments/{paymentId}/resolve` | admin | clears a review note |

## Reviews (`/api/v5`)

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/api/v5/listings/{listingId}/reviews?page=&size=` | public | newest first, includes average and count |
| POST | `/api/v5/bookings/{bookingId}/review` | guest of a `COMPLETED` booking | once (409) |
| PUT / DELETE | `/api/v5/reviews/{reviewId}` | review author | rating recalculated |
| PUT / DELETE | `/api/v5/reviews/{reviewId}/reply` | host of the listing | one reply |
| GET | host reviews list † | host | |
| DELETE | `/api/v5/admin/reviews/{reviewId}` | admin | |

## Dashboards (`HostDashboardController`, `AdminDashboardController`)

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | host earnings: summary, monthly, per listing, ledger † | host | range by check-in date, default 1 year back and ahead, max ~800 days |
| GET | admin overview † | admin | range by confirm date (`soldOn`) |

## Status codes

200 OK · 201 created · 400 invalid request · 401 not logged in (with `code`) · 403 wrong role or CSRF · 404 missing or not yours · 405 wrong method · 409 conflicts with current state · 429 slow down (OTP) · 500 unexpected (and the webhook on purpose, so Razorpay retries) · 502 payment provider failed.
