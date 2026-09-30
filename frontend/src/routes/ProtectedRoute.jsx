import React from "react";
import { useAuth } from "../context/AuthContext";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import Loading from "../components/common/Loading";

const ProtectedRoute = () => {
  const { isAuthenticated, loading } = useAuth();

  const location = useLocation();

  // Show the global spinner during initial session re-hydration
  if (loading) {
    return <Loading />;
  }

  // If authenticated render matching child routes via <Outlet/>
  // If unauthenticated, redirect to /login and preserve state to redirect back after login
  return isAuthenticated ? (
    <Outlet />
  ) : (
    <Navigate to={"/login"} state={{ from: location }} replace />
  );
};

export default ProtectedRoute;
