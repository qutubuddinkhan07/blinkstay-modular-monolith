import axiosInstance from "../../api/axiosInstance";

// { success, message, data } -> data.
// A booking with no review answers data: null, so there is deliberately no fallback to the wrapper.
const unwrap = (res) => res.data?.data ?? null;

export const REVIEW_LIMITS = { comment: 1000, commentMin: 10, reply: 1000 };

/* ---- guest ---- */

// data: Review, or null when this booking has no review yet
export const getBookingReview = (bookingId) =>
  axiosInstance.get(`/v5/bookings/${bookingId}/review`).then(unwrap);

// body: { rating, comment }
export const createReview = (bookingId, body) =>
  axiosInstance.post(`/v5/bookings/${bookingId}/review`, body).then(unwrap);

export const updateReview = (reviewId, body) =>
  axiosInstance.put(`/v5/reviews/${reviewId}`, body).then(unwrap);

export const deleteReview = (reviewId) =>
  axiosInstance.delete(`/v5/reviews/${reviewId}`).then(unwrap);

/* ---- public ---- */

// data: { content, page, size, totalElements, totalPages, averageRating, reviewCount }
export const getListingReviews = (listingId, { page = 0, size = 10 } = {}) =>
  axiosInstance
    .get(`/v5/listings/${listingId}/reviews`, { params: { page, size } })
    .then(unwrap);

/* ---- host ---- */

// same page shape; averageRating is null here
export const getHostReviews = ({ page = 0, size = 10 } = {}) =>
  axiosInstance
    .get("/v5/host/reviews", { params: { page, size } })
    .then(unwrap);

export const saveReply = (reviewId, reply) =>
  axiosInstance.put(`/v5/reviews/${reviewId}/reply`, { reply }).then(unwrap);

export const deleteReply = (reviewId) =>
  axiosInstance.delete(`/v5/reviews/${reviewId}/reply`).then(unwrap);

/* ---- admin ---- */

export const adminDeleteReview = (reviewId) =>
  axiosInstance.delete(`/v5/admin/reviews/${reviewId}`).then(unwrap);
