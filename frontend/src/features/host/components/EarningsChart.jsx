import React, { useEffect, useMemo, useState } from "react";
import { formatCount, formatMoney, monthLabel } from "../dashboardUtils";

const EarningsChart = ({ months, currency }) => {
  const [active, setActive] = useState(null);

  // a new set of months: clear the readout
  useEffect(() => {
    setActive(null);
  }, [months]);

  // only used to scale the bar heights
  const max = useMemo(
    () => months.reduce((m, x) => Math.max(m, Number(x.hostAmount) || 0), 0),
    [months],
  );

  const current = active != null ? months[active] : null;

  return (
    <div>
      {/* The readout: the same details a tooltip would show, without being clipped by scrolling */}
      <div
        className="min-h-14 rounded-xl bg-muted px-4 py-3 text-sm"
        aria-live="polite"
      >
        {current ? (
          <p className="flex flex-wrap items-center gap-x-4 gap-y-1">
            <span className="font-semibold text-text">
              {monthLabel(current.year, current.month)}
            </span>
            <span className="text-text">
              You earn{" "}
              <strong>{formatMoney(current.hostAmount, currency)}</strong>
            </span>
            <span className="text-subtext">
              {formatCount(current.bookings)}{" "}
              {Number(current.bookings) === 1 ? "booking" : "bookings"}
            </span>
            <span className="text-subtext">
              Gross {formatMoney(current.gross, currency)}
            </span>
          </p>
        ) : (
          <p className="text-subtext">
            Hover, tap or focus a bar to see its details
          </p>
        )}
      </div>

      <div className="mt-4 overflow-x-auto pb-2">
        <ul className="flex w-max min-w-full gap-2">
          {months.map((m, i) => {
            const value = Number(m.hostAmount) || 0;
            const label = monthLabel(m.year, m.month);
            const summary = `${label}: you earn ${formatMoney(m.hostAmount, currency)}, ${formatCount(m.bookings)} ${Number(m.bookings === 1 ? "booking" : "bookings")},
                gross ${formatMoney(m.gross, currency)}`;

            return (
              <li
                key={`${m.year}-${m.month}`}
                className="flex min-w-14 flex-1 flex-col items-center"
              >
                <button
                  type="button"
                  title={summary}
                  aria-label={summary}
                  aria-pressed={active === i}
                  onMouseEnter={() => setActive(i)}
                  onFocus={() => setActive(i)}
                  onClick={() => setActive(i)}
                  className="group flex h-48 w-full cursor-pointer items-end justify-center rounded-md px-1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <span
                    className={`block w-full max-w-10 rounded-t-md transition-colors motion-reduce:transition-none ${
                      value > 0
                        ? active === i
                          ? "bg-primary-hover"
                          : "bg-primary group-hover:bg-primary-hover"
                        : "bg-border"
                    }`}
                    style={{
                      height:
                        value > 0 && max > 0
                          ? `${Math.max(3, (value / max) * 100)}%`
                          : "2px",
                    }}
                  />
                </button>
                <span className="mt-2 whitespace-nowrap text-[11px] text-subtext">
                  {label}
                </span>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
};

export default EarningsChart;
