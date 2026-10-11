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
