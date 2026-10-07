import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Loading from "../../../components/common/Loading";
import { fetchListingById } from "../listingService";
import { handleApiError } from "../../../api/errors/handleApiError";
import ListingGallery from "../components/listing/ListingGallery";
import BookingCard from "../components/booking/BookingCard";
import ListingMap from "../components/listing/ListingMap";

// 'room-service" -> "Room service", "wifi" -> "Wifi"
const prettyAmenity = (a) => {
  const s = a.replace(/[-_]/g, " ");
  return s.charAt(0).toUpperCase() + s.slice(1);
};

const Listing = () => {
  const { id } = useParams();
  const [listing, setListing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await fetchListingById(id);

        // API shape: { success, message, data: {...} }
        if (!ignore) setListing(res.data.data);
      } catch (error) {
        console.log("error fetching listing");
        if (!ignore) {
          setError(handleApiError(error, "listing"));
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    };

    load();
    window.scrollTo({ top: 0 });

    return () => {
      ignore = true;
    };
  }, [id]);

  if (loading) {
    return <Loading />;
  }

  if (error || !listing) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-4 text-center">
        <div>
          <h2 className="text-2xl font-bold">Listing unavailable</h2>
          <p className="mt-2 text-neutral-500">
            {error || "Lisiting not found."}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
      {/* Title: location */}
      <h1 className="mb-6 text-2xl font-semibold sm:text-3xl">
        {listing.title}
        <span className="font-normal text-neutral-500">
          : {listing.location}
        </span>
      </h1>

      <ListingGallery images={listing.images} title={listing.title} />

      <div className="mt-10 grid gap-12 lg:grid-cols-[minmax(0,1fr)_380px]">
        {/* Left: description + amenities */}
        <div>
          <p className="max-w-prose whitespace-pre-line leading-relaxed text-neutral-700">
            {listing.description}
          </p>

          <section className="mt-10 border-t border-neutral-200 pt-8">
            <h2 className="mb-5 text-xl font-semibold">
              What this place offers:
            </h2>

            <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {listing.amenities?.map((a) => (
                <li key={a} className="flex items-center gap-3">
                  <span className="h-1.5 w-1.5 rounded-full bg-neutral-900" />
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

      <section className="mt-12 border-t border-neutral-200 pt-8">
        <h2 className="mb-2 text-xl font-semibold">Where you'll be</h2>
        <p className="mb-5 text-neutral-600">{listing.location}</p>

        <ListingMap
          latitude={listing.geometry?.latitude}
          longitude={listing.geometry?.longitude}
          title={listing.title}
          address={listing.geometry?.address}
        />
      </section>
    </div>
  );
};

export default Listing;
