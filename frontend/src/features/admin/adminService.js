import axiosInstance from "../../api/axiosInstance";

const ADMIN_LISTINGS = "/v3/admin/listings";

// POST /{id}/suspend  body { reason }  (reason required, max 500)
export const suspendListing = (id, reason) => {
  return axiosInstance.post(`${ADMIN_LISTINGS}/${id}/suspend`, { reason });
};

// POST /{id}/unsuspend  (409 if the listing was suspended because its owner is blocked)
export const unsuspendListing = (id) => {
  return axiosInstance.post(`${ADMIN_LISTINGS}/${id}/unsuspend`);
};

const USERS = "/v2/admin/users";

// blocked: "all" | "blocked" | "active"; empty values are not sent
export const fetchUsers = ({ q, blocked, role, page = 0, size = 10 }) => {
  const params = { page, size };
  if (q) params.q = q;
  if (blocked === "blocked") params.blocked = true;
  if (blocked === "active") params.blocked = false;
  if (role) params.role = role;

  return axiosInstance.get(USERS, { params });
};

// POST /{id}/block  body { reason } (required, max 500)
export const blockUser = (id, reason) => {
  return axiosInstance.post(`${USERS}/${id}/block`, { reason });
};

// POST /{id}/unblock
export const unblockUser = (id) => {
  return axiosInstance.post(`${USERS}/${id}/unblock`);
};

//! --- HOST REQUEST ---
export const HOST_REQUESTS_CHANGED = "host-requests:changed"; // lets the navbar badge refresh

const HOST_REQUESTS = "/v2/admin/host-requests";

// status: "PENDING" | "APPROVED" | "REJECTED" | "ALL" ("ALL" sends no filter); oldest pending first
export const fetchHostRequests = ({
  status = "PENDING",
  page = 0,
  size = 10,
}) => {
  const params = { page, size };
  if (status && status !== "ALL") params.status = status;
  return axiosInstance.get(HOST_REQUESTS, { params });
};

export const fetchPendingHostRequestCount = () =>
  axiosInstance.get(`${HOST_REQUESTS}/pending-count`);

export const approveHostRequest = (id) =>
  axiosInstance.post(`${HOST_REQUESTS}/${id}/approve`);

// reason is required, max 500
export const rejectHostRequest = (id, reason) =>
  axiosInstance.post(`${HOST_REQUESTS}/${id}/reject`, { reason });
