const pad = (n) => String(n).padStart(2, "0");

// Date -> "YYYY-MM-DD" from the LOCAL date parts (toISOString would shift the day in some timezones)
export const toISODate = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

// "YYYY-MM-DD" -> a local Date, with no timezone shift
export const parsePlainDate = (value) => {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value ?? "");
  return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : null;
};

// "2026-11-09" -> "9 Nov 2026"
export const formatPlainDate = (value) => {
  const d = parsePlainDate(value);
  return d
    ? d.toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
      })
    : "—";
};

// A UTC instant ("...Z") shown in the browser's local time
export const formatInstant = (value) => {
  if (!value) return "";
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? ""
    : d.toLocaleString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
      });
};

// (2026, 11) -> "Nov 2026". month is 1 to 12
export const monthLabel = (year, month) =>
  new Date(year, month - 1, 1).toLocaleDateString("en-US", {
    month: "short",
    year: "numeric",
  });

export const formatCount = (n) =>
  new Intl.NumberFormat("en-IN").format(Number(n) || 0);

// Shows the number exactly as the server sent it, in the currency the server sent
const formatters = new Map();
export const formatMoney = (amount, currency = "INR") => {
  if (amount == null || amount === "") return "—";

  try {
    let f = formatters.get(currency);
    if (!f) {
      f = new Intl.NumberFormat("en-IN", { style: "currency", currency });
      formatters.set(currency, f);
    }
    return f.format(Number(amount));
  } catch {
    return `${currency} ${amount}`; // unknown currency code
  }
};

export const PRESETS = [
  { key: "last12", label: "Last 12 months" },
  { key: "thisYear", label: "This year" },
  { key: "last3", label: "Last 3 months" },
  { key: "custom", label: "Custom" },
];

// Empty from/to = the server's default range
export const rangeForPreset = (key, today = new Date()) => {
  const year = today.getFullYear();

  switch (key) {
    case "thisYear":
      return { from: `${year}-01-01`, to: `${year}-12-31` };
    case "last3":
      return {
        from: toISODate(new Date(year, today.getMonth() - 2, 1)),
        to: toISODate(today),
      };
    default:
      return { from: "", to: "" };
  }
};
