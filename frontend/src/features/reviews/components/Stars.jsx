import { useRef, useState } from "react";
import { FaStar } from "react-icons/fa";
import { plural } from "../../../utils/format";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const SIZES = { sm: 14, md: 18, lg: 24 };

const spoken = (n) => (Number.isInteger(n) ? String(n) : n.toFixed(1));

// Five read-only stars. value may be fractional (4.6): the stars show it rounded.
export const Stars = ({ value, size = "md", className = "" }) => {
  const v = Number(value) || 0;
  const rounded = Math.round(v);

  return (
    <span
      role="img"
      aria-label={`${spoken(v)} out of 5 stars`}
      className={`inline-flex items-center gap-0.5 ${className}`}
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <FaStar
          key={n}
          size={SIZES[size] ?? SIZES.md}
          aria-hidden="true"
          className={n <= rounded ? "text-amber-500" : "text-border"}
        />
      ))}
    </span>
  );
};

// ★ 4.5 (12 reviews). With no reviews it shows emptyLabel ("New" on cards, "No reviews yet" on details).
export const RatingSummary = ({
  average,
  count,
  emptyLabel = "New",
  size = "sm",
  className = "",
}) => {
  const n = Number(count) || 0;

  if (average == null || n === 0) {
    return (
      <span className={`text-sm text-subtext ${className}`}>{emptyLabel}</span>
    );
  }

  const avg = Number(average).toFixed(1);

  return (
    <span className={`inline-flex items-center gap-1.5 text-sm ${className}`}>
      <span className="sr-only">
        Rated {avg} out of 5 from {plural(n, "review")}
      </span>
      <FaStar
        size={SIZES[size] ?? SIZES.sm}
        className="text-amber-500"
        aria-hidden="true"
      />
      <span aria-hidden="true" className="font-semibold text-text">
        {avg}
      </span>
      <span aria-hidden="true" className="text-subtext">
        ({plural(n, "review")})
      </span>
    </span>
  );
};

// A radio group of 1 to 5 stars. Keyboard: arrow keys change the rating, Home = 1, End = 5.
export const StarPicker = ({
  value,
  onChange,
  disabled = false,
  autoFocus = false,
  labelledBy,
}) => {
  const [hover, setHover] = useState(0);
  const refs = useRef([]);
  const shown = hover || value;
  const tabbable = value >= 1 ? value : 1; // one star is in the tab order at a time

  const choose = (n, { focus = false } = {}) => {
    const next = Math.min(5, Math.max(1, n));
    onChange(next);
    if (focus) refs.current[next - 1]?.focus();
  };

  const onKeyDown = (e) => {
    if (disabled) return;

    const step = { ArrowRight: 1, ArrowUp: 1, ArrowLeft: -1, ArrowDown: -1 }[
      e.key
    ];

    if (step) {
      e.preventDefault();
      choose((value || 0) + step, { focus: true });
    } else if (e.key === "Home") {
      e.preventDefault();
      choose(1, { focus: true });
    } else if (e.key === "End") {
      e.preventDefault();
      choose(5, { focus: true });
    }
  };

  return (
    <div
      role="radiogroup"
      aria-labelledby={labelledBy}
      onKeyDown={onKeyDown}
      onMouseLeave={() => setHover(0)}
      className="inline-flex items-center gap-1"
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          ref={(el) => {
            refs.current[n - 1] = el;
          }}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} ${n === 1 ? "star" : "stars"}`}
          tabIndex={n === tabbable ? 0 : -1}
          autoFocus={autoFocus && n === tabbable}
          disabled={disabled}
          onClick={() => choose(n)}
          onMouseEnter={() => !disabled && setHover(n)}
          className={`cursor-pointer rounded p-0.5 disabled:cursor-not-allowed disabled:opacity-60 ${focusRing}`}
        >
          <FaStar
            size={30}
            aria-hidden="true"
            className={n <= shown ? "text-amber-500" : "text-border"}
          />
        </button>
      ))}
    </div>
  );
};
