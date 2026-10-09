import React from "react";
import { useAuth } from "../context/AuthContext";
import { Link, Navigate, Outlet, useLocation } from "react-router-dom";
import Loading from "../components/common/Loading";

const Forbidden = () => (
  <div className="flex min-h-[calc(100vh-72px)] items-center justify-center bg-bg px-4 text-text">
    <div className="max-w-md text-center">
      <h1 className="font-display text-2xl font-semibold">
        You don't have access to this page
      </h1>
      <p className="mt-2 text-subtext">
        Your account doesn't have the role needed to view this page.
      </p>
      <Link
        to="/explore"
        className="mt-5 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        Back to explore
      </Link>
    </div>
  </div>
);

/**
 * <ProtectedRoute />                              -> any logged-in user
 * <ProtectedRoute allowedRoles={["ADMIN"]} />     -> logged in AND has at least one of these roles
 *
 * This only decides what the UI shows. The backend's @PreAuthorize is the real security.
 */
const ProtectedRoute = ({ allowedRoles }) => {
  const { isAuthenticated, loading, user } = useAuth();

  const location = useLocation();

  // Wait for the session to be re-hydrated deciding anything
  if (loading) return <Loading />;

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles?.length) {
    const roles = user?.roles ?? [];
    const allowed = allowedRoles.some((role) => roles.includes(role));
    if (!allowed) return <Forbidden />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
