import React, { useEffect, useState } from "react";
import { fetchListings } from "../listingservice";
import { handleApiError } from "../../../api/errors/handleApiError";
import Card from "../components/Card";
import SkeletonCard from "../components/SkeletonCard";
import Pagination from "../components/Pagination";
import { Link } from "react-router-dom";

const PAGE_SIZE = 10;

const ExplorePage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState({});
  const [page, setPage] = useState(0);
  const [country, setCountry] = useState("");

  const fetchData = async () => {
    try {
      setLoading(true);
      setError({});

      const { data } = await fetchListings({
        page,
        size: PAGE_SIZE,
        sortBy: "createdAt",
        direction: "desc",
        country,
      });

      setData(data);
    } catch (error) {
      console.error("Failed to fetch listings ", error);
      setError((prev) => ({ ...prev, api: handleApiError(error, "listings") }));
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchData();
      window.scrollTo({ top: 0, behavior: "smooth" });
    }, 2000);

    return () => clearTimeout(timer);
  }, [page, country]);

  if (error.api) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4">
        <div className="text-center">
          <h2 className="text-2xl font-bold">Listings are unavailable</h2>
          <p className="mt-2 text-gray-500">{error.api}</p>

          <button
            onClick={fetchData}
            className="mt-4 rounded-lg bg-[#e01f59] px-4 py-2 text-white"
          >
            Try again
          </button>
        </div>
      </div>
    );
  }

  const listings = data?.content ?? [];

  return (
    <div className="mx-auto min-h-screen w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-10 bg-amber-200">
      <h2 className="mb-6 text-2xl font-semibold">Explore listings</h2>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {loading
          ? Array.from({ length: PAGE_SIZE }, (_, i) => (
              <SkeletonCard key={i} />
            ))
          : listings.map((item) => (
              <Link to={`/listings/${item.id}`} key={item.id}>
                <Card listing={item} />
              </Link>
            ))}
      </div>

      {!loading && listings.length === 0 && (
        <p className="py-16 text-center text-gray-500">No listings found.</p>
      )}

      <Pagination
        page={page}
        totalPages={data?.totalPages || 1}
        onChange={setPage}
      />
    </div>
  );
};

export default ExplorePage;
