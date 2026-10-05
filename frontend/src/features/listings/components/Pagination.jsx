import React from "react";
import { FiChevronLeft, FiChevronRight } from "react-icons/fi";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

// 0-based page indexes: first, last, and the current page with its neighbours
const getPages = (page, total) => {
  const set = new Set([0, total - 1, page - 1, page, page + 1]);
  const sorted = [...set]
    .filter((p) => p >= 0 && p < total)
    .sort((a, b) => a - b);

  const out = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) out.push(`gap-${p}`);
    out.push(p);
  });
  return out;
};

const base =
  "flex h-10 min-w-10 cursor-pointer items-center justify-center rounded-lg border px-3 text-sm font-medium transition-colors duration-200";

const Pagination = ({ page, totalPages, onChange }) => {
  if (totalPages <= 1) return null;

  return (
    <nav
      aria-label="Pagination"
      className="mt-10 flex flex-wrap items-center justify-center gap-2"
    >
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        aria-label="Previous page"
        className={`${base} border-border bg-surface text-text hover:bg-bg disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-surface ${focusRing}`}
      >
        <FiChevronLeft size={18} />
      </button>

      {getPages(page, totalPages).map((p) =>
        typeof p === "string" ? (
          <span key={p} className="px-1 text-subtext" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={p}
            type="button"
            onClick={() => onChange(p)}
            aria-current={p === page ? "page" : undefined}
            aria-label={`Page ${p + 1}`}
            className={`${base} ${focusRing} ${
              p === page
                ? "border-primary bg-primary text-white"
                : "border-border bg-surface text-text hover:bg-bg"
            }`}
          >
            {p + 1}
          </button>
        ),
      )}

      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="Next page"
        className={`${base} border-border bg-surface text-text hover:bg-bg disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-surface ${focusRing}`}
      >
        <FiChevronRight size={18} />
      </button>
    </nav>
  );
};

export default Pagination;
