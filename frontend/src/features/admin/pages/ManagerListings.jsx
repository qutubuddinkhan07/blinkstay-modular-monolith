import { Link, useLocation, useParams } from "react-router-dom";
import { FiArrowLeft } from "react-icons/fi";
import { useGetListingsByManagerQuery } from "../../../store/api";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import MyListingCard from "../../listings/components/my-listings/MyListingCard";

const ManagerListings = () => {
  const { managerId } = useParams();
  const { state } = useLocation();
  const owner = state?.username; // sent by the Users table link; absent on a direct visit

  // Suspend, unsuspend and block/unblock elsewhere refetch this through the "ManagerListings" tag
  const {
    data: listings = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useGetListingsByManagerQuery(managerId, {
    refetchOnMountOrArgChange: true,
  });

  const showError = isError && listings.length === 0;
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
            {!isLoading &&
              !showError &&
              ` ${listings.length} total${
                suspendedCount ? `, ${suspendedCount} suspended` : ""
              }.`}
          </p>
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
              Couldn't load these listings
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
            This host hasn't created any listings yet.
          </p>
        )}

        {!isLoading && !showError && listings.length > 0 && (
          <ul className="space-y-4">
            {listings.map((l) => (
              <MyListingCard key={l.id} listing={l} variant="admin" />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
};

export default ManagerListings;
