import {
  Children,
  createContext,
  useContext,
  useEffect,
  useState,
} from "react";
import * as authService from "../features/auth/authService";
import { notify } from "../utils/notify";
import axiosInstance from "../api/axiosInstance";
import { Navigate } from "react-router-dom";

const AuthContext = createContext(undefined);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Session Re-hydration: Check authentication status on app start/refresh
  useEffect(() => {
    const initializeAuth = async () => {
      let ignore = false;

      try {
        // First re-hydrate CSRF token
        await axiosInstance.get("/api/v1/auth/csrf");
      } catch (error) {
        console.error("CSRF fetch failed", error);
      }

      // Only ask the server if this browser has logged in before
      if (localStorage.getItem("hasSession") === "1") {
        try {
          // Fetch active user details (using BLINKSTAY_TOKEN cookie sent automatically)
          const response = await axiosInstance.get("/api/v2/user/me");
          if (!ignore) setUser(response.data.data);
        } catch (error) {
          // User is not authenticated or cookie has expired
          localStorage.removeItem("hasSession"); // cookie expired
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
    try {
      const response = await authService.login(email, password);

      // Fetch profile details immediately after successful login
      const profileResponse = await axiosInstance.get("/api/v2/user/me");
      setUser(profileResponse.data?.data || profileResponse.data);
      localStorage.setItem("hasSession", "1"); // acts as a flag

      notify.success(response.data?.message || "Login successful!");

      return {
        success: true,
        error: null,
      };
    } catch (error) {
      console.error("Login failed", error);
      return {
        success: false,
        error,
      };
    }
  };

  const logout = async () => {
    try {
      const response = await authService.logout();

      notify.success(response.data?.message || "Logged out successfully.");
    } catch (error) {
      console.error("Logout request failed:", error);

      notify.info("Your session has ended.");
    } finally {
      setUser(null);
    }
  };

  const value = {
    user,
    isAuthenticated: Boolean(user),
    loading,
    loginUser,
    logout,
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
