# BlinkStay B3: Cancellation, Refunds and Host/Admin Booking Lists

B3 builds on B1 (availability and booking creation) and B2 (payment and confirmation). It adds:

- cancellation by the guest, the host and the admin, with a refund policy
- a full refund through the same payment gateway used in B2
- a cancellation notification for the people involved
- booking lists for hosts and for admins

---

## Contents

1. [Rules](#1-rules)
2. [Use cases](#2-use-cases)
3. [Where B3 sits in the modules](#3-where-b3-sits-in-the-modules)
4. [Class diagram](#4-class-diagram)
5. [Booking status after B3](#5-booking-status-after-b3)
6. [Decision flow for a cancel](#6-decision-flow-for-a-cancel)
7. [Sequence: guest cancels a paid booking](#7-sequence-guest-cancels-a-paid-booking)
8. [Sequence: host or admin cancels](#8-sequence-host-or-admin-cancels)
9. [Sequence: cancel racing with a payment](#9-sequence-cancel-racing-with-a-payment)
10. [Lock order (why there is no deadlock)](#10-lock-order-why-there-is-no-deadlock)
11. [Sequence: notifications after a cancel](#11-sequence-notifications-after-a-cancel)
12. [Sequence: host and admin lists](#12-sequence-host-and-admin-lists)
13. [Endpoints](#13-endpoints)
14. [Files added and changed](#14-files-added-and-changed)
15. [Design decisions and limits](#15-design-decisions-and-limits)
16. [Test checklist](#16-test-checklist)

---

## 1. Rules

| Who | Unpaid booking (`PENDING_PAYMENT`) | Paid booking (`CONFIRMED`) |
|---|---|---|
| Guest | Can cancel any time. No money moves. | Full refund until 48 hours before check-in (measured from the start of the check-in day in the hotel time zone). After that the cancel is still allowed, but no refund is due. Not allowed once the check-in day has started. |
| Host | Cannot cancel | Always a full refund. A reason is required. Allowed until the stay ends. |
| Admin | Can cancel | Same as host. |

Other statuses (`CANCELLED`, `EXPIRED`, `COMPLETED`) return **409**.

A cancelled booking stops counting against availability right away, so the rooms are free again at once.

---

## 2. Use cases

```mermaid
flowchart LR
    G(["Guest"])
    H(["Host (HOTEL_MANAGER)"])
    A(["Admin"])
    N(["Notification module"])
    P(["Payment gateway"])

    subgraph B3["B3: cancellation and lists"]
        UC1["See cancellation preview"]
        UC2["Cancel own booking"]
        UC3["Cancel a booking of my listing"]
        UC4["Cancel any booking"]
        UC5["List bookings of my listings"]
        UC6["Search all bookings"]
        UC7["Refund the payment"]
        UC8["Notify the people involved"]
    end

    G --> UC1
    G --> UC2
    H --> UC3
    H --> UC5
    A --> UC4
    A --> UC6

    UC2 -.->|"includes, if refund is due"| UC7
    UC3 -.->|"includes, always"| UC7
    UC4 -.->|"includes, if paid"| UC7
    UC2 -.->|includes| UC8
    UC3 -.->|includes| UC8
    UC4 -.->|includes| UC8
    UC7 --> P
    UC8 --> N
```

---

## 3. Where B3 sits in the modules

Booking never imports payment. Payment never imports booking. They meet only through interfaces and events in `common`.

```mermaid
flowchart TB
    subgraph BOOKING["booking module"]
        BC["BookingController"]
        HBC["HostBookingController"]
        ABC["AdminBookingController"]
        BCS["BookingCancellationServiceImpl"]
        SBS["StaffBookingServiceImpl"]
        CP["CancellationPolicy"]
        BM["BookingMapper"]
        BR["BookingRepository"]
    end

    subgraph COMMON["common"]
        PR{{"PaymentRefunds (interface)"}}
        UDIR{{"UserDirectory (interface)"}}
        EV[["BookingCancelledEvent"]]
    end

    subgraph PAYMENT["payment module"]
        PRI["PaymentRefundsImpl"]
        PREPO["PaymentRepository"]
        GW["PaymentGateway (mock now, real in B5)"]
    end

    subgraph NOTIF["notification module"]
        NEL["NotificationEventListener"]
        NS["NotificationService"]
        INAPP["InAppNotificationService"]
    end

    subgraph AUTH["auth module"]
        UDI["UserDirectoryImpl"]
    end

    BC --> BCS
    HBC --> BCS
    HBC --> SBS
    ABC --> BCS
    ABC --> SBS
    BCS --> CP
    BCS --> BM
    BCS --> BR
    BCS --> PR
    BCS -. publishes .-> EV
    SBS --> BR
    SBS --> UDIR
    PRI -. implements .-> PR
    PRI --> PREPO
    PRI --> GW
    UDI -. implements .-> UDIR
    EV -. after commit, async .-> NEL
    NEL --> NS
    NEL --> INAPP
```

---

## 4. Class diagram

### 4.1 Cancellation core

```mermaid
classDiagram
    direction LR

    class BookingCancellationService {
        <<interface>>
        +preview(guestId, bookingId) CancellationPreviewDto
        +cancelByGuest(guestId, bookingId, reason) BookingResponseDto
        +cancelByHost(hostId, bookingId, reason) BookingResponseDto
        +cancelByAdmin(adminId, bookingId, reason) BookingResponseDto
    }

    class BookingCancellationServiceImpl {
        -BookingRepository bookingRepo
        -BookingRules rules
        -CancellationPolicy policy
        -BookingMapper mapper
        -PaymentRefunds payments
        -ApplicationEventPublisher publisher
        -cancel(bookingId, actorId, actor, reason) BookingResponseDto
        -mayCancel(booking, actorId, actor) boolean
    }

    class CancelActor {
        <<enumeration>>
        GUEST
        HOST
        ADMIN
    }

    class CancellationPolicy {
        -BookingProperties props
        -BookingRules rules
        +freeCancellationUntil(booking) Instant
        +guestCanCancel(booking) boolean
        +guestRefund(booking, now) BigDecimal
    }

    class PaymentRefunds {
        <<interface>>
        +lockPayment(paymentId) void
        +refundInFull(paymentId, reason) BigDecimal
    }

    class PaymentRefundsImpl {
        -PaymentRepository paymentRepo
        -PaymentGateway gateway
    }

    class PaymentGateway {
        <<interface>>
        +name() String
        +createOrder(receipt, amount, currency) ProviderOrder
        +refund(providerPaymentId, amount, reason) ProviderRefund
    }

    class BookingCancelledEvent {
        <<record>>
        UUID bookingId
        String reference
        UUID guestId
        UUID hostId
        String listingTitle
        LocalDate checkIn
        LocalDate checkOut
        String cancelledBy
        String reason
        BigDecimal refundAmount
        String currency
        boolean wasPaid
    }

    BookingCancellationService <|.. BookingCancellationServiceImpl
    BookingCancellationServiceImpl --> CancelActor
    BookingCancellationServiceImpl --> CancellationPolicy
    BookingCancellationServiceImpl --> PaymentRefunds
    BookingCancellationServiceImpl ..> BookingCancelledEvent : publishes
    PaymentRefunds <|.. PaymentRefundsImpl
    PaymentRefundsImpl --> PaymentGateway
```

### 4.2 Lists and DTOs

```mermaid
classDiagram
    direction LR

    class StaffBookingService {
        <<interface>>
        +hostBookings(hostId, filter) PageResult
        +adminBookings(q, guestId, hostId, filter) PageResult
    }

    class StaffBookingServiceImpl {
        -BookingRepository bookingRepo
        -UserDirectory userDirectory
        -BookingRules rules
        -toPage(page, admin) PageResult
        -pageable(filter) Pageable
    }

    class BookingFilter {
        <<record>>
        List statuses
        UUID listingId
        LocalDate from
        LocalDate to
        String sort
        String direction
        int page
        int size
    }

    class BookingSpecifications {
        +forGuest(guestId, statuses) Specification
        +forHost(hostId, statuses, listingId, from, to) Specification
        +forAdmin(q, guestId, hostId, statuses, listingId, from, to) Specification
    }

    class StaffBookingDto {
        UUID id
        String reference
        String listingTitle
        BookingStatus status
        BigDecimal totalAmount
        BigDecimal refundAmount
        String cancelledByRole
        String cancellationReason
        boolean cancellable
        String guestName
        String guestEmail
        String hostName
    }

    class BookingResponseDto {
        boolean cancellable
        Instant freeCancellationUntil
        String cancelledByRole
        BigDecimal refundAmount
    }

    class CancellationPreviewDto {
        UUID bookingId
        BookingStatus status
        boolean cancellable
        BigDecimal refundAmount
        Instant freeCancellationUntil
        String message
    }

    class CancelBookingDto {
        String reason
    }

    StaffBookingService <|.. StaffBookingServiceImpl
    StaffBookingServiceImpl --> BookingFilter
    StaffBookingServiceImpl --> BookingSpecifications
    StaffBookingServiceImpl ..> StaffBookingDto : returns
```

### 4.3 Controllers

```mermaid
classDiagram
    direction LR

    class BookingController {
        +GET cancellation-preview
        +POST cancel
    }
    class HostBookingController {
        <<PreAuthorize HOTEL_MANAGER>>
        +GET list
        +POST cancel
    }
    class AdminBookingController {
        <<PreAuthorize ADMIN>>
        +GET list
        +POST cancel
    }

    BookingController --> BookingCancellationService
    HostBookingController --> BookingCancellationService
    HostBookingController --> StaffBookingService
    AdminBookingController --> BookingCancellationService
    AdminBookingController --> StaffBookingService
```

---

## 5. Booking status after B3

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT : create booking (B1)
    PENDING_PAYMENT --> CONFIRMED : payment succeeded (B2)
    PENDING_PAYMENT --> EXPIRED : hold ran out (timer)
    EXPIRED --> CONFIRMED : late payment and rooms still free (B2)
    PENDING_PAYMENT --> CANCELLED : guest or admin cancels (B3, no money)
    CONFIRMED --> CANCELLED : guest, host or admin cancels (B3, refund by rule)
    CONFIRMED --> COMPLETED : stay finished (B4 timer)
    CANCELLED --> [*]
    EXPIRED --> [*]
    COMPLETED --> [*]
```

Payment status stays in step with it:

```mermaid
stateDiagram-v2
    [*] --> CREATED : start payment
    CREATED --> SUCCEEDED : provider success
    CREATED --> FAILED : provider failure or amount mismatch
    SUCCEEDED --> REFUNDED : duplicate or unavailable (B2), or cancellation (B3)
    REFUNDED --> [*]
    FAILED --> [*]
```

A guest cancel inside the no-refund period leaves the payment `SUCCEEDED`. The booking is `CANCELLED` with `refundAmount = 0`.

---

## 6. Decision flow for a cancel

```mermaid
flowchart TD
    S(["POST cancel"]) --> R{"Host or admin and reason empty?"}
    R -->|yes| E400["400 A reason is required"]
    R -->|no| L["Lock payment (if any), then lock booking"]
    L --> O{"Allowed to touch this booking? guest owns it, host owns the listing, admin any"}
    O -->|no| E404["404 Booking not found"]
    O -->|yes| C{"Payment changed between the two reads?"}
    C -->|yes| E409a["409 The booking just changed, try again"]
    C -->|no| ST{"Booking status"}
    ST -->|"CANCELLED, EXPIRED, COMPLETED"| E409b["409 cannot be cancelled"]
    ST -->|PENDING_PAYMENT| PP{"Actor is host?"}
    PP -->|yes| E409c["409 Only paid bookings can be cancelled by a host"]
    PP -->|no| NOREF["refund = null, nothing was paid"]
    ST -->|CONFIRMED| WHO{"Actor"}
    WHO -->|guest| STARTED{"Check-in day started?"}
    STARTED -->|yes| E409d["409 The stay has already started"]
    STARTED -->|no| WIN{"Now is before free-until?"}
    WIN -->|yes| FULL["refund = total"]
    WIN -->|no| ZERO["refund = 0"]
    WHO -->|"host or admin"| ENDED{"Stay already ended?"}
    ENDED -->|yes| E409e["409 This stay has already ended"]
    ENDED -->|no| FULL2["refund = total (always)"]
    FULL --> PAY
    FULL2 --> PAY
    PAY["Refund in full through PaymentRefunds"] --> DONE
    ZERO --> DONE
    NOREF --> DONE
    DONE["Set CANCELLED, cancelledAt, cancelledBy, reason, refundAmount. Save. Publish BookingCancelledEvent"] --> OK(["200 booking returned"])
```

---

## 7. Sequence: guest cancels a paid booking

```mermaid
sequenceDiagram
    autonumber
    actor G as Guest
    participant C as BookingController
    participant S as BookingCancellationServiceImpl
    participant BR as BookingRepository
    participant PR as PaymentRefundsImpl
    participant GW as PaymentGateway
    participant PDB as payments table
    participant BDB as bookings table
    participant BUS as Event bus

    G->>C: GET cancellation-preview
    C->>S: preview(guestId, bookingId)
    S-->>G: refundAmount, freeCancellationUntil, message

    G->>C: POST cancel (optional reason, CSRF header)
    C->>S: cancelByGuest(guestId, bookingId, reason)
    activate S
    Note over S: one transaction starts
    S->>BR: findPaymentIdById (plain value, no entity loaded)
    BR-->>S: paymentId
    S->>PR: lockPayment(paymentId)
    PR->>PDB: select for update (payment locked)
    S->>BR: findByIdForUpdate(bookingId)
    BR->>BDB: select for update (booking locked)
    S->>S: check owner, status, paymentId unchanged
    S->>S: refund = policy.guestRefund (total or 0)
    alt refund is more than 0
        S->>PR: refundInFull(paymentId, reason)
        PR->>GW: refund(providerPaymentId, amount, reason)
        GW-->>PR: ProviderRefund
        PR->>PDB: status REFUNDED, refundedAmount, refundedAt
        PR-->>S: refunded amount
    end
    S->>BDB: status CANCELLED, cancelledAt, cancelledBy, reason, refundAmount
    S->>BUS: publish BookingCancelledEvent
    deactivate S
    Note over S,BDB: COMMIT. Booking and payment change together or not at all.
    C-->>G: 200 booking with CANCELLED and refundAmount
```

If the gateway call throws, the whole transaction rolls back and the booking stays `CONFIRMED`.

---

## 8. Sequence: host or admin cancels

```mermaid
sequenceDiagram
    autonumber
    actor H as Host or Admin
    participant C as HostBookingController or AdminBookingController
    participant S as BookingCancellationServiceImpl
    participant PR as PaymentRefunds
    participant BUS as Event bus

    H->>C: POST cancel with reason (required)
    Note over C: PreAuthorize checks HOTEL_MANAGER or ADMIN
    C->>S: cancelByHost or cancelByAdmin
    S->>S: reason blank gives 400
    S->>PR: lockPayment, then lock booking
    S->>S: host must own the booking, admin may cancel any
    S->>S: stay already ended gives 409
    S->>PR: refundInFull (always the full amount)
    S->>BUS: BookingCancelledEvent (cancelledBy = HOST or ADMIN)
    C-->>H: 200, the guest was refunded in full
```

---

## 9. Sequence: cancel racing with a payment

A guest starts a payment, and then cancels before it completes. The payment arrives afterwards.

```mermaid
sequenceDiagram
    autonumber
    actor G as Guest
    participant S as Cancel service
    participant B as Booking row
    participant PP as PaymentEventProcessor
    participant BP as BookingPaymentsImpl
    participant PAY as Payment row
    participant GW as Gateway

    G->>S: cancel (booking is PENDING_PAYMENT, no paymentId yet)
    S->>B: lock booking
    S->>B: status CANCELLED
    Note over S,B: COMMIT
    G->>PP: provider sends PAYMENT_SUCCEEDED
    PP->>PAY: lock payment
    PP->>BP: confirmPayment(bookingId, paymentId, amount)
    BP->>B: lock booking, sees CANCELLED
    BP-->>PP: UNAVAILABLE
    PP->>GW: refund
    PP->>PAY: status REFUNDED
    Note over PP: PaymentRefundedEvent tells the guest why
```

No code was added for this case in B3. It works because B2 already returns `UNAVAILABLE` for a cancelled booking.

---

## 10. Lock order (why there is no deadlock)

Two code paths touch both rows. They must always lock them in the same order.

```mermaid
flowchart LR
    subgraph P1["Payment event path (B2)"]
        A1["1. lock payment"] --> A2["2. lock booking"]
    end
    subgraph P2["Cancel path (B3)"]
        B1["1. lock payment (if the booking has one)"] --> B2["2. lock booking"]
    end
    P1 --- NOTE["Same order on both paths, so neither can wait on a row the other already holds"]
    P2 --- NOTE
```

How the cancel service gets there:

1. `findPaymentIdById` is a plain scalar query. It does not load the entity, so the later locked read is not served from a stale cache.
2. `lockPayment` locks the payment row first.
3. `findByIdForUpdate` locks the booking row.
4. The service checks that `paymentId` is still the same. If a payment was confirmed in between, it returns 409 and asks the caller to retry.

When the booking has no payment yet (unpaid hold), there is nothing to lock first. A concurrent payment then waits on the booking row, and sees `CANCELLED` once the cancel commits (section 9).

---

## 11. Sequence: notifications after a cancel

```mermaid
sequenceDiagram
    autonumber
    participant BUS as Event bus
    participant NL as NotificationEventListener
    participant UD as UserDirectory
    participant IA as InAppNotificationService
    participant NS as NotificationService
    participant EM as EmailMessageBuilderUtil
    participant EL as EmailEventListeners

    BUS-->>NL: BookingCancelledEvent (after commit, background thread)
    NL->>UD: findContact(guest)
    opt paid, or cancelled by host or admin
        NL->>IA: create BOOKING_CANCELLED for the guest
        NL->>NS: sendBookingCancelledToGuest
        NS->>EM: bookingCancelledGuestMessage
        NS->>BUS: SimpleMessageEvent
        BUS-->>EL: send the email
    end
    opt paid, and not cancelled by the host
        NL->>UD: findContact(host)
        NL->>IA: create BOOKING_CANCELLED for the host
        NL->>NS: sendBookingCancelledToHost
    end
```

Who is told:

| Cancelled by | Booking was | Guest is told | Host is told |
|---|---|---|---|
| Guest | Unpaid | No | No |
| Guest | Paid | Yes (with the refund result) | Yes |
| Host | Paid | Yes (with reason and refund) | No (they did it) |
| Admin | Unpaid | Yes | No |
| Admin | Paid | Yes (with reason and refund) | Yes (with reason) |

`PaymentRefundedEvent` is **not** published for a cancellation refund. The cancellation message already carries the refund details, so the guest does not get two messages.

---

## 12. Sequence: host and admin lists

```mermaid
sequenceDiagram
    autonumber
    actor U as Host or Admin
    participant C as HostBookingController or AdminBookingController
    participant S as StaffBookingServiceImpl
    participant SP as BookingSpecifications
    participant DB as bookings table
    participant UD as UserDirectory

    U->>C: GET list with status, listingId, from, to, q, sort, page
    C->>S: hostBookings or adminBookings
    S->>SP: forHost or forAdmin
    Note over SP: host sees only bookings that were paid at some point (confirmedAt is set)
    S->>DB: findAll(spec, page, size up to 50)
    DB-->>S: page of bookings
    S->>UD: findContact for each distinct guest (and host for admin)
    UD-->>S: usernames and emails
    S-->>C: StaffBookingDto rows
    Note over S: host sees guestName only. Admin also sees guestEmail and hostName.
    C-->>U: 200 page of rows
```

---

## 13. Endpoints

| Method and path | Who | Purpose |
|---|---|---|
| `GET /api/v5/bookings/{id}/cancellation-preview` | Guest (owner) | What happens if I cancel now (refund, message) |
| `POST /api/v5/bookings/{id}/cancel` | Guest (owner) | Cancel. Body optional: `{ "reason" }` |
| `GET /api/v5/host/bookings` | `HOTEL_MANAGER` | Bookings of my listings. Filters: `status`, `listingId`, `from`, `to`, `sort`, `direction`, `page`, `size` |
| `POST /api/v5/host/bookings/{id}/cancel` | `HOTEL_MANAGER` | Cancel with full refund. Body: `{ "reason" }` (required) |
| `GET /api/v5/admin/bookings` | `ADMIN` | All bookings. Extra filters: `q` (reference), `guestId`, `hostId` |
| `POST /api/v5/admin/bookings/{id}/cancel` | `ADMIN` | Cancel any booking. Body: `{ "reason" }` (required) |

All POST calls need the CSRF header (`X-XSRF-TOKEN`). `from` and `to` filter on the check-in date (`YYYY-MM-DD`).

---

## 14. Files added and changed

**New**

| File | Module |
|---|---|
| `PaymentRefunds` | common/payment |
| `BookingCancelledEvent` | common/events |
| `PaymentRefundsImpl` | payment/serviceImpl |
| `CancellationPolicy` | booking/service |
| `CancelActor` | booking/enums |
| `BookingCancellationService` and `Impl` | booking/service, booking/serviceImpl |
| `StaffBookingService` and `Impl`, `BookingFilter` | booking |
| `CancelBookingDto`, `CancellationPreviewDto`, `StaffBookingDto` | booking/dto |
| `HostBookingController`, `AdminBookingController` | booking/controller |

**Changed**

| File | Change |
|---|---|
| `PaymentRepository` | `findByIdForUpdate` |
| `BookingRepository` | `findPaymentIdById` |
| `BookingSpecifications` | `forHost`, `forAdmin`, shared filters |
| `BookingResponseDto` | `cancellable`, `freeCancellationUntil`, `cancelledByRole` |
| `BookingMapper` | injects `CancellationPolicy`, fills the new fields |
| `BookingController` | preview and cancel endpoints |
| `NotificationType` | `BOOKING_CANCELLED` |
| `EmailMessageBuilderUtil`, `NotificationService`, `NotificationServiceImpl` | two cancellation emails |
| `NotificationEventListener` | `on(BookingCancelledEvent)` |

If the `notifications` table still has its old check constraint on `type`, run `ALTER TABLE notifications DROP CHECK notifications_chk_1;` and put `@JdbcTypeCode(SqlTypes.VARCHAR)` on the field. Otherwise `BOOKING_CANCELLED` is rejected in the same way `PAYMENT_REFUNDED` was.

---

## 15. Design decisions and limits

1. **Cancellation lives in its own service.** `BookingServiceImpl` already handles create, read and expiry. Cancellation touches payments and notifications, so it gets its own class.
2. **Booking calls payment through `PaymentRefunds` in `common`.** Booking never imports payment classes, and payment never imports booking classes. This is the same idea as `BookingPayments` in B2, in the opposite direction.
3. **One cancel method for all three actors.** The differences are small: who may touch the booking, which statuses are allowed, and how the refund is computed. They live in one method, so a bug fix applies to all three.
4. **Full refund or none.** There is no partial refund, so the interface has `refundInFull` only. Partial refunds would be a new rule, not a rewrite.
5. **The refund runs inside the database transaction.** That is fine for the mock gateway. A real provider call should happen after commit, so B5 will move it and add a retry or an outbox.
6. **48 hours is measured from the start of the check-in day.** A fixed check-in time such as 14:00 can be added later by changing `freeCancellationUntil` only.
7. **`cancelledByRole` is derived, not stored.** It is computed by comparing `cancelledBy` with the guest and host IDs. Anything else is an admin. No new column is needed.
8. **Hosts never see unpaid holds.** The host list filters on `confirmedAt is not null`, so unpaid, expired and never-paid bookings do not appear.
9. **Hosts see the guest name only.** Email stays admin-only, as decided in B2.
10. **Known gap:** `COMPLETED` is still not set by anything. A past confirmed stay stays `CONFIRMED` until B4. Host and admin cancel return 409 for it ("stay has already ended"), so no harm is done.
11. **Known gap:** blocking or deleting a user does not cancel their bookings yet.

---

## 16. Test checklist

1. **Preview:** more than 48 h ahead gives a full refund. Within 48 h gives refund 0 and the no-refund message. A pending booking says it costs nothing.
2. **Guest cancels unpaid:** `CANCELLED`, `refundAmount` null, rooms free again, no email or bell row.
3. **Guest cancels paid, in time:** `CANCELLED`, full `refundAmount`, payment `REFUNDED`, guest and host both notified, no separate "payment refunded" message.
4. **Guest cancels paid, too late:** `refundAmount` 0, payment stays `SUCCEEDED`, message says no refund was due.
5. **Stay already started:** guest gets 409.
6. **Cancel twice, or an expired or cancelled booking:** 409. Another user's booking: 404.
7. **Host cancels:** reason required (empty gives 400), full refund, `cancelledByRole: HOST`, guest notified with the reason, host not notified. A pending booking gives 409. Another host's booking gives 404.
8. **Admin cancels:** works for unpaid and paid. Guest and host are both notified when paid. A non-admin gets 403.
9. **Host list:** only paid-at-some-point bookings of this host, `guestEmail` null, filters work.
10. **Admin list:** `q=BS-` search, `guestId`, `hostId`, `status` filters, `guestEmail` and `hostName` present.
11. **Cancel vs payment race:** start a payment, cancel the booking, complete the payment with `SUCCESS`. The booking stays `CANCELLED` and the payment becomes `REFUNDED`.
12. **Double cancel race:** two cancels of one paid booking at the same moment. One succeeds, the other gets 409, and the payment is refunded exactly once.
13. **Rollback:** force the gateway refund to throw. The booking must stay `CONFIRMED` and the payment `SUCCEEDED`.
