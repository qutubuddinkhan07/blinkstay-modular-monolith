import React, { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { AnimatePresence, motion } from "motion/react";
import { FiX } from "react-icons/fi";
import { useCooldown } from "../../../utils/useCooldown";
import { formatINR } from "../../../utils/format";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const mmss = (s) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

/**
 * checkout: the /pay response (null = closed)
 * busy:     "SUCCESS" | "FAILURE" | null (which button is running)
 * message:  { tone: "danger" | "info", text } | null
 */
const MockCheckoutDialog = ({ checkout, busy, message, onPay, onFail, onClose }) => {
  const open = Boolean(checkout);
  const dialogRef = useRef(null);
  const payRef = useRef(null);
  const { secondsLeft, startUntil } = useCooldown();

  // Latest values for the keyboard handler, without re-running the setup effect
  const busyRef = useRef(busy);
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    busyRef.current = busy;
    onCloseRef.current = onClose;
  });

  const holdEndsAt = checkout?.holdExpiresAt
    ? new Date(checkout.holdExpiresAt).getTime()
    : null;

  useEffect(() => {
    if (holdEndsAt) startUntil(holdEndsAt);
  }, [holdEndsAt, startUntil]);

  // Scroll lock, first focus, Escape, and a simple focus trap
  useEffect(() => {
    if (!open) return;

    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    payRef.current?.focus();

    const onKey = (e) => {
      if (e.key === "Escape" && !busyRef.current) {
        e.preventDefault();
        onCloseRef.current?.();
        return;
      }
      if (e.key !== "Tab" || !dialogRef.current) return;

      const nodes = dialogRef.current.querySelectorAll(
        'button:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
      );
      if (!nodes.length) return;
      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus?.();
    };
  }, [open]);

  const amount = checkout ? formatINR(checkout.amount, checkout.currency) : "";
  const disabled = Boolean(busy);

  return createPortal(
    <AnimatePresence>
      {open && (
        <motion.div
          key="mock-checkout"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.15 }}
          className="fixed inset-0 z-[60] flex items-end justify-center bg-black/50 p-4 sm:items-center"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget && !busy) onClose();
          }}
        >
          <motion.div
            ref={dialogRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby="checkout-title"
            aria-describedby="checkout-test-label"
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 16, scale: 0.98 }}
            transition={{ duration: 0.18 }}
            className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 text-text shadow-2xl"
          >
            <div className="flex items-start justify-between gap-4">
              <h2 id="checkout-title" className="font-display text-xl font-semibold">
                Checkout
              </h2>
              <button
                type="button"
                onClick={onClose}
                disabled={disabled}
                aria-label="Close checkout"
                className={`-m-1 cursor-pointer rounded-full p-1 text-subtext transition-colors hover:text-text disabled:cursor-not-allowed disabled:opacity-40 ${focusRing}`}
              >
                <FiX size={20} aria-hidden="true" />
              </button>
            </div>

            <p
              id="checkout-test-label"
              className="mt-4 rounded-lg border border-primary/40 bg-primary/10 px-3 py-2 text-sm font-semibold text-text"
            >
              Test payment: no real money
            </p>

            <dl className="mt-5 space-y-3 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-subtext">Booking</dt>
                <dd className="font-semibold tracking-wide">{checkout.bookingReference}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-subtext">Time left</dt>
                <dd className="font-semibold tabular-nums">
                  {secondsLeft > 0 ? mmss(secondsLeft) : "Hold ended"}
                </dd>
              </div>
              <div className="flex items-baseline justify-between gap-4 border-t border-border pt-3">
                <dt className="text-subtext">Amount</dt>
                <dd className="font-display text-2xl font-semibold">{amount}</dd>
              </div>
            </dl>

            {message && (
              <p
                role={message.tone === "danger" ? "alert" : "status"}
                className={`mt-4 rounded-lg border px-3 py-2 text-sm ${
                  message.tone === "danger"
                    ? "border-danger/40 text-danger"
                    : "border-border text-subtext"
                }`}
              >
                {message.text}
              </p>
            )}

            <div className="mt-6 flex flex-col gap-3 sm:flex-row-reverse">
              <button
                ref={payRef}
                type="button"
                onClick={onPay}
                disabled={disabled}
                className={`flex-1 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
              >
                {busy === "SUCCESS" ? "Processing…" : `Pay ${amount}`}
              </button>
              <button
                type="button"
                onClick={onFail}
                disabled={disabled}
                className={`flex-1 cursor-pointer rounded-lg border border-danger/50 px-5 py-2.5 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
              >
                {busy === "FAILURE" ? "Processing…" : "Simulate failure"}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>,
    document.body,
  );
};

export default MockCheckoutDialog;