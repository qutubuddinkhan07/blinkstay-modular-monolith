import { Children, createContext, useContext, useState } from "react";
import * as authService from "../features/auth/authService";
import { notify } from "../utils/notify";

const AuthContext = createContext(undefined);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => {
    return localStorage.getItem("token");
  });

  const isAuthenticated = Boolean(token);

  const loginUser = async (email, password) => {
    try {
      const response = await authService.login(email, password);
      const newToken = response.data.data;

      localStorage.setItem("token", newToken);
      setToken(newToken);

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
      localStorage.removeItem("token");
      setToken(null);
    }
  };

  const value = {
    token,
    isAuthenticated,
    loginUser,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
};
