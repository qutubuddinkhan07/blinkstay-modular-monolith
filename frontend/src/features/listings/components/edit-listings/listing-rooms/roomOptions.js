// IMPORTANT: `value` must match your backend RoomCategory enum exactly.
// These four are placeholders: replace them with the real enum values.
export const ROOM_TYPES = [
  { value: "SINGLE", label: "Single" },
  { value: "DOUBLE", label: "Double" },
  { value: "DELUXE", label: "Deluxe" },
  { value: "SUITE", label: "Suite" },
];

export const CURRENCY = "INR"; // change if your prices are in another currency

// Falls back to a tidy version of the raw value if it isn't in the list above
export const roomTypeLabel = (value) => {
  const known = ROOM_TYPES.find((t) => t.value === value);
  if (known) return known.label;
  if (!value) return "Room";
  return value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, " ");
};
