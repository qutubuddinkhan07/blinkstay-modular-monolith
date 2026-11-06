import axiosInstance from "../../api/axiosInstance";

const BASE = "/v5/host/dashboard";

// { success, message, data } -> data
const unwrap = (res) => res.data?.data ?? res.data;

// Leaves out empty values: no dates = the server's default range
const clean = (params = {}) =>
  Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== "" && v != null),
  );

// range: { from, to } as "YYYY-MM-DD" (either can be empty)

// { from, to, currency, bookings, cancelled, gross, refunded, commission,
//   hostAmount, earned, pending, averageBookingValue }
export const getEarningsSummary = (range) =>
  axiosInstance.get(`${BASE}/summary`, { params: clean(range) }).then(unwrap);

// [{ year, month, bookings, gross, hostAmount }]
export const getEarningsMonthly = (range) =>
  axiosInstance.get(`${BASE}/monthly`, { params: clean(range) }).then(unwrap);

// [{ listingId, listingTitle, bookings, gross, hostAmount }]
export const getEarningsByListing = (range) =>
  axiosInstance
    .get(`${BASE}/by-listing`, { params: clean(range) })
    .then(unwrap);

// status: "" | PENDING | EARNED | REVERSED. Returns the page object { content, totalPages, ... }
export const getEarningsLedger = ({ status, from, to, page = 0, size = 10 }) =>
  axiosInstance
    .get(`${BASE}/ledger`, {
      params: clean({ status, from, to, page, size }),
    })
    .then(unwrap);
