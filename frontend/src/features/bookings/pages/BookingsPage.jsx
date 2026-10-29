import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { FiSearch, FiX } from "react-icons/fi";
import StatusBadge from "../../../components/common/StatusBadge";
import { handleApiError } from "../../../api/errors/handleApiError";
import { formatDateLabel, formatINR, plural } from "../../../utils/format";
import { useGetMyListingsQuery } from "../../../store/api";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { inputCls, labelCls, primaryBtn } from "../../user/formStyles";
import {
  BOOKING_STATUS,
  effectiveStatus,
  getAdminBookings,
  getHostBookings,
} from "../bookingService";
import BookingCancelDialog, {
  CancellationNote,
} from "../components/BookingCancelDialog";
import { usePageTitle } from "../../../utils/usePageTitle";

const PAGE_SIZE = 10;

const MODES = {
  host: {
    title: "Reservations",
    subtitle:
      "Guests who booked your listings. Unpaid and expired holds aren't shown.",
    fetch: getHostBookings,
    statuses: ["CONFIRMED", "COMPLETED", "CANCELLED"],
  },
  admin: {
    title: "All bookings",
    subtitle: "Search every booking on the platform.",
    fetch: getAdminBookings,
    statuses: [
      "PENDING_PAYMENT",
      "CONFIRMED",
      "COMPLETED",
      "CANCELLED",
      "EXPIRED",
    ],
  },
};

const statusLabel = (s) =>
  BOOKING_STATUS?.[s]?.label ??
  s.charAt(0) + s.slice(1).toLowerCase().replace("_", " ");

const Chip = ({ label, onRemove }) => (
  <span className="inline-flex items-center gap-1.5 rounded-full border border-border bg-surface px-3 py-1 text-xs font-medium text-text">
    {label}
    <button
      type="button"
      onClick={onRemove}
      aria-label={`Remove filter: ${label}`}
      className={`cursor-pointer rounded-full text-subtext hover:text-text ${focusRing}`}
    >
      <FiX size={14} aria-hidden="true" />
    </button>
  </span>
);

const BookingRow = ({ booking: b, mode, onCancel }) => {
  const status = effectiveStatus(b);

  return (
    <li className="rounded-2xl border border-border bg-surface p-4 transition-colors duration-300 sm:p-5">
      <div className="flex flex-col gap-4 md:flex-row md:items-start">
        <div className="flex min-w-0 flex-1 gap-4">
          {b.coverImageUrl && (
            <img
              src={b.coverImageUrl}
              alt=""
              loading="lazy"
              className="h-24 w-24 shrink-0 rounded-xl bg-input object-cover sm:h-28 sm:w-36"
            />
          )}

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <h2 className="truncate font-semibold text-text">
                {b.listingId ? (
                  <Link
                    to={`/listings/${b.listingId}`}
                    className={`rounded hover:underline ${focusRing}`}
                  >
                    {b.listingTitle}
                  </Link>
                ) : (
                  b.listingTitle
                )}
              </h2>
              <StatusBadge status={status} statuses={BOOKING_STATUS} />
            </div>

            {b.listingLocation && (
              <p className="truncate text-sm text-subtext">
                {b.listingLocation}
              </p>
            )}

            <p className="mt-1 text-sm text-text">
              {formatDateLabel(b.checkIn)} – {formatDateLabel(b.checkOut)}
            </p>
            <p className="text-sm text-subtext">
              {b.nights != null && plural(b.nights, "night")}
              {b.guests != null && ` · ${plural(b.guests, "guest")}`}
            </p>

            <p className="mt-1 text-sm text-subtext">
              {b.guestName && (
                <>
                  Guest:{" "}
                  <span className="font-medium text-text">{b.guestName}</span>
                  {mode === "admin" && b.guestEmail && (
                    <span className="break-all"> ({b.guestEmail})</span>
                  )}
                </>
              )}
              {mode === "admin" && b.hostName && (
                <>
                  {b.guestName ? " · " : ""}
                  Host:{" "}
                  <span className="font-medium text-text">{b.hostName}</span>
                </>
              )}
            </p>

            {b.reference && (
              <p className="mt-1 font-mono text-xs text-subtext">
                {b.reference}
              </p>
            )}
          </div>
        </div>

        <div className="flex shrink-0 items-center justify-between gap-3 md:flex-col md:items-end">
          {b.totalAmount != null && (
            <p className="text-lg font-semibold text-text">
              {formatINR(b.totalAmount, b.currency)}
            </p>
          )}
          {b.cancellable && (
            <button
              type="button"
              onClick={() => onCancel(b)}
              className={`cursor-pointer rounded-lg border border-danger/50 bg-bg px-3 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 ${focusRing}`}
            >
              Cancel booking
            </button>
          )}
        </div>
      </div>

      <CancellationNote booking={b} />
    </li>
  );
};

const BookingsPage = ({ mode = "host" }) => {
  const cfg = MODES[mode];
  if (!cfg) throw new Error('BookingsPage needs mode="host" or mode="admin"');

  usePageTitle(cfg.title);

  const isAdmin = mode === "admin";

  const [params, setParams] = useSearchParams();
  const page = parseInt(params.get("page") || "0", 10) || 0;
  const status = params.get("status") || "";
  const q = params.get("q") || "";
  const listingId = params.get("listingId") || "";
  const from = params.get("from") || "";
  const to = params.get("to") || "";
  const guestId = params.get("guestId") || "";
  const hostId = params.get("hostId") || "";

  const [search, setSearch] = useState(q);
  const [rows, setRows] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [toCancel, setToCancel] = useState(null);
  const reqRef = useRef(0); // only the latest request may write state

  // the host's own listings fill the listing filter (already cached by My listings)
  const { data: myListings = [] } = useGetMyListingsQuery(undefined, {
    skip: isAdmin,
  });

  // set or clear one filter; any filter change goes back to the first page
  const setFilter = (key, value) =>
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (value) next.set(key, value);
        else next.delete(key);
        next.delete("page");
        return next;
      },
      { replace: true },
    );

  const goToPage = (p) => {
    setParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set("page", String(p));
      return next;
    });
    window.scrollTo({ top: 0 });
  };

  // admin search: wait until typing stops
  useEffect(() => {
    const term = search.trim();
    if (term === q) return;
    const t = setTimeout(() => setFilter("q", term), 400);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  useEffect(() => {
    const req = ++reqRef.current;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const data = await cfg.fetch({
          page,
          size: PAGE_SIZE,
          status,
          ...(isAdmin ? { q, guestId, hostId } : { listingId, from, to }),
        });
        if (req !== reqRef.current) return;

        const list = Array.isArray(data) ? data : (data?.content ?? []);
        const pages = data?.totalPages ?? 0;

        // the last page emptied out (after a cancel): step back one page
        if (list.length === 0 && page > 0 && pages > 0) {
          goToPage(pages - 1);
          return;
        }

        setRows(list);
        setTotalPages(pages);
        setTotalElements(data?.totalElements ?? null);
      } catch (err) {
        if (req !== reqRef.current) return;
        setError(handleApiError(err, "bookings"));
      } finally {
        if (req === reqRef.current) setLoading(false);
      }
    };

    load();
    return () => {
      reqRef.current++; // ignore anything still in flight
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.toString(), reloadKey, mode]);

  const reload = () => setReloadKey((k) => k + 1);
  const filtered = Boolean(
    status || q || listingId || from || to || guestId || hostId,
  );
  const firstLoad = loading && rows.length === 0;

  const clearAll = () => {
    setSearch("");
    setParams({}, { replace: true });
  };

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <h1 className="font-display text-3xl font-semibold">{cfg.title}</h1>
          <p className="mt-2 text-subtext">
            {cfg.subtitle}
            {totalElements != null && ` ${totalElements} found.`}
          </p>
        </div>

        {/* Filters */}
        <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {isAdmin && (
            <div className="relative sm:col-span-2">
              <FiSearch
                className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-subtext"
                size={16}
                aria-hidden="true"
              />
              <input
                type="search"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search by reference, e.g. BS-"
                aria-label="Search bookings by reference"
                className={`${inputCls} pl-9`}
              />
            </div>
          )}

          <div>
            <label htmlFor="f-status" className="sr-only">
              Status
            </label>
            <select
              id="f-status"
              value={status}
              onChange={(e) => setFilter("status", e.target.value)}
              className={inputCls}
            >
              <option value="">All statuses</option>
              {cfg.statuses.map((s) => (
                <option key={s} value={s}>
                  {statusLabel(s)}
                </option>
              ))}
            </select>
          </div>

          {!isAdmin && (
            <>
              <div>
                <label htmlFor="f-listing" className="sr-only">
                  Listing
                </label>
                <select
                  id="f-listing"
                  value={listingId}
                  onChange={(e) => setFilter("listingId", e.target.value)}
                  className={inputCls}
                >
                  <option value="">All listings</option>
                  {myListings.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.title}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="f-from"
                  className={`${labelCls} !mb-1 text-xs text-subtext`}
                >
                  Stays from
                </label>
                <input
                  id="f-from"
                  type="date"
                  value={from}
                  max={to || undefined}
                  onChange={(e) => setFilter("from", e.target.value)}
                  className={inputCls}
                />
              </div>

              <div>
                <label
                  htmlFor="f-to"
                  className={`${labelCls} !mb-1 text-xs text-subtext`}
                >
                  Stays until
                </label>
                <input
                  id="f-to"
                  type="date"
                  value={to}
                  min={from || undefined}
                  onChange={(e) => setFilter("to", e.target.value)}
                  className={inputCls}
                />
              </div>
            </>
          )}
        </div>

        {isAdmin && (guestId || hostId) && (
          <div className="mb-4 flex flex-wrap gap-2">
            {guestId && (
              <Chip
                label={`Guest ${guestId.slice(0, 8)}…`}
                onRemove={() => setFilter("guestId", "")}
              />
            )}
            {hostId && (
              <Chip
                label={`Host ${hostId.slice(0, 8)}…`}
                onRemove={() => setFilter("hostId", "")}
              />
            )}
          </div>
        )}

        {firstLoad && (
          <ul className="space-y-3" aria-busy="true">
            {[0, 1, 2].map((i) => (
              <li
                key={i}
                className="h-36 animate-pulse rounded-2xl bg-input motion-reduce:animate-none"
              />
            ))}
          </ul>
        )}

        {!loading && error && (
          <div
            role="alert"
            className="rounded-2xl border border-border bg-surface p-6 text-center"
          >
            <p className="text-danger">{error}</p>
            <button
              type="button"
              onClick={reload}
              className={`mt-4 cursor-pointer ${primaryBtn}`}
            >
              Try again
            </button>
          </div>
        )}

        {!loading && !error && rows.length === 0 && (
          <div className="rounded-2xl border border-border bg-surface p-8 text-center text-subtext">
            <p>
              {filtered
                ? "No bookings match these filters."
                : isAdmin
                  ? "No bookings yet."
                  : "No reservations yet. They'll show up here once guests book your listings."}
            </p>
            {filtered && (
              <button
                type="button"
                onClick={clearAll}
                className={`mt-4 cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text hover:bg-bg ${focusRing}`}
              >
                Clear filters
              </button>
            )}
          </div>
        )}

        {!error && rows.length > 0 && (
          <div
            className={loading ? "opacity-60 transition-opacity" : ""}
            aria-busy={loading}
          >
            <ul className="space-y-3">
              {rows.map((b) => (
                <BookingRow
                  key={b.id}
                  booking={b}
                  mode={mode}
                  onCancel={setToCancel}
                />
              ))}
            </ul>
            <Pagination
              page={page}
              totalPages={totalPages}
              onChange={goToPage}
            />
          </div>
        )}
      </div>

      <BookingCancelDialog
        open={Boolean(toCancel)}
        booking={toCancel}
        role={isAdmin ? "admin" : "host"}
        onClose={() => setToCancel(null)}
        onDone={() => {
          setToCancel(null);
          reload();
        }}
      />
    </div>
  );
};

export default BookingsPage;
