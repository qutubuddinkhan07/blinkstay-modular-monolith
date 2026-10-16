import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useAuth } from "./AuthContext";
import {
  addFavorite,
  fetchFavorites,
  fetchFavoriteIds,
  removeFavorite,
} from "../features/favorites/favoritesService";
import { notify } from "../utils/notify";
import { handleApiError } from "../api/errors/handleApiError";

const FavoritesContext = createContext(undefined);

// Fired when every pending favorite change has reached the server (the Favorites page refills on it)
export const FAVORITES_SETTLED = "favorites:settled";

const DEBOUNCE_MS = 400;

export const FavoritesProvider = ({ children }) => {
  const { isAuthenticated } = useAuth();

  const [ids, setIds] = useState(() => new Set()); // what the hearts show
  const [ready, setReady] = useState(false);

  const shownRef = useRef(new Set()); // same as `ids`, but readable inside callbacks
  const serverRef = useRef(new Set()); // what the server has confirmed
  const timersRef = useRef(new Map()); // id -> debounce timer
  const inFlightRef = useRef(new Set()); // ids with a request running
  const sessionRef = useRef(0); // changes on login/logout so late responses are ignored

  const show = useCallback((next) => {
    shownRef.current = next;
    setIds(next);
  }, []);

  const announceIfSettled = useCallback(() => {
    if (timersRef.current.size === 0 && inFlightRef.current.size === 0) {
      window.dispatchEvent(new Event(FAVORITES_SETTLED));
    }
  }, []);

  // Sends the final state of one listing, if it differs from what the server has
  const sync = useCallback(
    async (id) => {
      timersRef.current.delete(id);
      if (inFlightRef.current.has(id)) return; // re-checked when the running request ends

      const session = sessionRef.current;
      const want = shownRef.current.has(id);

      // clicked and unclicked: nothing to send
      if (want === serverRef.current.has(id)) {
        announceIfSettled();
        return;
      }

      inFlightRef.current.add(id);
      try {
        if (want) await addFavorite(id);
        else await removeFavorite(id);
        if (session !== sessionRef.current) return;

        const confirmed = new Set(serverRef.current);
        if (want) confirmed.add(id);
        else confirmed.delete(id);
        serverRef.current = confirmed;
      } catch (err) {
        if (session !== sessionRef.current) return;

        if (!want && err.response?.status === 404) {
          // already gone on the server: the removal is done
          const confirmed = new Set(serverRef.current);
          confirmed.delete(id);
          serverRef.current = confirmed;
        } else {
          // undo: the heart goes back to what the server has.
          // 404 "Listing not available" (unpublished meanwhile) and 409 (limit reached) land here too.
          const back = new Set(shownRef.current);
          if (serverRef.current.has(id)) back.add(id);
          else back.delete(id);
          show(back);
          notify.error(handleApiError(err));
        }
      } finally {
        inFlightRef.current.delete(id);
        if (session === sessionRef.current) {
          const differs =
            shownRef.current.has(id) !== serverRef.current.has(id);
          // clicked again while the request was running
          if (differs && !timersRef.current.has(id)) sync(id);
          else announceIfSettled();
        }
      }
    },
    [announceIfSettled, show],
  );

  const toggle = useCallback(
    (rawId) => {
      const id = String(rawId);

      const next = new Set(shownRef.current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      show(next); // the heart changes right away

      clearTimeout(timersRef.current.get(id));
      timersRef.current.set(
        id,
        setTimeout(() => sync(id), DEBOUNCE_MS),
      );
    },
    [show, sync],
  );

  // Load the ids after login (or when the app starts with a session); clear them on logout
  useEffect(() => {
    sessionRef.current += 1;
    timersRef.current.forEach((t) => clearTimeout(t));
    timersRef.current.clear();
    inFlightRef.current.clear();

    if (!isAuthenticated) {
      serverRef.current = new Set();
      show(new Set());
      setReady(false);
      return;
    }

    let ignore = false;
    setReady(false);

    fetchFavoriteIds()
      .then((res) => {
        if (ignore) return;
        const list = res.data?.data ?? [];
        const loaded = new Set(list.map(String));
        serverRef.current = loaded;
        show(new Set(loaded));
      })
      .catch(() => {
        // hearts simply show as empty; nothing else to do
      })
      .finally(() => {
        if (!ignore) setReady(true);
      });

    return () => {
      ignore = true;
    };
  }, [isAuthenticated, show]);

  const value = useMemo(
    () => ({
      count: ids.size,
      ready,
      ids,
      isFavorite: (id) => ids.has(String(id)),
      toggle,
    }),
    [ids, ready, toggle],
  );

  return (
    <FavoritesContext.Provider value={value}>
      {children}
    </FavoritesContext.Provider>
  );
};

export const useFavorites = () => {
  const context = useContext(FavoritesContext);
  if (context === undefined) {
    throw new Error("useFavorites must be used within a FavoritesProvider");
  }
  return context;
};
