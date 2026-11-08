import { useRef, useState } from "react";
import { inputCls, labelCls, primaryBtn } from "../../features/user/formStyles";
import { PRESETS, rangeForPreset } from "../../features/host/dashboardUtils";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

export const chip = (active) =>
  `cursor-pointer rounded-md px-4 py-1.5 text-sm font-semibold transition-colors ${focusRing} ${
    active ? "bg-primary text-on-primary" : "text-subtext hover:text-text"
  }`;

export const Skeleton = ({ className = "" }) => (
  <div
    className={`animate-pulse rounded bg-input motion-reduce:animate-none ${className}`}
    aria-hidden="true"
  />
);

export const Kpi = ({ label, value, hint, big = false, loading = false }) => (
  <div
    className={`rounded-2xl border bg-surface ${
      big ? "border-primary/40 p-6" : "border-border p-4"
    }`}
  >
    <p className="text-sm font-medium text-subtext">{label}</p>
    {loading ? (
      <Skeleton className={`mt-2 ${big ? "h-9 w-40" : "h-7 w-28"}`} />
    ) : (
      <p
        className={`mt-1 font-display font-semibold tabular-nums ${
          big ? "text-3xl text-primary" : "text-xl text-text"
        }`}
      >
        {value}
      </p>
    )}
    {!loading && hint && <p className="mt-1 text-xs text-subtext">{hint}</p>}
  </div>
);

export const SectionError = ({ onRetry }) => (
  <div
    role="alert"
    className="rounded-2xl border border-border bg-surface p-6 text-center"
  >
    <p className="text-sm text-danger">We couldn't load this section.</p>
    <button
      type="button"
      onClick={onRetry}
      className={`mt-3 cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text hover:bg-bg ${focusRing}`}
    >
      Try again
    </button>
  </div>
);

export const EmptyState = ({ children = "No bookings in this period" }) => (
  <div className="rounded-2xl border border-border bg-surface p-8 text-center text-subtext">
    {children}
  </div>
);

// Presets plus a custom range. Calls onChange({ from, to }); empty strings = the server's default.
export const DateRangeBar = ({
  onChange,
  fromLabel = "From",
  toLabel = "Until",
}) => {
  const [preset, setPreset] = useState("last12");
  const [draft, setDraft] = useState({ from: "", to: "" });
  const [error, setError] = useState("");
  const applied = useRef({ from: "", to: "" });

  const apply = (range) => {
    applied.current = range;
    onChange(range);
  };

  const choose = (key) => {
    setPreset(key);
    setError("");
    if (key === "custom") {
      setDraft(applied.current); // start from what is showing now
      return;
    }
    apply(rangeForPreset(key));
  };

  const submitCustom = (e) => {
    e.preventDefault();
    if (draft.from && draft.to && draft.from > draft.to) {
      setError("The start date must be before the end date.");
      return;
    }
    setError("");
    apply({ from: draft.from, to: draft.to });
  };

  return (
    <div>
      <div
        className="inline-flex flex-wrap rounded-lg border border-border bg-surface p-1"
        role="group"
        aria-label="Date range"
      >
        {PRESETS.map((p) => (
          <button
            key={p.key}
            type="button"
            aria-pressed={preset === p.key}
            onClick={() => choose(p.key)}
            className={chip(preset === p.key)}
          >
            {p.label}
          </button>
        ))}
      </div>

      {preset === "custom" && (
        <form
          onSubmit={submitCustom}
          className="mt-3 flex flex-wrap items-end gap-3"
          noValidate
        >
          <div className="w-40">
            <label
              htmlFor="range-from"
              className={`${labelCls} !mb-1 text-xs text-subtext`}
            >
              {fromLabel}
            </label>
            <input
              id="range-from"
              type="date"
              value={draft.from}
              max={draft.to || undefined}
              onChange={(e) =>
                setDraft((d) => ({ ...d, from: e.target.value }))
              }
              className={inputCls}
            />
          </div>
          <div className="w-40">
            <label
              htmlFor="range-to"
              className={`${labelCls} !mb-1 text-xs text-subtext`}
            >
              {toLabel}
            </label>
            <input
              id="range-to"
              type="date"
              value={draft.to}
              min={draft.from || undefined}
              onChange={(e) => setDraft((d) => ({ ...d, to: e.target.value }))}
              className={inputCls}
            />
          </div>
          <button type="submit" className={`cursor-pointer ${primaryBtn}`}>
            Apply
          </button>
          {error && (
            <p role="alert" className="w-full text-sm text-danger">
              {error}
            </p>
          )}
        </form>
      )}
    </div>
  );
};
