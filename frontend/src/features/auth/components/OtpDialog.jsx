import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { FiMail } from "react-icons/fi";
import { handleApiError } from "../../../api/errors/handleApiError";

const formatTime = (ms) => {
  const s = Math.max(0, Math.ceil(ms / 1000));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
};

const primaryBtn =
  "w-full cursor-pointer rounded-full bg-primary py-3 text-sm font-semibold text-white transition-colors duration-200 hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50";

// Deliberately has no backdrop-click or Escape handling: it can only be left on purpose.
const OtpDialog = ({ email, expiresAt, onVerify, onStartOver }) => {
  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [confirmLeave, setConfirmLeave] = useState(false);
  const [now, setNow] = useState(() => Date.now());

  const dialogRef = useRef(null);
  const inputRef = useRef(null);

  const remaining = expiresAt - now;
  const expired = remaining <= 0;

  // countdown
  useEffect(() => {
    if (expired) return;
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, [expired]);

  // lock page scroll, warn before a refresh or tab close, keep focus inside
  useEffect(() => {
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    inputRef.current?.focus();

    const warn = (e) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);

    const onKey = (e) => {
      if (e.key !== "Tab") return;
      const items = [
        ...(dialogRef.current?.querySelectorAll("button, input") ?? []),
      ].filter((el) => !el.disabled);
      if (items.length === 0) return;

      const first = items[0];
      const last = items[items.length - 1];
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
      document.body.style.overflow = prevOverflow;
      window.removeEventListener("beforeunload", warn);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (verifying || expired) return;

    const code = otp.trim();
    if (!code) {
      setError("Please enter the OTP");
      return;
    }

    setVerifying(true);
    setError("");
    try {
      await onVerify(code); // on success the parent navigates away
    } catch (err) {
      setError(handleApiError(err));
      setVerifying(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 p-4">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="otp-title"
        className="w-full max-w-sm rounded-3xl border border-border bg-surface p-8 font-body shadow-2xl"
      >
        <div className="mb-6 text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
            <FiMail className="h-8 w-8 text-primary" aria-hidden="true" />
          </div>
          <h3 id="otp-title" className="mb-1 font-display text-2xl text-text">
            Verify your email
          </h3>
          <p className="text-sm text-subtext">
            We sent a code to{" "}
            <span className="break-all font-semibold text-text">{email}</span>
          </p>
        </div>

        {expired ? (
          <div className="space-y-4 text-center">
            <p role="alert" className="text-sm text-danger">
              This code has expired.
            </p>
            <button type="button" onClick={onStartOver} className={primaryBtn}>
              Start over
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div>
              <label
                htmlFor="otp-input"
                className="mb-1.5 block text-sm font-medium text-text"
              >
                Enter OTP
              </label>
              <input
                id="otp-input"
                ref={inputRef}
                type="text"
                value={otp}
                onChange={(e) => {
                  setOtp(e.target.value);
                  if (error) setError("");
                }}
                maxLength={6}
                autoComplete="one-time-code"
                placeholder="······"
                aria-invalid={Boolean(error)}
                className={`w-full rounded-lg border bg-input px-4 py-3 text-center font-mono text-xl tracking-[0.5em] text-text outline-none transition-all duration-200 focus:ring-2 focus:ring-primary/30 ${
                  error ? "border-danger" : "border-border"
                }`}
              />
              {error && (
                <p
                  role="alert"
                  className="mt-1.5 text-center text-xs text-danger"
                >
                  {error}
                </p>
              )}
              <p
                className="mt-2 text-center text-xs text-subtext"
                aria-live="off"
              >
                Code expires in{" "}
                <span className="font-semibold text-text">
                  {formatTime(remaining)}
                </span>
              </p>
            </div>

            <button type="submit" disabled={verifying} className={primaryBtn}>
              {verifying ? "Verifying…" : "Verify & create account"}
            </button>

            {confirmLeave ? (
              <div className="rounded-xl border border-border bg-bg p-3 text-center text-sm">
                <p className="text-text">Start over?</p>
                <p className="mt-1 text-xs text-subtext">
                  This code will be discarded, and you'll need to sign up again
                  to get a new one.
                </p>
                <div className="mt-3 flex gap-2">
                  <button
                    type="button"
                    onClick={() => setConfirmLeave(false)}
                    disabled={verifying}
                    className="flex-1 cursor-pointer rounded-full border border-border px-3 py-2 text-xs font-semibold text-text transition-colors hover:bg-surface"
                  >
                    Keep this code
                  </button>
                  <button
                    type="button"
                    onClick={onStartOver}
                    disabled={verifying}
                    className="flex-1 cursor-pointer rounded-full border border-danger/50 px-3 py-2 text-xs font-semibold text-danger transition-colors hover:bg-danger/10"
                  >
                    Start over
                  </button>
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setConfirmLeave(true)}
                disabled={verifying}
                className="w-full cursor-pointer py-1 text-sm text-subtext transition-colors hover:underline disabled:opacity-50"
              >
                Wrong email? Start over
              </button>
            )}
          </form>
        )}
      </div>
    </div>,
    document.body,
  );
};

export default OtpDialog;
