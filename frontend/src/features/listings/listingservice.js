import axiosInstance from "../../api/axiosInstance";

const LISTING_URL = "/api/v3/listings";

export const fetchListings = ({ page, size, sortBy, direction, country }) => {
  return axiosInstance.get(`${LISTING_URL}/all`, {
    params: {
      page,
      size,
      sortBy,
      direction,
      country,
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
