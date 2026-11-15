import { useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Stars } from "./Stars";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

// An ISO UTC instant shown as a date in the browser's local time
export const formatReviewDate = (iso) => {
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? ""
    : d.toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric",
      });
};

export const EditedTag = ({ at }) => (
  <span
    title={`Edited ${formatReviewDate(at)}`}
    className="rounded-full border border-border px-2 py-0.5 text-xs text-subtext"
  >
    Edited
  </span>
);

// Long text is cut to 4 lines with "Read more". The button only appears when the text really is cut.
export const ExpandableText = ({ text, className = "" }) => {
  const id = useId();
  const ref = useRef(null);
  const [expanded, setExpanded] = useState(false);
  const [overflows, setOverflows] = useState(false);

  // a different text starts collapsed
  useEffect(() => {
    setExpanded(false);
  }, [text]);

  // measure only while collapsed (expanded, everything is visible), before the browser paints
  useLayoutEffect(() => {
    const el = ref.current;
    if (!el || expanded) return;

    const measure = () => setOverflows(el.scrollHeight > el.clientHeight + 1);
    measure();

    const observer = new ResizeObserver(measure); // the width changes when the window does
    observer.observe(el);
    return () => observer.disconnect();
  }, [text, expanded]);

  return (
    <div className={className}>
      <p
        id={id}
        ref={ref}
        className={`whitespace-pre-line break-words ${expanded ? "" : "line-clamp-4"}`}
      >
        {text}
      </p>

      {overflows && (
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          aria-expanded={expanded}
          aria-controls={id}
          className={`mt-1 cursor-pointer rounded text-sm font-semibold text-primary hover:underline ${focusRing}`}
        >
          {expanded ? "Show less" : "Read more"}
        </button>
      )}
    </div>
  );
};

// The host's answer. actions: buttons shown under it (the host page). compact: no indent (guest row).
export const HostReplyBlock = ({
  review: r,
  actions = null,
  compact = false,
}) => {
  if (!r.hostReply) return null;

  return (
    <div
      className={`mt-4 rounded-xl border-l-4 border-primary/40 bg-muted ${
        compact ? "p-3 text-sm" : "ml-4 p-4 sm:ml-8"
      }`}
    >
      <div className="flex flex-wrap items-baseline justify-between gap-x-3">
        <p className="text-sm font-semibold text-text">
          Response from the host
        </p>
        {r.hostRepliedAt && (
          <time dateTime={r.hostRepliedAt} className="text-xs text-subtext">
            {formatReviewDate(r.hostRepliedAt)}
          </time>
        )}
      </div>

      <ExpandableText text={r.hostReply} className="mt-1.5 text-text" />

      {actions && <div className="mt-3 flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
};

// One review. actions: top-right slot (admin trash). children: shown under the comment (the reply).
export const ReviewCard = ({
  review: r,
  showListing = false,
  actions = null,
  children,
}) => (
  <li className="rounded-2xl border border-border bg-surface p-5">
    <div className="flex items-start justify-between gap-3">
      <div className="min-w-0">
        {showListing && r.listingTitle && (
          <p className="truncate text-sm text-subtext">
            <Link
              to={`/listings/${r.listingId}`}
              className={`rounded hover:underline ${focusRing}`}
            >
              {r.listingTitle}
            </Link>
          </p>
        )}
        <p className="truncate font-semibold text-text">{r.guestName}</p>
      </div>
      {actions}
    </div>

    <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
      <Stars value={r.rating} />
      <time dateTime={r.createdAt} className="text-subtext">
        {formatReviewDate(r.createdAt)}
      </time>
      {r.editedAt && <EditedTag at={r.editedAt} />}
    </div>

    {r.comment && (
      <ExpandableText text={r.comment} className="mt-3 text-text" />
    )}

    {children}
  </li>
);
