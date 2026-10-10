import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import {
  fetchMyListings,
  publishListing,
  pauseListing,
  resumeListing,
  deleteListing,
} from "../listingService";
import { normalizeListing } from "../listingMappers";
import { focusRing } from "../components/create-listing/formStyles";
import DeleteListingDialog from "../components/edit-listings/DeleteListingDialog";
import MyListingCard from "../components/my-listings/MyListingCard";
import ListingFilters from "../components/my-listings/ListingFilters";

// normalizeListing may not carry the status fields, so add them from the raw response
const toRow = (raw) => ({
  ...normalizeListing(raw),
  status: raw.status,
  suspensionSource: raw.suspensionSource ?? null,
  suspensionReason: raw.suspensionReason ?? null,
  suspendedAt: raw.suspendedAt ?? null,
});

const ACTIONS = {
  publish: {
    call: publishListing,
    next: "PUBLISHED",
    success: "Listing published successfully!",
  },
  pause: {
    call: pauseListing,
    next: "PAUSED",
    success: "Listing paused. Guests can't see it until you resume.",
  },
  resume: {
    call: resumeListing,
    next: "PUBLISHED",
    success: "Listing is live again.",
  },
};

const MyListings = () => {
  const navigate = useNavigate();
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [busy, setBusy] = useState(null); // { id, action } while a status call runs
  const [toDelete, setToDelete] = useState(null); // listing waiting for confirmation
  const [deleting, setDeleting] = useState(false);

  // Filter States
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const res = await fetchMyListings();
        if (!ignore) setListings((res.data.data ?? []).map(toRow));
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

  // Computed/Filtered listings list
  const filteredListings = useMemo(() => {
    return listings.filter((l) => {
      const matchesSearch =
        !search ||
        l.title.toLowerCase().includes(search.toLowerCase()) ||
        (l.country && l.country.toLowerCase().includes(search.toLowerCase()));

      const matchesStatus = !statusFilter || l.status === statusFilter;
      const matchesCategory =
        !categoryFilter || l.category === categoryFilter;

      return matchesSearch && matchesStatus && matchesCategory;
    });
  }, [listings, search, statusFilter, categoryFilter]);

  const handleAction = async (listing, action) => {
    const cfg = ACTIONS[action];
    setBusy({ id: listing.id, action });

    try {
      const res = await cfg.call(listing.id);
      const returned = res.data?.data?.status; // use the server's status if it sends one

      setListings((prev) =>
        prev.map((l) =>
          l.id === listing.id
            ? {
                ...l,
                status: returned ?? cfg.next,
                suspensionReason: null,
                suspendedAt: null,
              }
            : l,
        ),
      );
      notify.success(cfg.success);
    } catch (err) {
      console.error(`${action} error:`, err);
      const status = err.response?.status;
      const message = handleApiError(err);

      if (status === 409 && /room/i.test(message)) {
        // "no rooms": point them to the edit page where rooms are added
        notify.error(message, {
          duration: 8000,
          action: {
            label: "Add rooms",
            onClick: () => navigate(`/listings/${listing.id}/edit`),
          },
        });
      } else {
        notify.error(message);
        // 403 usually means it was suspended after this page loaded: refresh the list
        if (status === 403) setReloadKey((k) => k + 1);
      }
    } finally {
      setBusy(null);
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
      notify.error(handleApiError(err));
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

        {/* Search & Filter Component integrated cleanly here */}
        {!loading && !error && listings.length > 0 && (
          <ListingFilters
            search={search}
            setSearch={setSearch}
            statusFilter={statusFilter}
            setStatusFilter={setStatusFilter}
            categoryFilter={categoryFilter}
            setCategoryFilter={setCategoryFilter}
          />
        )}

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
            {filteredListings.map((l) => (
              <MyListingCard
                key={l.id}
                listing={l}
                busyAction={busy?.id === l.id ? busy.action : null}
                onAction={handleAction}
                onDelete={setToDelete}
              />
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
