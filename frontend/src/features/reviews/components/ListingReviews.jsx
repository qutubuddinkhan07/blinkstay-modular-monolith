import { useEffect, useRef, useState } from "react";
import { FiTrash2 } from "react-icons/fi";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import {
  EmptyState,
  SectionError,
  Skeleton,
} from "../../../components/common/DashboardParts";
import { handleApiError } from "../../../api/errors/handleApiError";
import { notify } from "../../../utils/notify";
import { useLoad } from "../../../utils/useLoad";
import { plural } from "../../../utils/format";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { adminDeleteReview, getListingReviews } from "../reviewService";
import { HostReplyBlock, ReviewCard } from "./ReviewCard";
import { Stars } from "./Stars";

const PAGE_SIZE = 10;

// onSummary({ averageRating, reviewCount }): passes the fresh numbers up, so the page header stays right
const ListingReviews = ({
  listingId,
  canDelete = false,
  onSummary,
  className = "",
}) => {
  const [page, setPage] = useState(0);
  const [removed, setRemoved] = useState([]); // ids deleted by the admin, hidden until the reload lands
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const sectionRef = useRef(null);

  const { data, loading, failed, reload } = useLoad(
    () => getListingReviews(listingId, { page, size: PAGE_SIZE }),
    [listingId, page],
  );

  // another listing: start from the first page
  useEffect(() => {
    setPage(0);
    setRemoved([]);
  }, [listingId]);

  // fresh data replaces anything we hid by hand
  useEffect(() => {
    setRemoved([]);
  }, [data]);

  useEffect(() => {
    if (data) {
      onSummary?.({
        averageRating: data.averageRating,
        reviewCount: data.reviewCount,
      });
    }
  }, [data, onSummary]);

  // the last page emptied out (after a delete): step back one page
  useEffect(() => {
    if (data && (data.content?.length ?? 0) === 0 && page > 0) {
      setPage(Math.max(0, (data.totalPages || 1) - 1));
    }
  }, [data, page]);

  const changePage = (p) => {
    setPage(p);
    sectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  const confirmDelete = async () => {
    if (!toDelete || deleting) return;
    setDeleting(true);
    try {
      await adminDeleteReview(toDelete.id);
      setRemoved((ids) => [...ids, toDelete.id]);
      notify.success("Review deleted.");
      setToDelete(null);
      reload(); // also refreshes the average and the count
    } catch (err) {
      notify.error(handleApiError(err, "review"));
      if (err.response?.status === 404) {
        setToDelete(null); // already gone
        reload();
      }
    } finally {
      setDeleting(false);
    }
  };

  const rows = (data?.content ?? []).filter((r) => !removed.includes(r.id));
  const firstLoad = loading && !data;
  const hasReviews =
    Number(data?.reviewCount) > 0 && data?.averageRating != null;

  return (
    <section
      id="reviews"
      ref={sectionRef}
      aria-labelledby="reviews-heading"
      className={`scroll-mt-24 border-t border-border pt-8 ${className}`}
    >
      <h2 id="reviews-heading" className="text-xl font-semibold">
        Reviews
      </h2>

      <div className="mt-2 flex flex-wrap items-center gap-3">
        {firstLoad ? (
          <Skeleton className="h-8 w-48" />
        ) : hasReviews ? (
          <>
            <span className="font-display text-3xl font-semibold tabular-nums">
              {Number(data.averageRating).toFixed(1)}
            </span>
            <Stars value={data.averageRating} size="lg" />
            <span className="text-subtext">
              {plural(data.reviewCount, "review")}
            </span>
          </>
        ) : failed ? null : (
          <span className="text-subtext">No reviews yet</span>
        )}
      </div>

      <div className="mt-5">
        {firstLoad ? (
          <ul className="space-y-3" aria-busy="true">
            {[0, 1, 2].map((i) => (
              <li
                key={i}
                className="h-32 animate-pulse rounded-2xl bg-input motion-reduce:animate-none"
              />
            ))}
          </ul>
        ) : failed ? (
          <SectionError onRetry={reload} />
        ) : rows.length === 0 ? (
          <EmptyState>
            No reviews yet. Guests can review a stay after check-out.
          </EmptyState>
        ) : (
          <div
            className={loading ? "opacity-60 transition-opacity" : ""}
            aria-busy={loading}
          >
            <ul className="space-y-3">
              {rows.map((r) => (
                <ReviewCard
                  key={r.id}
                  review={r}
                  actions={
                    canDelete ? (
                      <button
                        type="button"
                        onClick={() => setToDelete(r)}
                        aria-label={`Delete review by ${r.guestName}`}
                        title="Delete review"
                        className="flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-full text-subtext transition-colors hover:bg-danger/10 hover:text-danger focus-visible:outline-2 focus-visible:outline-danger"
                      >
                        <FiTrash2 size={16} aria-hidden="true" />
                      </button>
                    ) : null
                  }
                >
                  <HostReplyBlock review={r} />
                </ReviewCard>
              ))}
            </ul>

            {(data?.totalPages ?? 0) > 1 && (
              <Pagination
                page={page}
                totalPages={data.totalPages}
                onChange={changePage}
              />
            )}
          </div>
        )}
      </div>

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Delete this review?"
        description={
          toDelete
            ? `Delete ${toDelete.guestName}'s ${toDelete.rating}-star review? This can't be undone, and the listing's rating is recalculated.`
            : ""
        }
        confirmLabel="Delete review"
        busyLabel="Deleting…"
        tone="danger"
        busy={deleting}
        onConfirm={confirmDelete}
        onCancel={() => setToDelete(null)}
      />
    </section>
  );
};

export default ListingReviews;
