import axiosInstance from "./axiosInstance";
import Cookies from "js-cookie";

const MUTATING = ["post", "put", "patch", "delete"];

// 401 codes that mean "your session is gone"
const SESSION_CODE = [
  "TOKEN_EXPIRED",
  "TOKEN_INVALID",
  "USER_NOT_FOUND",
  "UNAUTHENTICATED",
];

// A 401 here is an expected failure (wrong password, etc.), not a lost session
const SKIP_401_URLS = [
  "/api/v1/auth/login",
  "/api/v1/auth/register",
  "/api/v2/user/register-init",
  "/api/v2/user/verify-otp",
  "/api/v3/listings/all",
  "/api/v1/auth/logout",
];

const CSRF_URL = "/auth.v1/auth/csrf";

// AuthContext registers this on mount, so the interceptor can reach React state
let authFailureHandler = null;
export const setAuthFailureHandler = (fn) => {
  authFailureHandler = fn;
};

// One CSRF refresh at a time, even if several requests fail together
let csrfRefresh = null;
const refreshCsrf = () => {
  if (!csrfRefresh) {
    csrfRefresh = axiosInstance.get(CSRF_URL).finally(() => {
      csrfRefresh = null;
    });
  }
  return csrfRefresh;
};

const urlMatches = (url = "", list) => list.some((u) => url.startsWith(u));

axiosInstance.interceptors.request.use(
  (config) => {
    // console.log("REQUEST INTERCEPTOR:", config.method, config.url);

    //! Read the non-HttpOnly CSRF cookie set by Spring Security
    const csrfToken = Cookies.get("XSRF-TOKEN");

    if (csrfToken && MUTATING.includes(config.method?.toLowerCase())) {
      // console.log("csrfToken ", csrfToken);
      config.headers["X-XSRF-TOKEN"] = csrfToken;
    }

    return config;
  },
  (err) => {
    // console.log("REQUEST ERROR INTERCEPTOR");
    return Promise.reject(err);
  },
);

axiosInstance.interceptors.response.use(
  (response) => {
    // console.log("RESPONSE INTERCEPTOR:", response.status, response.config.url);
    return response;
  },
  async (err) => {
    const { response, config } = err;

    if (!response) return Promise.reject(err); // network error, caller shows a message

    const { status, data } = response;
    const code = data?.code;
    const message = data?.message;

    // 403 CSRF_INVALID: get a fresh token, retry the request once
    if (
      status === 403 &&
      code === "CSRF_INVALID" &&
      config &&
      !config._csrfRetired &&
      !config.url?.startsWith(CSRF_URL)
    ) {
      config._csrfRetried = true;
      try {
        await refreshCsrf();
        return axiosInstance(config); // request interceptor re-attaches the new token
      } catch {
        return Promise.reject(err);
      }
    }

    // 401: lost session or blocked account
    if (status === 401 && !urlMatches(config?.url, SKIP_401_URLS)) {
      if (code === "ACCOUNT_BLOCKED") {
        authFailureHandler?.({ type: "blocked", message });
      } else if (!code || SESSION_CODES.includes(code)) {
        authFailureHandler?.({ type: "expired", message });
      }
    }

    // 400, 403 (other), 404, 409 etc.: the caller shows the message
    return Promise.reject(err);
  },
);
