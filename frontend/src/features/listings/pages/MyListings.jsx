import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { notify } from "../../../utils/notify";
import {
  useDeleteListingMutation,
  useGetMyListingsQuery,
  usePauseListingMutation,
  usePublishListingMutation,
  useResumeListingMutation,
} from "../../../store/api";
import { focusRing } from "../components/create-listing/formStyles";
import DeleteListingDialog from "../components/edit-listings/DeleteListingDialog";
import MyListingCard from "../components/my-listings/MyListingCard";

const SUCCESS = {
  publish: "Listing published successfully!",
  pause: "Listing paused. Guests can't see it until you resume.",
  resume: "Listing is live again.",
};

const MyListings = () => {
  const navigate = useNavigate();

  // refetchOnMountOrArgChange: a listing created or edited elsewhere shows up straight away,
  // while the cached list is still shown instantly in the meantime
  const {
    data: listings = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useGetMyListingsQuery(undefined, { refetchOnMountOrArgChange: true });

  const [publish] = usePublishListingMutation();
  const [pause] = usePauseListingMutation();
  const [resume] = useResumeListingMutation();
  const [removeListing, { isLoading: deleting }] = useDeleteListingMutation();
  const calls = { publish, pause, resume };

  const [busy, setBusy] = useState(null); // { id, action } while a status call runs
  const [toDelete, setToDelete] = useState(null); // listing waiting for confirmation

  const handleAction = async (listing, action) => {
    setBusy({ id: listing.id, action });

    try {
      await calls[action](listing.id).unwrap();
      notify.success(SUCCESS[action]);
    } catch (err) {
      const status = err?.status;
      const message = err?.message ?? "Something went wrong.";

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
        if (status === 403) refetch();
      }
    } finally {
      setBusy(null);
    }
  };

  const handleDelete = async () => {
    try {
      await removeListing(toDelete.id).unwrap();
      notify.success("Listing deleted.");
      setToDelete(null);
    } catch (err) {
      notify.error(err?.message ?? "Failed to delete listing.");
    }
  };

  // when a refetch fails but we still have a list, keep showing the list
  const showError = isError && listings.length === 0;

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

        {isLoading && (
          <ul className="space-y-4" aria-busy="true">
            {Array.from({ length: 3 }, (_, i) => (
              <li
                key={i}
                className="h-32 animate-pulse rounded-2xl border border-border bg-surface motion-reduce:animate-none"
              />
            ))}
          </ul>
        )}

        {!isLoading && showError && (
          <div className="py-16 text-center">
            <h2 className="font-display text-xl font-semibold">
              Couldn't load your listings
            </h2>
            <p className="mt-2 text-subtext">{error?.message}</p>
            <button
              type="button"
              onClick={refetch}
              className={`mt-5 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
            >
              Try again
            </button>
          </div>
        )}

        {!isLoading && !showError && listings.length === 0 && (
          <p className="py-16 text-center text-subtext">
            You haven't created any listings yet.
          </p>
        )}

        {!isLoading && !showError && listings.length > 0 && (
          <ul className="space-y-4">
            {listings.map((l) => (
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
