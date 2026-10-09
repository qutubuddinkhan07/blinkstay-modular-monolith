import React from "react";
import { useAuth } from "../context/AuthContext";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import Loading from "../components/common/Loading";

const GuestRoute = () => {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) return <Loading />;

  if (isAuthenticated) {
    const to = location.state?.from?.pathname ?? "/explore";
    return <Navigate to={to} replace />;
  }
  return <Outlet />;
};

export default GuestRoute;
