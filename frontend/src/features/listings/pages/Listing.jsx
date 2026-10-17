import React, { useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import Loading from "../../../components/common/Loading";
import StatusBanner from "../../../components/common/StatusBanner";
import { useGetListingByIdQuery } from "../../../store/api";
import ListingGallery from "../components/listing/ListingGallery";
import ListingAdminControls from "../components/listing/ListingAdminControls";
import BookingCard from "../components/booking/BookingCard";
import ListingMap from "../components/listing/ListingMap";
import FavoriteButton from "../../favorites/components/FavoriteButton";
import { focusRing } from "../components/create-listing/formStyles";

// 'room-service' -> 'Room service', 'wifi' -> 'Wifi'
const prettyAmenity = (a) => {
  const s = a.replace(/[-_]/g, " ");
  return s.charAt(0).toUpperCase() + s.slice(1);
};

const primaryLink = `inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`;

const Listing = () => {
  const { id } = useParams();

  // After a suspend, a pause or an edit anywhere, the tags refetch this by themselves
  const {
    currentData: listing,
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useGetListingByIdQuery(id, { refetchOnMountOrArgChange: true });

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [id]);

  if (isLoading || (isFetching && !listing)) {
    return <Loading />;
  }

  // 404: missing, or not published and you're not the owner or an admin
  if (isError && error?.status === 404) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4 text-center text-text">
        <div className="max-w-md">
          <h2 className="font-display text-2xl font-semibold">
            Listing not available
          </h2>
          <p className="mt-2 text-subtext">
            This listing doesn't exist, or it isn't open to guests right now.
          </p>
          <Link to="/explore" className={`mt-5 ${primaryLink}`}>
            Browse listings
          </Link>
        </div>
      </div>
    );
  }

  if (isError && !listing) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4 text-center text-text">
        <div className="max-w-md">
          <h2 className="font-display text-2xl font-semibold">
            Couldn't load this listing
          </h2>
          <p className="mt-2 text-subtext">
            {error?.message || "Something went wrong."}
          </p>
          <button
            type="button"
            onClick={refetch}
            className={`mt-5 cursor-pointer ${primaryLink}`}
          >
            Try again
          </button>
        </div>
      </div>
    );
  }

  if (!listing) return null;

  // ↓ the main `return ( ... )` stays exactly as you have it, with one change:
  //   <ListingAdminControls listing={listing} className="mb-6" />   (no onChanged)

  return (
    <div className="min-h-screen bg-bg text-text">
      <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
        {/* Only the owner or an admin ever gets a non-PUBLISHED listing back */}
        <StatusBanner
          status={listing.status}
          reason={listing.suspensionReason}
          suspendedAt={listing.suspendedAt}
          title={listing.title}
          className="mb-4"
        />

        {/* Admins only; renders nothing for everyone else */}
        <ListingAdminControls listing={listing} className="mb-6" />

        {/* Title: location, with the favorite button */}
        <div className="mb-6 flex items-start justify-between gap-4">
          <h1 className="text-2xl font-semibold sm:text-3xl">
            {listing.title}
            <span className="font-normal text-subtext">
              : {listing.location}
            </span>
          </h1>

          {/* only published listings can be favorited (the API answers 404 for the others) */}
          {listing.status === "PUBLISHED" && (
            <FavoriteButton
              variant="button"
              listingId={listing.id}
              title={listing.title}
              className="shrink-0"
            />
          )}
        </div>

        <ListingGallery images={listing.images} title={listing.title} />

        <div className="mt-10 grid gap-12 lg:grid-cols-[minmax(0,1fr)_380px]">
          {/* Left: description + amenities */}
          <div>
            <p className="max-w-prose whitespace-pre-line leading-relaxed text-subtext">
              {listing.description}
            </p>

            <section className="mt-10 border-t border-border pt-8">
              <h2 className="mb-5 text-xl font-semibold">
                What this place offers:
              </h2>

              <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {listing.amenities?.map((a) => (
                  <li key={a} className="flex items-center gap-3 text-text">
                    <span className="h-1.5 w-1.5 rounded-full bg-primary" />

                    {prettyAmenity(a)}
                  </li>
                ))}
              </ul>
            </section>
          </div>

          {/* Right: floating booking card */}
          <BookingCard
            rooms={listing.rooms}
            onReserve={(booking) => console.log("Reserve", booking)}
          />
        </div>

        <section className="mt-12 border-t border-border pt-8">
          <h2 className="mb-2 text-xl font-semibold">Where you'll be</h2>

          <p className="mb-5 text-subtext">{listing.location}</p>

          <ListingMap
            latitude={listing.geometry?.latitude}
            longitude={listing.geometry?.longitude}
            title={listing.title}
            address={listing.geometry?.address}
          />
        </section>
      </div>
    </div>
  );
};

export default Listing;
