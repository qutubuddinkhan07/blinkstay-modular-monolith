# Blinkstay Development Roadmap: Steps 2 through 6

This document outlines the detailed specs, processes, endpoints, and execution order for Steps 2 to 6 of the Blinkstay application refactoring and feature implementation.

---

## ⚠️ Pre-requisite Check (Before Step 2)

> **Important Security Check:** Verify that `@EnableMethodSecurity` is present on your `SecurityConfig` class. If omitted, all `@PreAuthorize` annotations across services and controllers will be ignored.

---

## 📌 Implementation Sequence & Dependencies

The steps **must** be executed in the following order:

$$\text{Step 2} \longrightarrow \text{Step 3} \longrightarrow \text{Step 4} \longrightarrow \text{Step 5} \longrightarrow \text{Step 6}$$

* **Step 4** strictly depends on **Step 2** and **Step 3** (blocking functionality relies on listing suspension statuses and domain event listeners).
* **Step 5** follows Step 4 safely.
* **Step 6** is implemented last as it consumes events published across Steps 3, 4, and 5.

---

## 📑 Step 2: Listing Statuses (`listing` module)

### Process & Domain Changes
* Update `ListingStatus` enum: Add `PAUSED` and `SUSPENDED`.
* Add field `statusBeforeSuspension` to the `Listing` entity to track state prior to suspension.
* **Publishing Rule:** A listing can only transition to `PUBLISHED` if its current status is `DRAFT` or `PAUSED`, and it has at least one associated room.
* **Visibility Rule:** `GET /listings/{id}` must return `404 Not Found` for any listing that is not in `PUBLISHED` status, unless the requester is the listing owner or an Admin.
* **Suspension Rules:** Admin suspension requires a mandatory `reason`. Lifting a suspension restores the listing to `statusBeforeSuspension`.

### Endpoints
| Role | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Host** | `POST` | `/api/v3/listings/{id}/pause` | Pause an active listing |
| **Host** | `POST` | `/api/v3/listings/{id}/resume` | Resume a paused listing |
| **Admin** | `POST` | `/api/v3/admin/listings/{id}/suspend` | Suspend listing (Request body: `{ "reason": "string" }`) |
| **Admin** | `POST` | `/api/v3/admin/listings/{id}/unsuspend` | Lift admin suspension on listing |

---

## 📑 Step 3: Domain Events & Synchronous Listeners

### Process & Domain Changes
* Create event records in `blinkstay.common.events`:
  * `UserBlockedEvent`
  * `UserUnblockedEvent`
  * `HotelManagerRequestSubmittedEvent`
  * `HotelManagerRequestReviewedEvent`
  * `ListingSuspendedEvent`
* Add `UserLifecycleListener` in the `listing` module:
  * Listens for user block/unblock domain events.
  * Runs within the same transaction context as the block operation.
  * Suspends or restores all listings owned by the target host using bulk queries.
* Add `@EnableAsync` annotation once across the application configuration (enables asynchronous notification listeners for Step 6).

### Endpoints
* *None (Internal event wiring)*

---

## 📑 Step 4: User Block and Unblock (`auth` / `user` module)

### Process & Rules
* **Blocking a User:**
  * Sets `isActive = false` on the target user.
  * Admin must provide a reason.
  * Publishes `UserBlockedEvent`, causing `UserLifecycleListener` to set all host listings to `SUSPENDED` with source `OWNER_BLOCKED`.
* **Unblocking a User:**
  * Sets `isActive = true` and publishes `UserUnblockedEvent`.
  * Restores each listing back to its `statusBeforeSuspension` (`PUBLISHED` listings go live; `DRAFT` and `PAUSED` return to their prior states).
* **Guards & Edge Cases:**
  * An Admin cannot block themselves or the last remaining active Admin.
  * Attempting to block an already blocked user returns `409 Conflict`.
* **Account Deletion:** `DELETE /me` for host accounts must trigger listing suspension.
* **Exception Handling:** Add global handlers for new $404$ (Not Found) and $409$ (Conflict) exceptions.
* **Cache Eviction:** Evict entries from `usersById`, `usersByEmail`, `hotelManagerIds`, `listingById`, and `publishedListings` caches upon block/unblock.

### Endpoints
| Role | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Admin** | `GET` | `/api/v2/admin/users` | List users (paged, searchable, uses `AdminUserDto`) |
| **Admin** | `POST` | `/api/v2/admin/users/{id}/block` | Block user (Request body: `{ "reason": "string" }`) |
| **Admin** | `POST` | `/api/v2/admin/users/{id}/unblock` | Unblock user |

---

## 📑 Step 5: Host Request Application Flow (`auth` module)

### Entities & Data Model
1. **`HotelManagerRequest`**: Holds application state.
   * Fields: `id`, `userId`, `businessName`, `phone`, `address`, `description`, `status` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`), `reviewedBy`, `reviewedAt`, `rejectionReason`.
2. **`HotelManagerProfile`**: Host details created automatically upon application approval.

### Process & Rules
* Users without the `HOTEL_MANAGER` role can submit an application.
* A user can only have **one** pending application at a time.
* Applicants can cancel pending requests (`CANCELLED`).
* **Approval Flow (Transactional):**
  * Grants the user the `HOTEL_MANAGER` role.
  * Instantiates the `HotelManagerProfile`.
  * Sets request status to `APPROVED`.
  * Evicts relevant user/role caches.
* **Rejection Flow:**
  * Requires a mandatory `rejectionReason`.
  * Users can view the rejection reason and re-apply later.

### Endpoints
| Role | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **User** | `POST` | `/api/v2/host-requests` | Submit hotel manager application |
| **User** | `GET` | `/api/v2/host-requests/me` | View current user's request status |
| **User** | `DELETE` | `/api/v2/host-requests/me` | Cancel pending request |
| **Admin** | `GET` | `/api/v2/admin/host-requests?status=` | List applications filtered by status |
| **Admin** | `POST` | `/api/v2/admin/host-requests/{id}/approve` | Approve host application |
| **Admin** | `POST` | `/api/v2/admin/host-requests/{id}/reject` | Reject host application (Request body: `{ "reason": "string" }`) |

---

## 📑 Step 6: Notifications & In-App Alerts

### Process
* **Phase 1 — Email Notifications:**
  * Dispatched via existing `SimpleMessageEvent` using `@TransactionalEventListener(AFTER_COMMIT)`.
  * Guarantees emails are only sent if the database transaction commits successfully.
  * Triggers:
    * **Request Submitted:** Sent to Admins.
    * **Request Approved/Rejected:** Sent to Applicant.
    * **Account Blocked/Unblocked:** Sent to User.
    * **Listing Suspended/Restored:** Sent to Listing Owner.
  * Implements `UserDirectory` interface in the `auth` module to resolve admin email addresses for notifications.
* **Phase 2 — In-App Notifications:**
  * Database table `notifications` powering user bell icon updates.

### Endpoints (In-App Notifications)
| Role | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **User** | `GET` | `/api/v2/notifications` | Fetch user notifications (paged) |
| **User** | `GET` | `/api/v2/notifications/unread-count` | Get unread notification count |
| **User** | `PATCH` | `/api/v2/notifications/{id}/read` | Mark single notification as read |
| **User** | `PATCH` | `/api/v2/notifications/read-all` | Mark all notifications as read |