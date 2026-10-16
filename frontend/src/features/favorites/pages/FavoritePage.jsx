import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { FaRegHeart } from "react-icons/fa";
import {
  FAVORITES_SETTLED,
  useFavorites,
} from "../../../context/FavoriteContext";
import { handleApiError } from "../../../api/errors/handleApiError";
import { fetchFavorites } from "../favoritesService";
import Card from "../../listings/components/explore-page/Card";
import SkeletonCard from "../../listings/components/explore-page/SkeletonCard";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";

const PAGE_SIZE = 12;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary";

const FavoritesPage = () => {
  const { ids, ready, count } = useFavorites();

  const [page, setPage] = useState(0);
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const reqRef = useRef(0); // only the latest request may update state

  // silent = refill after changes, without the skeleton
  const load = useCallback(
    async (silent = false) => {
      const req = ++reqRef.current;
      if (!silent) {
        setLoading(true);
        setError(null);
      }

      try {
        const res = await fetchFavorites({ page, size: PAGE_SIZE });
        if (req !== reqRef.current) return;

        const d = res.data?.data ?? res.data ?? {};
        const next = d.content ?? [];

        // the last page emptied out: step back one page
        if (next.length === 0 && page > 0 && (d.totalPages ?? 0) > 0) {
          setPage(d.totalPages - 1);
          return;
        }

        setItems(next);
        setTotalPages(d.totalPages ?? 0);
        setError(null);
      } catch (err) {
        if (req !== reqRef.current) return;
        if (!silent) setError(handleApiError(err, "favorites"));
      } finally {
        if (req === reqRef.current) setLoading(false);
      }
    },
    [page],
  );

  useEffect(() => {
    load();
    window.scrollTo({ top: 0, behavior: "smooth" });
    return () => {
      reqRef.current++; // ignore any request still in flight
    };
  }, [load]);

  // a heart removed here disappears immediately
  const visible = ready ? items.filter((i) => ids.has(String(i.id))) : items;
  const removedHere = items.length - visible.length;

  // once the removal has reached the server, refill the page quietly
  const removedRef = useRef(0);
  removedRef.current = removedHere;
  useEffect(() => {
    const refill = () => {
      if (removedRef.current > 0) load(true);
    };
    window.addEventListener(FAVORITES_SETTLED, refill);
    return () => window.removeEventListener(FAVORITES_SETTLED, refill);
  }, [load]);

  if (error) {
    return (
      <div className="flex min-h-[calc(100vh-72px)] items-center justify-center bg-bg px-4 text-text">
        <div className="max-w-md text-center">
          <h2 className="font-display text-2xl font-semibold">
            Couldn't load your favorites
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
      </div>
    );
  }

  const empty = !loading && visible.length === 0;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg text-text transition-colors duration-300">
      <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-10">
        <div className="mb-6">
          <h1 className="font-display text-2xl font-semibold sm:text-3xl">
            Favorites
          </h1>
          {ready && count > 0 && (
            <p className="mt-1 text-sm text-subtext">
              {count} saved {count === 1 ? "stay" : "stays"}
            </p>
          )}
        </div>

        <div
          aria-busy={loading}
          className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
        >
          {loading
            ? Array.from({ length: 8 }, (_, i) => <SkeletonCard key={i} />)
            : visible.map((item) => <Card key={item.id} listing={item} />)}
        </div>

        {empty && (
          <div className="py-16 text-center">
            <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-muted text-subtext">
              <FaRegHeart size={22} aria-hidden="true" />
            </span>
            <h2 className="mt-4 text-lg font-medium">No favorites yet</h2>
            <p className="mt-1 text-sm text-subtext">
              Explore stays and tap the heart to save them here.
            </p>
            <Link
              to="/explore"
              className={`mt-5 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
            >
              Explore stays
            </Link>
          </div>
        )}

        <Pagination
          page={page}
          totalPages={totalPages || 1}
          onChange={setPage}
        />
      </div>
    </div>
  );
};

export default FavoritesPage;
