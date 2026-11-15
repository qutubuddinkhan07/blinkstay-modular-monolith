import { useId, useRef, useState } from "react";
import { handleApiError } from "../../../api/errors/handleApiError";
import { REVIEW_LIMITS } from "../reviewService";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

// onSubmit(text) must return a promise. It throws on failure: the message shows here.
const ReplyForm = ({
  initial = "",
  submitLabel = "Post reply",
  onSubmit,
  onCancel,
}) => {
  const id = useId();
  const [text, setText] = useState(initial);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const busyRef = useRef(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (busyRef.current) return; // no double submit

    const trimmed = text.trim();
    if (!trimmed) {
      setError("Write a reply first.");
      return;
    }

    busyRef.current = true;
    setBusy(true);
    setError("");

    try {
      await onSubmit(trimmed);
    } catch (err) {
      setError(handleApiError(err, "reply"));
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="ml-0 sm:ml-8">
      <label htmlFor={`${id}-reply`} className="sr-only">
        Your reply
      </label>
      <textarea
        id={`${id}-reply`}
        value={text}
        onChange={(e) => setText(e.target.value)}
        maxLength={REVIEW_LIMITS.reply}
        rows={4}
        autoFocus
        disabled={busy}
        placeholder="Write a public reply to this guest…"
        aria-describedby={`${id}-count`}
        className="min-h-24 w-full resize-y rounded-lg border border-border bg-input px-3 py-2.5 text-sm text-text placeholder:text-subtext focus-visible:outline-2 focus-visible:outline-primary"
      />
      <p
        id={`${id}-count`}
        className={`mt-1 text-right text-xs tabular-nums ${
          text.length >= REVIEW_LIMITS.reply ? "text-danger" : "text-subtext"
        }`}
      >
        {text.length}/{REVIEW_LIMITS.reply}
      </p>

      {error && (
        <p role="alert" className="mt-2 text-sm text-danger">
          {error}
        </p>
      )}

      <div className="mt-3 flex gap-2">
        <button
          type="submit"
          disabled={busy || !text.trim()}
          className={`cursor-pointer rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
        >
          {busy ? "Saving…" : submitLabel}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={busy}
          className={`cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text transition-colors hover:bg-bg disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
        >
          Cancel
        </button>
      </div>
    </form>
  );
};

export default ReplyForm;
