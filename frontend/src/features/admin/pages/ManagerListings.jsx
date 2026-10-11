import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { FiArrowLeft } from "react-icons/fi";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import { fetchListingsByManager } from "../../listings/listingService";
import { toListingRow } from "../../listings/listingMappers";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import MyListingCard from "../../listings/components/my-listings/MyListingCard";

const ManagerListings = () => {
  const { managerId } = useParams();
  const { state } = useLocation();
  const owner = state?.username; // sent by the Users table link; absent on a direct visit

  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const reqRef = useRef(0); // only the latest request may update state

  // silent = refetch after a moderation action, without the loading skeleton
  const load = useCallback(
    async (silent = false) => {
      const req = ++reqRef.current;

      if (!silent) {
        setLoading(true);
        setError(null);
      }

      try {
        const res = await fetchListingsByManager(managerId);
        if (req === reqRef.current) {
          setListings((res.data.data ?? []).map(toListingRow));
        }
      } catch (err) {
        if (req !== reqRef.current) return;
        console.error("Failed to load manager listings", err);
        if (silent) notify.error(handleApiError(err));
        else setError(handleApiError(err, "listings"));
      } finally {
        if (req === reqRef.current) setLoading(false);
      }
    },
    [managerId],
  );

  useEffect(() => {
    load();
    return () => {
      reqRef.current++; // ignore any request still in flight
    };
  }, [load]);

  const suspendedCount = listings.filter(
    (l) => l.status === "SUSPENDED",
  ).length;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-5xl">
        <Link
          to="/admin/users"
          className={`mb-4 inline-flex items-center gap-1.5 rounded-md text-sm font-medium text-subtext transition-colors hover:text-text ${focusRing}`}
        >
          <FiArrowLeft size={16} aria-hidden="true" />
          Back to users
        </Link>

        <div className="mb-8">
          <h1 className="font-display text-3xl font-semibold">
            {owner ? `Listings by ${owner}` : "Host listings"}
          </h1>
          <p className="mt-2 text-subtext">
            Review this host's properties and suspend or restore them.
            {!loading &&
              !error &&
              ` ${listings.length} total${
                suspendedCount ? `, ${suspendedCount} suspended` : ""
              }.`}
          </p>
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
              Couldn't load these listings
            </h2>
            <p className="mt-2 text-subtext">{error}</p>
            <button
              type="button"
              onClick={() => load()}
              className={`mt-5 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
            >
              Try again
            </button>
          </div>
        )}

        {!loading && !error && listings.length === 0 && (
          <p className="py-16 text-center text-subtext">
            This host hasn't created any listings yet.
          </p>
        )}

        {!loading && !error && listings.length > 0 && (
          <ul className="space-y-4">
            {listings.map((l) => (
              <MyListingCard
                key={l.id}
                listing={l}
                variant="admin"
                onChanged={() => load(true)}
              />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
};

export default ManagerListings;
