import React, { useState } from "react";
import { FiChevronLeft, FiChevronRight } from "react-icons/fi";
import { addDays } from "../../../../utils/format";

const WEEKDAYS = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];

export const startOfDay = (d) =>
  new Date(d.getFullYear(), d.getMonth(), d.getDate());
const sameDay = (a, b) => Boolean(a && b && a.getTime() === b.getTime());
const addMonths = (d, n) => new Date(d.getFullYear(), d.getMonth() + n, 1);

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const longDate = (d) =>
  d.toLocaleDateString("en-US", {
    weekday: "long",
    month: "long",
    day: "numeric",
    year: "numeric",
  });

const Month = ({
  month,
  checkIn,
  checkOut,
  hover,
  today,
  lastDay,
  onPick,
  onHover,
}) => {
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
    checkOut ||
    (checkIn && hover && hover > checkIn && hover <= lastDay ? hover : null);

  return (
    <div className="w-full">
      <h3 className="mb-4 text-center text-sm font-semibold text-text">
        {month.toLocaleDateString("en-US", { month: "long", year: "numeric" })}
      </h3>

      <div className="grid grid-cols-7 text-center text-xs font-medium text-subtext">
        {WEEKDAYS.map((d) => (
          <div key={d} className="pb-2">
            {d}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7">
        {cells.map((date, i) => {
          if (!date) return <div key={`blank-${i}`} />;

          const disabled = date < today || date > lastDay;
          const isStart = sameDay(date, checkIn);
          const isEnd = sameDay(date, rangeEnd);
          const selected = isStart || isEnd;
          const isToday = sameDay(date, today);
          const inRange =
            checkIn && rangeEnd && date > checkIn && date < rangeEnd;

          // Continuous band behind the day buttons (a tint of the theme's accent)
          const band = [
            inRange ? "bg-primary/15" : "",
            isStart && rangeEnd ? "bg-primary/15 rounded-l-full" : "",
            isEnd && checkIn ? "bg-primary/15 rounded-r-full" : "",
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
                aria-label={longDate(date)}
                aria-pressed={selected}
                className={`h-9 w-9 rounded-full text-sm transition-colors sm:h-10 sm:w-10 ${focusRing} ${
                  selected
                    ? "bg-primary font-semibold text-on-primary"
                    : disabled
                      ? "cursor-not-allowed text-subtext/40 line-through"
                      : `hover:ring-1 hover:ring-text ${
                          isToday ? "font-bold text-primary" : "text-text"
                        }`
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

const navBtn = `absolute top-[-4px] z-10 flex h-8 w-8 cursor-pointer items-center justify-center rounded-full text-text transition-colors hover:bg-bg disabled:cursor-not-allowed disabled:opacity-30 ${focusRing}`;

const DateRangePicker = ({
  checkIn,
  checkOut,
  onChange,
  onClose,
  maxDate, // last allowed check-in day
  maxNights = 30,
}) => {
  const today = startOfDay(new Date());
  const [view, setView] = useState(
    new Date(
      (checkIn || today).getFullYear(),
      (checkIn || today).getMonth(),
      1,
    ),
  );
  const [hover, setHover] = useState(null);

  // Picking check-out: at most maxNights after check-in. Picking check-in: up to maxDate.
  const choosingCheckOut = Boolean(checkIn && !checkOut);
  const lastDay = choosingCheckOut
    ? addDays(checkIn, maxNights)
    : (maxDate ?? addDays(today, 3650));

  const pick = (date) => {
    if (!checkIn || checkOut || date <= checkIn) {
      onChange({ checkIn: date, checkOut: null });
      return;
    }
    onChange({ checkIn, checkOut: date });
    onClose?.();
  };

  const canGoBack = view > new Date(today.getFullYear(), today.getMonth(), 1);
  const canGoForward =
    addMonths(view, 1) <=
    new Date(lastDay.getFullYear(), lastDay.getMonth(), 1);

  return (
    <div onMouseLeave={() => setHover(null)} className="text-text">
      <div className="relative">
        <button
          type="button"
          onClick={() => setView(addMonths(view, -1))}
          disabled={!canGoBack}
          aria-label="Previous month"
          className={`${navBtn} left-0`}
        >
          <FiChevronLeft size={18} aria-hidden="true" />
        </button>
        <button
          type="button"
          onClick={() => setView(addMonths(view, 1))}
          disabled={!canGoForward}
          aria-label="Next month"
          className={`${navBtn} right-0`}
        >
          <FiChevronRight size={18} aria-hidden="true" />
        </button>

        {/* Two months side by side from sm up; one month on phones */}
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">
          <Month
            month={view}
            {...{ checkIn, checkOut, hover, today, lastDay }}
            onPick={pick}
            onHover={setHover}
          />
          <div className="hidden sm:block">
            <Month
              month={addMonths(view, 1)}
              {...{ checkIn, checkOut, hover, today, lastDay }}
              onPick={pick}
              onHover={setHover}
            />
          </div>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-4 text-sm">
        <p className="text-xs text-subtext" aria-live="polite">
          {choosingCheckOut
            ? `Now pick check-out (up to ${maxNights} nights)`
            : "Pick your check-in date"}
        </p>
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => onChange({ checkIn: null, checkOut: null })}
            className={`cursor-pointer rounded-md font-medium text-text underline underline-offset-2 hover:text-primary ${focusRing}`}
          >
            Clear dates
          </button>
          <button
            type="button"
            onClick={onClose}
            className={`cursor-pointer rounded-lg bg-primary px-4 py-2 font-medium text-on-primary transition-colors hover:bg-primary-hover ${focusRing}`}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default DateRangePicker;
