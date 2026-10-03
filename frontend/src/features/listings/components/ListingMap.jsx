import React, { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";

mapboxgl.accessToken = import.meta.env.VITE_MAPBOX_TOKEN;

const CURRENCY = "INR"; // keep in sync with BookingCard

const compactMoney = (n) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: CURRENCY,
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(n);

/** Popup shown when a nearby marker is clicked. Built with the DOM API (no innerHTML) so text is never parsed as HTML. */
const buildPopup = (item, navigate) => {
  const link = document.createElement("a");
  link.href = `/listings/${item.id}`;
  link.className = "block w-48 text-neutral-900 no-underline";
  link.addEventListener("click", (e) => {
    if (e.metaKey || e.ctrlKey) return; // let "open in new tab" work
    e.preventDefault();
    navigate(`/listings/${item.id}`); // client-side navigation, no page reload
  });

  if (item.thumbnailUrl) {
    const img = document.createElement("img");
    img.src = item.thumbnailUrl;
    img.alt = item.title;
    img.className = "mb-2 h-28 w-full rounded-lg object-cover";
    link.appendChild(img);
  }

  const title = document.createElement("div");
  title.className = "truncate text-sm font-semibold";
  title.textContent = item.title;
  link.appendChild(title);

  const meta = document.createElement("div");
  meta.className = "text-xs text-neutral-600";
  const parts = [item.location];
  if (item.startingPrice != null)
    parts.push(`from ${compactMoney(item.startingPrice)} / night`);
  if (item.distanceKm != null)
    parts.push(`${item.distanceKm.toFixed(1)} km away`);
  meta.textContent = parts.filter(Boolean).join(" · ");
  link.appendChild(meta);

  return new mapboxgl.Popup({
    offset: 20,
    closeButton: false,
    maxWidth: "220px",
  }).setDOMContent(link);
};

/** One Mapbox map. Used both inline and in the full-screen view. */
const MapView = ({
  latitude,
  longitude,
  title,
  address,
  nearby,
  fullscreen,
}) => {
  const containerRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!containerRef.current) return;

    // Mapbox wants [longitude, latitude]
    const center = [longitude, latitude];

    const map = new mapboxgl.Map({
      container: containerRef.current,
      style: "mapbox://styles/mapbox/streets-v12",
      center,
      zoom: 11,
      // Inline: Ctrl + scroll to zoom so the page can still scroll. Full screen: normal zoom.
      cooperativeGestures: !fullscreen,
    });

    map.addControl(
      new mapboxgl.NavigationControl({ showCompass: false }),
      "top-right",
    );
    if (fullscreen)
      map.addControl(new mapboxgl.FullscreenControl(), "top-right");

    // Main listing: red pin
    const mainPopup = new mapboxgl.Popup({
      offset: 28,
      closeButton: false,
    }).setText(
      address && address !== "Seed location" ? `${title} - ${address}` : title,
    );
    new mapboxgl.Marker({ color: "#e01f59" })
      .setLngLat(center)
      .setPopup(mainPopup)
      .addTo(map);

    // Nearby listings: price pills
    const bounds = new mapboxgl.LngLatBounds(center, center);
    nearby.forEach((n) => {
      if (n.latitude == null || n.longitude == null) return;

      const pill = document.createElement("button");
      pill.type = "button";
      // NOTE: don't use transform (e.g. hover:scale) here -- Mapbox positions markers with it.
      pill.className =
        "rounded-full bg-white px-3 py-1 text-sm font-semibold text-neutral-900 shadow-md ring-1 ring-black/10 transition-colors hover:bg-neutral-900 hover:text-white";
      pill.textContent =
        n.startingPrice != null ? compactMoney(n.startingPrice) : "Stay";
      pill.setAttribute("aria-label", `${n.title}, ${n.location}`);

      new mapboxgl.Marker({ element: pill })
        .setLngLat([n.longitude, n.latitude])
        .setPopup(buildPopup(n, navigate))
        .addTo(map);

      bounds.extend([n.longitude, n.latitude]);
    });

    // Zoom out just enough to show everything
    if (nearby.length > 0) {
      map.fitBounds(bounds, { padding: 70, maxZoom: 14, duration: 0 });
    }

    return () => map.remove();
  }, [latitude, longitude, title, address, nearby, fullscreen, navigate]);

  return (
    <div
      ref={containerRef}
      className="h-full w-full"
      role="region"
      aria-label={`Map showing the location of ${title}`}
    />
  );
};

const ListingMap = ({ latitude, longitude, title, address, nearby = [] }) => {
  const [expanded, setExpanded] = useState(false);

  // While full screen: lock page scroll and close on Escape
  useEffect(() => {
    if (!expanded) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    
    const onKey = (e) => e.key === "Escape" && setExpanded(false);
    window.addEventListener("keydown", onKey);

    return () => {
      document.body.style.overflow = previous;
      window.removeEventListener("keydown", onKey);
    };
  }, [expanded]);

  if (latitude == null || longitude == null) {
    return (
      <div className="flex h-72 items-center justify-center rounded-2xl bg-neutral-100 text-neutral-500">
        Location not available
      </div>
    );
  }

  const mapProps = { latitude, longitude, title, address, nearby };

  return (
    <>
      <div className="relative h-72 w-full overflow-hidden rounded-2xl sm:h-96">
        <MapView {...mapProps} fullscreen={false} />

        <button
          type="button"
          onClick={() => setExpanded(true)}
          className="absolute left-3 top-3 z-10 rounded-lg bg-white px-3 py-2 text-sm font-medium shadow-md hover:bg-neutral-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-900"
        >
          Expand map
        </button>
      </div>

      {/* Portal to <body> so no parent (sticky, transform, overflow) can clip the overlay */}
      {expanded &&
        createPortal(
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Full screen map"
            className="fixed inset-0 z-50 bg-white"
          >
            <MapView {...mapProps} fullscreen />

            <button
              type="button"
              autoFocus
              onClick={() => setExpanded(false)}
              className="absolute left-4 top-4 z-10 rounded-lg bg-white px-4 py-2 text-sm font-medium shadow-lg hover:bg-neutral-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-900"
            >
              Close map
            </button>

            {nearby.length > 0 && (
              <p className="absolute bottom-4 left-1/2 z-10 -translate-x-1/2 rounded-full bg-white px-4 py-2 text-sm shadow-lg">
                {nearby.length} nearby stay{nearby.length > 1 ? "s" : ""}
              </p>
            )}
          </div>,
          document.body,
        )}
    </>
  );
};

export default ListingMap;
