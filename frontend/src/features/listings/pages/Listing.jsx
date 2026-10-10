import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Loading from "../../../components/common/Loading";
import StatusBanner from "../../../components/common/StatusBanner";
import { fetchListingById } from "../listingService";
import { handleApiError } from "../../../api/errors/handleApiError";
import ListingGallery from "../components/listing/ListingGallery";
import ListingAdminControls from "../components/listing/ListingAdminControls";
import BookingCard from "../components/booking/BookingCard";
import ListingMap from "../components/listing/ListingMap";
import { focusRing } from "../components/create-listing/formStyles";

// 'room-service' -> 'Room service', 'wifi' -> 'Wifi'
const prettyAmenity = (a) => {
  const s = a.replace(/[-_]/g, " ");
  return s.charAt(0).toUpperCase() + s.slice(1);
};

const primaryLink = `inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`;

const Listing = () => {
  const { id } = useParams();
  const [listing, setListing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const reqRef = useRef(0); // only the latest request may update state

  // silent = refetch without the full-page loader (used after admin actions)
  const load = useCallback(
    async (silent = false) => {
      const req = ++reqRef.current;

      if (!silent) {
        setLoading(true);
        setError(null);
        setNotFound(false);
      }

      try {
        const res = await fetchListingById(id);

        // API shape: { success, message, data: {...} }
        if (req === reqRef.current) {
          setListing(res.data.data);
          setError(null);
          setNotFound(false);
        }
      } catch (err) {
        if (req !== reqRef.current) return;
        console.error("error fetching listing", err);
        setListing(null);
        setNotFound(err.response?.status === 404);
        setError(handleApiError(err, "listing"));
      } finally {
        if (req === reqRef.current) setLoading(false);
      }
    },
    [id],
  );

  useEffect(() => {
    load();
    window.scrollTo({ top: 0 });

    return () => {
      reqRef.current++; // ignore any request still in flight
    };
  }, [load]);

  if (loading) {
    return <Loading />;
  }

  // 404: missing, or not published and you're not the owner or an admin
  if (notFound) {
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

  if (error || !listing) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4 text-center text-text">
        <div className="max-w-md">
          <h2 className="font-display text-2xl font-semibold">
            Couldn't load this listing
          </h2>
          <p className="mt-2 text-subtext">
            {error || "Something went wrong."}
          </p>
          <button
            type="button"
            onClick={() => load()}
            className={`mt-5 cursor-pointer ${primaryLink}`}
          >
            Try again
          </button>
        </div>
      </div>
    );
  }

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
        <ListingAdminControls
          listing={listing}
          onChanged={() => load(true)}
          className="mb-6"
        />

        {/* Title: location */}
        <h1 className="mb-6 text-2xl font-semibold sm:text-3xl">
          {listing.title}

          <span className="font-normal text-subtext">: {listing.location}</span>
        </h1>

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
