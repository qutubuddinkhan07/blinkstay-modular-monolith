import React, { useEffect, useMemo, useRef, useState } from "react";
import DateRangePicker from "../listing/Daterangepicker";

const CURRENCY = "INR"; // change to match your backend
const formatMoney = (n) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: CURRENCY,
    maximumFractionDigits: 0,
  }).format(n);

const formatDate = (d) =>
  d
    ? d.toLocaleDateString("en-US", {
        day: "numeric",
        month: "short",
        year: "numeric",
      })
    : "Add date";

const nightsBetween = (a, b) =>
  a && b ? Math.round((b.getTime() - a.getTime()) / 86400000) : 0;

const BookingCard = ({ rooms = [], onReserve }) => {
  const [open, setOpen] = useState(false);
  const [dates, setDates] = useState({ checkIn: null, checkOut: null });
  const [roomId, setRoomId] = useState(rooms[0]?.roomId ?? "");
  const wrapperRef = useRef(null);

  // Close the picker on outside click or Escape
  useEffect(() => {
    if (!open) return;

    const onDown = (e) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target))
        setOpen(false);
    };

    const onKey = (e) => e.key === "Escape" && setOpen(false);

    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);

    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const room = useMemo(
    () => rooms.find((r) => r.roomId === roomId) ?? rooms[0],
    [rooms, roomId],
  );
  const nights = nightsBetween(dates.checkIn, dates.checkOut);
  const total = room ? nights * room.price : 0;

  const fieldBtn =
    "px-4 py-3 text-left transition-colors hover:bg-neutral-50 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-neutral-900";

  return (
    <aside className="lg:sticky lg:top-24 lg:self-start">
      <div
        ref={wrapperRef}
        className="relative rounded-2xl border border-neutral-200 bg-white p-6 shadow-lg"
      >
        <p className="text-neutral-600">
          <span className="text-2xl font-semibold text-neutral-900">
            {room ? formatMoney(room.price) : "—"}
          </span>{" "}
          / night
        </p>

        {/* Date fields */}
        <div className="mt-5 grid grid-cols-2 divide-x divide-neutral-300 overflow-hidden rounded-xl border border-neutral-300">
          <button
            type="button"
            onClick={() => setOpen(true)}
            className={fieldBtn}
          >
            <span className="block text-xs font-semibold">Check-in</span>
            <span
              className={dates.checkIn ? "text-sm" : "text-sm text-neutral-500"}
            >
              {formatDate(dates.checkIn)}
            </span>
          </button>
          <button
            type="button"
            onClick={() => setOpen(true)}
            className={fieldBtn}
          >
            <span className="block text-xs font-semibold">Check-out</span>
            <span
              className={
                dates.checkOut ? "text-sm" : "text-sm text-neutral-500"
              }
            >
              {formatDate(dates.checkOut)}
            </span>
          </button>
        </div>

        {/* Room type */}
        {rooms.length > 0 && (
          <label className="mt-3 block">
            <span className="mb-1 block text-xs font-semibold">Room type</span>
            <select
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
              className="w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm"
            >
              {rooms.map((r) => (
                <option key={r.roomId} value={r.roomId}>
                  {r.roomType.charAt(0) + r.roomType.slice(1).toLowerCase()} ·{" "}
                  {formatMoney(r.price)}
                </option>
              ))}
            </select>
          </label>
        )}

        <button
          type="button"
          disabled={!nights}
          onClick={() => onReserve?.({ ...dates, room, nights, total })}
          className="mt-4 w-full rounded-xl bg-[#e01f59] py-3 font-semibold text-white transition-opacity disabled:cursor-not-allowed disabled:opacity-50"
        >
          {nights ? "Reserve" : "Select dates"}
        </button>

        {nights > 0 && room && (
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-neutral-600">
                {formatMoney(room.price)} × {nights} night
                {nights > 1 ? "s" : ""}
              </dt>
              <dd>{formatMoney(total)}</dd>
            </div>
            <div className="flex justify-between border-t border-neutral-200 pt-3 font-semibold">
              <dt>Total</dt>
              <dd>{formatMoney(total)}</dd>
            </div>
          </dl>
        )}

        {/*
          Date picker panel. It is anchored to the card's right edge and grows
          to the LEFT on large screens, so the card appears to enlarge over the
          listing info. On small screens it simply fills the card width.
        */}
        <div
          className={`absolute right-0 top-[5.5rem] z-30 w-full origin-top-right rounded-2xl border border-neutral-200 bg-white p-6 shadow-2xl transition duration-200 lg:w-[44rem] ${
            open
              ? "scale-100 opacity-100"
              : "pointer-events-none scale-95 opacity-0"
          }`}
          aria-hidden={!open}
        >
          <DateRangePicker
            checkIn={dates.checkIn}
            checkOut={dates.checkOut}
            onChange={setDates}
            onClose={() => setOpen(false)}
          />
        </div>
      </div>
    </aside>
  );
};

export default BookingCard;
