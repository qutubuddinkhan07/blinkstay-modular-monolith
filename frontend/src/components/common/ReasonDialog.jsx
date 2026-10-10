import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

const MAX = 500;

const ReasonDialog = ({
  open,
  title,
  description,
  confirmLabel = "Confirm",
  busyLabel = "Working…",
  busy = false,
  onConfirm, // receives the trimmed reason
  onCancel,
}) => {
  const [reason, setReason] = useState("");
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    if (!open) {
      setReason("");
      setTouched(false);
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e) => e.key === "Escape" && !busy && onCancel();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, busy, onCancel]);

  if (!open) return null;

  const trimmed = reason.trim();
  const error = touched && !trimmed ? "A reason is required" : "";

  const submit = (e) => {
    e.preventDefault();
    setTouched(true);
    if (!trimmed || busy) return;
    onConfirm(trimmed);
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4"
      onClick={() => !busy && onCancel()}
    >
      <form
        role="dialog"
        aria-modal="true"
        aria-labelledby="reason-title"
        onSubmit={submit}
        noValidate
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 text-text shadow-xl"
      >
        <h2 id="reason-title" className="font-display text-xl font-semibold">
          {title}
        </h2>
        {description && (
          <p className="mt-2 text-sm text-subtext">{description}</p>
        )}

        <label
          htmlFor="reason-input"
          className="mt-4 block text-sm font-medium"
        >
          Reason
        </label>
        <textarea
          id="reason-input"
          rows={4}
          maxLength={MAX}
          autoFocus
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          onBlur={() => setTouched(true)}
          aria-invalid={Boolean(error)}
          className="mt-1.5 w-full rounded-lg border border-border bg-input px-3 py-2.5 text-sm text-text focus-visible:border-primary focus-visible:outline-2 focus-visible:outline-primary"
        />
        <div className="mt-1.5 flex justify-between gap-4">
          {error ? <p className="text-xs text-danger">{error}</p> : <span />}
          <span className="shrink-0 text-xs text-subtext">
            {reason.length}/{MAX}
          </span>
        </div>

        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            className="cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text hover:bg-bg disabled:opacity-60"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={busy}
            className="cursor-pointer rounded-lg bg-danger px-4 py-2 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
          >
            {busy ? busyLabel : confirmLabel}
          </button>
        </div>
      </form>
    </div>,
    document.body,
  );
};

export default ReasonDialog;
