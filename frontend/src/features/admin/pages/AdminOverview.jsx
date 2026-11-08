import { useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import { usePageTitle } from "../../../utils/usePageTitle";
import { useLoad } from "../../../utils/useLoad";
import {
  DateRangeBar,
  EmptyState,
  Kpi,
  SectionError,
  Skeleton,
} from "../../../components/common/DashboardParts";
import {
  formatCount,
  formatMoney,
  formatPlainDate,
} from "../../host/dashboardUtils";
import { TableSkeleton } from "../../host/components/DashboardTables";
import { BOOKING_STATUS } from "../../bookings/bookingService";
import {
  getAdminMonthly,
  getAdminOverview,
  getTopHosts,
  getTopListings,
} from "../adminService";
import OverviewChart from "../components/OverviewChart";
import { TopHostsTable, TopListingsTable } from "../components/OverviewTables";

const TOP_LIMIT = 5;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const BOOKING_ORDER = [
  "CONFIRMED",
  "COMPLETED",
  "PENDING_PAYMENT",
  "CANCELLED",
  "EXPIRED",
];

const LISTING_STATUS = [
  { key: "PUBLISHED", label: "Live", dot: "bg-emerald-500" },
  { key: "PAUSED", label: "Paused", dot: "bg-amber-500" },
  { key: "DRAFT", label: "Draft", dot: "bg-subtext" },
  { key: "SUSPENDED", label: "Suspended", dot: "bg-danger" },
];

// shows the number the server sent: formatting only
const formatPercent = (v) =>
  v == null || v === ""
    ? "—"
    : `${new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 }).format(Number(v))}%`;

const AttentionCard = ({ count, loading }) => {
  const hot = Number(count) > 0;

  return (
    <Link
      to="/admin/payments"
      className={`group flex items-center justify-between gap-4 rounded-2xl border p-5 transition-colors ${focusRing} ${
        hot
          ? "border-danger/50 bg-danger/10 hover:bg-danger/15"
          : "border-border bg-surface hover:bg-bg"
      }`}
    >
      <div>
        <p className="text-sm font-medium text-subtext">
          Payments needing attention
        </p>
        {loading ? (
          <Skeleton className="mt-2 h-9 w-16" />
        ) : (
          <p
            className={`mt-1 font-display text-3xl font-semibold tabular-nums ${
              hot ? "text-danger" : "text-text"
            }`}
          >
            {formatCount(count)}
          </p>
        )}
        {!loading && (
          <p className="mt-1 text-xs text-subtext">
            {hot
              ? "Failed refunds and flagged payments are waiting."
              : "Nothing waiting right now."}
          </p>
        )}
      </div>
      <span className="shrink-0 text-sm font-semibold text-primary group-hover:underline">
        Review →
      </span>
    </Link>
  );
};

const StatBlock = ({ title, note, items, loading }) => (
  <section className="rounded-2xl border border-border bg-surface p-5">
    <h2 className="font-display text-lg font-semibold">{title}</h2>
    {note && <p className="mt-1 text-xs text-subtext">{note}</p>}
    <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
      {items.map(({ label, value, dot }) => (
        <div key={label}>
          <dt className="flex items-center gap-1.5 text-xs text-subtext">
            {dot && (
              <span
                className={`h-2 w-2 rounded-full ${dot}`}
                aria-hidden="true"
              />
            )}
            {label}
          </dt>
          <dd className="mt-0.5 text-xl font-semibold tabular-nums text-text">
            {loading ? <Skeleton className="h-6 w-12" /> : value}
          </dd>
        </div>
      ))}
    </dl>
  </section>
);

const AdminOverview = () => {
  usePageTitle("Overview");
  const navigate = useNavigate();

  const [range, setRange] = useState({ from: "", to: "" });

  const forbiddenRef = useRef(false);
  const handleError = (err) => {
    if (err.response?.status === 403) {
      if (!forbiddenRef.current) {
        forbiddenRef.current = true;
        notify.error("You don't have access to the admin overview.");
        navigate("/explore", { replace: true });
      }
      return;
    }
    // the same id replaces an earlier toast, so four failures show one message
    notify.error(handleApiError(err, "dashboard"), {
      id: "admin-overview-error",
    });
  };

  const rangeKey = `${range.from}|${range.to}`;
  const overview = useLoad(
    () => getAdminOverview(range),
    [rangeKey],
    handleError,
  );
  const monthly = useLoad(
    () => getAdminMonthly(range),
    [rangeKey],
    handleError,
  );
  const topHosts = useLoad(
    () => getTopHosts(range, TOP_LIMIT),
    [rangeKey],
    handleError,
  );
  const topListings = useLoad(
    () => getTopListings(range, TOP_LIMIT),
    [rangeKey],
    handleError,
  );

  const o = overview.data;
  const currency = o?.currency ?? "INR";
  const kpiLoading = overview.loading && !o;
  const money = (v) => (o ? formatMoney(v, currency) : "—");
  const m = o?.money;

  const months = monthly.data ?? [];
  const hosts = topHosts.data ?? [];
  const listings = topListings.data ?? [];

  const chartEmpty =
    months.length === 0 ||
    months.every(
      (x) =>
        Number(x.bookings) === 0 &&
        Number(x.gross) === 0 &&
        Number(x.commission) === 0,
    );

  const byStatus = o?.bookingsByStatus ?? {};
  const statusTotal = BOOKING_ORDER.reduce(
    (n, k) => n + (Number(byStatus[k]) || 0),
    0,
  );

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-6xl space-y-8">
        {/* Header and range picker */}
        <header>
          <h1 className="font-display text-3xl font-semibold">Overview</h1>
          <p className="mt-2 text-subtext">
            {o
              ? `Bookings confirmed from ${formatPlainDate(o.from)} to ${formatPlainDate(o.to)}.`
              : "How the platform is doing."}
          </p>
          <div className="mt-4">
            <DateRangeBar
              onChange={setRange}
              fromLabel="Confirmed from"
              toLabel="Confirmed until"
            />
          </div>
        </header>

        {/* Numbers */}
        <section aria-label="Summary" aria-busy={overview.loading}>
          <div
            className={`space-y-4 ${overview.loading && o ? "opacity-60 transition-opacity" : ""}`}
          >
            <AttentionCard
              count={o?.paymentsNeedingAttention}
              loading={kpiLoading}
            />

            <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
              <Kpi
                label="Sales (gross)"
                value={money(m?.gross)}
                hint={
                  m
                    ? `Average booking ${money(m.averageBookingValue)}`
                    : undefined
                }
                loading={kpiLoading}
              />
              <Kpi
                label="Platform revenue"
                value={money(m?.commission)}
                loading={kpiLoading}
              />
              <Kpi
                label="Owed to hosts"
                value={money(m?.owedToHosts)}
                hint={
                  m
                    ? `Earned ${money(m.earned)} · Pending ${money(m.pending)}`
                    : undefined
                }
                loading={kpiLoading}
              />
              <Kpi
                label="Refunded"
                value={money(m?.refunded)}
                loading={kpiLoading}
              />
              <Kpi
                label="Paid bookings"
                value={m ? formatCount(m.paidBookings) : "—"}
                loading={kpiLoading}
              />
              <Kpi
                label="Cancellation rate"
                value={m ? formatPercent(m.cancellationRatePercent) : "—"}
                hint={
                  m
                    ? `${formatCount(m.fullyRefunded)} fully refunded`
                    : undefined
                }
                loading={kpiLoading}
              />
            </div>
          </div>

          {overview.failed && (
            <div className="mt-4">
              <SectionError onRetry={overview.reload} />
            </div>
          )}
        </section>

        {/* Monthly chart */}
        <section
          aria-labelledby="monthly-heading"
          className="rounded-2xl border border-border bg-surface p-5"
        >
          <h2
            id="monthly-heading"
            className="font-display text-xl font-semibold"
          >
            Sales by month
          </h2>
          <p className="mt-1 text-sm text-subtext">
            Gross sales and platform commission, by the month bookings were
            confirmed.
          </p>

          <div className="mt-4">
            {monthly.loading && months.length === 0 ? (
              <Skeleton className="h-64 w-full" />
            ) : monthly.failed ? (
              <SectionError onRetry={monthly.reload} />
            ) : chartEmpty ? (
              <EmptyState />
            ) : (
              <div
                className={
                  monthly.loading ? "opacity-60 transition-opacity" : ""
                }
              >
                <OverviewChart months={months} currency={currency} />
              </div>
            )}
          </div>
        </section>

        {/* Bookings by status */}
        <section
          aria-labelledby="status-heading"
          className="rounded-2xl border border-border bg-surface p-5"
        >
          <h2
            id="status-heading"
            className="font-display text-xl font-semibold"
          >
            Bookings by status
          </h2>
          <p className="mt-1 text-sm text-subtext">
            Bookings created in this period.
          </p>

          <div className="mt-4">
            {kpiLoading ? (
              <Skeleton className="h-16 w-full" />
            ) : overview.failed ? (
              <SectionError onRetry={overview.reload} />
            ) : statusTotal === 0 ? (
              <EmptyState />
            ) : (
              <>
                <div
                  className="flex h-2 w-full overflow-hidden rounded-full bg-border"
                  aria-hidden="true"
                >
                  {BOOKING_ORDER.filter((k) => Number(byStatus[k]) > 0).map(
                    (k) => (
                      <span
                        key={k}
                        className={`h-full ${BOOKING_STATUS[k]?.dot ?? "bg-subtext"}`}
                        style={{ flexGrow: Number(byStatus[k]), flexBasis: 0 }}
                        title={`${BOOKING_STATUS[k]?.label ?? k}: ${formatCount(byStatus[k])}`}
                      />
                    ),
                  )}
                </div>

                <ul className="mt-4 flex flex-wrap gap-2">
                  {BOOKING_ORDER.map((k) => (
                    <li
                      key={k}
                      className="inline-flex items-center gap-2 rounded-full border border-border bg-bg px-3 py-1.5 text-sm"
                    >
                      <span
                        className={`h-2 w-2 rounded-full ${BOOKING_STATUS[k]?.dot ?? "bg-subtext"}`}
                        aria-hidden="true"
                      />
                      <span className="text-subtext">
                        {BOOKING_STATUS[k]?.label ?? k}
                      </span>
                      <strong className="tabular-nums text-text">
                        {formatCount(byStatus[k])}
                      </strong>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </div>
        </section>

        {/* Users and listings */}
        <div className="grid gap-6 md:grid-cols-2">
          <StatBlock
            title="Users"
            note="Accounts right now; new accounts are those created in this period."
            loading={kpiLoading}
            items={[
              { label: "Accounts", value: formatCount(o?.users?.accounts) },
              { label: "Hosts", value: formatCount(o?.users?.hosts) },
              { label: "Admins", value: formatCount(o?.users?.admins) },
              { label: "Blocked", value: formatCount(o?.users?.blocked) },
              {
                label: "New in this period",
                value: formatCount(o?.users?.newAccounts),
              },
            ]}
          />
          <StatBlock
            title="Listings"
            note="Current numbers, not filtered by date."
            loading={kpiLoading}
            items={LISTING_STATUS.map(({ key, label, dot }) => ({
              label,
              dot,
              value: formatCount(o?.listingsByStatus?.[key]),
            }))}
          />
        </div>

        {/* Top hosts and listings */}
        <section aria-labelledby="hosts-heading">
          <h2
            id="hosts-heading"
            className="mb-3 font-display text-xl font-semibold"
          >
            Top hosts
          </h2>
          {topHosts.loading && hosts.length === 0 ? (
            <TableSkeleton />
          ) : topHosts.failed ? (
            <SectionError onRetry={topHosts.reload} />
          ) : hosts.length === 0 ? (
            <EmptyState />
          ) : (
            <div
              className={
                topHosts.loading ? "opacity-60 transition-opacity" : ""
              }
            >
              <TopHostsTable rows={hosts} currency={currency} />
            </div>
          )}
        </section>

        <section aria-labelledby="listings-heading">
          <h2
            id="listings-heading"
            className="mb-3 font-display text-xl font-semibold"
          >
            Top listings
          </h2>
          {topListings.loading && listings.length === 0 ? (
            <TableSkeleton />
          ) : topListings.failed ? (
            <SectionError onRetry={topListings.reload} />
          ) : listings.length === 0 ? (
            <EmptyState />
          ) : (
            <div
              className={
                topListings.loading ? "opacity-60 transition-opacity" : ""
              }
            >
              <TopListingsTable rows={listings} currency={currency} />
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default AdminOverview;
