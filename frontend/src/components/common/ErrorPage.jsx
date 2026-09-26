import React from "react";
import { Link, useRouteError } from "react-router-dom";

const ErrorPage = () => {
  const error = useRouteError();

  console.error("Route Error: ", error);

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full text-center bg-white rounded-2xl shadow-lg p-8">
        <div className="text-6xl mb-4">😕</div>

        <h1 className="text-2xl font-bold text-gray-800 mb-3">
          Something went wrong
        </h1>

        <p className="text-gray-500 mb-6">
          We couldn't load this page right now. Please try again or return to
          the home page.
        </p>

        <div className="flex justify-center gap-3">
          <button
            onClick={() => window.location.reload()}
            className="px-5 py-2.5 rounded-lg bg-[#ff2768] text-white font-medium hover:bg-[#f90e54] transition"
          >
            Try Again
          </button>

          <Link
            to="/"
            className="px-5 py-2.5 rounded-lg border border-gray-300
                       text-gray-700 font-medium hover:bg-gray-100 transition"
          >
            Go Home
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ErrorPage;
