import React from "react";
import { FaArrowLeft, FaArrowRight } from "react-icons/fa";

// Builds e.g. [0, "…", 4, 5, 6, "…", 19] (0-indexed)
const getPageItems = (current, total) => {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const items = [0];
  const start = Math.max(1, current - 1);
  const end = Math.min(total - 2, current + 1);
  if (start > 1) items.push("…");
  for (let i = start; i <= end; i++) items.push(i);
  if (end < total - 2) items.push("…");
  items.push(total - 1);
  return items;
};

const circle =
  "flex h-10 w-10 items-center justify-center rounded-full text-sm font-medium transition-colors";

const Pagination = ({ page, totalPages, onChange }) => {
  if (totalPages <= 1) return null;
  return (
    <nav
      aria-label="Pagination"
      className="mt-10 flex flex-wrap items-center justify-center gap-2"
    >
      <button
        aria-label="Previous page"
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        className={`${circle} border border-gray-300 text-gray-700 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40`}
      >
        <FaArrowLeft />
      </button>

      {getPageItems(page, totalPages).map((p, i) =>
        p === "…" ? (
          <span key={`dots-${i}`} className="px-1 text-gray-400">
            …
          </span>
        ) : (
          <button
            key={p}
            onClick={() => onChange(p)}
            aria-current={p === page ? "page" : undefined}
            className={`${circle} ${
              p === page
                ? "bg-[#e01f59] text-white"
                : "border border-gray-300 text-gray-700 hover:bg-gray-100"
            }`}
          >
            {p + 1}
          </button>
        ),
      )}

      <button
        aria-label="Next page"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        className={`${circle} border border-gray-300 text-gray-700 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40`}
      >
        <FaArrowRight />
      </button>
    </nav>
  );
};

export default Pagination;
