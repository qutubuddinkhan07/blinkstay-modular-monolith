import React from "react";

const Loading = () => {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin h-10 w-10 border-4 border-[#ff2768] border-t-transparent rounded-full mx-auto" />

        <p className="mt-4 text-gray-500">Loading Blinkstay...</p>
      </div>
    </div>
  );
};

export default Loading;
