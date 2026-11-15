import { useEffect, useState } from "react";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import ClampedText from "../../../components/common/ClampedText";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import { deleteReview, getBookingReview } from "../reviewService";
import { Stars } from "./Stars";
import {
  EditedTag,
  ExpandableText,
  HostReplyBlock,
  formatReviewDate,
} from "./ReviewCard";
import ReviewFormDialog from "./ReviewFormDialog";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
const smallBtn = `cursor-pointer rounded-lg border border-border bg-bg px-3 py-1.5 text-sm font-semibold text-text transition-colors hover:bg-input disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;
const dangerBtn = `cursor-pointer rounded-lg border border-danger/50 bg-bg px-3 py-1.5 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;
const primaryBtn = `cursor-pointer rounded-lg bg-primary px-3 py-1.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover ${focusRing}`;

const BookingReview = ({ booking }) => {
  const [state, setState] = useState({ status: "loading", review: null });
  const [formOpen, setFormOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [tick, setTick] = useState(0);

  // data is null when this booking has no review yet
  useEffect(() => {
    let ignore = false;

    getBookingReview(booking.id)
      .then((review) => {
        if (!ignore) setState({ status: "ready", review });
      })
      .catch(() => {
        if (!ignore)
          setState((s) => ({
            status: s.review ? "ready" : "failed",
            review: s.review,
          }));
      });

    return () => {
      ignore = true;
    };
  }, [booking.id, tick]);

  const retry = () => {
    setState({ status: "loading", review: null });
    setTick((t) => t + 1);
  };
  const refresh = () => setTick((t) => t + 1); // quiet: keep what is shown

  const handleDelete = async () => {
    if (deleting) return;
    setDeleting(true);
    try {
      await deleteReview(state.review.id);
      notify.success("Review deleted.");
      setState({ status: "ready", review: null });
      setConfirmOpen(false);
    } catch (err) {
      notify.error(handleApiError(err, "review"));
      if (err.response?.status === 404) {
        setConfirmOpen(false);
        refresh(); // it was already gone
      }
    } finally {
      setDeleting(false);
    }
  };

  const { status, review } = state;

  return (
    <div className="border-t border-border px-3 py-3 sm:px-4">
      {status === "loading" && (
        <div
          className="h-6 w-44 animate-pulse rounded bg-input motion-reduce:animate-none"
          aria-hidden="true"
        />
      )}

      {status === "failed" && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm text-subtext">Couldn't load your review.</p>
          <button type="button" onClick={retry} className={smallBtn}>
            Try again
          </button>
        </div>
      )}

      {status === "ready" && !review && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm text-subtext">How was your stay?</p>
          <button
            type="button"
            onClick={() => setFormOpen(true)}
            className={primaryBtn}
          >
            Write a review
          </button>
        </div>
      )}

      {status === "ready" && review && (
        <div>
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <Stars value={review.rating} />
              <span className="text-xs text-subtext">
                Your review · {formatReviewDate(review.createdAt)}
              </span>
              {review.editedAt && <EditedTag at={review.editedAt} />}
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setFormOpen(true)}
                className={smallBtn}
              >
                Edit review
              </button>
              <button
                type="button"
                onClick={() => setConfirmOpen(true)}
                className={dangerBtn}
              >
                Delete
              </button>
            </div>
          </div>

          {review.comment && (
            <ExpandableText
              text={review.comment}
              className="mt-2 text-sm text-text"
            />
          )}

          <HostReplyBlock review={review} compact />
        </div>
      )}

      {formOpen && (
        <ReviewFormDialog
          booking={booking}
          review={review}
          onClose={() => setFormOpen(false)}
          onSaved={(saved) => {
            setState({ status: "ready", review: saved });
            setFormOpen(false);
          }}
          onStale={() => {
            setFormOpen(false);
            refresh();
          }}
        />
      )}

      <ConfirmDialog
        open={confirmOpen}
        title="Delete your review?"
        description="It disappears from the listing and its rating is recalculated. You can write a new review afterwards."
        confirmLabel="Delete review"
        busyLabel="Deleting…"
        tone="danger"
        busy={deleting}
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
};

export default BookingReview;
