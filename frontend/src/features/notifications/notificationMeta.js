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
};

export const getNotificationIcon = (type) => ICONS[type] ?? FiBell;

// Where a notification opens
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

    // Guest's own booking (referenceId = bookingId)
    case "BOOKING_CONFIRMED":
    case "PAYMENT_REFUNDED":
      return n.referenceId ? `/bookings/${n.referenceId}` : "/bookings";

    // Host's copy: /bookings/{id} is guest-only and would 404 for the host.
    // Switch to the host reservations page once it exists.
    case "BOOKING_RECEIVED":
      return "/my-listings";

    default:
      return "/notifications";
  }
};

// An approved host still has the old roles in this browser, and /my-listings is manager-only,
// so refresh the user first or they'd land on the "no access" screen.
export const openNotification = async (n, { navigate, refreshUser }) => {
  if (n.type === "HOST_REQUEST_APPROVED") await refreshUser?.();
  navigate(getNotificationPath(n));
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
