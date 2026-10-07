import React, { useState } from "react";

const WEEKDAYS = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];

export const startOfDay = (d) =>
  new Date(d.getFullYear(), d.getMonth(), d.getDate());
const sameDay = (a, b) => Boolean(a && b && a.getTime() === b.getTime());
const addMonths = (d, n) => new Date(d.getFullYear(), d.getMonth() + n, 1);

const Month = ({ month, checkIn, checkOut, hover, today, onPick, onHover }) => {
  const y = month.getFullYear();
  const m = month.getMonth();
  const offset = new Date(y, m, 1).getDay();
  const daysInMonth = new Date(y, m + 1, 0).getDate();

  const cells = [
    ...Array(offset).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => new Date(y, m, i + 1)),
  ];

  // While choosing check-out, preview the range up to the hovered day
  const rangeEnd =
    checkOut || (checkIn && hover && hover > checkIn ? hover : null);

  return (
    <div className="w-full">
      <h3 className="mb-4 text-center text-sm font-semibold">
        {month.toLocaleDateString("en-US", { month: "long", year: "numeric" })}
      </h3>

      <div className="grid grid-cols-7 text-center text-xs text-neutral-500">
        {WEEKDAYS.map((d) => (
          <div key={d} className="pb-2">
            {d}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7">
        {cells.map((date, i) => {
          if (!date) return <div key={`blank-${i}`} />;

          const disabled = date < today;
          const isStart = sameDay(date, checkIn);
          const isEnd = sameDay(date, rangeEnd);
          const inRange =
            checkIn && rangeEnd && date > checkIn && date < rangeEnd;

          // Continuous band behind the day buttons
          const band = [
            inRange ? "bg-neutral-100" : "",
            isStart && rangeEnd ? "bg-neutral-100 rounded-l-full" : "",
            isEnd && checkIn ? "bg-neutral-100 rounded-r-full" : "",
          ].join(" ");

          return (
            <div
              key={date.toISOString()}
              className={`flex justify-center ${band}`}
            >
              <button
                type="button"
                disabled={disabled}
                onClick={() => onPick(date)}
                onMouseEnter={() => onHover(date)}
                className={`h-10 w-10 rounded-full text-sm transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-900 ${
                  isStart || isEnd
                    ? "bg-neutral-900 font-semibold text-white"
                    : disabled
                      ? "cursor-not-allowed text-neutral-300 line-through"
                      : "hover:ring-1 hover:ring-neutral-900"
                }`}
              >
                {date.getDate()}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const DateRangePicker = ({ checkIn, checkOut, onChange, onClose }) => {
  const today = startOfDay(new Date());
  const [view, setView] = useState(
    new Date(
      (checkIn || today).getFullYear(),
      (checkIn || today).getMonth(),
      1,
    ),
  );
  const [hover, setHover] = useState(null);

  const pick = (date) => {
    // Start a new range
    if (!checkIn || checkOut || date <= checkIn) {
      onChange({ checkIn: date, checkOut: null });
      return;
    }
    // Complete the range
    onChange({ checkIn, checkOut: date });
    onClose?.();
  };

  const canGoBack = view > new Date(today.getFullYear(), today.getMonth(), 1);

  return (
    <div onMouseLeave={() => setHover(null)}>
      <div className="relative">
        <button
          type="button"
          onClick={() => setView(addMonths(view, -1))}
          disabled={!canGoBack}
          aria-label="Previous month"
          className="absolute left-0 top-[-4px] z-10 flex h-8 w-8 items-center justify-center rounded-full hover:bg-neutral-100 disabled:opacity-30"
        >
          ‹
        </button>
        <button
          type="button"
          onClick={() => setView(addMonths(view, 1))}
          aria-label="Next month"
          className="absolute right-0 top-[-4px] z-10 flex h-8 w-8 items-center justify-center rounded-full hover:bg-neutral-100"
        >
          ›
        </button>

        {/* Two months side by side (stacked on small screens) */}
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">
          <Month
            month={view}
            {...{ checkIn, checkOut, hover, today }}
            onPick={pick}
            onHover={setHover}
          />
          <Month
            month={addMonths(view, 1)}
            {...{ checkIn, checkOut, hover, today }}
            onPick={pick}
            onHover={setHover}
          />
        </div>
      </div>

      <div className="mt-4 flex justify-end gap-4 text-sm">
        <button
          type="button"
          onClick={() => onChange({ checkIn: null, checkOut: null })}
          className="font-medium underline"
        >
          Clear dates
        </button>
        <button
          type="button"
          onClick={onClose}
          className="rounded-lg bg-neutral-900 px-4 py-2 font-medium text-white"
        >
          Close
        </button>
      </div>
    </div>
  );
};

export default DateRangePicker;
