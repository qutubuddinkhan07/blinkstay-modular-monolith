import {
  FiAlertTriangle,
  FiBell,
  FiCalendar,
  FiCheckCircle,
  FiInbox,
  FiRefreshCw,
  FiRotateCcw,
  FiSlash,
  FiUnlock,
  FiXCircle,
} from "react-icons/fi";
import { getBooking } from "../bookings/bookingService";

const ICONS = {
  HOST_REQUEST_SUBMITTED: FiInbox,
  HOST_REQUEST_APPROVED: FiCheckCircle,
  HOST_REQUEST_REJECTED: FiXCircle,
  LISTING_SUSPENDED: FiAlertTriangle,
  LISTING_RESTORED: FiRefreshCw,
  ACCOUNT_BLOCKED: FiSlash,
  ACCOUNT_UNBLOCKED: FiUnlock,
  BOOKING_CONFIRMED: FiCheckCircle,
  PAYMENT_REFUNDED: FiRotateCcw,
  BOOKING_RECEIVED: FiCalendar,
  BOOKING_CANCELLED: FiXCircle,
};

export const getNotificationIcon = (type) => ICONS[type] ?? FiBell;

// Booking and payment notifications can reach the guest or the host
const isBookingType = (type = "") =>
  type.startsWith("BOOKING_") || type.startsWith("PAYMENT_");

// Types that only ever go to the host. Used only when the booking can't be looked up.
const HOST_COPY = new Set(["BOOKING_RECEIVED"]);

// Guest -> their booking page. Host -> Reservations.
export const resolveBookingPath = async (n, { isManager = false } = {}) => {
  const mine = n.referenceId ? `/bookings/${n.referenceId}` : "/bookings";
  const asHost = "/manager/reservations";

  // Only hosts have a Reservations page, so anyone else can only be the guest
  if (!isManager) return mine;
  if (!n.referenceId) return HOST_COPY.has(n.type) ? asHost : mine;

  try {
    await getBooking(n.referenceId); // owner-only: succeeds only for the guest who booked it
    return mine;
  } catch (err) {
    // 404: it's someone else's booking, so this is the host's copy
    if (err?.response?.status === 404) return asHost;
    return HOST_COPY.has(n.type) ? asHost : mine; // couldn't tell: go by type
  }
};

// Where every other notification opens
export const getNotificationPath = (n) => {
  switch (n.type) {
    case "HOST_REQUEST_SUBMITTED":
      return "/admin/host-requests";
    case "HOST_REQUEST_APPROVED":
      return "/my-listings";
    case "HOST_REQUEST_REJECTED":
      return "/become-host"; // shows the rejection reason
    case "LISTING_SUSPENDED":
    case "LISTING_RESTORED":
      // a deleted listing makes the detail page show "Listing not available"
      return n.referenceId ? `/listings/${n.referenceId}` : "/my-listings";
    case "ACCOUNT_BLOCKED":
    case "ACCOUNT_UNBLOCKED":
      return "/profile";
    default:
      return "/notifications";
  }
};

// An approved host still has the old roles in this browser, and /my-listings is manager-only,
// so refresh the user first or they'd land on the "no access" screen.
export const openNotification = async (
  n,
  { navigate, refreshUser, isManager },
) => {
  if (n.type === "HOST_REQUEST_APPROVED") await refreshUser?.();

  const path = isBookingType(n.type)
    ? await resolveBookingPath(n, { isManager })
    : getNotificationPath(n);

  navigate(path);
};

const UNITS = [
  ["year", 31536000],
  ["month", 2592000],
  ["day", 86400],
  ["hour", 3600],
  ["minute", 60],
];

export const timeAgo = (value) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  const seconds = Math.round((date.getTime() - Date.now()) / 1000);
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });

  for (const [unit, size] of UNITS) {
    if (Math.abs(seconds) >= size)
      return rtf.format(Math.round(seconds / size), unit);
  }
  return "just now";
};
