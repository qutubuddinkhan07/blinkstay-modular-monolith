import { notify } from "../utils/notify";
import axiosInstance from "./axiosInstance";

const publicEndpoints = [
  "/api/v1/auth/login",
  "/api/v1/auth/register",
  "/api/v2/user/register-init",
  "/api/v2/user/verify-otp",
  "/api/v3/listings/all",
];

axiosInstance.interceptors.request.use(
  (config) => {
    console.log("REQUEST INTERCEPTOR:", config.method, config.url);

    const isPublicEndpoint = publicEndpoints.includes(config.url);

    if (!isPublicEndpoint) {
      const token = localStorage.getItem("token");

      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
        console.log("JWT attached");
      }
    }

    return config;
  },
  (err) => {
    console.log("REQUEST ERROR INTERCEPTOR");
    return Promise.reject(err);
  },
);

axiosInstance.interceptors.response.use(
  (response) => {
    console.log("RESPONSE INTERCEPTOR:", response.status, response.config.url);
    return response;
  },
  (err) => {
    console.log("RESPONSE ERROR INTERCEPTOR:", err.response?.status);

    if (err.response?.status === 401) {
      console.log("Authentication failed");
    }

    return Promise.reject(err);
  },
);
