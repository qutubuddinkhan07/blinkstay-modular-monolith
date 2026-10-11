import axios from "axios";
import axiosInstance from "../../api/axiosInstance";

const LISTING_URL = "/v3/listings";

export const fetchListings = ({
  page,
  size,
  sortBy,
  direction,
  location,
  name,
  category,
}) => {
  return axiosInstance.get(`${LISTING_URL}/all`, {
    params: {
      page,
      size,
      sortBy,
      direction,
      location,
      name,
      category,
    },
  });
};

export const fetchListingById = (id) => {
  return axiosInstance.get(`${LISTING_URL}/${id}`);
};

export const createListing = async (listing, images) => {
  const formData = new FormData();

  formData.append(
    "listing",
    new Blob([JSON.stringify(listing)], {
      type: "application/json",
    }),
  );

  images.forEach((image) => {
    formData.append("images", image);
  });

  const response = await axiosInstance.post(`${LISTING_URL}/create`, formData);

  return response.data;
};

// GET /my-listings (HOTEL_MANAGER only) -> res.data.data is array
export const fetchMyListings = () => {
  return axiosInstance.get(`${LISTING_URL}/my-listings`);
};

// PUT /{id} body = AddListingDto as JSON
export const updateListing = (id, listing) => {
  return axiosInstance.put(`${LISTING_URL}/${id}`, listing);
};

// POST /{id}/images multipart, part name 'images' -> res.data.data is ImageDto[]
export const addListingImages = (id, images) => {
  const formData = new FormData();

  images.forEach((image) => {
    formData.append("images", image);
  });

  return axiosInstance.post(`${LISTING_URL}/${id}/images`, formData);
};

// DELETE /{id}/images/{imageId}
export const deleteListingImage = (id, imageId) => {
  return axiosInstance.delete(`${LISTING_URL}/${id}/images/${imageId}`);
};

// POST /{id}/publish
export const publishListing = (id) => {
  return axiosInstance.post(`${LISTING_URL}/${id}/publish`);
};

// POST /{id}/pause
export const pauseListing = (id) => {
  return axiosInstance.post(`${LISTING_URL}/${id}/pause`);
};

// POST /{id}/resume
export const resumeListing = (id) => {
  return axiosInstance.post(`${LISTING_URL}/${id}/resume`);
};

// DELETE /{id} (owner or admin)
export const deleteListing = (id) => {
  return axiosInstance.delete(`${LISTING_URL}/${id}`);
};

// POST /api/v3/admin/listings/{id}/suspend  body { reason } (required, max 500)
export const suspendListing = (id, reason) => {
  return axiosInstance.post(`/v3/admin/listings/${id}/suspend`, { reason });
};

// POST /api/v3/admin/listings/{id}/unsuspend (409 if suspended because the owner is blocked)
export const unsuspendListing = (id) => {
  return axiosInstance.post(`/v3/admin/listings/${id}/unsuspend`);
};

// GET /api/v3/listings/manager/{managerId} (ADMIN only) -> res.data.data is an array
export const fetchListingsByManager = (managerId) => {
  return axiosInstance.get(`${LISTING_URL}/manager/${managerId}`);
};
