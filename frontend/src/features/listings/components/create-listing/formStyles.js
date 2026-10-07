// Shared class strings so every card, input and button uses the same theme tokens.
// 60% bg/surface · 30% text/subtext/border · 10% primary (submit, focus, highlights)

export const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

export const cardClass =
  "rounded-2xl border border-border bg-surface p-6 transition-colors duration-300";

export const headingClass = "mb-5 font-display text-xl font-medium text-text";

export const labelClass = "mb-2 block text-sm font-medium text-text";

export const inputClass =
  "w-full rounded-lg border border-border bg-input px-4 py-3 text-text placeholder:text-subtext transition-colors focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/25";

// Neutral secondary button (Add, choose files...) so the accent stays at ~10%
export const secondaryButtonClass = `cursor-pointer rounded-lg border border-border bg-bg px-5 py-3 text-sm font-semibold text-text transition-colors hover:bg-border/60 ${focusRing}`;

export const primaryButtonClass = `cursor-pointer rounded-lg bg-primary px-6 py-3 font-semibold text-white transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60 ${focusRing}`;
