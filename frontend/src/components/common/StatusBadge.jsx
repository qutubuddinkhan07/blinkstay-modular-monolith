import React from "react";

export const STATUS = {
  DRAFT: { label: "Draft", dot: "bg-subtext" },
  PUBLISHED: { label: "Live", dot: "bg-emerald-500" },
  PAUSED: { label: "Paused", dot: "bg-amber-500" },
  SUSPENDED: { label: "Suspended", dot: "bg-danger" },
};

const StatusBadge = ({ status }) => {
  const s = STATUS[status] ?? { label: status ?? "Unknown", dot: "bg-subtext" };

  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-border px-2.5 py-0.5 text-xs font-semibold text-text">
      <span className={`h-2 w-2 rounded-full ${s.dot}`} aria-hidden="true" />
      {s.label}
    </span>
  );
};

export default StatusBadge;
