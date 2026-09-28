import React from "react";

const SkeletonCard = () => {
  return (
    <div className="h-[21rem] animate-pulse overflow-hidden rounded-2xl border border-gray-200 bg-white">
      <div className="h-48 bg-gray-200" />
      <div className="space-y-3 p-4">
        <div className="h-4 w-3/4 rounded bg-gray-200" />
        <div className="h-4 w-1/2 rounded bg-gray-200" />
      </div>
    </div>
  );
};

export default SkeletonCard;
