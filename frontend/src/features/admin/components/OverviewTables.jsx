import { Link } from "react-router-dom";
import { formatCount, formatMoney } from "../../host/dashboardUtils";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const th =
  "px-4 py-3 text-xs font-semibold uppercase tracking-wide text-subtext";
const num = "px-4 py-3 text-right tabular-nums";

export const TopHostsTable = ({ rows, currency }) => (
  <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
    <table className="w-full min-w-lg text-left text-sm">
      <caption className="sr-only">Top hosts</caption>
      <thead className="border-b border-border">
        <tr>
          <th scope="col" className={th}>
            Host
          </th>
          <th scope="col" className={`${th} text-right`}>
            Bookings
          </th>
          <th scope="col" className={`${th} text-right`}>
            Gross
          </th>
          <th scope="col" className={`${th} text-right`}>
            Commission
          </th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.hostId} className="border-b border-border last:border-0">
            <td className="px-4 py-3">
              <div className="max-w-56 truncate" title={r.hostName}>
                <Link
                  to={`/admin/users/${r.hostId}/listings`}
                  state={{ username: r.hostName }}
                  className={`rounded font-medium text-text hover:underline ${focusRing}`}
                >
                  {r.hostName}
                </Link>
              </div>
            </td>
            <td className={num}>{formatCount(r.bookings)}</td>
            <td className={num}>{formatMoney(r.gross, currency)}</td>
            <td className={`${num} font-semibold text-text`}>
              {formatMoney(r.commission, currency)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

export const TopListingsTable = ({ rows, currency }) => (
  <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
    <table className="w-full min-w-lg text-left text-sm">
      <caption className="sr-only">Top listings</caption>
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
            Host earnings
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
              <div className="max-w-56 truncate" title={r.listingTitle}>
                <Link
                  to={`/listings/${r.listingId}`}
                  className={`rounded font-medium text-text hover:underline ${focusRing}`}
                >
                  {r.listingTitle}
                </Link>
              </div>
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
