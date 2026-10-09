// all /api/v2/user/me calls

import axiosInstance from "../../api/axiosInstance";

const BASE = "/api/v2/user/me";

export const getMe = axiosInstance.get(BASE);

// send only changed fields: "" clears phone/bio
export const updateProfile = (changes) => axiosInstance.patch(BASE, changes);

export const uploadAvatar = (file) => {
  const form = new FormData();
  form.append("image", file);
  // don't set Content-Type yourself, the browser adds the boundary

  return axiosInstance.put(`${BASE}/image`, form);
};

export const removeAvatar = () => axiosInstance.delete(`${BASE}/image`);

export const changePassword = (currentPassword, newPassword) =>
  axiosInstance.put(`${BASE}/password`, { currentPassword, newPassword });

export const deleteAccount = () => axiosInstance.delete(BASE);
