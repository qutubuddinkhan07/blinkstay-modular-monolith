import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import * as authService from "../features/auth/authService";
import { notify } from "../utils/notify";
import axiosInstance from "../api/axiosInstance";
import { setAuthFailureHandler } from "../api/axiosInterceptors";
import { useDispatch } from "react-redux";
import { api } from "../store/api";

const AuthContext = createContext(undefined);

// Written when an account is found to be blocked, read by BlockedPage
const clearBlockedNotes = () => {
  sessionStorage.removeItem("blockedMessage");
  sessionStorage.removeItem("blockedNotice");
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const updateUser = (nextUser) => setUser(nextUser);

  // Prevent concurrent duplicate calls to refreshUser
  const isRefreshingRef = useRef(false);

  const dispatch = useDispatch();
  const wasAuthRef = useRef(false);

  // Once the account is known to be blocked, nothing else may redirect or show "session expired"
  const blockedRef = useRef(false);

  // Whenever a logged-in session ends, drop every cached query
  useEffect(() => {
    const isAuth = Boolean(user);
    if (wasAuthRef.current && !isAuth) dispatch(api.util.resetApiState());
    wasAuthRef.current = isAuth;
  }, [user, dispatch]);

  // Stable identity: it is used in effect dependency lists elsewhere
  const refreshUser = useCallback(async () => {
    if (isRefreshingRef.current) return;
    isRefreshingRef.current = true;
    try {
      const res = await axiosInstance.get("/v2/user/me");
      setUser(res.data?.data ?? res.data);
    } catch (err) {
      console.error("Could not refresh the user", err);
    } finally {
      isRefreshingRef.current = false;
    }
  }, []);

  const clearSession = () => {
    localStorage.removeItem("hasSession");
    setUser(null);
  };

  const userRef = useRef(null);
  userRef.current = user;
  const loadingRef = useRef(true);
  loadingRef.current = loading;
  const expiredNoticeRef = useRef(false);

  // The axios interceptor reports a lost or blocked session through this
  useEffect(() => {
    setAuthFailureHandler(({ type, message }) => {
      if (blockedRef.current) return; // requests still in flight must not undo the blocked screen

      const hadSession =
        Boolean(userRef.current) || localStorage.getItem("hasSession") === "1";
      localStorage.removeItem("hasSession");

      if (type === "blocked") {
        blockedRef.current = true;
        sessionStorage.setItem("blockedMessage", message || "");
        sessionStorage.setItem("blockedNotice", "1"); // BlockedPage shows the toast after the reload
        setUser(null);
        window.location.replace("/blocked");
        return;
      }

      if (hadSession) {
        if (loadingRef.current) {
          expiredNoticeRef.current = true;
        } else {
          notify.info("Your session has expired. Please log in again.");
        }
      }
      setUser(null);
    });

    return () => setAuthFailureHandler(null);
  }, []);

  useEffect(() => {
    if (!loading && expiredNoticeRef.current) {
      expiredNoticeRef.current = false;
      notify.info("Your session has expired. Please log in again.");
    }
  }, [loading]);

  // Session re-hydration on app start / refresh with concurrency lock
  const hasInitializedRef = useRef(false);

  useEffect(() => {
    if (hasInitializedRef.current) return;
    hasInitializedRef.current = true;

    let ignore = false;

    const initializeAuth = async () => {
      try {
        await axiosInstance.get("/v1/auth/csrf");
      } catch (error) {
        console.error("CSRF fetch failed", error);
      }

      if (localStorage.getItem("hasSession") === "1") {
        try {
          const response = await axiosInstance.get("/v2/user/me");
          if (!ignore) setUser(response.data.data);
        } catch (error) {
          localStorage.removeItem("hasSession");
          // a blocked account gets its own screen (the interceptor redirects), so no "expired" toast
          if (
            error.response?.status === 401 &&
            error.response?.data?.code !== "ACCOUNT_BLOCKED"
          ) {
            expiredNoticeRef.current = true;
          }
          if (!ignore) setUser(null);
        }
      }

      if (!ignore) setLoading(false);
    };

    initializeAuth();

    return () => {
      ignore = true;
    };
  }, []);

  const loginUser = async (email, password) => {
    let response;
    try {
      response = await authService.login(email, password);
    } catch (error) {
      console.error("Login request failed", error);
      return { success: false, error };
    }

    localStorage.setItem("hasSession", "1");

    try {
      const profileResponse = await axiosInstance.get("/v2/user/me");
      setUser(profileResponse.data?.data ?? profileResponse.data);
    } catch (error) {
      console.error("Logged in, but loading the profile failed", error);
      localStorage.removeItem("hasSession");
      return { success: false, error };
    }

    // a successful login means any earlier block is over: forget its reason
    clearBlockedNotes();
    blockedRef.current = false;

    notify.success(response.data?.message || "Login successful!");
    return { success: true, error: null };
  };

  const logout = async () => {
    try {
      const response = await authService.logout();
      notify.success(response.data?.message || "Logged out successfully.");
    } catch (error) {
      console.error("Logout request failed:", error);
      notify.info("Your session has ended.");
    } finally {
      clearSession();
    }
  };

  const roles = user?.roles ?? [];
  const hasRole = (...wanted) => wanted.some((r) => roles.includes(r));

  const value = {
    user,
    isAuthenticated: Boolean(user),
    isAdmin: roles.includes("ADMIN"),
    isManager: roles.includes("HOTEL_MANAGER"),
    hasRole,
    loading,
    loginUser,
    logout,
    updateUser,
    refreshUser,
    clearSession,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

// Separated clearly to ensure Vite Fast Refresh compatibility
export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
