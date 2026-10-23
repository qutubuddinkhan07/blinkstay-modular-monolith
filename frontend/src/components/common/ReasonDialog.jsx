import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

const MAX = 500;

const ReasonDialog = ({
  open,
  title,
  description,
  notice, // anything shown above the textarea (a refund preview, a warning)
  label = "Reason",
  placeholder,
  required = true,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  busyLabel = "Working…",
  busy = false,
  confirmDisabled = false,
  tone = "danger", // "danger" | "primary"
  onConfirm, // receives the trimmed reason (can be "" when not required)
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
  const error = required && touched && !trimmed ? "A reason is required" : "";

  const submit = (e) => {
    e.preventDefault();
    setTouched(true);
    if ((required && !trimmed) || busy || confirmDisabled) return;
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
        className="max-h-[90dvh] w-full max-w-md overflow-y-auto rounded-2xl border border-border bg-surface p-6 text-text shadow-xl"
      >
        <h2 id="reason-title" className="font-display text-xl font-semibold">
          {title}
        </h2>
        {description && (
          <p className="mt-2 text-sm text-subtext">{description}</p>
        )}

        {notice && <div className="mt-4">{notice}</div>}

        <label
          htmlFor="reason-input"
          className="mt-4 block text-sm font-medium"
        >
          {label}
          {!required && (
            <span className="font-normal text-subtext"> (optional)</span>
          )}
        </label>
        <textarea
          id="reason-input"
          rows={4}
          maxLength={MAX}
          autoFocus
          value={reason}
          placeholder={placeholder}
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
            {cancelLabel}
          </button>
          <button
            type="submit"
            disabled={busy || confirmDisabled}
            className={`cursor-pointer rounded-lg px-4 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-60 ${
              tone === "primary"
                ? "bg-primary text-on-primary hover:bg-primary-hover"
                : "bg-danger text-white hover:opacity-90"
            }`}
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
