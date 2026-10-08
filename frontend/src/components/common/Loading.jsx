import React from "react";

const Loading = () => {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg text-text">
      <div className="text-center">
        <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />

        <p className="mt-4 text-subtext">Loading Blinkstay...</p>
      </div>
    </div>
  );
};

export default Loading;
