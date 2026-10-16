import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { AnimatePresence, motion } from "motion/react";
import { FiBell } from "react-icons/fi";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { openNotification } from "../notificationMeta";
import {
  deleteNotification,
  fetchPreview,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
  notificationsReset,
  selectPreview,
  selectUnreadCount,
  selectVersion,
} from "../notificationsSlice";
import NotificationItem from "./NotificationItem";
import NotificationMenu from "./NotificationMenu";

const POLL_MS = 60_000;

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const NotificationBell = ({ onOpenChange }) => {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const dispatch = useDispatch();
  const { refreshUser } = useAuth();

  const count = useSelector(selectUnreadCount);
  const version = useSelector(selectVersion);
  const { items, status, error } = useSelector(selectPreview);

  const [open, setOpen] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);

  const containerRef = useRef(null);
  const buttonRef = useRef(null);
  const refreshRef = useRef(refreshUser);
  refreshRef.current = refreshUser;

  /* ---------- unread count: on mount, every 60s and on tab focus ---------- */
  const loadCount = useCallback(async () => {
    const result = await dispatch(fetchUnreadCount());

    // Something new arrived: roles may have changed (e.g. a host request was approved)
    if (fetchUnreadCount.fulfilled.match(result) && result.payload.increased) {
      refreshRef.current?.();
    }
  }, [dispatch]);

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

  // The bell only exists while logged in, so unmounting means logout: empty the store
  useEffect(
    () => () => {
      dispatch(notificationsReset());
    },
    [dispatch],
  );

  /* ---------- the dropdown loads on open, and again after any change ---------- */
  useEffect(() => {
    onOpenChange?.(open); // lets the navbar keep itself visible while this is open
    if (open) dispatch(fetchPreview());
  }, [open, version, dispatch, onOpenChange]);

  useEffect(() => {
    if (open) loadCount();
  }, [open, loadCount]);

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
    if (!n.read) dispatch(markNotificationRead(n)); // optimistic; rolls itself back on failure
    await openNotification(n, { navigate, refreshUser: refreshRef.current });
  };

  const handleDelete = (n) => {
    dispatch(deleteNotification(n))
      .unwrap()
      .catch((err) =>
        notify.error(err?.message || "Couldn't delete the notification."),
      );
  };

  const handleMarkAll = async () => {
    setMarkingAll(true);
    try {
      await dispatch(markAllNotificationsRead()).unwrap();
    } catch (err) {
      notify.error(err?.message || "Something went wrong.");
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
              {status === "loading" && items === null && (
                <ul aria-busy="true" className="divide-y divide-border">
                  {Array.from({ length: 3 }, (_, i) => (
                    <li
                      key={i}
                      className="h-[76px] animate-pulse bg-surface motion-reduce:animate-none"
                    />
                  ))}
                </ul>
              )}

              {status === "failed" && (
                <div className="px-4 py-8 text-center text-sm">
                  <p className="text-subtext">{error}</p>
                  <button
                    type="button"
                    onClick={() => dispatch(fetchPreview())}
                    className={`mt-3 cursor-pointer rounded-md font-semibold text-primary hover:underline ${focusRing}`}
                  >
                    Try again
                  </button>
                </div>
              )}

              {status !== "failed" && items?.length === 0 && (
                <p className="px-4 py-10 text-center text-sm text-subtext">
                  You're all caught up.
                </p>
              )}

              {status !== "failed" && items?.length > 0 && (
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
