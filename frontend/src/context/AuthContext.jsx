import { createContext, useContext, useEffect, useRef, useState } from "react";
import * as authService from "../features/auth/authService";
import { notify } from "../utils/notify";
import axiosInstance from "../api/axiosInstance";
import { setAuthFailureHandler } from "../api/axiosInterceptors";

const AuthContext = createContext(undefined);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const updateUser = (nextUser) => setUser(nextUser);

  const clearSession = () => {
    localStorage.removeItem("hasSession");
    setUser(null);
  };

  const userRef = useRef(null);
  userRef.current = user;

  // Lets the axios interceptor tell React that the session is gone or blocked
  useEffect(() => {
    setAuthFailureHandler(({ type, message }) => {
      localStorage.removeItem("hasSession");

      if (type === "blocked") {
        sessionStorage.setItem("blockedMessage", message || "");
        setUser(null);
        window.location.replace("/blocked"); // full reload: clean slate
        return;
      }

      // Only tell people who were actually logged in.
      // ProtectedRoute redirects to /login once user becomes null.
      if (userRef.current) {
        notify.info("Your session has expired. Please log in again.");
      }
      setUser(null);
    });

    return () => setAuthFailureHandler(null);
  }, []);

  // Session re-hydration on app start / refresh
  useEffect(() => {
    let ignore = false; // must live in the effect scope so the cleanup can see it

    const initializeAuth = async () => {
      try {
        await axiosInstance.get("/api/v1/auth/csrf");
      } catch (error) {
        console.error("CSRF fetch failed", error);
      }

      // Only ask the server if this browser has logged in before
      if (localStorage.getItem("hasSession") === "1") {
        try {
          const response = await axiosInstance.get("/api/v2/user/me");
          if (!ignore) setUser(response.data.data);
        } catch {
          localStorage.removeItem("hasSession"); // cookie expired or revoked
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

    // Step 1: the login request itself
    try {
      response = await authService.login(email, password);
    } catch (error) {
      console.error("Login request failed", error);
      return { success: false, error };
    }

    // The server accepted us and set the cookie: remember that right away
    localStorage.setItem("hasSession", "1");

    // Step 2: load the profile, reported separately so we know which step failed
    try {
      const profileResponse = await axiosInstance.get("/api/v2/user/me");
      setUser(profileResponse.data?.data ?? profileResponse.data);
    } catch (error) {
      console.error("Logged in, but loading the profile failed", error);
      localStorage.removeItem("hasSession");
      return { success: false, error };
    }

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
    clearSession,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
};
