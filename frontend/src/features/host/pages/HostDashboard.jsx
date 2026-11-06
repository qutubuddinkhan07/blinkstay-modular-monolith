import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import { usePageTitle } from "../../../utils/usePageTitle";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { inputCls, labelCls, primaryBtn } from "../../user/formStyles";
import {
  getEarningsByListing,
  getEarningsLedger,
  getEarningsMonthly,
  getEarningsSummary,
} from "../dashboardService";
import {
  PRESETS,
  formatCount,
  formatMoney,
  formatPlainDate,
  rangeForPreset,
} from "../dashboardUtils";
import EarningsChart from "../components/EarningsChart";
import {
  ByListingTable,
  LedgerTable,
  TableSkeleton,
} from "../components/DashboardTables";

const LEDGER_SIZE = 10;

const LEDGER_FILTERS = [
  { value: "", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "EARNED", label: "Earned" },
  { value: "REVERSED", label: "Reversed" },
];

/* One request with its own loading and error state. A new range keeps the old numbers
   on screen (dimmed) until the new ones arrive; a failure clears them. */
const useLoad = (loader, deps, onError) => {
  const [state, setState] = useState({
    data: null,
    loading: true,
    failed: false,
  });
  const [tick, setTick] = useState(0);
  const reqRef = useRef(0); // only the latest request may write state
  const loaderRef = useRef(loader);
  const onErrorRef = useRef(onError);
  loaderRef.current = loader;
  onErrorRef.current = onError;

  useEffect(() => {
    const req = ++reqRef.current;
    setState((s) => ({ ...s, loading: true, failed: false }));

    loaderRef
      .current()
      .then((data) => {
        if (req === reqRef.current)
          setState({ data, loading: false, failed: false });
      })
      .catch((err) => {
        if (req !== reqRef.current) return;
        setState({ data: null, loading: false, failed: true });
        onErrorRef.current?.(err);
      });

    return () => {
      reqRef.current++; // ignore anything still in flight
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  return { ...state, reload: () => setTick((t) => t + 1) };
};

const Skeleton = ({ className = "" }) => (
  <div
    className={`animate-pulse rounded bg-input motion-reduce:animate-none ${className}`}
    aria-hidden="true"
  />
);

const Kpi = ({ label, value, big = false, loading }) => (
  <div
    className={`rounded-2xl border bg-surface ${
      big ? "border-primary/40 p-6" : "border-border p-4"
    }`}
  >
    <p className="text-sm font-medium text-subtext">{label}</p>
    {loading ? (
      <Skeleton className={`mt-2 ${big ? "h-9 w-40" : "h-7 w-28"}`} />
    ) : (
      <p
        className={`mt-1 font-display font-semibold tabular-nums ${
          big ? "text-3xl text-primary" : "text-xl text-text"
        }`}
      >
        {value}
      </p>
    )}
  </div>
);

const SectionError = ({ onRetry }) => (
  <div
    role="alert"
    className="rounded-2xl border border-border bg-surface p-6 text-center"
  >
    <p className="text-sm text-danger">We couldn't load this section.</p>
    <button
      type="button"
      onClick={onRetry}
      className={`mt-3 cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text hover:bg-bg ${focusRing}`}
    >
      Try again
    </button>
  </div>
);

const Empty = () => (
  <div className="rounded-2xl border border-border bg-surface p-8 text-center text-subtext">
    No bookings in this period
  </div>
);

const chip = (active) =>
  `cursor-pointer rounded-md px-4 py-1.5 text-sm font-semibold transition-colors ${focusRing} ${
    active ? "bg-primary text-on-primary" : "text-subtext hover:text-text"
  }`;

const HostDashboard = () => {
  usePageTitle("Earnings");
  const navigate = useNavigate();

  // The range that is applied to the requests. Empty = the server's default.
  const [preset, setPreset] = useState("last12");
  const [range, setRange] = useState({ from: "", to: "" });
  const [draft, setDraft] = useState({ from: "", to: "" }); // custom inputs
  const [rangeError, setRangeError] = useState("");

  // Ledger-only state
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);

  const forbiddenRef = useRef(false);
  const handleError = (err) => {
    if (err.response?.status === 403) {
      if (!forbiddenRef.current) {
        forbiddenRef.current = true;
        notify.error("You don't have access to the earnings dashboard.");
        navigate("/explore", { replace: true });
      }
      return;
    }
    // the same id replaces an earlier toast, so four failures show one message
    notify.error(handleApiError(err, "earnings"), {
      id: "host-dashboard-error",
    });
  };

  const rangeKey = `${range.from}|${range.to}`;
  const summary = useLoad(
    () => getEarningsSummary(range),
    [rangeKey],
    handleError,
  );
  const monthly = useLoad(
    () => getEarningsMonthly(range),
    [rangeKey],
    handleError,
  );
  const byListing = useLoad(
    () => getEarningsByListing(range),
    [rangeKey],
    handleError,
  );
  const ledger = useLoad(
    () => getEarningsLedger({ ...range, status, page, size: LEDGER_SIZE }),
    [rangeKey, status, page],
    handleError,
  );

  // Changing the range or the status always goes back to the first ledger page
  const applyRange = (next) => {
    setRange(next);
    setPage(0);
  };

  const choosePreset = (key) => {
    setPreset(key);
    setRangeError("");
    if (key === "custom") {
      setDraft({ from: range.from, to: range.to });
      return;
    }
    applyRange(rangeForPreset(key));
  };

  const applyCustom = (e) => {
    e.preventDefault();
    if (draft.from && draft.to && draft.from > draft.to) {
      setRangeError("The start date must be before the end date.");
      return;
    }
    setRangeError("");
    applyRange({ from: draft.from, to: draft.to });
  };

  const chooseStatus = (value) => {
    setStatus(value);
    setPage(0);
  };

  const s = summary.data;
  const currency = s?.currency ?? "INR";
  const kpiLoading = summary.loading && !s;
  const money = (v) => (s ? formatMoney(v, currency) : "—");
  const months = monthly.data ?? [];
  const listings = byListing.data ?? [];
  const rows = ledger.data?.content ?? [];

  const chartEmpty =
    months.length === 0 ||
    months.every(
      (m) =>
        Number(m.bookings) === 0 &&
        Number(m.gross) === 0 &&
        Number(m.hostAmount) === 0,
    );

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-6xl space-y-8">
        {/* Header and range picker */}
        <header>
          <h1 className="font-display text-3xl font-semibold">Earnings</h1>
          <p className="mt-2 text-subtext">
            {s
              ? `Stays checking in from ${formatPlainDate(s.from)} to ${formatPlainDate(s.to)}.`
              : "What your listings have earned."}
          </p>

          <div
            className="mt-4 inline-flex flex-wrap rounded-lg border border-border bg-surface p-1"
            role="group"
            aria-label="Date range"
          >
            {PRESETS.map((p) => (
              <button
                key={p.key}
                type="button"
                aria-pressed={preset === p.key}
                onClick={() => choosePreset(p.key)}
                className={chip(preset === p.key)}
              >
                {p.label}
              </button>
            ))}
          </div>

          {preset === "custom" && (
            <form
              onSubmit={applyCustom}
              className="mt-3 flex flex-wrap items-end gap-3"
              noValidate
            >
              <div className="w-40">
                <label
                  htmlFor="range-from"
                  className={`${labelCls} !mb-1 text-xs text-subtext`}
                >
                  Check-in from
                </label>
                <input
                  id="range-from"
                  type="date"
                  value={draft.from}
                  max={draft.to || undefined}
                  onChange={(e) =>
                    setDraft((d) => ({ ...d, from: e.target.value }))
                  }
                  className={inputCls}
                />
              </div>
              <div className="w-40">
                <label
                  htmlFor="range-to"
                  className={`${labelCls} !mb-1 text-xs text-subtext`}
                >
                  Check-in until
                </label>
                <input
                  id="range-to"
                  type="date"
                  value={draft.to}
                  min={draft.from || undefined}
                  onChange={(e) =>
                    setDraft((d) => ({ ...d, to: e.target.value }))
                  }
                  className={inputCls}
                />
              </div>
              <button type="submit" className={`cursor-pointer ${primaryBtn}`}>
                Apply
              </button>
              {rangeError && (
                <p role="alert" className="w-full text-sm text-danger">
                  {rangeError}
                </p>
              )}
            </form>
          )}
        </header>

        {/* KPI cards */}
        <section aria-label="Summary">
          <div
            className={`space-y-4 ${summary.loading && s ? "opacity-60 transition-opacity" : ""}`}
            aria-busy={summary.loading}
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <Kpi
                big
                label="Earned"
                value={money(s?.earned)}
                loading={kpiLoading}
              />
              <Kpi
                big
                label="Pending"
                value={money(s?.pending)}
                loading={kpiLoading}
              />
            </div>
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
              <Kpi
                label="Total bookings"
                value={s ? formatCount(s.bookings) : "—"}
                loading={kpiLoading}
              />
              <Kpi
                label="Average booking value"
                value={money(s?.averageBookingValue)}
                loading={kpiLoading}
              />
              <Kpi
                label="Commission paid"
                value={money(s?.commission)}
                loading={kpiLoading}
              />
              <Kpi
                label="Cancelled"
                value={s ? formatCount(s.cancelled) : "—"}
                loading={kpiLoading}
              />
            </div>
          </div>

          {summary.failed && (
            <div className="mt-4">
              <SectionError onRetry={summary.reload} />
            </div>
          )}

          <p className="mt-4 text-sm text-subtext">
            Earned = stays that are over. Pending = paid, stay not finished yet.
            Payouts to your bank are not enabled yet.
          </p>
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
            Earnings by month
          </h2>
          <p className="mt-1 text-sm text-subtext">
            What you earn after commission, by check-in month.
          </p>

          <div className="mt-4">
            {monthly.loading && months.length === 0 ? (
              <Skeleton className="h-64 w-full" />
            ) : monthly.failed ? (
              <SectionError onRetry={monthly.reload} />
            ) : chartEmpty ? (
              <Empty />
            ) : (
              <div
                className={
                  monthly.loading ? "opacity-60 transition-opacity" : ""
                }
              >
                <EarningsChart months={months} currency={currency} />
              </div>
            )}
          </div>
        </section>

        {/* By listing */}
        <section aria-labelledby="listing-heading">
          <h2
            id="listing-heading"
            className="mb-3 font-display text-xl font-semibold"
          >
            By listing
          </h2>

          {byListing.loading && listings.length === 0 ? (
            <TableSkeleton />
          ) : byListing.failed ? (
            <SectionError onRetry={byListing.reload} />
          ) : listings.length === 0 ? (
            <Empty />
          ) : (
            <div
              className={
                byListing.loading ? "opacity-60 transition-opacity" : ""
              }
            >
              <ByListingTable rows={listings} currency={currency} />
            </div>
          )}
        </section>

        {/* Ledger */}
        <section aria-labelledby="ledger-heading">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
            <h2
              id="ledger-heading"
              className="font-display text-xl font-semibold"
            >
              Ledger
              {ledger.data?.totalElements != null && (
                <span className="ml-2 text-sm font-normal text-subtext">
                  {formatCount(ledger.data.totalElements)} total
                </span>
              )}
            </h2>

            <div
              className="inline-flex flex-wrap rounded-lg border border-border bg-surface p-1"
              role="group"
              aria-label="Filter the ledger by status"
            >
              {LEDGER_FILTERS.map((f) => (
                <button
                  key={f.value || "all"}
                  type="button"
                  aria-pressed={status === f.value}
                  onClick={() => chooseStatus(f.value)}
                  className={chip(status === f.value)}
                >
                  {f.label}
                </button>
              ))}
            </div>
          </div>

          {ledger.loading && rows.length === 0 ? (
            <TableSkeleton rows={5} />
          ) : ledger.failed ? (
            <SectionError onRetry={ledger.reload} />
          ) : rows.length === 0 ? (
            <Empty />
          ) : (
            <div
              className={ledger.loading ? "opacity-60 transition-opacity" : ""}
              aria-busy={ledger.loading}
            >
              <LedgerTable rows={rows} />
              <Pagination
                page={page}
                totalPages={ledger.data?.totalPages ?? 0}
                onChange={setPage}
              />
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default HostDashboard;
