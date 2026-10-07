// Mapped to ListingDetailsResponseDto / ImageDto from the backend.

const PUBLISHED = "PUBLISHED"; // value of your ListingStatus enum for a live listing

const labelOf = (status) =>
  status
    ? status.charAt(0) + status.slice(1).toLowerCase().replace(/_/g, " ")
    : "Draft";

// GeometryDto { address, latitude, longitude }  (BigDecimal arrives as a JSON number)
export const getCoordinates = (g) => {
  if (g?.latitude == null || g?.longitude == null) return null;

  const latitude = Number(g.latitude);
  const longitude = Number(g.longitude);

  return Number.isFinite(latitude) && Number.isFinite(longitude)
    ? { latitude, longitude }
    : null;
};

// ImageDto { id, imageUrl, publicId, displayOrder }
export const normalizeImage = (img) => ({
  id: img.id, // needed for DELETE /{listingId}/images/{imageId}
  url: img.imageUrl,
  displayOrder: img.displayOrder ?? 0,
});

const byDisplayOrder = (a, b) => a.displayOrder - b.displayOrder;

// ListingDetailsResponseDto
export const normalizeListing = (l) => ({
  id: l.id,
  title: l.title ?? "",
  description: l.description ?? "",
  category: l.category ?? "HOTEL",
  country: l.country ?? "INDIA",
  location: l.location ?? "",
  amenities: l.amenities ?? [],
  images: (l.images ?? []).map(normalizeImage).sort(byDisplayOrder),
  coordinates: getCoordinates(l.geometry),
  address: l.geometry?.address ?? "",
  status: l.status,
  statusLabel: labelOf(l.status),
  isPublished: l.status === PUBLISHED,
});

// Only the fields sent on PUT /{listingId} (AddListingDto)
export const toFormData = (l) => ({
  title: l.title,
  location: l.location,
  description: l.description,
  country: l.country,
  amenities: l.amenities,
  category: l.category,
});
