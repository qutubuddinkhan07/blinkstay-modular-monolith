import axiosInstance from "../../api/axiosInstance";

const LISTING_URL = "/v3/listings";

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

// DELETE /{id} (owner or admin)
export const deleteListing = (id) => {
  return axiosInstance.delete(`${LISTING_URL}/${id}`);
};
