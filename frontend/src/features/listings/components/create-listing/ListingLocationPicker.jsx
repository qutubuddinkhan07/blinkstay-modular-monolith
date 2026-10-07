import { useEffect, useRef, useState } from "react";
import mapboxgl from "mapbox-gl";
import axios from "axios";
import { notify } from "../../../../utils/notify"; // adjust to where your util lives
import "mapbox-gl/dist/mapbox-gl.css";
import { useTheme } from "../../../../context/ThemeContext"; // adjust to your folder depth
import { cardClass, labelClass, inputClass } from "./formStyles";

mapboxgl.accessToken = import.meta.env.VITE_MAPBOX_TOKEN;

const MAP_STYLES = {
  light: "mapbox://styles/mapbox/streets-v12",
  dark: "mapbox://styles/mapbox/dark-v11",
};

// Reads the live theme color so the marker matches light/dark primary
const getPrimaryColor = () =>
  getComputedStyle(document.documentElement)
    .getPropertyValue("--color-primary")
    .trim() || "#710019";

const ListingLocationPicker = ({ value, onLocationSelect }) => {
  const { isDark } = useTheme();
  const styleUrl = isDark ? MAP_STYLES.dark : MAP_STYLES.light;

  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const styleRef = useRef(styleUrl);
  const debounceRef = useRef(null);
  const requestIdRef = useRef(0);

  const [search, setSearch] = useState(value || "");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  /* Create the map once */
  useEffect(() => {
    if (!mapContainerRef.current) return;

    const map = new mapboxgl.Map({
      container: mapContainerRef.current,
      style: styleRef.current,
      center: [78.9629, 20.5937],
      zoom: 4,
    });

    map.addControl(new mapboxgl.NavigationControl(), "top-right");
    mapRef.current = map;

    return () => {
      clearTimeout(debounceRef.current);
      map.remove();
    };
  }, []);

  /* Swap the map style when the theme changes (markers are DOM, so they stay) */
  useEffect(() => {
    if (mapRef.current && styleRef.current !== styleUrl) {
      styleRef.current = styleUrl;
      mapRef.current.setStyle(styleUrl);
    }
  }, [styleUrl]);

  /* Form was reset from the parent -> clear the picker too */
  useEffect(() => {
    if (!value) {
      setSearch("");
      setResults([]);
      markerRef.current?.remove();
      markerRef.current = null;
    }
  }, [value]);

  const searchLocation = async (query) => {
    const requestId = ++requestIdRef.current;

    try {
      setLoading(true);

      const response = await axios.get(
        `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(
          query,
        )}.json`,
        {
          params: {
            access_token: mapboxgl.accessToken,
            autocomplete: true,
            limit: 5,
            language: "en",
          },
        },
      );

      if (requestId !== requestIdRef.current) return; // stale response
      setResults(response.data.features || []);
    } catch (error) {
      if (requestId !== requestIdRef.current) return;
      console.error("Mapbox search failed:", error);
      setResults([]);
      notify.error("Couldn't search locations. Please try again.", {
        toastId: "geocode-error", // avoids stacking duplicates while typing
      });
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  };

  const handleSearchChange = (e) => {
    const query = e.target.value;
    setSearch(query);

    clearTimeout(debounceRef.current);

    if (!query.trim()) {
      requestIdRef.current++;
      setResults([]);
      setLoading(false);
      return;
    }

    debounceRef.current = setTimeout(() => searchLocation(query), 300);
  };

  const handleSelect = (place) => {
    clearTimeout(debounceRef.current);
    requestIdRef.current++;

    const [longitude, latitude] = place.center;

    const countryContext = place.context?.find((item) =>
      item.id.startsWith("country."),
    );

    const country = countryContext?.text?.toUpperCase();
    const location = place.text || place.place_name;

    onLocationSelect({ location, country, latitude, longitude });

    setSearch(place.place_name);
    setResults([]);
    setLoading(false);

    if (mapRef.current) {
      mapRef.current.flyTo({
        center: [longitude, latitude],
        zoom: 12,
        essential: true,
      });

      markerRef.current?.remove();

      markerRef.current = new mapboxgl.Marker({ color: getPrimaryColor() })
        .setLngLat([longitude, latitude])
        .addTo(mapRef.current);
    }
  };

  return (
    <div className={cardClass}>
      <h2 className="mb-2 font-display text-xl font-medium text-text">
        Property location
      </h2>

      <p className="mb-5 text-sm text-subtext">
        Search for the property and select the correct location.
      </p>

      <div className="relative">
        <label htmlFor="location-search" className={labelClass}>
          Search location
        </label>

        <input
          id="location-search"
          type="text"
          value={search}
          onChange={handleSearchChange}
          onKeyDown={(e) => e.key === "Escape" && setResults([])}
          placeholder="Search city, area or landmark..."
          autoComplete="off"
          className={inputClass}
        />

        {loading && <p className="mt-2 text-sm text-subtext">Searching...</p>}

        {results.length > 0 && (
          <div className="absolute left-0 right-0 z-20 mt-1 overflow-hidden rounded-lg border border-border bg-surface shadow-lg shadow-text/10">
            {results.map((place) => (
              <button
                key={place.id}
                type="button"
                onClick={() => handleSelect(place)}
                className="block w-full cursor-pointer border-b border-border px-4 py-3 text-left transition-colors last:border-b-0 hover:bg-bg focus-visible:bg-bg focus-visible:outline-none"
              >
                <p className="font-medium text-text">{place.text}</p>
                <p className="text-xs text-subtext">{place.place_name}</p>
              </button>
            ))}
          </div>
        )}
      </div>

      <div
        ref={mapContainerRef}
        className="mt-5 h-[400px] w-full overflow-hidden rounded-xl border border-border"
      />

      {value && (
        <div className="mt-4 rounded-lg border border-border border-l-4 border-l-primary bg-bg p-4">
          <p className="text-sm font-semibold text-text">Selected location</p>
          <p className="text-sm text-subtext">{value}</p>
        </div>
      )}
    </div>
  );
};

export default ListingLocationPicker;
