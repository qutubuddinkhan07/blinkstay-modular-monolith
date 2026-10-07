import { useState } from "react";
import {
  cardClass,
  headingClass,
  focusRing,
} from "../create-listing/formStyles";

const ListingImagesManager = ({ images, onUpload, onDelete }) => {
  const [confirmId, setConfirmId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [uploading, setUploading] = useState(false);

  const isLast = images.length <= 1; // a listing must keep at least one image

  const handlePick = async (e) => {
    const files = Array.from(e.target.files);
    e.target.value = ""; // allow picking the same file again later
    if (files.length === 0) return;

    setUploading(true);
    try {
      await onUpload(files);
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (imageId) => {
    setDeletingId(imageId);
    try {
      await onDelete(imageId);
    } finally {
      setDeletingId(null);
      setConfirmId(null);
    }
  };

  return (
    <div className={cardClass}>
      <h2 className={headingClass}>Property images</h2>

      {images.length === 0 ? (
        <p className="mb-4 text-sm text-subtext">No images yet.</p>
      ) : (
        <div className="mb-5 grid grid-cols-2 gap-4 sm:grid-cols-3">
          {images.map((image) => {
            const confirming = confirmId === image.id;
            const deleting = deletingId === image.id;

            return (
              <div
                key={image.id}
                className={`relative overflow-hidden rounded-lg border border-border bg-bg transition-opacity ${
                  deleting ? "opacity-50" : ""
                }`}
              >
                <img
                  src={image.url}
                  alt="Listing"
                  loading="lazy"
                  className="h-32 w-full object-cover"
                />

                {confirming ? (
                  <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-surface/90 p-2 backdrop-blur">
                    <p className="text-xs font-medium text-text">
                      Delete this image?
                    </p>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => handleDelete(image.id)}
                        disabled={deleting}
                        className={`cursor-pointer rounded-md bg-danger px-3 py-1 text-xs font-semibold text-white disabled:opacity-60 ${focusRing}`}
                      >
                        {deleting ? "Deleting..." : "Delete"}
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmId(null)}
                        disabled={deleting}
                        className={`cursor-pointer rounded-md border border-border bg-bg px-3 py-1 text-xs font-semibold text-text ${focusRing}`}
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => setConfirmId(image.id)}
                    disabled={isLast || deleting}
                    aria-label="Delete image"
                    title={
                      isLast
                        ? "A listing needs at least one image"
                        : "Delete image"
                    }
                    className={`absolute right-2 top-2 flex h-7 w-7 cursor-pointer items-center justify-center rounded-full border border-border bg-surface/90 text-text backdrop-blur transition-colors hover:text-danger disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:text-text ${focusRing}`}
                  >
                    ×
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      <label
        htmlFor="add-images"
        className="mb-2 block text-sm font-medium text-text"
      >
        Add more images
      </label>
      <input
        id="add-images"
        type="file"
        accept="image/*"
        multiple
        onChange={handlePick}
        disabled={uploading}
        className={`block w-full cursor-pointer rounded-lg border border-border bg-input p-3 text-sm text-subtext file:mr-4 file:cursor-pointer file:rounded-lg file:border file:border-border file:bg-bg file:px-4 file:py-2 file:text-sm file:font-semibold file:text-text hover:file:bg-border/60 disabled:cursor-wait disabled:opacity-60 ${focusRing}`}
      />
      <p className="mt-2 text-sm text-subtext" aria-live="polite">
        {uploading ? "Uploading..." : "Images upload as soon as you pick them."}
      </p>
    </div>
  );
};

export default ListingImagesManager;
