import { useEffect, useRef, useState } from "react";
import { Link, useBlocker, useNavigate, useParams } from "react-router-dom";
import { FiArrowLeft } from "react-icons/fi";
import { handleApiError } from "../../../api/errors/handleApiError";
import {
  fetchListingById,
  updateListing,
  addListingImages,
  deleteListingImage,
  publishListing,
  deleteListing,
} from "../listingService";
import { focusRing } from "../components/create-listing/formStyles";
import { notify } from "../../../utils/notify";
import {
  normalizeImage,
  normalizeListing,
  toFormData,
} from "../components/edit-listings/listingMappers";
import UnsavedChangesDialog from "../components/edit-listings/UnsavedChangesDialog";
import DeleteListingDialog from "../components/edit-listings/DeleteListingDialog";
import EditListingForm from "../components/edit-listings/EditListingForm";

const pageClass =
  "min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300";

const EditListing = () => {
  const { listingId } = useParams();
  const navigate = useNavigate();

  const [listing, setListing] = useState(null);
  const [formData, setFormData] = useState(null);
  const [savedData, setSavedData] = useState(null); // last saved snapshot, for the "dirty" check

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const skipBlockRef = useRef(false); // lets us leave after a delete without the "unsaved" prompt

  /* ---------- Load ---------- */
  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const res = await fetchListingById(listingId);
        if (ignore) return;

        const normalized = normalizeListing(res.data.data);
        const form = toFormData(normalized);

        setListing(normalized);
        setFormData(form);
        setSavedData(form);
      } catch (err) {
        if (ignore) return;
        console.error("Failed to load listing", err);
        setError(handleApiError(err, "listing"));
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    return () => {
      ignore = true;
    };
  }, [listingId, reloadKey]);

  const dirty =
    formData && savedData
      ? JSON.stringify(formData) !== JSON.stringify(savedData)
      : false;

  /* Warn before closing the tab with unsaved edits */
  useEffect(() => {
    if (!dirty) return;
    const warn = (e) => e.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  /* Ask before leaving this page (back button, links, navbar) with unsaved edits.
     The beforeunload effect above only covers refresh / closing the tab. */
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      !skipBlockRef.current &&
      dirty &&
      currentLocation.pathname !== nextLocation.pathname,
  );

  /* ---------- Form handlers ---------- */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleLocationSelect = (loc) => {
    setFormData((prev) => ({
      ...prev,
      location: loc.location,
      country: loc.country || prev.country,
    }));
  };

  const handleAmenitiesChange = (amenities) => {
    setFormData((prev) => ({ ...prev, amenities }));
  };

  /* ---------- Save ---------- */
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.title.trim()) {
      notify.error("Title is required.");
      return;
    }
    if (!formData.location) {
      notify.error("Please select a location.");
      return;
    }

    try {
      setSaving(true);
      await updateListing(listingId, formData);
      setSavedData(formData);
      notify.success("Listing updated successfully!");
    } catch (err) {
      console.error("Update listing error:", err);
      notify.error(err.response?.data?.message || "Failed to update listing.");
    } finally {
      setSaving(false);
    }
  };

  /* ---------- Images (these hit the API immediately, independent of Save) ---------- */
  const handleUploadImages = async (files) => {
    try {
      const res = await addListingImages(listingId, files);
      const added = (res.data.data ?? []).map(normalizeImage);

      setListing((prev) => ({ ...prev, images: [...prev.images, ...added] }));
      notify.success(
        `${files.length} ${files.length === 1 ? "image" : "images"} added.`,
      );
    } catch (err) {
      console.error("Upload images error:", err);
      notify.error(err.response?.data?.message || "Failed to upload images.");
    }
  };

  const handleDeleteImage = async (imageId) => {
    try {
      await deleteListingImage(listingId, imageId);

      setListing((prev) => ({
        ...prev,
        images: prev.images.filter((img) => img.id !== imageId),
      }));
      notify.success("Image deleted.");
    } catch (err) {
      console.error("Delete image error:", err);
      notify.error(err.response?.data?.message || "Failed to delete image.");
    }
  };

  /* ---------- Publish ---------- */
  const handlePublish = async () => {
    try {
      setPublishing(true);
      await publishListing(listingId);
      setListing((prev) => ({
        ...prev,
        isPublished: true,
        statusLabel: "Published",
      }));
      notify.success("Listing published successfully!");
    } catch (err) {
      console.error("Publish error:", err);
      notify.error(err.response?.data?.message || "Failed to publish listing.");
    } finally {
      setPublishing(false);
    }
  };

  /* ---------- Delete ---------- */
  const handleDelete = async () => {
    try {
      setDeleting(true);
      await deleteListing(listingId);

      notify.success("Listing deleted.");
      skipBlockRef.current = true;
      navigate("/my-listings", { replace: true });
    } catch (err) {
      console.error("Delete listing error:", err);
      notify.error(err.response?.data?.message || "Failed to delete listing.");
      setDeleting(false);
      setShowDelete(false);
    }
  };

  /* ---------- States ---------- */
  if (loading) {
    return (
      <div className={pageClass}>
        <div className="mx-auto max-w-7xl animate-pulse motion-reduce:animate-none">
          <div className="mb-8 h-9 w-56 rounded bg-border" />
          <div className="grid gap-8 lg:grid-cols-2">
            <div className="space-y-6">
              <div className="h-96 rounded-2xl border border-border bg-surface" />
              <div className="h-40 rounded-2xl border border-border bg-surface" />
            </div>
            <div className="h-[34rem] rounded-2xl border border-border bg-surface" />
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-[calc(100vh-72px)] items-center justify-center bg-bg px-4 text-text">
        <div className="max-w-md text-center">
          <h2 className="font-display text-2xl font-semibold">
            Couldn't load this listing
          </h2>
          <p className="mt-2 text-subtext">{error}</p>
          <button
            type="button"
            onClick={() => setReloadKey((k) => k + 1)}
            className={`mt-5 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
          >
            Try again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={pageClass}>
      {blocker.state === "blocked" && (
        <UnsavedChangesDialog
          onStay={() => blocker.reset()}
          onLeave={() => blocker.proceed()}
        />
      )}

      <div className="mx-auto max-w-7xl">
        <Link
          to="/my-listings"
          className={`inline-flex items-center gap-2 rounded text-sm font-medium text-subtext transition-colors hover:text-text ${focusRing}`}
        >
          <FiArrowLeft size={16} />
          My listings
        </Link>

        <div className="mb-8 mt-3 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="font-display text-3xl font-semibold text-text">
              Edit listing
            </h1>
            <p className="mt-2 text-subtext">
              Update the details, amenities, location and images.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <span
              className={`rounded-full border px-3 py-1 text-xs font-semibold ${
                listing.isPublished
                  ? "border-primary text-primary"
                  : "border-border text-subtext"
              }`}
            >
              {listing.statusLabel}
            </span>

            {!listing.isPublished && (
              <button
                type="button"
                onClick={handlePublish}
                disabled={publishing || dirty}
                title={
                  dirty ? "Save your changes before publishing" : undefined
                }
                className={`cursor-pointer rounded-lg border border-border bg-surface px-4 py-2 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-surface ${focusRing}`}
              >
                {publishing ? "Publishing..." : "Publish"}
              </button>
            )}
          </div>
        </div>

        <EditListingForm
          formData={formData}
          images={listing.images}
          initialCoordinates={listing.coordinates}
          initialAddress={listing.address}
          saving={saving}
          dirty={dirty}
          onChange={handleChange}
          onLocationSelect={handleLocationSelect}
          onAmenitiesChange={handleAmenitiesChange}
          onUploadImages={handleUploadImages}
          onDeleteImage={handleDeleteImage}
          onSubmit={handleSubmit}
        />

        {/* Danger zone: far from Save, so it can't be hit by accident */}
        <section className="mt-12 rounded-2xl border border-danger/40 bg-surface p-6">
          <h2 className="font-display text-xl font-medium text-text">
            Delete listing
          </h2>
          <p className="mt-2 text-sm text-subtext">
            Permanently removes this listing and all of its images. This can't
            be undone.
          </p>
          <button
            type="button"
            onClick={() => setShowDelete(true)}
            className={`mt-4 cursor-pointer rounded-lg border border-danger px-4 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 ${focusRing}`}
          >
            Delete listing
          </button>
        </section>

        {showDelete && (
          <DeleteListingDialog
            title={listing.title}
            deleting={deleting}
            onCancel={() => setShowDelete(false)}
            onConfirm={handleDelete}
          />
        )}
      </div>
    </div>
  );
};

export default EditListing;
