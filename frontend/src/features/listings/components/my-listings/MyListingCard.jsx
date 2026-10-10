import { Link } from "react-router-dom";
import { focusRing } from "../create-listing/formStyles";
import StatusBadge from "../../../../components/common/StatusBadge";
import StatusBanner from "../../../../components/common/StatusBanner";

const smallBtn = `cursor-pointer rounded-lg border border-border bg-bg px-3 py-2 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

const dangerBtn = `cursor-pointer rounded-lg border border-danger/50 bg-bg px-3 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`;

// One status action per status. SUSPENDED has none.
const STATUS_ACTION = {
  DRAFT: { key: "publish", label: "Publish", busyLabel: "Publishing..." },
  PUBLISHED: { key: "pause", label: "Pause", busyLabel: "Pausing..." },
  PAUSED: { key: "resume", label: "Resume", busyLabel: "Resuming..." },
};

const MyListingCard = ({ listing: l, busyAction, onAction, onDelete }) => {
  const action = STATUS_ACTION[l.status];

  return (
    <li className="rounded-2xl border border-border bg-surface p-4 transition-colors duration-300">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
        <div className="h-32 w-full shrink-0 overflow-hidden rounded-lg bg-bg sm:h-24 sm:w-36">
          {l.images[0]?.url ? (
            <img
              src={l.images[0].url}
              alt=""
              loading="lazy"
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-sm text-subtext">
              No image
            </div>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="truncate font-display text-lg font-medium">
              {l.title}
            </h2>
            <StatusBadge status={l.status} />
          </div>

          <p className="mt-1 truncate text-sm text-subtext">
            {[l.location, l.country].filter(Boolean).join(", ") ||
              "Location not listed"}
          </p>
          <p className="mt-0.5 text-xs capitalize text-subtext">
            {l.category.toLowerCase()}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <Link to={`/listings/${l.id}`} className={smallBtn}>
            View
          </Link>
          <Link to={`/listings/${l.id}/edit`} className={smallBtn}>
            Edit
          </Link>
          {action && (
            <button
              type="button"
              onClick={() => onAction(l, action.key)}
              disabled={Boolean(busyAction)}
              className={smallBtn}
            >
              {busyAction === action.key ? action.busyLabel : action.label}
            </button>
          )}
          <button
            type="button"
            onClick={() => onDelete(l)}
            className={dangerBtn}
          >
            Delete
          </button>
        </div>
      </div>

      {l.status === "SUSPENDED" && (
        <StatusBanner
          status="SUSPENDED"
          reason={l.suspensionReason}
          suspendedAt={l.suspendedAt}
          title={l.title}
          className="mt-4"
        />
      )}
    </li>
  );
};

export default MyListingCard;
