import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { FiX } from "react-icons/fi";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import { formatDateLabel } from "../../../utils/format";
import { REVIEW_LIMITS, createReview, updateReview } from "../reviewService";
import { StarPicker } from "./Stars";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
const fieldCls =
  "w-full rounded-lg border border-border bg-input px-3 py-2.5 text-sm text-text placeholder:text-subtext focus-visible:outline-2 focus-visible:outline-primary";

// booking: the booking being reviewed. review: the existing review when editing (else null).
// onSaved(review) after a save. onStale(): the booking was already reviewed or can't be (404/409): reload the row.
const ReviewFormDialog = ({
  booking,
  review = null,
  onClose,
  onSaved,
  onStale,
}) => {
  const id = useId();
  const editing = Boolean(review);

  const [rating, setRating] = useState(review?.rating ?? 0);
  const [comment, setComment] = useState(review?.comment ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const trimmed = comment.trim();
  const tooShort = trimmed.length < REVIEW_LIMITS.commentMin;

  const busyRef = useRef(false);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  // lock page scroll, close on Escape (never while saving)
  useEffect(() => {
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKey = (e) => {
      if (e.key === "Escape" && !busyRef.current) closeRef.current();
    };
    document.addEventListener("keydown", onKey);

    return () => {
      document.body.style.overflow = previous;
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (busyRef.current) return; // no double submit

    if (rating < 1 || rating > 5) {
      setError("Choose a rating from 1 to 5 stars.");
      return;
    }

    if (tooShort) {
      setError(
        `Please write at least ${REVIEW_LIMITS.commentMin} characters about your stay.`,
      );
      return;
    }

    busyRef.current = true;
    setBusy(true);
    setError("");

    try {
      const body = { rating, comment: trimmed };
      const saved = editing
        ? await updateReview(review.id, body)
        : await createReview(booking.id, body);

      notify.success(editing ? "Review updated." : "Thanks for your review!");
      onSaved(saved);
    } catch (err) {
      const message = handleApiError(err, "review");

      if ([404, 409].includes(err.response?.status)) {
        // "You already reviewed this stay", "You can review a stay after check-out", or it's gone
        notify.error(message);
        onStale?.();
        return;
      }
      setError(message);
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };

  const summary = booking
    ? `${booking.listingTitle} · ${formatDateLabel(booking.checkIn)} – ${formatDateLabel(booking.checkOut)}`
    : "";

  return createPortal(
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4"
      onClick={() => !busy && onClose()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={`${id}-title`}
        onClick={(e) => e.stopPropagation()}
        className="max-h-[90dvh] w-full max-w-md overflow-y-auto rounded-2xl border border-border bg-surface p-6 text-text shadow-xl"
      >
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h2
              id={`${id}-title`}
              className="font-display text-xl font-semibold"
            >
              {editing ? "Edit your review" : "Write a review"}
            </h2>
            {summary && (
              <p className="mt-1 truncate text-sm text-subtext">{summary}</p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            aria-label="Close"
            className={`-mr-2 -mt-1 flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-full text-subtext hover:bg-bg hover:text-text disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
          >
            <FiX size={18} aria-hidden="true" />
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate className="mt-5">
          <p id={`${id}-rating`} className="text-sm font-semibold">
            Your rating
          </p>
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <StarPicker
              value={rating}
              onChange={setRating}
              disabled={busy}
              autoFocus
              labelledBy={`${id}-rating`}
            />
            <span className="text-sm text-subtext" aria-live="polite">
              {rating ? `${rating} out of 5` : "Choose a rating"}
            </span>
          </div>

          <div className="mt-5">
            <label htmlFor={`${id}-comment`} className="text-sm font-semibold">
              + Your review +{" "}
              <span className="font-normal text-subtext">
                + (at least {REVIEW_LIMITS.commentMin} characters)
              </span>
            </label>
            <textarea
              id={`${id}-comment`}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              maxLength={REVIEW_LIMITS.comment}
              rows={5}
              disabled={busy}
              required
              aria-required="true"
              placeholder="What did you like? Anything the next guest should know?"
              aria-describedby={`${id}-count`}
              className={`${fieldCls} mt-2 min-h-28 resize-y`}
            />

            <div className="mt-1 flex justify-between gap-3 text-xs">
              <span className="text-subtext" aria-live="polite">
                {comment.length > 0 && tooShort
                  ? `${REVIEW_LIMITS.commentMin - trimmed.length} more characters needed`
                  : ""}
              </span>
              <span
                id={`${id}-count`}
                className={`tabular-nums ${
                  comment.length >= REVIEW_LIMITS.comment
                    ? "text-danger"
                    : "text-subtext"
                }`}
              >
                {comment.length}/{REVIEW_LIMITS.comment}
              </span>
            </div>
          </div>

          {error && (
            <p role="alert" className="mt-3 text-sm text-danger">
              {error}
            </p>
          )}

          <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button
              type="button"
              onClick={onClose}
              disabled={busy}
              className={`cursor-pointer rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-text transition-colors hover:bg-bg disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={busy || rating < 1 || tooShort}
              className={`cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
            >
              {busy ? "Saving…" : editing ? "Save changes" : "Save review"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body,
  );
};

export default ReviewFormDialog;
