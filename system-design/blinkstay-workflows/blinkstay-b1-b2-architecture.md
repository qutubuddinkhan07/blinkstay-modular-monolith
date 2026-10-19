# BlinkStay Architecture Guide: Modules, Booking (B1), Payment (B2) and Notifications

> **How to view the diagrams:** open this file on GitHub, in VS Code with a Mermaid preview extension, or in Obsidian, or paste any single `mermaid` block into https://mermaid.live. If a diagram does not render, copy the error text and ask for a fix.

## Contents

1. [The folder pattern](#1-the-folder-pattern)
2. [B1: availability and booking](#2-b1-availability-and-booking)
3. [B2: payment](#3-b2-payment)
4. [The notification module](#4-the-notification-module)
5. [Reading order for the code](#5-reading-order-for-the-code)

---

## 1. The folder pattern

It is called **package by module, with layers inside each module**. The top-level folders (`auth`, `listing`, `room`, `booking`, `payment`, `notification`, `seed`, `common`) are the business modules. Inside each one the same layer folders repeat.

### Layer folders

| Folder | What lives there |
|---|---|
| `controller` | HTTP endpoints only: read the request, call a service, return the response |
| `dto` | the shapes of requests and responses, never database objects |
| `entities` | the classes that map to database tables |
| `enums` | fixed lists of values (statuses, roles) |
| `mapper` | converts entities to DTOs |
| `repository` | database queries |
| `service` | **interfaces**: what the module offers to everyone else |
| `serviceImpl` | the real logic behind those interfaces |
| `eventListeners`, `config`, `scheduler`, `util` | extras for that module |

The `service` and `serviceImpl` split matters. Another module should depend on the interface only, so the logic behind it can change freely.

```mermaid
flowchart TB
  subgraph M["One module, for example booking"]
    direction TB
    C["controller<br/>HTTP endpoints"]
    D["dto<br/>request and response shapes"]
    S["service<br/>interfaces: what the module offers"]
    SI["serviceImpl<br/>the real logic"]
    MP["mapper<br/>entity to dto"]
    R["repository<br/>database queries"]
    E["entities<br/>database tables"]
    L["eventListeners<br/>react to events from other modules"]
    C --> S
    C --> D
    S -. implemented by .-> SI
    SI --> R
    SI --> MP
    R --> E
    L --> SI
  end
```

### The rules that make it a modular monolith

1. Inside a module, calls go downward: controller, service, repository.
2. A module never touches another module's repositories or entities.
3. Modules talk through **service interfaces**, or through `common`, which holds shared events, small interfaces and shared DTOs.
4. `common` depends on nothing. Seed may depend on others, but nothing depends on seed.

```mermaid
flowchart LR
  common["common<br/>events, interfaces, shared dto"]
  auth["auth<br/>implements UserDirectory and HostDirectory"]
  listing["listing"]
  room["room"]
  booking["booking<br/>implements BookingGuard and BookingPayments"]
  payment["payment"]
  notification["notification"]
  seed["seed"]

  auth & listing & room & booking & payment & notification --> common
  booking --> listing
  booking --> room
  listing --> room
  room -. cycle to fix later .-> listing
  auth --> notification
  seed --> auth
  seed --> listing
  seed --> room
```

Payment and booking never import each other. They meet only at `BookingPayments`, which lives in `common`. The dotted arrow is the listing and room cycle that should be untangled later.

---

## 2. B1: availability and booking

### Design decisions

- **No "rooms left" counter.** Free rooms on a night equal `totalRooms` minus the rooms booked that night. A stay is bookable if its **busiest night** still has enough rooms.
- **Check-out is exclusive,** so a stay ending on the 6th and one starting on the 6th never conflict.
- **A 15-minute hold.** A new booking is `PENDING_PAYMENT` and counts against availability only while its hold has not expired. Queries ignore expired holds immediately, and a timer relabels them as `EXPIRED` once a minute.
- **A database lock on the room row** stops two guests booking the last room at the same moment.
- **Snapshots.** The booking stores the price and the listing's title, location and image, so later edits or deletions do not change it.

### Booking statuses

```mermaid
stateDiagram-v2
  [*] --> PENDING_PAYMENT: guest books and rooms are held
  PENDING_PAYMENT --> CONFIRMED: payment succeeds
  PENDING_PAYMENT --> EXPIRED: hold runs out
  PENDING_PAYMENT --> CANCELLED: cancelled
  EXPIRED --> CONFIRMED: late payment and rooms still free
  CONFIRMED --> CANCELLED: cancelled in B3
  CONFIRMED --> COMPLETED: after check-out in B4
  EXPIRED --> [*]
  CANCELLED --> [*]
  COMPLETED --> [*]
```

### Availability check (public, no login)

```mermaid
sequenceDiagram
  autonumber
  actor Visitor
  participant AC as AvailabilityController
  participant AVS as AvailabilityServiceImpl
  participant BR as BookingRules
  participant LS as ListingService
  participant BRP as BookingRepository
  participant RS as RoomService

  Visitor->>AC: GET availability for listing, dates, rooms, guests
  AC->>AVS: check(listingId, dates, rooms, guests)
  AVS->>BR: validateStay (dates, 30 nights max, rooms, guests)
  BR-->>AVS: number of nights, or a 400 error
  AVS->>LS: getBookingInfo(listingId)
  LS-->>AVS: published or not (404 if not)
  AVS->>BRP: active bookings overlapping these dates
  BRP-->>AVS: bookings
  AVS->>RS: getRoomsByListingId(listingId)
  RS-->>AVS: room types with price, totalRooms, maxGuests
  Note over AVS: for each room type, free rooms = totalRooms minus the busiest night in the range
  AVS-->>AC: AvailabilityResponse
  AC-->>Visitor: freeRooms, totalPrice, bookable and a reason when not bookable
```

### Creating a booking (the part with the lock)

```mermaid
sequenceDiagram
  autonumber
  actor Guest
  participant BC as BookingController
  participant BS as BookingServiceImpl
  participant RS as RoomService
  participant LS as ListingService
  participant DB as Database

  Guest->>BC: POST /api/v5/bookings
  BC->>BS: create(guestId, request)
  Note over BS,DB: one database transaction starts
  BS->>BS: validate dates, rooms and guests
  BS->>RS: lockRoom(roomId)
  RS->>DB: SELECT the room row FOR UPDATE
  Note over RS,DB: a second guest booking this room type waits here
  RS-->>BS: price, totalRooms, maxGuests
  BS->>LS: getBookingInfo(listingId)
  LS-->>BS: published, hostId, title, location, cover image
  BS->>BS: not your own listing, capacity ok, fewer than 2 unpaid holds
  BS->>DB: active bookings overlapping these nights
  BS->>BS: free rooms = totalRooms minus the busiest night
  alt not enough rooms
    BS-->>Guest: 409 Sold out or Only N left
  else enough rooms
    BS->>DB: save booking PENDING_PAYMENT with price snapshot and a 15 minute hold
    Note over BS,DB: commit releases the lock
    BS-->>Guest: 201 with the booking and holdExpiresAt
  end
```

---

## 3. B2: payment

### Design decisions

- **The payment module does not know the booking module.** It asks `BookingPayments`, an interface in `common`, and booking implements it.
- **One event processor decides every payment outcome.** The mock endpoint calls it today, and the real provider's webhook will call the same code in B5.
- **The server is the source of truth for the amount.** The amount comes from the booking, and a payment whose amount differs is never confirmed.
- **Every provider event is recorded once** (a unique key), so duplicates are ignored. The payment row and then the booking row are locked while processing, so parallel events run one at a time.
- **A money-back path instead of silent loss.** A payment for a booking that cannot be kept is refunded.

### Start the payment and confirm it

```mermaid
sequenceDiagram
  autonumber
  actor Guest
  participant PC as PaymentController
  participant MC as MockPaymentController
  participant PS as PaymentServiceImpl
  participant GW as PaymentGateway Mock
  participant PE as PaymentEventProcessor
  participant BP as BookingPaymentsImpl
  participant EB as Spring event bus
  participant NL as NotificationEventListener

  Note over Guest,GW: Part 1 - start the payment
  Guest->>PC: POST bookings/id/pay
  PC->>PS: startPayment(guestId, bookingId)
  PS->>BP: getPayableBooking(guestId, bookingId)
  BP-->>PS: reference, amount, currency, holdExpiresAt (or 404 and 409 errors)
  PS->>GW: createOrder(reference, amount, currency)
  GW-->>PS: orderId
  PS->>PS: save Payment CREATED (an open order is reused)
  PS-->>Guest: orderId, amount, mock true

  Note over Guest,NL: Part 2 - the provider reports the result (the mock buttons)
  Guest->>MC: POST payments/mock/orderId/complete with SUCCESS
  MC->>PS: completeMock(guestId, orderId, SUCCESS)
  PS->>PE: process(provider event PAYMENT_SUCCEEDED)
  Note over PE: lock the payment row and skip events already seen
  PE->>BP: confirmPayment(bookingId, paymentId, amount, currency)
  Note over BP: lock the booking row, check amount and status, revive if the rooms are free
  BP->>EB: publish BookingConfirmedEvent
  BP-->>PE: CONFIRMED
  PE->>PE: mark the payment SUCCEEDED
  Note over PE,EB: commit
  EB-->>NL: after the commit, on a background thread
  NL->>NL: bell notifications and emails for guest and host
  PS-->>Guest: Payment received, booking confirmed
```

### What `confirmPayment` decides

```mermaid
flowchart TD
  A["Payment succeeded event"] --> B{"Amount and currency match the booking?"}
  B -- no --> MM["MISMATCH: payment FAILED, error logged, booking untouched"]
  B -- yes --> C{"Booking status"}
  C -- "CONFIRMED or COMPLETED" --> D{"Same payment that confirmed it?"}
  D -- yes --> AC["ALREADY_CONFIRMED: nothing to do"]
  D -- no --> DU["DUPLICATE: the second payment is refunded"]
  C -- CANCELLED --> UN["UNAVAILABLE: payment refunded"]
  C -- "PENDING_PAYMENT or EXPIRED" --> H{"Hold still valid?"}
  H -- yes --> OK["CONFIRMED: booking confirmed, event published"]
  H -- no --> RR{"Listing published and rooms still free?"}
  RR -- yes --> OK
  RR -- no --> UN
```

### The classes and who uses whom

```mermaid
classDiagram
  class BookingGuard {
    <<interface>>
    listingHasActiveBookings()
    roomHasActiveBookings()
    peakRoomsBooked()
    guestHasActiveBookings()
    hostHasActiveBookings()
  }
  class BookingPayments {
    <<interface>>
    getPayableBooking()
    confirmPayment()
  }
  class PaymentGateway {
    <<interface>>
    createOrder()
    refund()
  }
  class RoomService {
    <<interface>>
    lockRoom()
    getRoomsByListingId()
  }
  class ListingService {
    <<interface>>
    getBookingInfo()
  }
  class BookingGuardImpl
  class BookingPaymentsImpl
  class MockPaymentGateway
  class AvailabilityServiceImpl
  class BookingServiceImpl
  class BookingRules
  class BookingRepository
  class Booking
  class PaymentServiceImpl
  class PaymentEventProcessor
  class Payment

  BookingGuard <|.. BookingGuardImpl
  BookingPayments <|.. BookingPaymentsImpl
  PaymentGateway <|.. MockPaymentGateway

  AvailabilityServiceImpl --> BookingRules
  AvailabilityServiceImpl --> BookingRepository
  AvailabilityServiceImpl --> RoomService
  AvailabilityServiceImpl --> ListingService
  BookingServiceImpl --> BookingRules
  BookingServiceImpl --> BookingRepository
  BookingServiceImpl --> RoomService
  BookingServiceImpl --> ListingService
  BookingGuardImpl --> BookingRepository
  BookingPaymentsImpl --> BookingRepository
  BookingPaymentsImpl --> RoomService
  BookingPaymentsImpl --> ListingService
  BookingRepository --> Booking

  PaymentServiceImpl --> BookingPayments
  PaymentServiceImpl --> PaymentGateway
  PaymentServiceImpl --> PaymentEventProcessor
  PaymentEventProcessor --> BookingPayments
  PaymentEventProcessor --> PaymentGateway
  PaymentEventProcessor --> Payment
```

`BookingGuard` is used by `ListingServiceImpl`, `RoomServiceImpl` and `UserServiceImpl` to refuse deletions and edits that would break upcoming bookings. They depend on the interface only.

---

## 4. The notification module

There are **two entry points** and one shared email pipeline.

### Classes and their jobs

| Class | Folder | Job |
|---|---|---|
| `NotificationEventListener` | `eventListeners` | Listens to domain events from other modules (blocked, unblocked, host request, listing suspended, booking confirmed, refunded). It decides who gets a notification and runs after the commit, in the background. |
| `UserDirectory` (an interface in `common`, implemented by `UserDirectoryImpl` in auth) | | Gives names and emails. Notification never reads the users table. |
| `InAppNotificationService` and `Impl` | `service`, `serviceImpl` | Saves, lists, counts, marks read and deletes the bell rows, and cleans up after 90 days. Each save is its own transaction. |
| `Notification`, `NotificationType`, `NotificationRepository` | `entities`, `enums`, `repository` | The `notifications` table. |
| `NotificationController` | `controller` | The bell endpoints. A user only ever sees their own rows. |
| `NotificationService` and `Impl` | `service`, `serviceImpl` | One method per kind of email. It builds the text, then publishes a `SimpleMessageEvent`. |
| `EmailMessageBuilderUtil` | `util` | All email texts in one place. |
| `SimpleMessageEvent` | `event` | A small message: recipient, subject, body. |
| `EmailEventListeners` | `eventListeners` | Receives that message after the commit, in the background, and calls `MailService`. |
| `MailService` and `MailServiceImpl` | `service`, `serviceImpl` | Prints the email to the console, or sends through SMTP when `app.mail.enabled=true`. It never throws. |
| `AsyncConfig` | `config` | Turns on `@Async` and scheduling, so those listeners run on background threads. |

### Overview

```mermaid
flowchart LR
  subgraph T["What starts a notification"]
    T1["Domain events from other modules<br/>blocked, unblocked, host request,<br/>listing suspended, booking confirmed, refund"]
    T2["Direct commands from services<br/>registration code, welcome,<br/>reset code, password changed"]
  end

  T1 --> NEL["NotificationEventListener"]
  NEL --> UD["UserDirectory<br/>names and emails"]
  NEL --> INAPP["InAppNotificationService<br/>saves the bell rows"]
  NEL --> NS["NotificationService<br/>one method per email"]
  T2 --> NS
  INAPP --> TBL["notifications table"]
  NC["NotificationController<br/>bell endpoints"] --> INAPP

  NS --> BUILD["EmailMessageBuilderUtil<br/>writes the text"]
  NS --> SME["SimpleMessageEvent<br/>recipient, subject, body"]
  SME --> EEL["EmailEventListeners<br/>after commit, background"]
  EEL --> MAIL["MailService<br/>console or SMTP"]
```

### One full example: a confirmed booking

```mermaid
sequenceDiagram
  autonumber
  participant BP as BookingPaymentsImpl
  participant EB as Spring event bus
  participant NL as NotificationEventListener
  participant UD as UserDirectory
  participant IA as InAppNotificationService
  participant DB as notifications table
  participant NS as NotificationServiceImpl
  participant EM as EmailMessageBuilderUtil
  participant EL as EmailEventListeners
  participant MS as MailServiceImpl

  BP->>EB: publish BookingConfirmedEvent
  Note over BP,EB: the booking transaction commits
  EB-->>NL: after the commit, background thread
  NL->>UD: findContact for the guest and for the host
  UD-->>NL: names and emails
  NL->>IA: create(bell row) for the guest, then for the host
  IA->>DB: insert, each in its own transaction
  NL->>NS: sendBookingConfirmed and sendBookingReceived
  NS->>EM: build the email text
  NS->>EB: publish SimpleMessageEvent
  EB-->>EL: background thread again
  EL->>MS: sentEmail(to, body, subject)
  alt app.mail.enabled is false
    MS->>MS: print the email in the console
  else app.mail.enabled is true
    MS->>MS: send through SMTP
  end
```

### Why they stay in sync without breaking each other

- **After the commit.** Nothing is announced for an action that rolled back. A failed email can never undo a block or a payment.
- **Background threads.** The user's request does not wait for emails.
- **`fallbackExecution = true` on both listeners.** `EmailEventListeners` receives its event while no transaction is open, because it is published from an after-commit callback. Without this flag Spring would silently skip it.
- **The bell save is wrapped in try and catch.** If the database refuses a row, the email still goes out.
- **Two hops for email on purpose.** `NotificationService` writes the text and `EmailEventListeners` and `MailService` send it. To switch to a real mail provider, only `MailServiceImpl` changes.

### To add a new notification

1. Add the event to `common.events`, and publish it from the module where it happens.
2. Add a message to `EmailMessageBuilderUtil`, and a method to `NotificationService` and its implementation.
3. Add a type to `NotificationType`.
4. Add a method in `NotificationEventListener` that saves the bell row and calls the new method. Use the same two annotations as the others.

---

## 5. Reading order for the code

`BookingRules`, then `AvailabilityServiceImpl`, then `BookingServiceImpl.create`, then `BookingPaymentsImpl.confirmPayment`, then `PaymentEventProcessor.process`, then `NotificationEventListener`. The diagrams above follow that same path.
