import React from "react";

// Same outer dimensions as Card (image h-48 + body h-36) so nothing jumps on load
const SkeletonCard = () => (
  <div
    className="flex h-full w-full animate-pulse flex-col overflow-hidden rounded-2xl border border-border bg-surface motion-reduce:animate-none"
    aria-hidden="true"
  >
    <div className="h-48 w-full shrink-0 bg-border" />

    <div className="flex h-36 flex-col gap-2 p-4">
      <div className="h-4 w-4/5 rounded bg-border" />
      <div className="h-4 w-3/5 rounded bg-border" />
      <div className="mt-1 h-3 w-2/5 rounded bg-border" />

      <div className="mt-auto flex gap-1.5">
        <div className="h-5 w-14 rounded-md bg-border" />
        <div className="h-5 w-16 rounded-md bg-border" />
        <div className="h-5 w-12 rounded-md bg-border" />
      </div>
    </div>
  </div>
);

export default SkeletonCard;
