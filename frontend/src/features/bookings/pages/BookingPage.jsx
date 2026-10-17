import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FiArrowLeft, FiClock, FiMapPin } from "react-icons/fi";
import Loading from "../../../components/common/Loading";
import StatusBadge from "../../../components/common/StatusBadge";
import { handleApiError } from "../../../api/errors/handleApiError";
import { notify } from "../../../utils/notify";
import { useCooldown } from "../../../utils/useCooldown";
import {
  formatDateLabel,
  formatINR,
  formatRoomType,
  formatTime,
  formatTimestamp,
  plural,
} from "../../../utils/format";
import {
  BOOKING_STATUS,
  effectiveStatus,
  getBooking,
  listingExists,
} from "../bookingService";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
const primaryLink = `inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover ${focusRing}`;
const ghostLink = `inline-block rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-text transition-colors hover:bg-input ${focusRing}`;

const mmss = (s) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

const Centered = ({ title, text, children }) => (
  <div className="flex min-h-[60vh] items-center justify-center px-4 text-center text-text">
    <div className="max-w-md">
      <h1 className="font-display text-2xl font-semibold">{title}</h1>
      {text && <p className="mt-2 text-subtext">{text}</p>}
      <div className="mt-5">{children}</div>
    </div>
  </div>
);

const Row = ({ label, children, strong }) => (
  <div className="flex justify-between gap-4 py-2.5">
    <dt className="text-subtext">{label}</dt>
    <dd
      className={`text-right ${strong ? "font-semibold text-text" : "text-text"}`}
    >
      {children}
    </dd>
  </div>
);

const BookingPage = () => {
  const { id } = useParams();
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null); // { status, message }
  const [hasListing, setHasListing] = useState(false);
  const requestRef = useRef(0); // only the latest request may update state
  const { secondsLeft, startUntil } = useCooldown();

  // quiet = refresh in the background (keep showing the booking, toast on failure)
  const load = useCallback(
    async ({ quiet = false } = {}) => {
      const req = ++requestRef.current;
      if (!quiet) setLoading(true);
      try {
        const data = await getBooking(id);
        if (req !== requestRef.current) return;
        setBooking(data);
        setError(null);
      } catch (err) {
        if (req !== requestRef.current) return;
        const message = handleApiError(err, "booking");
        if (quiet) notify.error(message);
        else setError({ status: err.response?.status, message });
      } finally {
        if (req === requestRef.current) setLoading(false);
      }
    },
    [id],
  );

  useEffect(() => {
    load();
  }, [load]);

  // Link to the listing only if it's still open to guests
  const listingId = booking?.listingId;
  useEffect(() => {
    if (!listingId) return;
    let ignore = false;
    listingExists(listingId).then((ok) => {
      if (!ignore) setHasListing(ok);
    });
    return () => {
      ignore = true;
    };
  }, [listingId]);

  const status = booking ? effectiveStatus(booking) : null;
  const holdEndsAt =
    status === "PENDING_PAYMENT" && booking.holdExpiresAt
      ? new Date(booking.holdExpiresAt).getTime()
      : null;

  // Visible countdown
  useEffect(() => {
    if (holdEndsAt) startUntil(holdEndsAt);
  }, [holdEndsAt, startUntil]);

  // When the hold ends, ask the server (it then says payable=false -> "expired")
  useEffect(() => {
    if (!holdEndsAt) return;
    const timer = setTimeout(
      () => load({ quiet: true }),
      Math.max(0, holdEndsAt - Date.now()) + 1000,
    );
    return () => clearTimeout(timer);
  }, [holdEndsAt, load]);

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [id]);

  if (loading) return <Loading />;

  if (error?.status === 404) {
    return (
      <Centered
        title="Booking not found"
        text="This booking doesn't exist, or it belongs to another account."
      >
        <Link to="/bookings" className={primaryLink}>
          My bookings
        </Link>
      </Centered>
    );
  }

  if (error || !booking) {
    return (
      <Centered title="Couldn't load this booking" text={error?.message}>
        <button
          type="button"
          onClick={() => load()}
          className={`cursor-pointer ${primaryLink}`}
        >
          Try again
        </button>
      </Centered>
    );
  }

  const b = booking;

  return (
    <div className="min-h-screen bg-bg text-text">
      <div className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6">
        <Link
          to="/bookings"
          className={`inline-flex items-center gap-1.5 rounded-md text-sm font-medium text-subtext hover:text-text ${focusRing}`}
        >
          <FiArrowLeft aria-hidden="true" /> My bookings
        </Link>

        <header className="mt-4 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="font-display text-2xl font-semibold sm:text-3xl">
              Your booking
            </h1>
            <p className="mt-1 text-sm text-subtext">
              Reference{" "}
              <span className="font-semibold tracking-wide text-text">
                {b.reference}
              </span>
            </p>
          </div>
          <StatusBadge status={status} statuses={BOOKING_STATUS} />
        </header>

        {/* What happens next, per status */}
        {status === "PENDING_PAYMENT" && (
          <section
            aria-labelledby="hold-heading"
            className="mt-6 rounded-2xl border border-primary/40 bg-surface p-5"
          >
            <div className="flex items-start gap-3">
              <FiClock
                className="mt-1 shrink-0 text-primary"
                size={20}
                aria-hidden="true"
              />
              <div className="flex-1">
                <h2 id="hold-heading" className="font-semibold">
                  {secondsLeft > 0 ? (
                    <>
                      Complete payment within{" "}
                      <span role="timer" className="tabular-nums text-primary">
                        {mmss(secondsLeft)}
                      </span>
                    </>
                  ) : (
                    "Your hold is ending…"
                  )}
                </h2>
                <p className="mt-1 text-sm text-subtext">
                  Your rooms are held until {formatTime(b.holdExpiresAt)}. After
                  that they're released to other guests.
                </p>
                <button
                  type="button"
                  disabled
                  className="mt-4 w-full cursor-not-allowed rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary opacity-50 sm:w-auto"
                >
                  Payment coming soon
                </button>
              </div>
            </div>
          </section>
        )}

        {status === "EXPIRED" && (
          <section
            role="status"
            className="mt-6 rounded-2xl border border-danger/40 bg-surface p-5"
          >
            <h2 className="font-semibold">
              This booking expired, the rooms were released.
            </h2>
            <p className="mt-1 text-sm text-subtext">
              The payment wasn't completed in time. You can pick your dates
              again.
            </p>
            <Link
              to={
                hasListing
                  ? `/listings/${b.listingId}?checkIn=${b.checkIn}&checkOut=${b.checkOut}&rooms=${b.rooms}&guests=${b.guests}`
                  : "/explore"
              }
              className={`mt-4 ${primaryLink}`}
            >
              {hasListing ? "Book again" : "Browse listings"}
            </Link>
          </section>
        )}

        {status === "CONFIRMED" && (
          <section
            role="status"
            className="mt-6 rounded-2xl border border-border bg-surface p-5"
          >
            <h2 className="font-semibold">Your stay is confirmed</h2>
            <p className="mt-1 text-sm text-subtext">
              Confirmed on {formatTimestamp(b.confirmedAt)}.
            </p>
          </section>
        )}

        {status === "CANCELLED" && (
          <section
            role="status"
            className="mt-6 rounded-2xl border border-border bg-surface p-5"
          >
            <h2 className="font-semibold">
              Cancelled on {formatTimestamp(b.cancelledAt)}
            </h2>
            {b.cancellationReason && (
              <p className="mt-1 text-sm text-subtext">
                Reason: {b.cancellationReason}
              </p>
            )}
            {b.refundAmount != null && (
              <p className="mt-1 text-sm text-subtext">
                Refund: {formatINR(b.refundAmount, b.currency)}
              </p>
            )}
          </section>
        )}

        <div className="mt-6 grid gap-6 md:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
          {/* Snapshot saved with the booking: stays correct even if the listing changes */}
          <article className="overflow-hidden rounded-2xl border border-border bg-surface">
            {b.coverImageUrl && (
              <img
                src={b.coverImageUrl}
                alt=""
                className="aspect-[4/3] w-full object-cover"
                loading="lazy"
              />
            )}
            <div className="p-5">
              <h2 className="font-display text-lg font-semibold">
                {b.listingTitle}
              </h2>
              <p className="mt-1 flex items-center gap-1.5 text-sm text-subtext">
                <FiMapPin aria-hidden="true" /> {b.listingLocation}
              </p>
              {hasListing && (
                <Link
                  to={`/listings/${b.listingId}`}
                  className={`mt-4 ${ghostLink}`}
                >
                  View listing
                </Link>
              )}
            </div>
          </article>

          <section
            aria-labelledby="details-heading"
            className="rounded-2xl border border-border bg-surface p-5"
          >
            <h2
              id="details-heading"
              className="font-display text-lg font-semibold"
            >
              Stay details
            </h2>
            <dl className="mt-3 divide-y divide-border text-sm">
              <Row label="Check-in">{formatDateLabel(b.checkIn)}</Row>
              <Row label="Check-out">{formatDateLabel(b.checkOut)}</Row>
              <Row label="Length">{plural(b.nights, "night")}</Row>
              <Row label="Room">
                {formatRoomType(b.roomType)} × {b.rooms}
              </Row>
              <Row label="Guests">{b.guests}</Row>
              <Row label="Price per night">
                {formatINR(b.pricePerNight, b.currency)} per room
              </Row>
              <Row label="Total" strong>
                {formatINR(b.totalAmount, b.currency)}
              </Row>
              <Row label="Booked on">{formatTimestamp(b.createdAt)}</Row>
            </dl>
          </section>
        </div>
      </div>
    </div>
  );
};

export default BookingPage;
