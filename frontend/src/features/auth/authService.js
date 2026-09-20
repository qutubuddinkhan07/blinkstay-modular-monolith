import axiosInstance from "../../api/axiosInstance";

const AUTH_URL = "/api/v1/auth";
const USER_URL = "/api/v2/user";

export const registerInit = (userData, profileImg) => {
  const formData = new FormData();

  formData.append(
    "userData",
    new Blob([JSON.stringify(userData)], { type: "application/json" }),
  );

  if (profileImg) {
    formData.append("profileImg", profileImg);
  }
  return axiosInstance.post(`${USER_URL}/register-init`, formData);
};

export const verifyOtp = (email, otp) => {
  return axiosInstance.post(`${USER_URL}/verify-otp`, { email, otp });
};

export const login = (username, password) => {
  return axiosInstance.post(`${AUTH_URL}/login`, { username, password });
};

export const logout = () => {
  return axiosInstance.post(`${AUTH_URL}/logout`);
};
