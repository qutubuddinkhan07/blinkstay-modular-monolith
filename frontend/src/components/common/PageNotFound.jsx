import React from "react";
import { Link } from "react-router-dom";

const PageNotFound = () => {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full text-center">
        <h1 className="text-4xl font-bold text-[#002642]">404</h1>

        <h2 className="mt-4 text-2xl font-bold text-gray-800">
          Page not found
        </h2>

        <p className="mt-3 text-gray-500">
          Sorry, the page you're looking for doesn't exist.
        </p>

        <Link
          to={"/"}
          className="inline-block mt-6 px-6 py-3 
            bg-[#ff2768] text-white rounded-lg font-medium hover:bg-[#f90e54] transition"
        >
          Back to Home
        </Link>
      </div>
    </div>
  );
};

export default PageNotFound;
