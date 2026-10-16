import axiosInstance from "../../api/axiosInstance";

const BASE = "/v3/favorites";

// data: array of listing ids (everything the user has favorited, even paused listings)
export const fetchFavoriteIds = () => axiosInstance.get(`${BASE}/ids`);

// the listing cards of the current page (published listings only)
export const fetchFavorites = ({ page = 0, size = 12 } = {}) =>
  axiosInstance.get(BASE, { params: { page, size } });

// both are idempotent: repeating them is safe
export const addFavorite = (id) => axiosInstance.put(`${BASE}/${id}`);
export const removeFavorite = (id) => axiosInstance.delete(`${BASE}/${id}`);
