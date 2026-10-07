import { useState } from "react";
import {
  cardClass,
  headingClass,
  inputClass,
  focusRing,
  secondaryButtonClass,
} from "./formStyles";

const AmenitySelector = ({ amenities = [], onChange }) => {
  const [input, setInput] = useState("");

  const addAmenity = () => {
    const value = input.trim();

    if (!value) return;

    if (
      amenities.some((amenity) => amenity.toLowerCase() === value.toLowerCase())
    ) {
      setInput("");
      return;
    }

    onChange([...amenities, value]);
    setInput("");
  };

  const removeAmenity = (amenityToRemove) => {
    onChange(amenities.filter((amenity) => amenity !== amenityToRemove));
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      addAmenity();
    }
  };

  return (
    <div className={cardClass}>
      <h2 className={headingClass}>Amenities</h2>

      <div className="flex gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="e.g. Wi-Fi"
          aria-label="Add an amenity"
          className={`flex-1 ${inputClass}`}
        />

        <button
          type="button"
          onClick={addAmenity}
          className={secondaryButtonClass}
        >
          Add
        </button>
      </div>

      {amenities.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-2">
          {amenities.map((amenity) => (
            <div
              key={amenity}
              className="flex items-center gap-2 rounded-full border border-border bg-bg px-3 py-1.5 text-sm text-text"
            >
              <span>{amenity}</span>

              <button
                type="button"
                onClick={() => removeAmenity(amenity)}
                aria-label={`Remove ${amenity}`}
                className={`cursor-pointer rounded-full font-bold leading-none text-subtext transition-colors hover:text-danger ${focusRing}`}
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AmenitySelector;
