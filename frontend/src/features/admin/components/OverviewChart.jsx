import { useEffect, useMemo, useState } from "react";
import {
  formatCount,
  formatMoney,
  monthLabel,
} from "../../host/dashboardUtils";

// only used to size the bars
const barHeight = (value, max) =>
  value > 0 && max > 0 ? `${Math.max(3, (value / max) * 100)}%` : "2px";

const OverviewChart = ({ months, currency }) => {
  const [active, setActive] = useState(null);

  useEffect(() => {
    setActive(null);
  }, [months]);

  const max = useMemo(
    () =>
      months.reduce(
        (m, x) => Math.max(m, Number(x.gross) || 0, Number(x.commission) || 0),
        0,
      ),
    [months],
  );

  const current = active != null ? months[active] : null;

  return (
    <div>
      <ul
        className="mb-3 flex flex-wrap gap-4 text-xs text-subtext"
        aria-label="Legend"
      >
        <li className="inline-flex items-center gap-1.5">
          <span
            className="h-2.5 w-2.5 rounded-sm bg-primary"
            aria-hidden="true"
          />
          Gross sales
        </li>
        <li className="inline-flex items-center gap-1.5">
          <span
            className="h-2.5 w-2.5 rounded-sm bg-subtext"
            aria-hidden="true"
          />
          Platform commission
        </li>
      </ul>

      {/* The details a tooltip would show, in a strip that scrolling can't clip */}
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
              Gross <strong>{formatMoney(current.gross, currency)}</strong>
            </span>
            <span className="text-text">
              Commission{" "}
              <strong>{formatMoney(current.commission, currency)}</strong>
            </span>
            <span className="text-subtext">
              {formatCount(current.bookings)}{" "}
              {Number(current.bookings) === 1 ? "booking" : "bookings"}
            </span>
          </p>
        ) : (
          <p className="text-subtext">
            Hover, tap or focus a month to see its details.
          </p>
        )}
      </div>

      <div className="mt-4 overflow-x-auto pb-2">
        <ul className="flex w-max min-w-full gap-2">
          {months.map((m, i) => {
            const gross = Number(m.gross) || 0;
            const commission = Number(m.commission) || 0;
            const label = monthLabel(m.year, m.month);
            const summary = `${label}: gross ${formatMoney(m.gross, currency)}, commission ${formatMoney(m.commission, currency)}, ${formatCount(m.bookings)} ${
              Number(m.bookings) === 1 ? "booking" : "bookings"
            }`;

            return (
              <li
                key={`${m.year}-${m.month}`}
                className="flex min-w-16 flex-1 flex-col items-center"
              >
                <button
                  type="button"
                  title={summary}
                  aria-label={summary}
                  aria-pressed={active === i}
                  onMouseEnter={() => setActive(i)}
                  onFocus={() => setActive(i)}
                  onClick={() => setActive(i)}
                  className="flex h-48 w-full cursor-pointer items-end justify-center gap-1 rounded-md px-1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <span
                    className={`block w-3 rounded-t-sm transition-colors motion-reduce:transition-none sm:w-4 ${
                      gross > 0
                        ? active === i
                          ? "bg-primary-hover"
                          : "bg-primary"
                        : "bg-border"
                    }`}
                    style={{ height: barHeight(gross, max) }}
                  />
                  <span
                    className={`block w-3 rounded-t-sm transition-colors motion-reduce:transition-none sm:w-4 ${
                      commission > 0
                        ? active === i
                          ? "bg-text"
                          : "bg-subtext"
                        : "bg-border"
                    }`}
                    style={{ height: barHeight(commission, max) }}
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

export default OverviewChart;
