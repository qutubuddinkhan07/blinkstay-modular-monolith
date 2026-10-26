import React, { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { FiArrowLeft, FiCheckCircle, FiClock, FiMapPin } from "react-icons/fi";
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
import MockCheckoutDialog from "../components/MockCheckoutDialog";
import {
  BOOKING_STATUS,
  announceBookingsChanged,
  completeMockPayment,
  effectiveStatus,
  getBooking,
  listingExists,
  loadRazorpayCheckout,
  payBooking,
  resetRazorpayCheckout,
  verifyRazorpayPayment,
} from "../bookingService";
import BookingCancelDialog, {
  CancellationNote,
} from "../components/BookingCancelDialog";
import { useAuth } from "../../../context/AuthContext";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";
const primaryLink = `inline-block rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover ${focusRing}`;
const ghostLink = `inline-block rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-text transition-colors hover:bg-input ${focusRing}`;

const mmss = (s) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

// The line under "Need to cancel?": what cancelling would mean right now
const cancellationHint = (status, b) => {
  if (status === "PENDING_PAYMENT") {
    return "You haven't paid yet, so cancelling costs nothing.";
  }
  if (status !== "CONFIRMED" || !b.freeCancellationUntil) return "";

  return new Date(b.freeCancellationUntil) > new Date()
    ? `Free cancellation until ${formatTimestamp(b.freeCancellationUntil)}.`
    : "Free cancellation has ended. Cancelling now gives no refund.";
};

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

// Shown once, right after a successful payment
const ConfirmationPanel = ({ booking: b }) => (
  <section
    role="status"
    aria-labelledby="confirmed-heading"
    className="mt-6 rounded-2xl border border-primary/40 bg-surface p-6"
  >
    <div className="flex items-start gap-3">
      <FiCheckCircle
        className="mt-1 shrink-0 text-primary"
        size={24}
        aria-hidden="true"
      />
      <div className="flex-1">
        <h2
          id="confirmed-heading"
          className="font-display text-2xl font-semibold"
        >
          You're booked!
        </h2>
        <p className="mt-1 text-sm text-subtext">
          Payment received. Keep this reference for check-in:{" "}
          <span className="font-semibold tracking-wide text-text">
            {b.reference}
          </span>
        </p>

        <dl className="mt-4 grid grid-cols-2 gap-x-6 gap-y-3 text-sm sm:grid-cols-4">
          <div>
            <dt className="text-subtext">Dates</dt>
            <dd className="font-semibold">
              {formatDateLabel(b.checkIn)} – {formatDateLabel(b.checkOut)}
            </dd>
          </div>
          <div>
            <dt className="text-subtext">Rooms</dt>
            <dd className="font-semibold">
              {formatRoomType(b.roomType)} × {b.rooms}
            </dd>
          </div>
          <div>
            <dt className="text-subtext">Guests</dt>
            <dd className="font-semibold">{b.guests}</dd>
          </div>
          <div>
            <dt className="text-subtext">Total paid</dt>
            <dd className="font-semibold">
              {formatINR(b.totalAmount, b.currency)}
            </dd>
          </div>
        </dl>

        <Link to="/bookings" className={`mt-5 ${ghostLink}`}>
          View my bookings
        </Link>
      </div>
    </div>
  </section>
);

const BookingPage = () => {
  const { id } = useParams();
  const { user } = useAuth();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null); // { status, message }
  const [hasListing, setHasListing] = useState(false);
  const requestRef = useRef(0); // only the latest request may update state
  const { secondsLeft, startUntil } = useCooldown();

  // Payment
  const [paying, setPaying] = useState(false); // POST /pay running
  const [checkout, setCheckout] = useState(null); // mock provider: /pay response = dialog open
  const [completing, setCompleting] = useState(null); // "SUCCESS" | "FAILURE" | null
  const [checkoutMsg, setCheckoutMsg] = useState(null);
  const [justConfirmed, setJustConfirmed] = useState(false);
  const [notice, setNotice] = useState("");
  const [cancelOpen, setCancelOpen] = useState(false);

  // Razorpay (real provider)
  const [rzpOpen, setRzpOpen] = useState(false); // the window is open, or its result is being checked
  const [verifying, setVerifying] = useState(false); // POST .../verify running
  const rzpRef = useRef(null); // the open Razorpay instance
  const verifyingRef = useRef(false); // readable inside Razorpay's callbacks
  const savedOverflowRef = useRef(null); // page scroll setting from before Razorpay opened (null = nothing to restore)

  /* ------------------------------------------------------------------ */
  /* Razorpay cleanup: one function, used by every exit path             */
  /* ------------------------------------------------------------------ */

  // Closes the instance, removes its overlay and gives the page its scrolling back.
  // Safe to call at any time, including when nothing is open.
  const closeRazorpayDom = useCallback(() => {
    try {
      rzpRef.current?.close?.();
    } catch {
      // already closed
    }
    rzpRef.current = null;

    if (savedOverflowRef.current !== null) {
      document.body.style.overflow = savedOverflowRef.current;
      savedOverflowRef.current = null;
    }
  }, []);

  const endRazorpay = useCallback(() => {
    closeRazorpayDom();
    setRzpOpen(false);
  }, [closeRazorpayDom]);

  // leaving the page closes a payment window that is still open
  useEffect(() => closeRazorpayDom, [closeRazorpayDom]);

  /* ------------------------------------------------------------------ */
  /* Loading the booking                                                 */
  /* ------------------------------------------------------------------ */

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

  // A different booking: start clean
  useEffect(() => {
    setCheckout(null);
    setCheckoutMsg(null);
    setJustConfirmed(false);
    setNotice("");
    setCancelOpen(false);
    endRazorpay();
    window.scrollTo({ top: 0 });
  }, [id, endRazorpay]);

  // Razorpay doesn't always call ondismiss (for example when its checkout fails to open),
  // so also watch whether its window is still on screen
  useEffect(() => {
    if (!rzpOpen || verifying) return;

    const startedAt = Date.now();
    let seen = false; // the window has appeared at least once
    let closedAt = null;

    const timer = setInterval(() => {
      const el = document.querySelector(".razorpay-container");
      const showing =
        Boolean(el) &&
        el.getClientRects().length > 0 &&
        getComputedStyle(el).visibility !== "hidden";

      if (showing) {
        seen = true;
        closedAt = null;
        return;
      }

      if (seen) {
        // gone after being shown. Wait a moment: after a successful payment Razorpay
        // closes first and calls the success handler right after
        closedAt ??= Date.now();
        if (Date.now() - closedAt > 1500) {
          clearInterval(timer);
          endRazorpay();
        }
      } else if (Date.now() - startedAt > 10000) {
        clearInterval(timer);
        endRazorpay();
        resetRazorpayCheckout(); // the next click loads a fresh copy: no page reload needed
        notify.error("The payment window didn't open. Please try again.");
      }
    }, 400);

    return () => clearInterval(timer);
  }, [rzpOpen, verifying, endRazorpay]);

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

  // The booking stopped being payable while the mock dialog sat idle: close it
  useEffect(() => {
    if (checkout && !completing && status && status !== "PENDING_PAYMENT") {
      setCheckout(null);
    }
  }, [checkout, completing, status]);

  /* ------------------------------------------------------------------ */
  /* Payment                                                             */
  /* ------------------------------------------------------------------ */

  // Shared by the mock dialog and Razorpay. inDialog: show failures inside the mock dialog.
  const applyPaymentResult = async (res, { inDialog }) => {
    const paymentStatus = res.paymentStatus;

    if (paymentStatus === "SUCCEEDED") {
      setCheckout(null);
      setJustConfirmed(true);
      notify.success(res.message || "Payment received.");
    } else if (paymentStatus === "REFUNDED") {
      setCheckout(null);
      setNotice("The booking could not be kept, so your payment was refunded.");
      if (res.message) notify.info(res.message);
    } else if (paymentStatus === "FAILED") {
      const text = res.message || "The payment failed. You can try again.";
      if (inDialog) setCheckoutMsg({ tone: "danger", text });
      else notify.error(text);
    } else {
      // CREATED: still processing
      const text = res.message || "Payment is still processing.";
      if (inDialog) setCheckoutMsg({ tone: "info", text });
      else notify.info(text);
    }

    if (paymentStatus === "SUCCEEDED" || paymentStatus === "REFUNDED") {
      announceBookingsChanged();
      await load({ quiet: true });
    }
  };

  // Razorpay hands back the payment id and a signature; the server checks them
  const verifyRazorpay = async (orderId, response) => {
    verifyingRef.current = true;
    setVerifying(true);
    try {
      const res = await verifyRazorpayPayment(orderId, {
        razorpayPaymentId: response.razorpay_payment_id,
        razorpaySignature: response.razorpay_signature,
      });

      // If the server only sends a message, the booking itself tells us what happened
      let paymentStatus = res?.paymentStatus;
      if (!paymentStatus) {
        const fresh = await getBooking(id);
        setBooking(fresh);
        paymentStatus = fresh.status === "CONFIRMED" ? "SUCCEEDED" : "CREATED";
      }
      await applyPaymentResult({ ...res, paymentStatus }, { inDialog: false });
    } catch (err) {
      const message = handleApiError(err, "payment");
      if (err.response?.status === 409) {
        notify.error(message);
      } else {
        // The payment may still have gone through (the provider also tells the server directly)
        notify.error(
          "We couldn't confirm your payment yet. If it went through, this page will update shortly.",
        );
      }
      announceBookingsChanged();
      await load({ quiet: true });
    } finally {
      verifyingRef.current = false;
      setVerifying(false);
      endRazorpay();
    }
  };

  const openRazorpay = async (order) => {
    if (!order.publicKey) {
      notify.error("Online payment isn't set up yet.");
      return;
    }

    let Razorpay;
    try {
      Razorpay = await loadRazorpayCheckout();
    } catch {
      notify.error(
        "Couldn't load the payment window. Check your connection and try again.",
      );
      return;
    }

    // Razorpay wants a hex colour: use the theme's, only if it really is one
    const color = getComputedStyle(document.documentElement)
      .getPropertyValue("--color-primary")
      .trim();
    const theme = /^#[0-9a-f]{3,8}$/i.test(color) ? { color } : undefined;

    try {
      const rzp = new Razorpay({
        key: order.publicKey,
        order_id: order.orderId,
        amount: Math.round(Number(order.amount) * 100), // paise
        currency: order.currency,
        name: "BlinkStay",
        description: order.bookingReference
          ? `Booking ${order.bookingReference}`
          : "Booking",
        prefill: { name: user?.username, email: user?.email },
        theme,
        handler: (response) => verifyRazorpay(order.orderId, response),
        modal: {
          // closed without paying: do nothing, the booking stays on hold
          ondismiss: () => {
            if (!verifyingRef.current) endRazorpay();
          },
        },
      });

      // Razorpay keeps its window open after a failed payment so the user can retry:
      // we only say why
      rzp.on("payment.failed", (resp) => {
        const reason = resp?.error?.description;
        if (reason) notify.error(reason);
      });

      savedOverflowRef.current = document.body.style.overflow;
      rzpRef.current = rzp;
      setRzpOpen(true);
      rzp.open();
    } catch (err) {
      console.error("Razorpay could not open", err);
      endRazorpay();
      resetRazorpayCheckout();
      notify.error("Couldn't open the payment window. Please try again.");
    }
  };

  const handlePayNow = async () => {
    if (paying || rzpOpen) return;
    setPaying(true);
    setNotice("");
    try {
      const data = await payBooking(id);
      if (data.mock) {
        setCheckoutMsg(null);
        setCheckout(data);
      } else {
        await openRazorpay(data);
      }
    } catch (err) {
      notify.error(handleApiError(err, "payment"));
      // "already paid" or "hold expired / cancelled": show the real state
      if (err.response?.status === 409) {
        announceBookingsChanged();
        await load({ quiet: true });
      }
    } finally {
      setPaying(false);
    }
  };

  // Mock provider only
  const finishPayment = async (outcome) => {
    if (!checkout || completing) return;
    setCompleting(outcome);
    setCheckoutMsg(null);
    try {
      const res = await completeMockPayment(checkout.orderId, outcome);
      await applyPaymentResult(res, { inDialog: true });
    } catch (err) {
      const message = handleApiError(err, "payment");
      if (err.response?.status === 409) {
        setCheckout(null);
        notify.error(message);
        announceBookingsChanged();
        await load({ quiet: true });
      } else {
        setCheckoutMsg({ tone: "danger", text: message });
      }
    } finally {
      setCompleting(null);
    }
  };

  /* ------------------------------------------------------------------ */
  /* Render                                                              */
  /* ------------------------------------------------------------------ */

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

  // The server decides cancellable (false once the stay has started or the booking is closed)
  const canCancel =
    b.cancellable && (status === "PENDING_PAYMENT" || status === "CONFIRMED");
  const hint = cancellationHint(status, b);
  const paymentBusy =
    paying || rzpOpen || verifying || Boolean(checkout) || Boolean(completing);

  const handleCancelled = async () => {
    setCancelOpen(false);
    setCheckout(null);
    setJustConfirmed(false);
    // the dialog already told the navbar (BOOKINGS_CHANGED), so only this page needs the fresh booking
    await load({ quiet: true });
  };

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

        {notice && (
          <section
            role="alert"
            className="mt-6 rounded-2xl border border-danger/40 bg-surface p-5"
          >
            <p className="font-semibold">{notice}</p>
          </section>
        )}

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
                  onClick={handlePayNow}
                  disabled={paying || rzpOpen || secondsLeft === 0}
                  className={`mt-4 w-full cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto ${focusRing}`}
                >
                  {paying
                    ? "Starting payment…"
                    : verifying
                      ? "Confirming your payment…"
                      : rzpOpen
                        ? "Payment window open…"
                        : `Pay now · ${formatINR(b.totalAmount, b.currency)}`}
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

        {status === "CONFIRMED" &&
          (justConfirmed ? (
            <ConfirmationPanel booking={b} />
          ) : (
            <section
              role="status"
              className="mt-6 rounded-2xl border border-border bg-surface p-5"
            >
              <h2 className="font-semibold">Your stay is confirmed</h2>
              <p className="mt-1 text-sm text-subtext">
                Confirmed on {formatTimestamp(b.confirmedAt)}.
              </p>
            </section>
          ))}

        {status === "CANCELLED" && (
          <section
            role="status"
            className="mt-6 rounded-2xl border border-border bg-surface p-5"
          >
            <h2 className="font-semibold">
              Cancelled on {formatTimestamp(b.cancelledAt)}
            </h2>
            <CancellationNote booking={b} viewer="guest" />
          </section>
        )}

        {canCancel && (
          <section
            aria-labelledby="cancel-heading"
            className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-border bg-surface p-5"
          >
            <div className="min-w-0">
              <h2 id="cancel-heading" className="font-semibold">
                Need to cancel?
              </h2>
              {hint && <p className="mt-1 text-sm text-subtext">{hint}</p>}
            </div>
            <button
              type="button"
              onClick={() => setCancelOpen(true)}
              disabled={paymentBusy}
              title={
                paymentBusy
                  ? "Finish or close the payment window first"
                  : undefined
              }
              className={`cursor-pointer rounded-lg border border-danger/50 bg-bg px-4 py-2 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
            >
              Cancel booking
            </button>
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
              {b.confirmedAt && (
                <Row label="Confirmed on">{formatTimestamp(b.confirmedAt)}</Row>
              )}
              <Row label="Booked on">{formatTimestamp(b.createdAt)}</Row>
            </dl>
          </section>
        </div>
      </div>

      <MockCheckoutDialog
        checkout={checkout}
        busy={completing}
        message={checkoutMsg}
        onPay={() => finishPayment("SUCCESS")}
        onFail={() => finishPayment("FAILURE")}
        onClose={() => {
          if (!completing) setCheckout(null);
        }}
      />

      <BookingCancelDialog
        open={cancelOpen}
        booking={b}
        role="guest"
        onClose={() => setCancelOpen(false)}
        onDone={handleCancelled}
      />
    </div>
  );
};

export default BookingPage;
