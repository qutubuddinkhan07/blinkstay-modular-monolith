import { Link } from "react-router-dom";
import StatusBadge from "../../../components/common/StatusBadge";
import {
  formatCount,
  formatInstant,
  formatMoney,
  formatPlainDate,
} from "../dashboardUtils";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

export const LEDGER_STATUS = {
  PENDING: { label: "Pending", dot: "bg-amber-500" },
  EARNED: { label: "Earned", dot: "bg-emerald-500" },
  REVERSED: { label: "Reversed", dot: "bg-subtext" },
};

const STATUS_HINT = {
  PENDING: "Paid, stay not over yet",
  EARNED: "Stay finished, this amount is owed to you",
  REVERSED: "Fully refunded, nothing owed",
};

const th =
  "px-4 py-3 text-xs font-semibold uppercase tracking-wide text-subtext";
const num = "px-4 py-3 text-right tabular-nums";

const ListingName = ({ id, title }) => (
  <div className="max-w-56 truncate" title={title}>
    {id ? (
      <Link
        to={`/listings/${id}`}
        className={`rounded font-medium text-text hover:underline ${focusRing}`}
      >
        {title}
      </Link>
    ) : (
      <span className="font-medium text-text">{title}</span>
    )}
  </div>
);

export const ByListingTable = ({ rows, currency }) => (
  <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
    <table className="w-full min-w-lg text-left text-sm">
      <caption className="sr-only">Earnings by listing</caption>
      <thead className="border-b border-border">
        <tr>
          <th scope="col" className={th}>
            Listing
          </th>
          <th scope="col" className={`${th} text-right`}>
            Bookings
          </th>
          <th scope="col" className={`${th} text-right`}>
            Gross
          </th>
          <th scope="col" className={`${th} text-right`}>
            You earn
          </th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr
            key={r.listingId}
            className="border-b border-border last:border-0"
          >
            <td className="px-4 py-3">
              <ListingName id={r.listingId} title={r.listingTitle} />
            </td>
            <td className={num}>{formatCount(r.bookings)}</td>
            <td className={num}>{formatMoney(r.gross, currency)}</td>
            <td className={`${num} font-semibold text-text`}>
              {formatMoney(r.hostAmount, currency)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

export const LedgerTable = ({ rows }) => (
  <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
    <table className="w-full min-w-5xl text-left text-sm">
      <caption className="sr-only">Booking earnings ledger</caption>
      <thead className="border-b border-border">
        <tr>
          <th scope="col" className={th}>
            Reference
          </th>
          <th scope="col" className={th}>
            Listing
          </th>
          <th scope="col" className={th}>
            Stay
          </th>
          <th scope="col" className={`${th} text-right`}>
            Gross
          </th>
          <th scope="col" className={`${th} text-right`}>
            Refunded
          </th>
          <th scope="col" className={`${th} text-right`}>
            Commission
          </th>
          <th scope="col" className={`${th} text-right`}>
            You earn
          </th>
          <th scope="col" className={th}>
            Status
          </th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr
            key={r.bookingId}
            className="border-b border-border last:border-0"
          >
            <td className="px-4 py-3">
              <span
                className="font-mono text-xs font-semibold text-text"
                title={
                  r.createdAt
                    ? `Booked ${formatInstant(r.createdAt)}`
                    : undefined
                }
              >
                {r.bookingReference}
              </span>
            </td>
            <td className="px-4 py-3">
              <ListingName id={r.listingId} title={r.listingTitle} />
            </td>
            <td className="whitespace-nowrap px-4 py-3 text-subtext">
              {formatPlainDate(r.checkIn)} – {formatPlainDate(r.checkOut)}
            </td>
            <td className={num}>{formatMoney(r.gross, r.currency)}</td>
            <td className={num}>{formatMoney(r.refunded, r.currency)}</td>
            <td className={num}>
              <span title={`Commission rate: ${r.commissionPercent}%`}>
                {formatMoney(r.commission, r.currency)}
                <span className="sr-only"> ({r.commissionPercent}% rate)</span>
              </span>
            </td>
            <td className={`${num} font-semibold text-text`}>
              {formatMoney(r.hostAmount, r.currency)}
            </td>
            <td className="px-4 py-3">
              <span title={STATUS_HINT[r.status]}>
                <StatusBadge status={r.status} statuses={LEDGER_STATUS} />
                <span className="sr-only">. {STATUS_HINT[r.status]}</span>
              </span>
              {r.status === "EARNED" && r.earnedAt && (
                <p className="mt-1 text-xs text-subtext">
                  {formatInstant(r.earnedAt)}
                </p>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

export const TableSkeleton = ({ rows = 4 }) => (
  <div
    className="overflow-hidden rounded-2xl border border-border bg-surface"
    aria-busy="true"
  >
    {Array.from({ length: rows }, (_, i) => (
      <div
        key={i}
        className="h-12 animate-pulse border-b border-border bg-input/40 last:border-0 motion-reduce:animate-none"
      />
    ))}
  </div>
);
