import { useEffect, useState } from "react";
import { getRoomsTonight } from "../../../bookings/bookingService";
import {
  formatDateLabel,
  formatINR,
  formatRoomType,
  plural,
} from "../../../../utils/format";

const LOW_STOCK = 3; // "Only N left tonight" at this number or fewer

// Live numbers for tonight. Optional: if the request fails, the rooms simply show without them.
export const useRoomsTonight = (listingId) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!listingId) return;
    let ignore = false;

    const load = () =>
      getRoomsTonight(listingId)
        .then((d) => {
          if (!ignore) setData(d);
        })
        .catch(() => {
          // keep whatever we already show
        })
        .finally(() => {
          if (!ignore) setLoading(false);
        });

    setData(null);
    setLoading(true);
    load();

    // numbers go stale while a tab sits in the background: refresh when the user comes back
    const onVisible = () => {
      if (document.visibilityState === "visible") load();
    };
    document.addEventListener("visibilitychange", onVisible);

    return () => {
      ignore = true;
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [listingId]);

  return { data, loading };
};

const Pill = ({ dot, tone, children }) => (
  <span
    className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold text-text ${tone}`}
  >
    <span className={`h-2 w-2 rounded-full ${dot}`} aria-hidden="true" />
    {children}
  </span>
);

const TonightBadge = ({ free }) => {
  if (free == null) return null;
  const n = Number(free);

  if (n <= 0) {
    return (
      <Pill dot="bg-danger" tone="border-danger/40 bg-danger/10">
        Sold out tonight
      </Pill>
    );
  }
  if (n <= LOW_STOCK) {
    return (
      <Pill dot="bg-amber-500" tone="border-border bg-muted">
        Only {n} left tonight
      </Pill>
    );
  }
  // a quiet confirmation when plenty is left; delete this line if you'd rather show nothing
  return <span className="text-xs text-subtext">Available tonight</span>;
};

// tonight: the response of GET /v5/availability/{id}/tonight (or null)
const ListingRooms = ({
  rooms = [],
  tonight = null,
  loading = false,
  className = "",
}) => {
  if (rooms.length === 0) return null;

  // merge by roomId: capacity from totalRooms, live number from freeTonight
  const live = new Map((tonight?.rooms ?? []).map((r) => [r.roomId, r]));
  const merged = rooms.map((r) => {
    const l = live.get(r.roomId ?? r.id);
    return {
      ...r,
      totalRooms: l?.totalRooms ?? r.totalRooms,
      freeTonight: l?.freeTonight,
    };
  });

  return (
    <section
      aria-labelledby="rooms-heading"
      className={`border-t border-border pt-8 ${className}`}
    >
      <h2 id="rooms-heading" className="text-xl font-semibold">
        Rooms
      </h2>
      {tonight?.date && (
        <p className="mt-1 text-sm text-subtext">
          Live availability for tonight, {formatDateLabel(tonight.date)}.
        </p>
      )}

      <ul className="mt-5 grid gap-3 sm:grid-cols-2">
        {merged.map((r) => (
          <li
            key={r.roomId ?? r.id}
            className="rounded-2xl border border-border bg-surface p-4"
          >
            <div className="flex items-start justify-between gap-3">
              <h3 className="font-semibold text-text">
                {formatRoomType(r.roomType)}
              </h3>
              <p className="shrink-0 text-right font-semibold text-text">
                {formatINR(r.price, r.currency)}
                <span className="block text-xs font-normal text-subtext">
                  per night
                </span>
              </p>
            </div>

            <p className="mt-1 text-sm text-subtext">
              {r.maxGuests ? `Up to ${plural(r.maxGuests, "guest")} · ` : ""}
              {plural(r.totalRooms, "room")} of this type
            </p>

            <div className="mt-3 min-h-6">
              {loading ? (
                <span
                  className="block h-5 w-28 animate-pulse rounded-full bg-input motion-reduce:animate-none"
                  aria-hidden="true"
                />
              ) : (
                <TonightBadge free={r.freeTonight} />
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
};

export default ListingRooms;
