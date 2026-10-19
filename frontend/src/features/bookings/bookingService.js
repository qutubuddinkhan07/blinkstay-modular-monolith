import axiosInstance from "../../api/axiosInstance";

const BOOKINGS = "/v5/bookings";
const AVAILABILITY = "/v5/availability";

// Same limits the server enforces
export const BOOKING_LIMITS = {
  maxAdvanceDays: 356,
  maxNights: 30,
  maxRooms: 10,
  maxGuests: 50,
};

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

// status: "PENDING_PAYMENT,CONFIRMED, EXPIRED". data: { content[], page, size, totalElements, totalPages }
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

//! PAYMENT SERVICE
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

// Lets the Navbar badge refresh after a payment, without a route change
export const BOOKINGS_CHANGED = "blinkstay:bookings-changed";
export const announceBookingsChanged = () =>
  window.dispatchEvent(new Event(BOOKINGS_CHANGED));
