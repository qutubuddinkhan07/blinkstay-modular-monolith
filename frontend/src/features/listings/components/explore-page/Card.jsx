import React from "react";
import { Link, useNavigate } from "react-router-dom";
import ImageCarousel from "../../../../components/common/ImageCarousel";
import FavoriteButton from "../../../favorites/components/FavoriteButton";

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
  const navigate = useNavigate();
  const {
    id,
    address,
    amenities,
    category,
    country,
    coverImageUrl,
    imageUrls,
    images,
    title,
    location,
  } = listing;

  const to = `/listings/${id}`;

  // Use imageUrls first, then images, then coverImageUrl
  const slides =
    Array.isArray(imageUrls) && imageUrls.length > 0
      ? imageUrls
      : Array.isArray(images) && images.length > 0
        ? images
        : coverImageUrl
          ? [coverImageUrl]
          : [];

  // Use `location` if present, otherwise fall back to address + country
  const place =
    toText(location) ||
    [toText(address), toText(country)].filter(Boolean).join(", ");

  const amenityList = Array.isArray(amenities) ? amenities.map(toText) : [];
  const shown = amenityList.slice(0, MAX_AMENITIES);
  const extra = amenityList.length - shown.length;

  return (
    <article className="group relative flex h-full w-full flex-col overflow-hidden rounded-2xl border border-border bg-surface transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-text/10 has-[a:focus-visible]:outline-2 has-[a:focus-visible]:outline-offset-4 has-[a:focus-visible]:outline-primary motion-reduce:transition-none motion-reduce:hover:translate-y-0">
      {/* Image area sits above the stretched link, so the arrows are clickable.
          Clicking the picture itself opens the listing. */}
      <div
        onClick={() => navigate(to)}
        className="relative z-10 h-48 w-full shrink-0 cursor-pointer bg-bg"
      >
        <ImageCarousel
          images={slides}
          alt={title}
          zoomable={false}
          className="h-full w-full"
          imgClassName="object-cover h-full w-full"
          fallback={
            <div className="flex h-full items-center justify-center text-sm text-subtext">
              No image
            </div>
          }
        />

        {category && (
          <span className="pointer-events-none absolute left-3 top-3 max-w-[70%] truncate rounded-full border border-border bg-surface/90 px-3 py-1 text-xs font-medium text-text shadow-sm backdrop-blur">
            {toText(category)}
          </span>
        )}

        <FavoriteButton
          listingId={id}
          title={title}
          className="absolute right-3 top-3 z-20"
        />
      </div>

      {/* Body: fixed height, content is clamped instead of growing */}
      <div className="flex h-36 flex-col gap-1.5 p-4">
        <h3
          title={title}
          className="line-clamp-2 min-h-[2.73rem] font-display text-base font-medium leading-snug text-text"
        >
          {/* This link is stretched over the whole card (the after: overlay) */}
          <Link
            to={to}
            className="focus-visible:outline-none after:absolute after:inset-0"
          >
            {title}
          </Link>
        </h3>

        <p
          className="flex items-center gap-1 truncate text-sm text-subtext"
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
              className="max-w-[7rem] truncate rounded-md border border-border bg-bg px-2 py-0.5 text-xs text-subtext"
              title={a}
            >
              {a.toUpperCase()}
            </span>
          ))}
          {extra > 0 && (
            <span className="shrink-0 text-xs font-semibold text-primary">
              +{extra} more
            </span>
          )}
        </div>
      </div>
    </article>
  );
};

export default Card;
