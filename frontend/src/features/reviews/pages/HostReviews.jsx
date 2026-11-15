import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import {
  EmptyState,
  SectionError,
} from "../../../components/common/DashboardParts";
import { handleApiError } from "../../../api/errors/handleApiError";
import { notify } from "../../../utils/notify";
import { usePageTitle } from "../../../utils/usePageTitle";
import { useLoad } from "../../../utils/useLoad";
import { plural } from "../../../utils/format";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { deleteReply, getHostReviews, saveReply } from "../reviewService";
import { HostReplyBlock, ReviewCard } from "../components/ReviewCard";
import ReplyForm from "../components/ReplyForm";

const PAGE_SIZE = 10;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
const smallBtn = `cursor-pointer rounded-lg border border-border bg-bg px-3 py-1.5 text-sm font-semibold text-text transition-colors hover:bg-input ${focusRing}`;
const dangerBtn = `cursor-pointer rounded-lg border border-danger/50 bg-bg px-3 py-1.5 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 ${focusRing}`;
const primaryBtn = `cursor-pointer rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover ${focusRing}`;

const HostReviewItem = ({ review, onUpdated }) => {
  const [editing, setEditing] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const hasReply = Boolean(review.hostReply);

  // ReplyForm shows the error if this throws
  const save = async (reply) => {
    const updated = await saveReply(review.id, reply);
    onUpdated(updated);
    setEditing(false);
    notify.success(hasReply ? "Reply updated." : "Reply posted.");
  };

  const removeReply = async () => {
    if (deleting) return;
    setDeleting(true);
    try {
      const updated = await deleteReply(review.id);
      onUpdated(updated ?? { ...review, hostReply: null, hostRepliedAt: null });
      notify.success("Reply deleted.");
      setConfirmOpen(false);
    } catch (err) {
      notify.error(handleApiError(err, "reply"));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <ReviewCard review={review} showListing>
      {editing ? (
        <div className="mt-4">
          <ReplyForm
            initial={review.hostReply ?? ""}
            submitLabel={hasReply ? "Save reply" : "Post reply"}
            onSubmit={save}
            onCancel={() => setEditing(false)}
          />
        </div>
      ) : hasReply ? (
        <HostReplyBlock
          review={review}
          actions={
            <>
              <button
                type="button"
                onClick={() => setEditing(true)}
                className={smallBtn}
              >
                Edit
              </button>
              <button
                type="button"
                onClick={() => setConfirmOpen(true)}
                className={dangerBtn}
              >
                Delete reply
              </button>
            </>
          }
        />
      ) : (
        <div className="mt-4">
          <button
            type="button"
            onClick={() => setEditing(true)}
            className={primaryBtn}
          >
            Reply
          </button>
        </div>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title="Delete your reply?"
        description="The guest's review stays; only your response is removed."
        confirmLabel="Delete reply"
        busyLabel="Deleting…"
        tone="danger"
        busy={deleting}
        onConfirm={removeReply}
        onCancel={() => setConfirmOpen(false)}
      />
    </ReviewCard>
  );
};

const HostReviews = () => {
  usePageTitle("Reviews");
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [overrides, setOverrides] = useState({}); // reviews changed here, until the next load
  const forbiddenRef = useRef(false);

  const handleError = (err) => {
    if (err.response?.status === 403) {
      if (!forbiddenRef.current) {
        forbiddenRef.current = true;
        notify.error("You don't have access to host reviews.");
        navigate("/explore", { replace: true });
      }
      return;
    }
    notify.error(handleApiError(err, "reviews"), { id: "host-reviews-error" });
  };

  const { data, loading, failed, reload } = useLoad(
    () => getHostReviews({ page, size: PAGE_SIZE }),
    [page],
    handleError,
  );

  // fresh data from the server replaces our local changes
  useEffect(() => {
    setOverrides({});
  }, [data]);

  const rows = (data?.content ?? []).map((r) => overrides[r.id] ?? r);
  const total = data?.reviewCount ?? data?.totalElements;
  const firstLoad = loading && !data;

  const changePage = (p) => {
    setPage(p);
    window.scrollTo({ top: 0 });
  };

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-4xl">
        <header className="mb-6">
          <h1 className="font-display text-3xl font-semibold">Reviews</h1>
          <p className="mt-2 text-subtext">
            What guests say about your listings.
            {total != null && ` ${plural(total, "review")}.`}
          </p>
        </header>

        {firstLoad ? (
          <ul className="space-y-3" aria-busy="true">
            {[0, 1, 2].map((i) => (
              <li
                key={i}
                className="h-40 animate-pulse rounded-2xl bg-input motion-reduce:animate-none"
              />
            ))}
          </ul>
        ) : failed ? (
          <SectionError onRetry={reload} />
        ) : rows.length === 0 ? (
          <EmptyState>
            No reviews yet. They'll appear here after guests complete their
            stays.
          </EmptyState>
        ) : (
          <div
            className={loading ? "opacity-60 transition-opacity" : ""}
            aria-busy={loading}
          >
            <ul className="space-y-3">
              {rows.map((r) => (
                <HostReviewItem
                  key={r.id}
                  review={r}
                  onUpdated={(updated) =>
                    setOverrides((o) => ({
                      ...o,
                      [updated.id ?? r.id]: updated,
                    }))
                  }
                />
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
    </div>
  );
};

export default HostReviews;
