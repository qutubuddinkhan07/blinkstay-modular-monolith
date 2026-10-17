import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { AnimatePresence, motion } from "motion/react";
import { FiMinus, FiPlus } from "react-icons/fi";
import DateRangePicker, { startOfDay } from "../listing/Daterangepicker";
import { useAuth } from "../../../../context/AuthContext";
import { handleApiError } from "../../../../api/errors/handleApiError";
import { notify } from "../../../../utils/notify";
import {
  addDays,
  formatDateLabel,
  formatINR,
  formatRoomType,
  parseLocalDate,
  toLocalDateString,
} from "../../../../utils/format";
import {
  BOOKING_LIMITS,
  createBooking,
  getAvailability,
} from "../../../bookings/bookingService";

const { maxAdvanceDays, maxNights, maxRooms, maxGuests } = BOOKING_LIMITS;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;

const clampInt = (value, min, max, fallback) => {
  const n = Number.parseInt(value, 10);
  return Number.isNaN(n) ? fallback : Math.min(max, Math.max(min, n));
};

// The stay lives in the URL (?checkIn=&checkOut=&rooms=&guests=), so it survives
// a refresh and the trip to login and back. Anything the server would reject is dropped.
const readStay = (params) => {
  const today = startOfDay(new Date());
  let checkIn = parseLocalDate(params.get("checkIn"));
  let checkOut = parseLocalDate(params.get("checkOut"));

  if (
    checkIn &&
    (checkIn < today || checkIn > addDays(today, maxAdvanceDays))
  ) {
    checkIn = null;
  }
  if (
    !checkIn ||
    (checkOut &&
      (checkOut <= checkIn || checkOut > addDays(checkIn, maxNights)))
  ) {
    checkOut = null;
  }

  return {
    checkIn,
    checkOut,
    rooms: clampInt(params.get("rooms"), 1, maxRooms, 1),
    guests: clampInt(params.get("guests"), 1, maxGuests, 2),
  };
};

const stepBtn = `flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border text-text transition-colors hover:border-text disabled:cursor-not-allowed disabled:opacity-30 ${focusRing}`;

const Stepper = ({ label, value, min, max, onChange }) => (
  <div className="flex items-center justify-between gap-3 px-4 py-3">
    <span className="text-xs font-semibold text-text">{label}</span>
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={() => onChange(value - 1)}
        disabled={value <= min}
        aria-label={`Decrease ${label.toLowerCase()}`}
        className={stepBtn}
      >
        <FiMinus size={14} aria-hidden="true" />
      </button>
      <span
        className="w-6 text-center text-sm font-semibold tabular-nums"
        aria-live="polite"
      >
        {value}
      </span>
      <button
        type="button"
        onClick={() => onChange(value + 1)}
        disabled={value >= max}
        aria-label={`Increase ${label.toLowerCase()}`}
        className={stepBtn}
      >
        <FiPlus size={14} aria-hidden="true" />
      </button>
    </div>
  </div>
);

const RoomRow = ({
  room,
  nights,
  roomsCount,
  currency,
  disabled,
  reserving,
  onReserve,
}) => {
  const name = formatRoomType(room.roomType);
  return (
    <li className="rounded-xl border border-border p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-text">{name}</p>
          <p className="text-xs text-subtext">
            Up to {plural(room.maxGuests, "guest")} per room
          </p>
        </div>
        <p className="text-right text-sm text-subtext">
          <span className="font-semibold text-text">
            {formatINR(room.pricePerNight, currency)}
          </span>{" "}
          / night
        </p>
      </div>

      <div className="mt-3 flex items-end justify-between gap-3">
        <div>
          <p className="text-sm text-text">
            <span className="font-semibold">
              {formatINR(room.totalPrice, currency)}
            </span>{" "}
            total
          </p>
          <p className="text-xs text-subtext">
            {plural(nights, "night")} · {plural(roomsCount, "room")}
          </p>
          {room.reason && (
            <p
              className={`mt-1 text-xs font-medium ${room.bookable ? "text-subtext" : "text-danger"}`}
            >
              {room.reason}
            </p>
          )}
        </div>

        <button
          type="button"
          onClick={() => onReserve(room.roomId)}
          disabled={!room.bookable || disabled}
          aria-label={`Reserve ${name} room`}
          className={`shrink-0 cursor-pointer rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
        >
          {reserving ? "Reserving…" : "Reserve"}
        </button>
      </div>
    </li>
  );
};

const BookingCard = ({
  listingId,
  rooms = [],
  isOwner = false,
  onUnavailable,
}) => {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();

  const stay = readStay(searchParams);
  const checkInStr = toLocalDateString(stay.checkIn);
  const checkOutStr = toLocalDateString(stay.checkOut);
  const hasDates = Boolean(checkInStr && checkOutStr);

  const [open, setOpen] = useState(false);
  const [availability, setAvailability] = useState(null);
  const [checking, setChecking] = useState(false);
  const [availError, setAvailError] = useState("");
  const [reloadKey, setReloadKey] = useState(0); // bump to re-fetch availability
  const [reservingId, setReservingId] = useState(null);

  const wrapperRef = useRef(null);

  // Keep the latest callback without re-running the fetch effect when it changes
  const onUnavailableRef = useRef(onUnavailable);
  useEffect(() => {
    onUnavailableRef.current = onUnavailable;
  });

  const fromPrice = useMemo(
    () => (rooms.length ? Math.min(...rooms.map((r) => r.price)) : null),
    [rooms],
  );

  // Write changes into the URL without adding history entries
  const updateStay = useCallback(
    (changes) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          Object.entries(changes).forEach(([key, value]) => {
            if (value == null || value === "") next.delete(key);
            else next.set(key, String(value));
          });
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  const onDatesChange = ({ checkIn, checkOut }) =>
    updateStay({
      checkIn: toLocalDateString(checkIn),
      checkOut: toLocalDateString(checkOut),
    });

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

  // Availability: debounced, and a slow old response never overwrites a newer one
  useEffect(() => {
    if (isOwner || !checkInStr || !checkOutStr) {
      setAvailability(null);
      setAvailError("");
      setChecking(false);
      return;
    }

    let ignore = false;
    setChecking(true);
    setAvailError("");

    const timer = setTimeout(async () => {
      try {
        const data = await getAvailability(listingId, {
          checkIn: checkInStr,
          checkOut: checkOutStr,
          rooms: stay.rooms,
          guests: stay.guests,
        });
        if (!ignore) setAvailability(data);
      } catch (err) {
        if (ignore) return;
        if (err.response?.status === 404) {
          onUnavailableRef.current?.(); // the page re-checks and shows "Listing not available"
          return;
        }
        setAvailability(null);
        setAvailError(handleApiError(err, "availability"));
      } finally {
        if (!ignore) setChecking(false);
      }
    }, 300);

    return () => {
      ignore = true;
      clearTimeout(timer);
    };
  }, [
    listingId,
    checkInStr,
    checkOutStr,
    stay.rooms,
    stay.guests,
    isOwner,
    reloadKey,
  ]);

  const reserve = async (roomId) => {
    if (!isAuthenticated) {
      notify.info("Log in to reserve. You'll come back to these dates.");
      navigate("/login", { state: { from: location } }); // location includes ?checkIn=...
      return;
    }
    if (reservingId) return;

    setReservingId(roomId);
    try {
      const booking = await createBooking({
        roomId,
        checkIn: checkInStr,
        checkOut: checkOutStr,
        rooms: stay.rooms,
        guests: stay.guests,
      });
      notify.success("Rooms held. Complete the payment to confirm.");
      navigate(`/bookings/${booking.id}`);
    } catch (err) {
      const status = err.response?.status;
      notify.error(handleApiError(err, "booking"));
      if (status === 404) onUnavailableRef.current?.();
      if (status === 409) setReloadKey((k) => k + 1); // show the new room numbers
    } finally {
      setReservingId(null);
    }
  };

  if (isOwner) {
    return (
      <aside className="lg:sticky lg:top-24 lg:self-start">
        <div className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="font-display text-lg font-semibold text-text">
            This is your listing
          </h2>
          <p className="mt-1 text-sm text-subtext">
            Guests choose dates and rooms here. You can't book your own listing.
          </p>
        </div>
      </aside>
    );
  }

  const today = startOfDay(new Date());
  const nights = availability?.nights ?? 0;
  const fieldBtn = `cursor-pointer px-4 py-3 text-left text-text transition-colors hover:bg-input focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary`;

  return (
    <aside className="lg:sticky lg:top-24 lg:self-start">
      <div
        ref={wrapperRef}
        className="relative rounded-2xl border border-border bg-surface p-6 shadow-lg"
      >
        <p className="text-subtext">
          {hasDates && availability ? (
            <span className="font-semibold text-text">
              {plural(nights, "night")} · {formatDateLabel(stay.checkIn)} –{" "}
              {formatDateLabel(stay.checkOut)}
            </span>
          ) : (
            <>
              <span className="text-sm">From </span>
              <span className="text-2xl font-semibold text-text">
                {fromPrice != null ? formatINR(fromPrice) : "—"}
              </span>{" "}
              / night
            </>
          )}
        </p>

        {/* Date fields */}
        <div className="mt-5 grid grid-cols-2 divide-x divide-border overflow-hidden rounded-xl border border-border">
          {[
            ["Check-in", stay.checkIn],
            ["Check-out", stay.checkOut],
          ].map(([label, value]) => (
            <button
              key={label}
              type="button"
              onClick={() => setOpen(true)}
              aria-expanded={open}
              aria-controls="booking-dates"
              className={fieldBtn}
            >
              <span className="block text-xs font-semibold">{label}</span>
              <span className={value ? "text-sm" : "text-sm text-subtext"}>
                {formatDateLabel(value)}
              </span>
            </button>
          ))}
        </div>

        {/* Rooms and guests */}
        <div className="mt-3 divide-y divide-border rounded-xl border border-border">
          <Stepper
            label="Rooms"
            value={stay.rooms}
            min={1}
            max={maxRooms}
            onChange={(rooms) => updateStay({ rooms })}
          />
          <Stepper
            label="Guests"
            value={stay.guests}
            min={1}
            max={maxGuests}
            onChange={(guests) => updateStay({ guests })}
          />
        </div>

        {!hasDates ? (
          <button
            type="button"
            onClick={() => setOpen(true)}
            className={`mt-4 w-full cursor-pointer rounded-xl bg-primary py-3 font-semibold text-on-primary transition-colors hover:bg-primary-hover ${focusRing}`}
          >
            Select dates
          </button>
        ) : (
          <section
            className="mt-5"
            aria-busy={checking}
            aria-label="Rooms for your stay"
          >
            {availError && (
              <p role="alert" className="mb-3 text-sm text-danger">
                {availError}
              </p>
            )}

            {!availability && checking && (
              <ul className="space-y-3" aria-hidden="true">
                {[0, 1, 2].map((i) => (
                  <li
                    key={i}
                    className="h-28 animate-pulse rounded-xl bg-input"
                  />
                ))}
              </ul>
            )}

            {availability && availability.roomTypes.length === 0 && (
              <p className="text-sm text-subtext">
                No rooms are listed for this stay yet.
              </p>
            )}

            {availability && availability.roomTypes.length > 0 && (
              <ul
                className={`space-y-3 transition-opacity ${checking ? "opacity-60" : ""}`}
              >
                {availability.roomTypes.map((room) => (
                  <RoomRow
                    key={room.roomId}
                    room={room}
                    nights={availability.nights}
                    roomsCount={availability.rooms}
                    currency={availability.currency}
                    disabled={checking || Boolean(reservingId)}
                    reserving={reservingId === room.roomId}
                    onReserve={reserve}
                  />
                ))}
              </ul>
            )}

            <p className="mt-4 text-center text-xs text-subtext">
              You won't be charged yet.
            </p>
          </section>
        )}

        {/* Date picker panel */}
        <AnimatePresence>
          {open && (
            <motion.div
              id="booking-dates"
              role="dialog"
              aria-label="Choose your dates"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.15 }}
              className="absolute right-0 top-[5.5rem] z-30 w-full origin-top-right rounded-2xl border border-border bg-surface p-4 shadow-2xl sm:p-6 lg:w-[44rem]"
            >
              <DateRangePicker
                checkIn={stay.checkIn}
                checkOut={stay.checkOut}
                onChange={onDatesChange}
                onClose={() => setOpen(false)}
                maxDate={addDays(today, maxAdvanceDays)}
                maxNights={maxNights}
              />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </aside>
  );
};

export default BookingCard;
