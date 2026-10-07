import { useEffect, useState } from "react";
import { cardClass, headingClass, focusRing } from "./formStyles";

const fileKey = (f) => `${f.name}-${f.size}-${f.lastModified}`;

const ListingImageUploader = ({ images, onChange }) => {
  // Preview URLs live in state and are revoked on change/unmount,
  // instead of creating a new blob URL on every render.
  const [previews, setPreviews] = useState([]);

  useEffect(() => {
    const next = images.map((file) => ({
      file,
      url: URL.createObjectURL(file),
    }));
    setPreviews(next);

    return () => next.forEach((p) => URL.revokeObjectURL(p.url));
  }, [images]);

  const handleChange = (e) => {
    const picked = Array.from(e.target.files);
    const existing = new Set(images.map(fileKey));

    // Add to the current selection, skipping duplicates
    onChange([...images, ...picked.filter((f) => !existing.has(fileKey(f)))]);

    e.target.value = ""; // lets the same file be picked again after removing it
  };

  const removeImage = (index) => {
    onChange(images.filter((_, imageIndex) => imageIndex !== index));
  };

  return (
    <div className={cardClass}>
      <h2 className={headingClass}>Property images</h2>

      <input
        type="file"
        accept="image/*"
        multiple
        onChange={handleChange}
        aria-label="Upload property images"
        className={`block w-full cursor-pointer rounded-lg border border-border bg-input p-3 text-sm text-subtext file:mr-4 file:cursor-pointer file:rounded-lg file:border file:border-border file:bg-bg file:px-4 file:py-2 file:text-sm file:font-semibold file:text-text hover:file:bg-border/60 ${focusRing}`}
      />

      {images.length > 0 && (
        <p className="mt-3 text-sm text-subtext">
          {images.length} {images.length === 1 ? "image" : "images"} selected
        </p>
      )}

      <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
        {previews.map(({ file, url }, index) => (
          <div
            key={fileKey(file)}
            className="relative overflow-hidden rounded-lg border border-border bg-bg"
          >
            <img
              src={url}
              alt={file.name}
              className="h-32 w-full object-cover"
            />

            <button
              type="button"
              onClick={() => removeImage(index)}
              aria-label={`Remove ${file.name}`}
              className={`absolute right-2 top-2 flex h-7 w-7 cursor-pointer items-center justify-center rounded-full border border-border bg-surface/90 text-text backdrop-blur transition-colors hover:text-danger ${focusRing}`}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ListingImageUploader;
