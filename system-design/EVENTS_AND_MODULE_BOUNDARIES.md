# Events and Module Boundaries

> How BlinkStay uses domain events to keep its modules decoupled — and how that
> same design makes a future move to microservices cheap.

This document explains, in plain words, the idea behind the event system in
BlinkStay, where it lives in the code, what happens step by step when an admin
blocks a host, and what would change if the modules were split into separate
services later.

---

## Table of contents

1. [The idea in plain words](#the-idea-in-plain-words)
2. [Where it happens in your code](#where-it-happens-in-your-code)
3. [Use cases](#diagram-1-use-cases)
4. [Modules and the event bus](#diagram-2-modules-and-the-event-bus)
5. [Blocking a host (sequence)](#diagram-3-blocking-a-host-sequence)
6. [What a blocked user sees next](#diagram-4-what-a-blocked-user-sees-next)
7. [Listing statuses](#diagram-5-listing-statuses)
8. [Moving to microservices later](#moving-to-microservices-later)
9. [What to do now so the split stays cheap](#what-to-do-now-so-the-split-stays-cheap)

---

## The idea in plain words

An event is a small message that says **“this happened”**. The code that makes
it happen doesn’t need to know who cares.

When an admin blocks a host, the auth module has to make that host’s listings
disappear. Without events, auth would call listing code directly, so the two
modules would be glued together, and the day you split them into services, that
call would break. With events:

- Auth blocks the user and shouts `UserBlockedEvent(userId, reason)`.
- Spring delivers it to everyone listening for that type.
- The listing module’s listener reacts and suspends the listings.

Auth never imports anything from listing. The only thing the two modules share
is the event class in `blinkstay.common.events`.

---

## Where it happens in your code

1. `AdminUserController.block` calls `AdminUserServiceImpl.blockUser`, which is
   `@Transactional`. It saves `isActive=false` and the reason, then calls
   `publisher.publishEvent(new UserBlockedEvent(...))`.
2. Spring finds `UserLifecycleListener.on(UserBlockedEvent)` in the listing
   module and runs it **immediately, on the same thread and inside the same
   transaction**.
3. The listener calls `ListingModerationService.suspendAllByManager`, which sets
   every listing to `SUSPENDED` and saves what each one was before.
4. Control returns to `blockUser`, the transaction commits, and the listing
   caches are cleared **after the commit**.
5. If anything fails in steps 1 to 4, the user block and the listing changes roll
   back **together**.

Unblock works the same way with `UserUnblockedEvent`. A self-deleted account
also publishes `UserBlockedEvent`, so a host who deletes their account takes
their listings offline too.

Separately, the JWT filter **doesn’t** use events. It re-reads the user on every
request, so a blocked person’s next call is rejected.

In Step 6, notification will listen for events too, but **after the commit and
asynchronously**, so a failed email can never undo a block.

---

## Diagram 1: Use cases

_(green = built, yellow = coming)_

![Use case diagram](docs/diagrams/01-use-cases.png)

Source: [`system-design\EVENTS_AND_MODULE_BOUNDARIES.md`](docs/diagrams/01-use-cases.puml)

---

## Diagram 2: Modules and the event bus

The red arrow is a problem explained in
[Moving to microservices later](#moving-to-microservices-later).

![Modules and event bus](docs/diagrams/02-modules-event-bus.png)

Source: [`docs/diagrams/02-modules-event-bus.puml`](docs/diagrams/02-modules-event-bus.puml)

---

## Diagram 3: Blocking a host (sequence)

![Block host sequence](docs/diagrams/03-block-host-sequence.png)

Source: [`docs/diagrams/03-block-host-sequence.puml`](docs/diagrams/03-block-host-sequence.puml)

---

## Diagram 4: What a blocked user sees next

_(no event involved)_

![Blocked user next request](docs/diagrams/04-blocked-user-next-request.png)

Source: [`docs/diagrams/04-blocked-user-next-request.puml`](docs/diagrams/04-blocked-user-next-request.puml)

---

## Diagram 5: Listing statuses

![Listing statuses](docs/diagrams/05-listing-statuses.png)

Source: [`docs/diagrams/05-listing-statuses.puml`](docs/diagrams/05-listing-statuses.puml)

---

## Moving to microservices later

### How the modules would split

The natural cut is **three services**:

- **Auth service** — users, login, host requests.
- **Listing service** — listings and rooms. Rooms don’t make sense without
  listings, and today the two call each other (the red arrow in Diagram 2).
  Treat them as one unit.
- **Notification service.**

`Seed` stays a tool, not a service.

### Target picture

![Microservices target](docs/diagrams/06-microservices-target.png)

Source: [`docs/diagrams/06-microservices-target.puml`](docs/diagrams/06-microservices-target.puml)

### What stays the same

The event records, the publisher call and the listener methods. The transport
changes from Spring’s in-process bus to a message broker. **That’s the reason
for sending only IDs and strings in events.**

### What gets harder (so you can decide when it’s worth it)

- **No single transaction.** Today the block and the listing suspension are all
  or nothing. Across services they happen separately, so there is a short window
  where the host is blocked but listings are still public. The standard answer is
  an **outbox table**: auth writes the event row in the same database transaction
  as the block, and a relay sends it to the broker. Listeners must also cope with
  the same event arriving twice.
- **Checking blocked users.** Today the JWT filter reads the user table on every
  request. Listing service can’t read auth’s database. Either the gateway or each
  service checks status through a call to auth, or services keep a small local
  list of blocked users filled from the events.
- **Caches.** Caffeine lives inside one process, so with several instances you’d
  need Redis or short TTLs.
- **Data.** Each service owns its tables, with no joins across them. You already
  refer to the other side by UUID (`Listing.managerId`, `ListingRoom.listingId`),
  which is what makes this possible.

---

## What to do now so the split stays cheap

- Keep modules talking through **events or small interfaces**, and never reach
  into another module’s repositories or entities.
- **Break the listing–room cycle** at some point. The cleanest way is to put the
  ownership check on the listing side, so `RoomController` stops calling
  `ListingService`.
- In Step 6, notifications will be driven **only by events**, so auth never
  imports `NotificationService`. Today `UserServiceImpl` still injects it with
  the calls commented out, so remove that field then.
- A tool worth knowing is **Spring Modulith**. It checks at build time that
  modules don’t depend on each other’s internals, and it has a built-in event
  publication log that works like an outbox. You can adopt it later without
  rewriting anything.
