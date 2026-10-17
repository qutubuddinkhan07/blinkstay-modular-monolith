import React, { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import StatusBadge from "../../../components/common/StatusBadge";
import { handleApiError } from "../../../api/errors/handleApiError";
import {
  formatDateLabel,
  formatINR,
  formatTime,
  plural,
} from "../../../utils/format";
import {
  BOOKING_STATUS,
  effectiveStatus,
  getMyBookings,
} from "../bookingService";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
const PAGE_SIZE = 10;

const TABS = [
  {
    key: "upcoming",
    label: "Upcoming",
    query: {
      status: "PENDING_PAYMENT,CONFIRMED",
      sort: "checkIn",
      direction: "asc",
    },
    empty: "No upcoming trips yet.",
  },
  {
    key: "past",
    label: "Past",
    query: { status: "COMPLETED", sort: "checkIn", direction: "desc" },
    empty: "Your completed stays will show up here.",
  },
  {
    key: "cancelled",
    label: "Cancelled",
    query: {
      status: "CANCELLED,EXPIRED",
      sort: "createdAt",
      direction: "desc",
    },
    empty: "No cancelled or expired bookings.",
  },
];

const BookingRow = ({ booking: b }) => {
  const status = effectiveStatus(b);
  return (
    <li>
      <Link
        to={`/bookings/${b.id}`}
        className={`flex gap-4 rounded-2xl border border-border bg-surface p-3 transition-colors hover:bg-input sm:p-4 ${focusRing}`}
      >
        <img
          src={b.coverImageUrl}
          alt=""
          loading="lazy"
          className="h-24 w-24 shrink-0 rounded-xl bg-input object-cover sm:h-28 sm:w-36"
        />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <h2 className="truncate font-semibold text-text">
              {b.listingTitle}
            </h2>
            <StatusBadge status={status} statuses={BOOKING_STATUS} />
          </div>
          <p className="truncate text-sm text-subtext">{b.listingLocation}</p>
          <p className="mt-1 text-sm text-text">
            {formatDateLabel(b.checkIn)} – {formatDateLabel(b.checkOut)}
          </p>
          <p className="text-sm text-subtext">
            {plural(b.nights, "night")} · {plural(b.guests, "guest")} ·{" "}
            <span className="font-semibold text-text">
              {formatINR(b.totalAmount, b.currency)}
            </span>
          </p>
          <p className="mt-1 text-xs text-subtext">
            {b.reference}
            {status === "PENDING_PAYMENT" &&
              ` · Pay by ${formatTime(b.holdExpiresAt)}`}
          </p>
        </div>
      </Link>
    </li>
  );
};

const MyBookings = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = TABS.find((t) => t.key === searchParams.get("tab")) ?? TABS[0];
  const page = Math.max(0, Number.parseInt(searchParams.get("page"), 10) || 0);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError("");

    getMyBookings({ ...tab.query, page, size: PAGE_SIZE })
      .then((res) => {
        if (!ignore) setData(res);
      })
      .catch((err) => {
        if (!ignore) setError(handleApiError(err, "bookings"));
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [tab, page, reloadKey]);

  const goTo = (next) => {
    setSearchParams({ tab: tab.key, page: String(next) });
    window.scrollTo({ top: 0 });
  };

  const items = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="min-h-screen bg-bg text-text">
      <div className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6">
        <h1 className="font-display text-3xl font-semibold">My bookings</h1>

        <div
          role="tablist"
          aria-label="Booking status"
          className="mt-6 flex gap-2 border-b border-border"
        >
          {TABS.map((t) => {
            const active = t.key === tab.key;
            return (
              <button
                key={t.key}
                type="button"
                role="tab"
                id={`tab-${t.key}`}
                aria-selected={active}
                aria-controls="bookings-panel"
                onClick={() => setSearchParams({ tab: t.key })}
                className={`-mb-px cursor-pointer border-b-2 px-4 py-2.5 text-sm font-semibold transition-colors ${focusRing} ${
                  active
                    ? "border-primary text-text"
                    : "border-transparent text-subtext hover:text-text"
                }`}
              >
                {t.label}
              </button>
            );
          })}
        </div>

        <section
          id="bookings-panel"
          role="tabpanel"
          aria-labelledby={`tab-${tab.key}`}
          aria-busy={loading}
          className="mt-6"
        >
          {loading && (
            <ul className="space-y-3" aria-hidden="true">
              {[0, 1, 2].map((i) => (
                <li
                  key={i}
                  className="h-32 animate-pulse rounded-2xl bg-input"
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
                onClick={() => setReloadKey((k) => k + 1)}
                className={`mt-4 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary hover:bg-primary-hover ${focusRing}`}
              >
                Try again
              </button>
            </div>
          )}

          {!loading && !error && items.length === 0 && (
            <div className="rounded-2xl border border-border bg-surface p-8 text-center">
              <p className="text-subtext">{tab.empty}</p>
              {tab.key === "upcoming" && (
                <Link
                  to="/explore"
                  className={`mt-4 inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary hover:bg-primary-hover ${focusRing}`}
                >
                  Find a place to stay
                </Link>
              )}
            </div>
          )}

          {!loading && !error && items.length > 0 && (
            <ul className="space-y-3">
              {items.map((b) => (
                <BookingRow key={b.id} booking={b} />
              ))}
            </ul>
          )}

          {!loading && !error && totalPages > 1 && (
            <nav
              aria-label="Pages"
              className="mt-6 flex items-center justify-between text-sm"
            >
              <button
                type="button"
                onClick={() => goTo(page - 1)}
                disabled={page === 0}
                className={`cursor-pointer rounded-lg border border-border px-4 py-2 font-medium hover:bg-input disabled:cursor-not-allowed disabled:opacity-40 ${focusRing}`}
              >
                Previous
              </button>
              <span className="text-subtext">
                Page {page + 1} of {totalPages}
              </span>
              <button
                type="button"
                onClick={() => goTo(page + 1)}
                disabled={page + 1 >= totalPages}
                className={`cursor-pointer rounded-lg border border-border px-4 py-2 font-medium hover:bg-input disabled:cursor-not-allowed disabled:opacity-40 ${focusRing}`}
              >
                Next
              </button>
            </nav>
          )}
        </section>
      </div>
    </div>
  );
};

export default MyBookings;
