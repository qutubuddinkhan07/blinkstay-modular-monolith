import React, { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { FiSearch, FiX } from "react-icons/fi";
import { useGetListingsQuery } from "../../../store/api";
import Card from "../components/explore-page/Card";
import SkeletonCard from "../components/explore-page/SkeletonCard";
import Pagination from "../components/explore-page/pagination/Pagination";
import ExploreCategories from "../components/explore-page/ExploreCategories";

const PAGE_SIZE = 12;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary";

const ExplorePage = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Read params from URL with defaults
  const page = parseInt(searchParams.get("page") || "0", 10);
  const location = searchParams.get("location") || "";
  const name = searchParams.get("name") || "";
  const category = searchParams.get("category") || "";

  const [searchInput, setSearchInput] = useState(location || name);

  // The query re-runs by itself whenever these values change, and results are cached per combination
  const { data, isLoading, isFetching, isError, error, refetch } =
    useGetListingsQuery({
      page,
      size: PAGE_SIZE,
      sortBy: "createdAt",
      direction: "desc",
      location,
      name,
      category,
    });

  // Debounce search input to update URL params
  useEffect(() => {
    const term = searchInput.trim();

    // Nothing changed (for example on the first render): leave the URL alone.
    // Without this, opening /explore?page=3 was reset to page 0 after 400ms.
    if (term === location) return;

    const handler = setTimeout(() => {
      const newParams = new URLSearchParams(searchParams);

      if (term) {
        // Set the term for both location and name
        newParams.set("location", term);
        newParams.set("name", term);
      } else {
        newParams.delete("location");
        newParams.delete("name");
      }

      newParams.set("page", "0"); // Reset to page 0 on search change
      setSearchParams(newParams);
    }, 400);

    return () => clearTimeout(handler);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchInput]);

  // Back to the top whenever the results change
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, [page, location, name, category]);

  // Handlers for updating URL search params
  const handleCategoryChange = (catValue) => {
    const newParams = new URLSearchParams(searchParams);
    if (catValue) {
      newParams.set("category", catValue);
    } else {
      newParams.delete("category");
    }
    newParams.set("page", "0"); // Reset page on category change
    setSearchParams(newParams);
  };

  const handlePageChange = (newPage) => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set("page", newPage.toString());
    setSearchParams(newParams);
  };

  const handleClearFilters = () => {
    setSearchInput("");
    setSearchParams({});
  };

  const listings = data?.content ?? [];
  const hasActiveFilters = location || name || category;

  if (isError) {
    return (
      <div className="flex min-h-[calc(100vh-72px)] items-center justify-center bg-bg px-4 text-text">
        <div className="max-w-md text-center">
          <h2 className="font-display text-2xl font-semibold">
            Listings are unavailable
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
      </div>
    );
  }

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg text-text transition-colors duration-300">
      {/* Airbnb style Category Bar */}
      <ExploreCategories
        selectedCategory={category}
        onSelectCategory={handleCategoryChange}
      />

      <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
        {/* Search Bar Row */}
        <div className="mb-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="relative w-full sm:max-w-md">
            <FiSearch
              className="absolute left-3.5 top-1/2 -translate-y-1/2 text-subtext"
              size={18}
            />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by destination or title..."
              className={`w-full rounded-full border border-border bg-surface pl-10 pr-4 py-2.5 text-sm text-text placeholder:text-subtext shadow-sm transition-colors outline-none focus:border-primary ${focusRing}`}
            />
          </div>

          {hasActiveFilters && (
            <button
              type="button"
              onClick={handleClearFilters}
              className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline cursor-pointer"
            >
              <FiX size={16} />
              Clear all filters
            </button>
          )}
        </div>

        {/* Listings Grid: skeletons only on the very first load; later the old results stay, dimmed */}
        <div
          aria-busy={isFetching}
          className={`grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 ${
            isFetching && !isLoading ? "opacity-60 transition-opacity" : ""
          }`}
        >
          {isLoading
            ? Array.from({ length: PAGE_SIZE }, (_, i) => (
                <SkeletonCard key={i} />
              ))
            : listings.map((item) => <Card key={item.id} listing={item} />)}
        </div>

        {!isFetching && listings.length === 0 && (
          <div className="py-16 text-center">
            <h3 className="text-lg font-medium">No listings found</h3>
            <p className="mt-1 text-sm text-subtext">
              Try changing your filters or search keywords.
            </p>
            <button
              type="button"
              onClick={handleClearFilters}
              className={`mt-4 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
            >
              Clear filters
            </button>
          </div>
        )}

        <Pagination
          page={page}
          totalPages={data?.totalPages || 1}
          onChange={handlePageChange}
        />
      </div>
    </div>
  );
};

export default ExplorePage;
