import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "motion/react";
import { FiBell } from "react-icons/fi";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import {
  NOTIFICATIONS_CHANGED,
  deleteNotification,
  fetchNotifications,
  fetchUnreadCount,
  markAllRead,
  markRead,
} from "../notificationService";
import { openNotification } from "../notificationMeta";
import NotificationItem from "./NotificationItem";
import NotificationMenu from "./NotificationMenu";

const POLL_MS = 60_000;
const PREVIEW_SIZE = 5;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const NotificationBell = ({ onOpenChange, onCountChange }) => {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { refreshUser } = useAuth();

  const [count, setCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState(null); // null until the dropdown first loads
  const [loadingItems, setLoadingItems] = useState(false);
  const [error, setError] = useState(null);
  const [markingAll, setMarkingAll] = useState(false);

  const containerRef = useRef(null);
  const buttonRef = useRef(null);
  const prevCountRef = useRef(null); // null until the first count arrives
  const openRef = useRef(false);
  openRef.current = open;
  const refreshRef = useRef(refreshUser);
  refreshRef.current = refreshUser;

  /* ---------- unread count: on mount, every 60s and on tab focus ---------- */
  const loadCount = useCallback(async () => {
    try {
      const res = await fetchUnreadCount();
      const next = Number(res.data?.data) || 0;

      // Something new arrived: roles may have changed (e.g. a host request was approved)
      if (prevCountRef.current !== null && next > prevCountRef.current) {
        refreshRef.current?.();
      }

      prevCountRef.current = next;
      setCount(next);
    } catch {
      // the badge is optional, so fail quietly
    }
  }, []);

  useEffect(() => {
    loadCount();

    const refetchIfVisible = () => {
      if (document.visibilityState === "visible") loadCount();
    };

    const timer = setInterval(refetchIfVisible, POLL_MS);
    window.addEventListener("focus", refetchIfVisible);
    document.addEventListener("visibilitychange", refetchIfVisible);

    return () => {
      clearInterval(timer);
      window.removeEventListener("focus", refetchIfVisible);
      document.removeEventListener("visibilitychange", refetchIfVisible);
    };
  }, [loadCount]);

  // lets the navbar show the same number elsewhere (the mobile menu)
  useEffect(() => {
    onCountChange?.(count);
  }, [count, onCountChange]);

  /* ---------- the latest few, loaded each time the dropdown opens ---------- */
  const loadItems = useCallback(async () => {
    setLoadingItems(true);
    setError(null);
    try {
      const res = await fetchNotifications({ page: 0, size: PREVIEW_SIZE });
      setItems(res.data.data?.content ?? []);
    } catch (err) {
      setError(handleApiError(err));
    } finally {
      setLoadingItems(false);
    }
  }, []);

  // something changed (marked read, deleted, cleared): fix the badge, and refill the list if it's open
  const handleChanged = useCallback(() => {
    loadCount();
    if (openRef.current) loadItems();
  }, [loadCount, loadItems]);

  useEffect(() => {
    window.addEventListener(NOTIFICATIONS_CHANGED, handleChanged);
    return () =>
      window.removeEventListener(NOTIFICATIONS_CHANGED, handleChanged);
  }, [handleChanged]);

  useEffect(() => {
    onOpenChange?.(open); // lets the navbar keep itself visible while this is open
    if (open) {
      loadItems();
      loadCount();
    }
  }, [open, loadItems, loadCount, onOpenChange]);

  /* ---------- close on outside click, Escape and route change ---------- */
  useEffect(() => {
    if (!open) return;

    const onPointerDown = (e) => {
      // the "Clear all" confirm dialog renders outside this component; clicks in it aren't "outside"
      if (e.target.closest?.('[role="dialog"], [role="alertdialog"]')) return;
      if (containerRef.current && !containerRef.current.contains(e.target))
        setOpen(false);
    };
    const onKeyDown = (e) => {
      if (e.key === "Escape") {
        setOpen(false);
        buttonRef.current?.focus();
      }
    };

    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  useEffect(() => setOpen(false), [pathname]);

  /* ---------- actions ---------- */
  const handleSelect = async (n) => {
    setOpen(false);

    if (!n.read) {
      const next = Math.max(0, count - 1);
      prevCountRef.current = next;
      setCount(next);
      setItems(
        (prev) =>
          prev?.map((i) => (i.id === n.id ? { ...i, read: true } : i)) ?? prev,
      );

      markRead(n.id)
        .then(() => window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED)))
        .catch(() => loadCount()); // put the real number back if it failed
    }

    await openNotification(n, { navigate, refreshUser: refreshRef.current });
  };

  const handleDelete = async (n) => {
    const snapshot = items;

    // remove it straight away; an unread one also lowers the badge
    setItems((prev) => prev?.filter((i) => i.id !== n.id) ?? prev);
    if (!n.read) {
      const next = Math.max(0, count - 1);
      prevCountRef.current = next;
      setCount(next);
    }

    try {
      await deleteNotification(n.id);
    } catch (err) {
      // 404 means it was already gone (deleted in another tab): nothing to restore
      if (err.response?.status !== 404) {
        setItems(snapshot);
        notify.error(handleApiError(err));
        loadCount();
        return;
      }
    }

    window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED)); // re-fetches the count and refills the list
  };

  const handleMarkAll = async () => {
    setMarkingAll(true);
    try {
      await markAllRead();
      setItems((prev) => prev?.map((i) => ({ ...i, read: true })) ?? prev);
      prevCountRef.current = 0;
      setCount(0);
      window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED));
    } catch (err) {
      notify.error(handleApiError(err));
    } finally {
      setMarkingAll(false);
    }
  };

  const badge = count > 9 ? "9+" : String(count);

  return (
    // static on phones so the panel can span the navbar width; relative from sm up
    <div ref={containerRef} className="sm:relative">
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-controls="notification-panel"
        aria-label={
          count > 0 ? `Notifications, ${count} unread` : "Notifications"
        }
        className={`relative flex h-10 w-10 cursor-pointer items-center justify-center rounded-full text-text transition-colors duration-300 hover:bg-bg ${focusRing}`}
      >
        <FiBell size={20} aria-hidden="true" />
        {count > 0 && (
          <span
            className="absolute -right-0.5 -top-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-danger px-1 text-[10px] font-bold text-white ring-2 ring-surface"
            aria-hidden="true"
          >
            {badge}
          </span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            id="notification-panel"
            initial={{ opacity: 0, y: -6, scale: 0.97 }}
            animate={{
              opacity: 1,
              y: 0,
              scale: 1,
              transition: { duration: 0.18 },
            }}
            exit={{
              opacity: 0,
              y: -6,
              scale: 0.97,
              transition: { duration: 0.12 },
            }}
            style={{ transformOrigin: "top right" }}
            className="absolute inset-x-3 top-full z-50 mt-2 flex max-h-[min(32rem,calc(100dvh-6rem))] flex-col overflow-hidden rounded-2xl border border-border bg-surface shadow-xl sm:inset-x-auto sm:right-0 sm:mt-3 sm:w-96"
          >
            <div className="flex items-center justify-between gap-3 border-b border-border px-4 py-3">
              <h2 className="font-display text-base font-semibold text-text">
                Notifications
              </h2>

              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={handleMarkAll}
                  disabled={count === 0 || markingAll}
                  className={`cursor-pointer rounded-md text-xs font-semibold text-primary hover:underline disabled:cursor-not-allowed disabled:text-subtext disabled:no-underline ${focusRing}`}
                >
                  {markingAll ? "Marking…" : "Mark all as read"}
                </button>
                <NotificationMenu disabled={items?.length === 0} />
              </div>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto">
              {loadingItems && items === null && (
                <ul aria-busy="true" className="divide-y divide-border">
                  {Array.from({ length: 3 }, (_, i) => (
                    <li
                      key={i}
                      className="h-[76px] animate-pulse bg-surface motion-reduce:animate-none"
                    />
                  ))}
                </ul>
              )}

              {error && (
                <div className="px-4 py-8 text-center text-sm">
                  <p className="text-subtext">{error}</p>
                  <button
                    type="button"
                    onClick={loadItems}
                    className={`mt-3 cursor-pointer rounded-md font-semibold text-primary hover:underline ${focusRing}`}
                  >
                    Try again
                  </button>
                </div>
              )}

              {!error && items?.length === 0 && (
                <p className="px-4 py-10 text-center text-sm text-subtext">
                  You're all caught up.
                </p>
              )}

              {!error && items?.length > 0 && (
                <ul className="divide-y divide-border">
                  {items.map((n) => (
                    <NotificationItem
                      key={n.id}
                      notification={n}
                      onSelect={handleSelect}
                      onDelete={handleDelete}
                      compact
                    />
                  ))}
                </ul>
              )}
            </div>

            <div className="border-t border-border">
              <Link
                to="/notifications"
                className={`block px-4 py-3 text-center text-sm font-semibold text-primary transition-colors hover:bg-bg ${focusRing}`}
              >
                View all
              </Link>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default NotificationBell;
