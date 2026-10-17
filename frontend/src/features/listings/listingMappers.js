// Mapped to ListingDetailsResponseDto / ImageDto / RoomResponseDto from the backend.

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

// RoomResponseDto { roomId, listingId, roomType, price, totalRooms, maxGuests }
// availableRooms is deliberately not mapped any more. With bookings, how many rooms
// are free depends on the dates, so it comes from GET /api/v5/availability/{listingId}.
export const normalizeRoom = (r) => ({
  id: r.roomId,
  roomType: r.roomType ?? "",
  price: Number(r.price ?? 0), // BigDecimal arrives as JSON number
  totalRooms: r.totalRooms ?? 0, // rooms of this type, not guests
  maxGuests: r.maxGuests ?? 2, // guests PER ROOM, same default as the backend
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
  rooms: (l.rooms ?? []).map(normalizeRoom),
  coordinates: getCoordinates(l.geometry),
  address: l.geometry?.address ?? "",

  status: l.status,
  statusLabel: labelOf(l.status),
  isPublished: l.status === PUBLISHED,
  suspensionReason: l.suspensionReason ?? null,
  suspensionSource: l.suspensionSource ?? null,
  suspendedAt: l.suspendedAt ?? null,

  // RoomSummaryDto.totalRooms counts room TYPES (one row per type), so it is named for that.
  // The detail endpoint sends the full rooms list instead of a summary, so fall back to its length.
  roomTypeCount: l.roomSummary?.totalRooms ?? (l.rooms ?? []).length,
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

// Kept so existing imports still work: normalizeListing now carries every status field.
export const toListingRow = normalizeListing;
