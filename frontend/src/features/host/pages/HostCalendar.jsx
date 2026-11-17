import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { FiChevronLeft, FiChevronRight } from "react-icons/fi";
import StatusBadge from "../../../components/common/StatusBadge";
import {
  EmptyState,
  Skeleton,
  chip,
} from "../../../components/common/DashboardParts";
import { handleApiError } from "../../../api/errors/handleApiError";
import { useGetMyListingsQuery } from "../../../store/api";
import { useLoad } from "../../../utils/useLoad";
import { usePageTitle } from "../../../utils/usePageTitle";
import { formatRoomType, plural } from "../../../utils/format";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { inputCls } from "../../user/formStyles";
import { BOOKING_STATUS } from "../../bookings/bookingService";
import { getHostCalendar } from "../dashboardService";
import { formatPlainDate, parsePlainDate } from "../dashboardUtils";

const WEEK_STARTS_ON = 0; // 0 = Sunday, 1 = Monday
const WEEKDAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTH_RE = /^\d{4}-(0[1-9]|1[0-2])$/;

// an unpaid hold reads "On hold" here, not "Awaiting payment"
const CALENDAR_STATUS = {
  ...BOOKING_STATUS,
  PENDING_PAYMENT: { label: "On hold", dot: "bg-amber-500" },
};

const pad = (n) => String(n).padStart(2, "0");

const currentMonth = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
};

// "2026-12" + 1 -> "2027-01"
const shiftMonth = (month, delta) => {
  const [y, m] = month.split("-").map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
};

// "2026-12" -> "December 2026"
const monthTitle = (month) => {
  const [y, m] = month.split("-").map(Number);
  return new Date(y, m - 1, 1).toLocaleDateString("en-US", {
    month: "long",
    year: "numeric",
  });
};

// "2026-12-09" -> "Wed, 9 Dec 2026"
const longDate = (date) =>
  parsePlainDate(date)?.toLocaleDateString("en-IN", {
    weekday: "short",
    day: "numeric",
    month: "short",
    year: "numeric",
  }) ?? date;

// A booking covers the nights from checkIn up to, but not including, checkOut.
// ISO dates compare correctly as text.
const coversNight = (b, date) => b.checkIn <= date && date < b.checkOut;

const TONES = {
  free: "border-emerald-500/40 bg-emerald-500/15",
  partial: "border-amber-500/50 bg-amber-500/15",
  full: "border-danger/50 bg-danger/15",
};
const TONE_WORDS = { free: "all free", partial: "partly booked", full: "full" };

const toneOf = (day, total) => {
  if (Number(day.available) <= 0) return "full";
  return Number(day.available) >= Number(total) ? "free" : "partial";
};

const DayCell = ({ day, total, isToday, selected, onSelect }) => {
  const tone = toneOf(day, total);
  const held = Number(day.held) > 0;
  const dayNumber = Number(day.date.slice(8));

  const label = [
    longDate(day.date),
    `${day.available} of ${total} free (${TONE_WORDS[tone]})`,
    held ? `${day.held} on hold, unpaid` : "",
    day.past ? "past" : "",
    isToday ? "today" : "",
  ]
    .filter(Boolean)
    .join(", ");

  return (
    <button
      type="button"
      onClick={() => onSelect(day.date)}
      aria-pressed={selected}
      aria-label={label}
      title={label}
      className={`flex h-16 w-full cursor-pointer flex-col items-center justify-center rounded-lg text-text transition-opacity ${TONES[tone]} ${
        held ? "border-2 border-dashed" : "border"
      } ${day.past ? "opacity-45" : ""} ${
        selected ? "outline-2 outline-offset-2 outline-primary" : ""
      } ${focusRing}`}
    >
      <span
        className={`text-xs ${
          isToday
            ? "rounded-full bg-primary px-1.5 font-semibold text-on-primary"
            : "text-subtext"
        }`}
      >
        {dayNumber}
      </span>
      <span className="text-sm font-semibold tabular-nums">
        {day.available}/{total}
      </span>
      {held && <span className="text-[10px] leading-none">hold</span>}
    </button>
  );
};

const Legend = () => (
  <ul className="mt-4 flex flex-wrap gap-x-4 gap-y-2 text-xs text-subtext">
    {[
      ["All free", `border ${TONES.free}`],
      ["Partly booked", `border ${TONES.partial}`],
      ["Full", `border ${TONES.full}`],
      ["On hold, unpaid", "border-2 border-dashed border-border"],
      ["Past day", "border border-border bg-muted opacity-45"],
    ].map(([text, cls]) => (
      <li key={text} className="inline-flex items-center gap-1.5">
        <span className={`h-3.5 w-3.5 rounded ${cls}`} aria-hidden="true" />
        {text}
      </li>
    ))}
    <li>Each day shows free rooms / total rooms.</li>
  </ul>
);

const th =
  "px-4 py-3 text-xs font-semibold uppercase tracking-wide text-subtext";

const BookingsTable = ({ rows, showRoom = true, caption }) => (
  <div className="overflow-x-auto rounded-2xl border border-border bg-surface">
    <table className="w-full min-w-2xl text-left text-sm">
      <caption className="sr-only">{caption}</caption>
      <thead className="border-b border-border">
        <tr>
          <th scope="col" className={th}>
            Reference
          </th>
          {showRoom && (
            <th scope="col" className={th}>
              Room
            </th>
          )}
          <th scope="col" className={th}>
            Guest
          </th>
          <th scope="col" className={th}>
            Stay
          </th>
          <th scope="col" className={`${th} text-right`}>
            Rooms
          </th>
          <th scope="col" className={`${th} text-right`}>
            Guests
          </th>
          <th scope="col" className={th}>
            Status
          </th>
        </tr>
      </thead>
      <tbody>
        {rows.map((b) => (
          <tr
            key={b.bookingId}
            className="border-b border-border last:border-0"
          >
            <td className="px-4 py-3 font-mono text-xs font-semibold text-text">
              {b.reference}
            </td>
            {showRoom && (
              <td className="px-4 py-3">{formatRoomType(b.roomType)}</td>
            )}
            <td className="max-w-40 truncate px-4 py-3" title={b.guestName}>
              {b.guestName}
            </td>
            <td className="whitespace-nowrap px-4 py-3 text-subtext">
              {formatPlainDate(b.checkIn)} – {formatPlainDate(b.checkOut)}
            </td>
            <td className="px-4 py-3 text-right tabular-nums">
              {b.roomsBooked}
            </td>
            <td className="px-4 py-3 text-right tabular-nums">{b.guests}</td>
            <td className="px-4 py-3">
              <StatusBadge status={b.status} statuses={CALENDAR_STATUS} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const HostCalendar = () => {
  usePageTitle("Calendar");
  const [searchParams, setSearchParams] = useSearchParams();

  // Listings for the picker
  const {
    data: listingsData,
    isLoading: listingsLoading,
    isError: listingsFailed,
    refetch: refetchListings,
  } = useGetMyListingsQuery();
  const listings = Array.isArray(listingsData)
    ? listingsData
    : (listingsData?.content ?? []);

  const listing =
    listings.find((l) => String(l.id) === searchParams.get("listing")) ??
    listings[0] ??
    null;
  const listingId = listing?.id ?? "";

  const monthParam = searchParams.get("month");
  const month = MONTH_RE.test(monthParam ?? "") ? monthParam : currentMonth();

  const setParam = (key, value) =>
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set(key, value);
        return next;
      },
      { replace: true },
    );

  // Calendar data
  const [errorText, setErrorText] = useState("");
  const { data, loading, failed, reload } = useLoad(
    () =>
      listingId ? getHostCalendar(listingId, month) : Promise.resolve(null),
    [listingId, month],
    (err) => setErrorText(handleApiError(err, "calendar")), // 400 bad month, 403 not yours, 404
  );

  const rooms = data?.rooms ?? [];
  const bookings = useMemo(
    () =>
      [...(data?.bookings ?? [])].sort((a, b) =>
        a.checkIn.localeCompare(b.checkIn),
      ),
    [data],
  );

  const [roomId, setRoomId] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const room = rooms.find((r) => r.roomId === roomId) ?? rooms[0] ?? null;

  // another listing, month or room type: the chosen day no longer applies
  useEffect(() => {
    setSelectedDate(null);
  }, [listingId, month, room?.roomId]);

  // empty cells before day 1, so the first day sits under its weekday
  const days = room?.days ?? [];
  const firstWeekday = days.length ? parsePlainDate(days[0].date).getDay() : 0;
  const blanks = (firstWeekday - WEEK_STARTS_ON + 7) % 7;
  const weekdays = WEEKDAYS.map((_, i) => WEEKDAYS[(i + WEEK_STARTS_ON) % 7]);

  const selectedDay = days.find((d) => d.date === selectedDate) ?? null;
  const dayBookings = selectedDay
    ? bookings.filter(
        (b) => b.roomId === room.roomId && coversNight(b, selectedDay.date),
      )
    : [];

  const firstLoad = loading && !data;
  const navBtn = `flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg border border-border bg-surface text-text transition-colors hover:bg-bg ${focusRing}`;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-5xl space-y-6">
        <header>
          <h1 className="font-display text-3xl font-semibold">Calendar</h1>
          <p className="mt-2 text-subtext">
            Rooms booked, on hold and free for each night.
          </p>
        </header>

        {listingsLoading ? (
          <Skeleton className="h-96 w-full" />
        ) : listingsFailed ? (
          <div
            role="alert"
            className="rounded-2xl border border-border bg-surface p-6 text-center"
          >
            <p className="text-sm text-danger">
              We couldn't load your listings.
            </p>
            <button
              type="button"
              onClick={refetchListings}
              className={`mt-3 cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text hover:bg-bg ${focusRing}`}
            >
              Try again
            </button>
          </div>
        ) : listings.length === 0 ? (
          <EmptyState>
            You don't have any listings yet.{" "}
            <Link
              to="/create-listing"
              className="font-semibold text-primary underline"
            >
              Create one
            </Link>{" "}
            to see its calendar.
          </EmptyState>
        ) : (
          <>
            {/* Listing and month */}
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div className="w-full sm:w-80">
                <label
                  htmlFor="calendar-listing"
                  className="mb-1 block text-xs font-semibold text-subtext"
                >
                  Listing
                </label>
                <select
                  id="calendar-listing"
                  value={listingId}
                  onChange={(e) => setParam("listing", e.target.value)}
                  className={inputCls}
                >
                  {listings.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.title}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setParam("month", shiftMonth(month, -1))}
                  aria-label="Previous month"
                  className={navBtn}
                >
                  <FiChevronLeft aria-hidden="true" />
                </button>
                <p
                  className="min-w-36 text-center font-display text-lg font-semibold"
                  aria-live="polite"
                >
                  {monthTitle(month)}
                </p>
                <button
                  type="button"
                  onClick={() => setParam("month", shiftMonth(month, 1))}
                  aria-label="Next month"
                  className={navBtn}
                >
                  <FiChevronRight aria-hidden="true" />
                </button>
                {month !== currentMonth() && (
                  <button
                    type="button"
                    onClick={() => setParam("month", currentMonth())}
                    className={`cursor-pointer rounded-lg border border-border bg-surface px-3 py-2 text-sm font-semibold text-text hover:bg-bg ${focusRing}`}
                  >
                    This month
                  </button>
                )}
              </div>
            </div>

            {firstLoad ? (
              <Skeleton className="h-96 w-full" />
            ) : failed ? (
              <div
                role="alert"
                className="rounded-2xl border border-border bg-surface p-6 text-center"
              >
                <p className="text-sm text-danger">{errorText}</p>
                <button
                  type="button"
                  onClick={reload}
                  className={`mt-3 cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text hover:bg-bg ${focusRing}`}
                >
                  Try again
                </button>
              </div>
            ) : rooms.length === 0 ? (
              <EmptyState>
                This listing has no rooms yet.{" "}
                <Link
                  to={`/listings/${listingId}/edit`}
                  className="font-semibold text-primary underline"
                >
                  Add a room
                </Link>{" "}
                to see its calendar.
              </EmptyState>
            ) : (
              <div
                className={`space-y-6 ${loading ? "opacity-60 transition-opacity" : ""}`}
                aria-busy={loading}
              >
                {/* Room type tabs */}
                {rooms.length > 1 && (
                  <div
                    className="inline-flex flex-wrap rounded-lg border border-border bg-surface p-1"
                    role="group"
                    aria-label="Room type"
                  >
                    {rooms.map((r) => (
                      <button
                        key={r.roomId}
                        type="button"
                        aria-pressed={r.roomId === room.roomId}
                        onClick={() => setRoomId(r.roomId)}
                        className={chip(r.roomId === room.roomId)}
                      >
                        {formatRoomType(r.roomType)}
                      </button>
                    ))}
                  </div>
                )}

                {/* Month grid */}
                <section
                  aria-labelledby="grid-heading"
                  className="rounded-2xl border border-border bg-surface p-4 sm:p-5"
                >
                  <h2
                    id="grid-heading"
                    className="font-display text-lg font-semibold"
                  >
                    {formatRoomType(room.roomType)}
                    <span className="ml-2 text-sm font-normal text-subtext">
                      {plural(room.totalRooms, "room")}
                    </span>
                  </h2>

                  <div className="mt-4 overflow-x-auto pb-1">
                    <div className="min-w-[28rem]">
                      <div className="grid grid-cols-7 gap-1.5 text-center text-xs font-semibold text-subtext">
                        {weekdays.map((w) => (
                          <div key={w} className="py-1">
                            {w}
                          </div>
                        ))}
                      </div>
                      <div className="mt-1 grid grid-cols-7 gap-1.5">
                        {Array.from({ length: blanks }, (_, i) => (
                          <div key={`blank-${i}`} aria-hidden="true" />
                        ))}
                        {days.map((d) => (
                          <DayCell
                            key={d.date}
                            day={d}
                            total={room.totalRooms}
                            isToday={d.date === data.today}
                            selected={d.date === selectedDate}
                            onSelect={setSelectedDate}
                          />
                        ))}
                      </div>
                    </div>
                  </div>

                  <Legend />
                </section>

                {/* The chosen night */}
                <section aria-labelledby="day-heading" aria-live="polite">
                  <h2
                    id="day-heading"
                    className="mb-3 font-display text-lg font-semibold"
                  >
                    {selectedDay
                      ? `Night of ${longDate(selectedDay.date)}`
                      : "Bookings for a night"}
                  </h2>

                  {!selectedDay ? (
                    <EmptyState>
                      Select a day to see who is staying that night.
                    </EmptyState>
                  ) : (
                    <>
                      <p className="mb-3 text-sm text-subtext">
                        {formatRoomType(room.roomType)}: {selectedDay.booked}{" "}
                        booked
                        {Number(selectedDay.held) > 0 &&
                          `, ${selectedDay.held} on hold (unpaid)`}
                        , {selectedDay.available} of {room.totalRooms} free.
                      </p>
                      {dayBookings.length === 0 ? (
                        <EmptyState>No bookings for this night.</EmptyState>
                      ) : (
                        <BookingsTable
                          rows={dayBookings}
                          showRoom={false}
                          caption={`Bookings for the night of ${longDate(selectedDay.date)}`}
                        />
                      )}
                    </>
                  )}
                </section>

                {/* Everything in the month */}
                <section aria-labelledby="month-heading">
                  <h2
                    id="month-heading"
                    className="mb-3 font-display text-lg font-semibold"
                  >
                    All bookings in {monthTitle(month)}
                    <span className="ml-2 text-sm font-normal text-subtext">
                      {bookings.length}
                    </span>
                  </h2>
                  {bookings.length === 0 ? (
                    <EmptyState>No bookings this month.</EmptyState>
                  ) : (
                    <BookingsTable
                      rows={bookings}
                      caption={`All bookings in ${monthTitle(month)}`}
                    />
                  )}
                </section>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default HostCalendar;
