import axiosInstance from "../../api/axiosInstance";

const BOOKINGS = "/v5/bookings";
const AVAILABILITY = "/v5/availability";

/* ------------------------------------------------------------------ */
/* Helpers                                                             */
/* ------------------------------------------------------------------ */

// { success, message, data } -> data
const unwrapData = (res) => res.data?.data ?? res.data;

// leaves out empty values so the URL stays clean
const cleanParams = (params = {}) =>
  Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== "" && v != null),
  );

// Same limits the server enforces
export const BOOKING_LIMITS = {
  maxAdvanceDays: 356,
  maxNights: 30,
  maxRooms: 10,
  maxGuests: 50,
};

/* ------------------------------------------------------------------ */
/* Availability and bookings (guest)                                   */
/* ------------------------------------------------------------------ */

// Public. data: { listingId, checkIn, checkOut, nights, rooms, guests, currency, roomTypes[] }
export const getAvailability = (
  listingId,
  { checkIn, checkOut, rooms, guests },
) =>
  axiosInstance
    .get(`${AVAILABILITY}/${listingId}`, {
      params: { checkIn, checkOut, rooms, guests },
    })
    .then((res) => res.data.data);

// body: { roomId, checkIn, checkOut, rooms, guests } -> the new booking
export const createBooking = (body) =>
  axiosInstance.post(BOOKINGS, body).then((res) => res.data.data);

// Owner only (others get 404)
export const getBooking = (id) =>
  axiosInstance.get(`${BOOKINGS}/${id}`).then((res) => res.data.data);

// status: "PENDING_PAYMENT,CONFIRMED,EXPIRED". data: { content[], page, size, totalElements, totalPages }
export const getMyBookings = ({
  status,
  sort,
  direction,
  page = 0,
  size = 10,
} = {}) =>
  axiosInstance
    .get(`${BOOKINGS}/me`, { params: { status, sort, direction, page, size } })
    .then((res) => res.data.data);

// Labels and dots for <StatusBadge statuses={BOOKING_STATUS} />
export const BOOKING_STATUS = {
  PENDING_PAYMENT: { label: "Awaiting payment", dot: "bg-amber-500" },
  CONFIRMED: { label: "Confirmed", dot: "bg-emerald-500" },
  COMPLETED: { label: "Completed", dot: "bg-primary" },
  CANCELLED: { label: "Cancelled", dot: "bg-danger" },
  EXPIRED: { label: "Expired", dot: "bg-subtext" },
};

// A PENDING_PAYMENT booking with payable=false has already run out
// (the server's job marks it EXPIRED within a minute), so show it as expired now.
export const effectiveStatus = (b) =>
  b.status === "PENDING_PAYMENT" && !b.payable ? "EXPIRED" : b.status;

// The booking keeps its own snapshot; this only decides whether to link to the listing.
// 404 (deleted, paused, suspended) -> false
export const listingExists = (listingId) =>
  axiosInstance
    .get(`/v3/listings/${listingId}`)
    .then(() => true)
    .catch(() => false);

/* ------------------------------------------------------------------ */
/* Payment                                                             */
/* ------------------------------------------------------------------ */

// Starts payment. No body: the server uses its own amount.
// data: { paymentId, bookingId, bookingReference, provider, orderId, amount, currency, mock, holdExpiresAt }
export const payBooking = (bookingId) =>
  axiosInstance
    .post(`${BOOKINGS}/${bookingId}/pay`)
    .then((res) => res.data.data);

// Mock provider only. outcome: "SUCCESS" | "FAILURE"
// data: { paymentId, bookingId, paymentStatus, message }
export const completeMockPayment = (orderId, outcome) =>
  axiosInstance
    .post(`/v5/payments/mock/${encodeURIComponent(orderId)}/complete`, {
      outcome,
    })
    .then((res) => res.data.data);

// Lets the Navbar badge refresh after a payment or a cancel, without a route change
export const BOOKINGS_CHANGED = "blinkstay:bookings-changed";

export const announceBookingsChanged = () =>
  window.dispatchEvent(new Event(BOOKINGS_CHANGED));

/* ------------------------------------------------------------------ */
/* Cancellation (guest)                                                */
/* ------------------------------------------------------------------ */

// data: { refundAmount, cancellable, freeCancellationUntil, message }
export const getCancellationPreview = (id) =>
  axiosInstance.get(`${BOOKINGS}/${id}/cancellation-preview`).then(unwrapData);

// reason is optional: with none, send no body at all (an empty {} would trigger @NotBlank validation)
export const cancelMyBooking = (id, reason) =>
  (reason
    ? axiosInstance.post(`${BOOKINGS}/${id}/cancel`, { reason })
    : axiosInstance.post(`${BOOKINGS}/${id}/cancel`)
  ).then(unwrapData);

/* ------------------------------------------------------------------ */
/* Host reservations                                                   */
/* ------------------------------------------------------------------ */

// params: status, listingId, from, to, page, size
export const getHostBookings = (params) =>
  axiosInstance
    .get("/v5/host/bookings", { params: cleanParams(params) })
    .then(unwrapData);

// reason is required (max 500)
export const hostCancelBooking = (id, reason) =>
  axiosInstance
    .post(`/v5/host/bookings/${id}/cancel`, { reason })
    .then(unwrapData);

/* ------------------------------------------------------------------ */
/* Admin                                                               */
/* ------------------------------------------------------------------ */

// params: q, guestId, hostId, status, page, size
export const getAdminBookings = (params) =>
  axiosInstance
    .get("/v5/admin/bookings", { params: cleanParams(params) })
    .then(unwrapData);

// ASSUMED path: confirm it against your admin booking controller
export const adminCancelBooking = (id, reason) =>
  axiosInstance
    .post(`/v5/admin/bookings/${id}/cancel`, { reason })
    .then(unwrapData);
