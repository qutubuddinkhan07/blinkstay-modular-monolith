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
