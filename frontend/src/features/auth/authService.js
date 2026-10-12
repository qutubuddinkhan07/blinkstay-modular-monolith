import axiosInstance from "../../api/axiosInstance";

const AUTH_URL = "/v1/auth";
const USER_URL = "/v2/user";

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

// POST /api/v1/auth/forgot-password: always 200, whether or not the account exists
export const forgotPassword = ({ username, email }) => {
  return axiosInstance.post(`${AUTH_URL}/forgot-password`, { username, email });
};

// POST /api/v1/auth/reset-password: 400 "Invalid or expired code" on a wrong code
export const resetPassword = ({ email, otp, newPassword }) => {
  return axiosInstance.post(`${AUTH_URL}/reset-password`, {
    email,
    otp,
    newPassword,
  });
};
