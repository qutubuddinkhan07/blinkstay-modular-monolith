import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import {
  NOTIFICATIONS_CHANGED,
  fetchNotifications,
  deleteNotification,
  markAllRead,
  markRead,
} from "../notificationService";
import { openNotification } from "../notificationMeta";
import NotificationItem from "../components/NotificationItem";
import NotificationMenu from "../components/NotificationMenu";

const PAGE_SIZE = 15;

const TABS = [
  { value: "all", label: "All" },
  { value: "unread", label: "Unread" },
];

const NotificationsPage = () => {
  const navigate = useNavigate();
  const { refreshUser } = useAuth();

  const [filter, setFilter] = useState("all");
  const [page, setPage] = useState(0);
  const [data, setData] = useState({
    items: [],
    totalPages: 0,
    totalElements: null,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [markingAll, setMarkingAll] = useState(false);

  useEffect(() => {
    let ignore = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const res = await fetchNotifications({
          page,
          size: PAGE_SIZE,
          unreadOnly: filter === "unread",
        });
        if (ignore) return;

        const d = res.data.data ?? {};
        const next = {
          items: d.content ?? [],
          totalPages: d.totalPages ?? 0,
          totalElements: d.totalElements ?? null,
        };

        // the last page emptied out: step back one page
        if (next.items.length === 0 && page > 0 && next.totalPages > 0) {
          setPage(next.totalPages - 1);
          return;
        }
        setData(next);
      } catch (err) {
        if (!ignore) setError(handleApiError(err));
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    load();
    return () => {
      ignore = true;
    };
  }, [filter, page, reloadKey]);

  // the bell marked something read (or the list changed): refresh quietly
  useEffect(() => {
    const reload = () => setReloadKey((k) => k + 1);
    window.addEventListener(NOTIFICATIONS_CHANGED, reload);
    return () => window.removeEventListener(NOTIFICATIONS_CHANGED, reload);
  }, []);

  const handleSelect = async (n) => {
    if (!n.read) {
      setData((d) => ({
        ...d,
        items: d.items.map((i) => (i.id === n.id ? { ...i, read: true } : i)),
      }));
      markRead(n.id)
        .then(() => window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED)))
        .catch(() => {});
    }
    await openNotification(n, { navigate, refreshUser });
  };

  const handleMarkAll = async () => {
    setMarkingAll(true);
    try {
      await markAllRead();
      window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED)); // bell and list both refresh
    } catch (err) {
      notify.error(handleApiError(err));
    } finally {
      setMarkingAll(false);
    }
  };

  const handleDelete = async (n) => {
    const snapshot = data;

    // remove it straight away
    setData((d) => ({
      ...d,
      items: d.items.filter((i) => i.id !== n.id),
      totalElements: d.totalElements != null ? d.totalElements - 1 : null,
    }));

    try {
      await deleteNotification(n.id);
    } catch (err) {
      // 404 means it was already gone: nothing to restore
      if (err.response?.status !== 404) {
        setData(snapshot);
        notify.error(handleApiError(err));
        return;
      }
    }

    // refreshes the bell badge, and refills this page from the server
    window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED));
  };

  const firstLoad = loading && data.items.length === 0;

  return (
    <div className="min-h-[calc(100vh-72px)] bg-bg px-4 py-8 text-text transition-colors duration-300">
      <div className="mx-auto max-w-3xl">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="font-display text-3xl font-semibold">
              Notifications
            </h1>
            <p className="mt-2 text-subtext">
              Updates about your account, listings and host requests.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleMarkAll}
              disabled={markingAll}
              className={`cursor-pointer rounded-lg border border-border px-4 py-2 text-sm font-semibold text-text transition-colors hover:bg-surface disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
            >
              {markingAll ? "Marking…" : "Mark all as read"}
            </button>
            <NotificationMenu
              disabled={!loading && data.items.length === 0 && filter === "all"}
            />
          </div>
        </div>

        <div
          className="mb-4 inline-flex rounded-lg border border-border bg-surface p-1"
          role="group"
          aria-label="Filter notifications"
        >
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              aria-pressed={filter === t.value}
              onClick={() => {
                setFilter(t.value);
                setPage(0);
              }}
              className={`cursor-pointer rounded-md px-4 py-1.5 text-sm font-semibold transition-colors ${focusRing} ${
                filter === t.value
                  ? "bg-primary text-on-primary"
                  : "text-subtext hover:text-text"
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {firstLoad && (
          <ul
            className="overflow-hidden rounded-2xl border border-border bg-surface"
            aria-busy="true"
          >
            {Array.from({ length: 5 }, (_, i) => (
              <li
                key={i}
                className="h-20 animate-pulse border-b border-border last:border-0 motion-reduce:animate-none"
              />
            ))}
          </ul>
        )}

        {!loading && error && (
          <div className="py-16 text-center">
            <h2 className="font-display text-xl font-semibold">
              Couldn't load notifications
            </h2>
            <p className="mt-2 text-subtext">{error}</p>
            <button
              type="button"
              onClick={() => setReloadKey((k) => k + 1)}
              className={`mt-5 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
            >
              Try again
            </button>
          </div>
        )}

        {!loading && !error && data.items.length === 0 && (
          <p className="py-16 text-center text-subtext">
            {filter === "unread"
              ? "No unread notifications."
              : "You have no notifications yet."}
          </p>
        )}

        {!error && data.items.length > 0 && (
          <div
            className={loading ? "opacity-60 transition-opacity" : ""}
            aria-busy={loading}
          >
            <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border bg-surface">
              {data.items.map((n) => (
                <NotificationItem
                  key={n.id}
                  notification={n}
                  onSelect={handleSelect}
                  onDelete={handleDelete}
                />
              ))}
            </ul>
            <Pagination
              page={page}
              totalPages={data.totalPages}
              onChange={setPage}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;
