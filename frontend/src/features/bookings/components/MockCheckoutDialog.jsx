import { useEffect } from "react";
import { createPortal } from "react-dom";
import { FiInfo, FiX } from "react-icons/fi";
import { useCooldown } from "../../../utils/useCooldown";
import { formatINR as formatAmount } from "../../../utils/format";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const mmss = (s) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

// const formatAmount = (amount, currency) =>
//   new Intl.NumberFormat("en-IN", {
//     style: "currency",
//     currency: currency || "INR",
//   }).format(Number(amount) || 0);

// checkout: the payment-session response ({ bookingReference, amount, currency, holdExpiresAt, orderId, ... })
// busy: "SUCCESS" | "FAILURE" | null (which button's request is running)
// message: { tone: "danger" | "info", text } | null
const MockCheckoutDialog = ({
  checkout,
  busy,
  message,
  onPay,
  onFail,
  onClose,
}) => {
  const { secondsLeft, startUntil } = useCooldown();
  const open = Boolean(checkout);

  const holdEndsAt = checkout?.holdExpiresAt
    ? new Date(checkout.holdExpiresAt).getTime()
    : null;
  const hasHold = Number.isFinite(holdEndsAt);

  // the same countdown the Razorpay screen shows
  useEffect(() => {
    if (hasHold) startUntil(holdEndsAt);
  }, [hasHold, holdEndsAt, startUntil]);

  // lock page scroll, close on Escape (never while a request is running)
  useEffect(() => {
    if (!open) return;

    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKey = (e) => {
      if (e.key === "Escape" && !busy) onClose?.();
    };
    document.addEventListener("keydown", onKey);

    return () => {
      document.body.style.overflow = previous;
      document.removeEventListener("keydown", onKey);
    };
  }, [open, busy, onClose]);

  if (!open) return null;

  // re-evaluated on every tick of the countdown
  const expired = hasHold && Date.now() >= holdEndsAt;
  const disabled = Boolean(busy) || expired;

  return createPortal(
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4"
      onClick={() => !busy && onClose?.()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="mock-pay-title"
        onClick={(e) => e.stopPropagation()}
        className="max-h-[90dvh] w-full max-w-md overflow-y-auto rounded-2xl border border-border bg-surface p-6 text-text shadow-xl"
      >
        <div className="flex items-start justify-between gap-4">
          <h2
            id="mock-pay-title"
            className="font-display text-xl font-semibold"
          >
            Test payment
          </h2>
          <button
            type="button"
            onClick={() => onClose?.()}
            disabled={Boolean(busy)}
            aria-label="Close"
            className={`-mr-2 -mt-1 flex h-8 w-8 cursor-pointer items-center justify-center rounded-full text-subtext hover:bg-bg hover:text-text disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
          >
            <FiX size={18} aria-hidden="true" />
          </button>
        </div>

        <div className="mt-4 flex items-start gap-2 rounded-xl border border-amber-500/50 bg-amber-500/10 p-3 text-sm font-semibold">
          <FiInfo className="mt-0.5 shrink-0" size={16} aria-hidden="true" />
          Test mode: no real money is charged
        </div>

        <dl className="mt-5 space-y-3">
          <div className="flex items-baseline justify-between gap-4">
            <dt className="text-sm text-subtext">Booking reference</dt>
            <dd className="font-mono text-sm font-semibold">
              {checkout.bookingReference}
            </dd>
          </div>
          <div className="flex items-baseline justify-between gap-4">
            <dt className="text-sm text-subtext">Amount</dt>
            <dd className="text-2xl font-semibold tabular-nums">
              {formatAmount(checkout.amount, checkout.currency)}
            </dd>
          </div>
        </dl>

        {hasHold && (
          <p className="mt-4 text-sm text-subtext" role="timer">
            {expired ? (
              "The hold has ended, so this payment can't be completed."
            ) : (
              <>
                Hold ends in{" "}
                <span className="font-semibold tabular-nums text-primary">
                  {mmss(secondsLeft)}
                </span>
              </>
            )}
          </p>
        )}

        {message && (
          <div
            role={message.tone === "danger" ? "alert" : "status"}
            className={`mt-4 rounded-xl border p-3 text-sm ${
              message.tone === "danger"
                ? "border-danger/40 bg-danger/10 text-text"
                : "border-border bg-muted text-text"
            }`}
          >
            {message.text}
          </div>
        )}

        <div className="mt-6 flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            autoFocus
            onClick={onPay}
            disabled={disabled}
            className={`flex-1 cursor-pointer rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
          >
            {busy === "SUCCESS" ? "Processing…" : "Pay (simulate success)"}
          </button>
          <button
            type="button"
            onClick={onFail}
            disabled={disabled}
            className={`flex-1 cursor-pointer rounded-lg border border-danger/50 bg-bg px-4 py-2.5 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
          >
            {busy === "FAILURE"
              ? "Processing…"
              : "Fail payment (simulate failure)"}
          </button>
        </div>

        <p className="mt-4 text-xs text-subtext">
          The booking is confirmed or fails exactly as with a real payment.
        </p>
      </div>
    </div>,
    document.body,
  );
};

export default MockCheckoutDialog;
