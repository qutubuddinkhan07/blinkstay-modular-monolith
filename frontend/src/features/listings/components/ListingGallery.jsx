import React from "react";

/**
 * Airbnb-style gallery:
 *  - col 1 (big, full height)
 *  - col 2 + col 3 (2 stacked images each)
 *
 * Implemented as a 4-column grid where the first image spans 2 columns
 * and 2 rows, so it is visually "one big column + two small columns".
 */
const ListingGallery = ({ images = [], title }) => {
  const sorted = [...images].sort(
    (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0),
  );
  const [main, ...rest] = sorted;
  const small = rest.slice(0, 4);

  if (!main) {
    return (
      <div className="flex h-72 items-center justify-center rounded-2xl bg-neutral-100 text-neutral-500">
        No photos yet
      </div>
    );
  }

  return (
    <div className="grid h-[240px] grid-cols-4 grid-rows-2 gap-2 overflow-hidden rounded-2xl sm:h-[360px] lg:h-[460px]">
      {/* Big image: 2 cols wide, full height */}
      <img
        src={main.imageUrl}
        alt={title}
        className={`h-full w-full object-cover ${
          small.length === 0 ? "col-span-4" : "col-span-2"
        } row-span-2`}
      />

      {/* Up to 4 small images, 2 per column */}
      {small.map((img, i) => (
        <img
          key={img.id}
          src={img.imageUrl}
          alt={`${title} photo ${i + 2}`}
          className="h-full w-full object-cover"
          loading="lazy"
        />
      ))}

      {/* Keep the grid shape if there are fewer than 5 images */}
      {Array.from({ length: Math.max(0, 4 - small.length) }, (_, i) =>
        small.length > 0 ? (
          <div key={`empty-${i}`} className="bg-neutral-100" />
        ) : null,
      )}
    </div>
  );
};

export default ListingGallery;
