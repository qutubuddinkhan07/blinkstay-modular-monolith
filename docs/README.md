# BlinkStay documentation

BlinkStay is a stay-booking platform (a small Airbnb / Booking.com): guests search and book rooms, hosts manage listings and see earnings, admins moderate. Spring Boot modular monolith + React. Live at <https://blinkstay.netlify.app>.

## Read in this order

| # | Document | What it answers |
|---|---|---|
| 1 | [01-overview.md](01-overview.md) | What it is, tech stack, how it is deployed, screenshots |
| 2 | [02-requirements.md](02-requirements.md) | Roles, features and business rules |
| 3 | [03-architecture.md](03-architecture.md) | Modules, the rules between them, contracts in `common`, events, timers |
| 4 | [04-data-model.md](04-data-model.md) | Tables and how modules refer to each other |
| 5 | [05-api-reference.md](05-api-reference.md) | Endpoints by module |
| 6 | [06-security.md](06-security.md) | Login cookie, CSRF, JWT filter, URL rules, error codes, account lifecycle |
| 7 | [07-bookings-and-payments.md](07-bookings-and-payments.md) | Availability, locks, payments, refunds, idempotency |
| 8 | [08-caching.md](08-caching.md) | Which caches exist and how they stay correct |
| 9 | [09-scaling-and-microservices.md](09-scaling-and-microservices.md) | What breaks with more servers, and how a split would look |
| 10 | [10-testing-the-api.md](10-testing-the-api.md) | Testing with Postman or curl (cookie + CSRF) |

## Deep dives (design records, one per feature step)

| Document | Covers |
|---|---|
| [architecture/user-lifecycle-events.md](architecture/user-lifecycle-events.md) | Registration, host requests, block, delete, events |
| [architecture/notification-module.md](architecture/notification-module.md) | Bell and email pipeline, who gets what |
| [architecture/b1-b2-booking-and-payment.md](architecture/b1-b2-booking-and-payment.md) | Availability, booking creation, first payment flow |
| [architecture/b3-cancellation.md](architecture/b3-cancellation.md) | Cancellation rules, host and admin lists |
| [architecture/b5-payments-razorpay.md](architecture/b5-payments-razorpay.md) | Razorpay, refunds after commit, reconcile, admin queue |

Each deep dive starts with a **Status** box that says what changed after it was written. Their diagrams are Mermaid (GitHub renders them).

Other: [frontend/FRONTEND_ARCHITECTURE.md](frontend/FRONTEND_ARCHITECTURE.md) · [plans/nearby-listings.md](plans/nearby-listings.md) (not built)

## Diagrams

All diagrams are PlantUML sources in [`diagrams/`](diagrams/) with a rendered `.png` next to each. They share one style file, `diagrams/_style.iuml`.

| File | Diagram |
|---|---|
| `01-use-cases` | Use cases by role |
| `02-modules` | Modules and the contracts between them |
| `02b-event-bus` | Who publishes and who listens to each event |
| `03-block-host-sequence` | Admin blocks a host (one transaction) |
| `04-blocked-user-next-request` | A blocked user's next request |
| `05-listing-statuses` | Listing state machine |
| `06-microservices-target` | What a later split could look like |
| `07-deployment` | Netlify, Render, Aiven and the external services |
| `08-user-lifecycle` | Account states |
| `09-signup-with-otp` · `10-login-session-logout` · `11-forgot-password` | Auth flows |
| `12-request-security` | What every request passes through |
| `13-booking-statuses` · `14-create-booking-sequence` | Booking |
| `15-payment-statuses` · `16-payment-sequence` · `17-confirm-payment-decision` | Payment |
| `18-cancel-refund-sequence` · `19-refund-worker` | Cancellation and refunds |
| `20-notification-pipeline` | Notifications |
| `21-review-sequence` | Reviews and ratings |
| `22-data-model` | Tables by module |
| `23-earnings-ledger` | Earnings entries |
| `24-frontend` | Frontend structure |

**Re-render after editing** (from the repo root, needs Java and Graphviz, or the VS Code PlantUML extension):

```bash
java -jar plantuml.jar -tpng docs/diagrams/*.puml
```

## Not in this folder (on purpose)

- **Interview Q&A** and the **demo runbook** live in the Claude project, not in the repo.
- **Secrets** never go in docs. Environment variable names only.
