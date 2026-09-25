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
