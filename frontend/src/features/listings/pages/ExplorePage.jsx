import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchListings } from "../listingservice";
import { handleApiError } from "../../../api/errors/handleApiError";
import Card from "../components/Card";
import SkeletonCard from "../components/SkeletonCard";
import Pagination from "../components/Pagination";

const PAGE_SIZE = 10;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary";

const ExplorePage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [country] = useState(""); // hook up a country filter here later
  const [reloadKey, setReloadKey] = useState(0); // bump to retry after an error

  useEffect(() => {
    let ignore = false; // drops stale responses if page/country changes mid-request

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const { data: result } = await fetchListings({
          page,
          size: PAGE_SIZE,
          sortBy: "createdAt",
          direction: "desc",
          country,
        });

        if (!ignore) setData(result);
      } catch (err) {
        if (ignore) return;
        console.error("Failed to fetch listings", err);
        setError(handleApiError(err, "listings"));
        setData(null);
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    window.scrollTo({ top: 0, behavior: "smooth" });

    return () => {
      ignore = true;
    };
  }, [page, country, reloadKey]);

  const listings = data?.content ?? [];

  /* ---------- Error state ---------- */
  if (error) {
    return (
      <div className="flex min-h-[calc(100vh-72px)] items-center justify-center bg-bg px-4 text-text">
        <div className="max-w-md text-center">
          <h2 className="font-display text-2xl font-semibold">
            Listings are unavailable
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

  /* ---------- Page ---------- */
  return (
    // 60%: page background + card surfaces, 30%: text + borders, 10%: accent
    <div className="min-h-[calc(100vh-72px)] bg-bg text-text transition-colors duration-300">
      <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
        <h2 className="mb-6 font-display text-2xl font-semibold sm:text-3xl">
          Explore listings
        </h2>

        <div
          aria-busy={loading}
          className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
        >
          {loading
            ? Array.from({ length: PAGE_SIZE }, (_, i) => (
                <SkeletonCard key={i} />
              ))
            : listings.map((item) => (
                <Link
                  to={`/listings/${item.id}`}
                  key={item.id}
                  className={`block rounded-2xl ${focusRing}`}
                >
                  <Card listing={item} />
                </Link>
              ))}
        </div>

        {!loading && listings.length === 0 && (
          <p className="py-16 text-center text-subtext">No listings found.</p>
        )}

        <Pagination
          page={page}
          totalPages={data?.totalPages || 1}
          onChange={setPage}
        />
      </div>
    </div>
  );
};

export default ExplorePage;
