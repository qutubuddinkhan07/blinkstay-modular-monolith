// Dates here are plain calendar days ("YYYY-MM-DD"), never UTC timestamps.
const pad = (n) => String(n).padStart(2, "0");

// Date -> "2026-11-06". Never use toISOString() for this: it shifts to UTC.
export const toLocalDateString = (d) =>
  d ? `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` : "";

// "2026-11-06" -> local Date at midnight, or null if invalid (rejects "2026-02-31")
export const parseLocalDate = (s) => {
  if (!s || !/^\d{4}-\d{2}-\d{2}$/.test(s)) return null;
  const [y, m, d] = s.split("-").map(Number);
  const date = new Date(y, m - 1, d);
  return date.getMonth() === m - 1 && date.getDate() === d ? date : null;
};

export const addDays = (d, n) =>
  new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);

// 3700 -> "₹3,700", 3700.5 -> "₹3,700.50"
export const formatINR = (n, currency = "INR") =>
  n == null
    ? "—"
    : new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency,
        minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
        maximumFractionDigits: 2,
      }).format(n);

// Date or "YYYY-MM-DD" -> "6 Nov 2026"
export const formatDateLabel = (value, fallback = "Add date") => {
  const d = typeof value === "string" ? parseLocalDate(value) : value;
  return d
    ? d.toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
      })
    : fallback;
};

// "SINGLE" -> "Single"
export const formatRoomType = (t = "") =>
  t ? t.charAt(0) + t.slice(1).toLowerCase().replace(/_/g, " ") : "";

export const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;

// UTC timestamp ("...Z") -> local "17 Oct 2026, 10:16 pm"
export const formatTimestamp = (value) =>
  value
    ? new Date(value).toLocaleString("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "—";

// UTC timestamp -> local "10:16 pm"
export const formatTime = (value) =>
  value
    ? new Date(value).toLocaleTimeString("en-IN", {
        hour: "numeric",
        minute: "2-digit",
      })
    : "";
