import React from "react";

// Safely turns a string / object / array into readable text
const toText = (v) => {
  if (!v) return "";

  if (typeof v === "string") return v;

  if (Array.isArray(v)) return v.map(toText).filter(Boolean).join(", ");

  if (typeof v === "object") {
    return [v.name, v.city, v.state].filter(Boolean).join(", ");
  }

  return String(v);
};

const MAX_AMENITIES = 3;

const Card = ({ listing }) => {
  const {
    id,
    address,
    amenities,
    category,
    country,
    coverImageUrl,
    title,
    location,
  } = listing;

  const place = [toText(location) || "toText(address), toText(country)"]
    .filter(Boolean)
    .join(", ");

  const amenityList = Array.isArray(amenities) ? amenities.map(toText) : [];
  const shown = amenityList.slice(0, MAX_AMENITIES);
  const extra = amenityList.length - shown.length;

  return (
    <article className="group flex h-full w-full flex-col overflow-hidden rounded-2xl bg-white border border-gray-200 transition-shadow hover:shadow-lg">
      {/* Fixed image height so every card matches */}
      <div className="relative h-48 w-full shrik-0 overflow-hidden bg-gray-100">
        {coverImageUrl ? (
          <img
            src={coverImageUrl}
            alt={title}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-sm text-gray-400">
            No Image
          </div>
        )}

        {category && (
          <span className="absolute left-3 top-3 max-w-[70%] truncate rounded-full bg-white/90 px-3 py-1 font-medium text-gray-800 shadow-sm backdrop-blur">
            {toText(category)}
          </span>
        )}
      </div>

      {/* Body: fiexd height, content is clamped instead of growing */}
      <div className="flex h-36 flex-col gap-1.5 p-4">
        <h3
          title={title}
          className="line-clamp-2 min-h-[2.73rem] text-base font-semibold leading-snug text-gray-900"
        >
          {title}
        </h3>

        <p
          className="flex items-center gap-1 truncate text-sm text-gray-500"
          title={place}
        >
          <svg
            viewBox="0 0 24 24"
            className="h-4 w-4 shrink-0"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            aria-hidden="true"
          >
            <path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11Z" />
            <circle cx="12" cy="10" r="2.5" />
          </svg>

          <span className="truncate">{place || "Location not listed"}</span>
        </p>

        {/* Amenities: one row, never wraps */}
        <div className="mt-auto flex flex-nowrap items-center gap-1.5 overflow-hidden">
          {shown.map((a, i) => (
            <span
              key={`${a}-${i}`}
              className="max-w-[7rem] truncate rounded-md bg-gray-100 px-2 py-0.5 text-xs text-gray-600"
              title={a}
            >
              {a.toUpperCase()}
            </span>
          ))}
          {extra > 0 && (
            <span className="shrink-0 text-xs font-medium text-[#e01f59]">
              +{extra} more
            </span>
          )}
        </div>
      </div>
    </article>
  );
};

export default Card;
