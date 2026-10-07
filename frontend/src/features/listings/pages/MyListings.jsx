import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { handleApiError } from "../../../api/errors/handleApiError";
import {
  fetchMyListings,
  publishListing,
  deleteListing,
} from "../listingService";
import { focusRing } from "../components/create-listing/formStyles";
import DeleteListingDialog from "../components/edit-listings/DeleteListingDialog";
import { notify } from "../../../utils/notify";
import { normalizeListing } from "../components/edit-listings/listingMappers";

const smallBtn = `cursor-pointer rounded-lg border border-border bg-bg px-3 py-2 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

const dangerBtn = `cursor-pointer rounded-lg border border-danger/50 bg-bg px-3 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

const MyListings = () => {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [publishingId, setPublishingId] = useState(null);
  const [toDelete, setToDelete] = useState(null); // listing waiting for confirmation
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const res = await fetchMyListings();
        if (!ignore) setListings((res.data.data ?? []).map(normalizeListing));
      } catch (err) {
        if (ignore) return;
        console.error("Failed to load my listings", err);
        setError(handleApiError(err, "listings"));
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const handlePublish = async (id) => {
    try {
      setPublishingId(id);
      await publishListing(id);

      setListings((prev) =>
        prev.map((l) =>
          l.id === id
            ? { ...l, isPublished: true, statusLabel: "Published" }
            : l,
        ),
      );
      notify.success("Listing published successfully!");
    } catch (err) {
      console.error("Publish error:", err);
      notify.error(err.response?.data?.message || "Failed to publish listing.");
    } finally {
      setPublishingId(null);
    }
  };

  const handleDelete = async () => {
    try {
      setDeleting(true);
      await deleteListing(toDelete.id);

      setListings((prev) => prev.filter((l) => l.id !== toDelete.id));
      notify.success("Listing deleted.");
      setToDelete(null);
    } catch (err) {
      console.error("Delete error:", err);
      notify.error(err.response?.data?.message || "Failed to delete listing.");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-5xl">
        <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="font-display text-3xl font-semibold">My listings</h1>
            <p className="mt-2 text-subtext">
              Edit, publish and manage the properties you own.
            </p>
          </div>

          <Link
            to="/create-listing"
            className={`rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
          >
            Add listing
          </Link>
        </div>

        {loading && (
          <ul className="space-y-4" aria-busy="true">
            {Array.from({ length: 3 }, (_, i) => (
              <li
                key={i}
                className="h-32 animate-pulse rounded-2xl border border-border bg-surface motion-reduce:animate-none"
              />
            ))}
          </ul>
        )}

        {!loading && error && (
          <div className="py-16 text-center">
            <h2 className="font-display text-xl font-semibold">
              Couldn't load your listings
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
        )}

        {!loading && !error && listings.length === 0 && (
          <p className="py-16 text-center text-subtext">
            You haven't created any listings yet.
          </p>
        )}

        {!loading && !error && listings.length > 0 && (
          <ul className="space-y-4">
            {listings.map((l) => (
              <li
                key={l.id}
                className="flex flex-col gap-4 rounded-2xl border border-border bg-surface p-4 transition-colors duration-300 sm:flex-row sm:items-center"
              >
                <div className="h-32 w-full shrink-0 overflow-hidden rounded-lg bg-bg sm:h-24 sm:w-36">
                  {l.images[0]?.url ? (
                    <img
                      src={l.images[0].url}
                      alt=""
                      loading="lazy"
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center text-sm text-subtext">
                      No image
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="truncate font-display text-lg font-medium">
                      {l.title}
                    </h2>
                    <span
                      className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${
                        l.isPublished
                          ? "border-primary text-primary"
                          : "border-border text-subtext"
                      }`}
                    >
                      {l.statusLabel}
                    </span>
                  </div>

                  <p className="mt-1 truncate text-sm text-subtext">
                    {[l.location, l.country].filter(Boolean).join(", ") ||
                      "Location not listed"}
                  </p>
                  <p className="mt-0.5 text-xs capitalize text-subtext">
                    {l.category.toLowerCase()}
                  </p>
                </div>

                <div className="flex flex-wrap gap-2">
                  <Link to={`/listings/${l.id}`} className={smallBtn}>
                    View
                  </Link>
                  <Link to={`/listings/${l.id}/edit`} className={smallBtn}>
                    Edit
                  </Link>
                  {!l.isPublished && (
                    <button
                      type="button"
                      onClick={() => handlePublish(l.id)}
                      disabled={publishingId === l.id}
                      className={smallBtn}
                    >
                      {publishingId === l.id ? "Publishing..." : "Publish"}
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => setToDelete(l)}
                    className={dangerBtn}
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {toDelete && (
          <DeleteListingDialog
            title={toDelete.title}
            deleting={deleting}
            onCancel={() => setToDelete(null)}
            onConfirm={handleDelete}
          />
        )}
      </div>
    </div>
  );
};

export default MyListings;
