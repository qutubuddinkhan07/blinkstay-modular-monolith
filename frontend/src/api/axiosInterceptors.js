import axiosInstance from "./axiosInstance";
import Cookies from "js-cookie";

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

    // const requestPath = config.url?.split("?")[0]; // extract the url

    // const isPublicEndpoint = publicEndpoints.includes(requestPath);

    // if (!isPublicEndpoint) {
    //   const token = localStorage.getItem("token");

    //   if (token) {
    //     config.headers.Authorization = `Bearer ${token}`;
    //     console.log("JWT attached");
    //   }
    // }

    //! Read the non-HttpOnly CSRF cookie set by Spring Security
    const csrfToken = Cookies.get("XSRF-TOKEN");

    if (
      csrfToken &&
      ["post", "put", "delete", "patch"].includes(config.method?.toLowerCase())
    ) {
      console.log("csrfToken ", csrfToken);
      config.headers["X-XSRF-TOKEN"] = csrfToken;
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
    if (!err.response) {
      console.error(
        "RESPONSE ERROR INTERCEPTOR: No response from server.",
        err.message,
      );
    } else {
      console.error(
        "RESPONSE ERROR INTERCEPTOR:",
        err.response.status,
        err.config?.url,
      );
    }

    if (err.response?.status === 401) {
      console.log("Authentication failed");
    }

    return Promise.reject(err);
  },
);
