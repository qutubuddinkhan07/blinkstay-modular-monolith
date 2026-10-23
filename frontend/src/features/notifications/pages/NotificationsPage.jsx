import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import Pagination from "../../listings/components/explore-page/pagination/Pagination";
import { focusRing } from "../../listings/components/create-listing/formStyles";
import { openNotification } from "../notificationMeta";
import {
  deleteNotification,
  fetchList,
  markAllNotificationsRead,
  markNotificationRead,
  selectList,
  selectVersion,
} from "../notificationsSlice";
import NotificationItem from "../components/NotificationItem";
import NotificationMenu from "../components/NotificationMenu";

const TABS = [
  { value: "all", label: "All" },
  { value: "unread", label: "Unread" },
];

const NotificationsPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { refreshUser, isManager } = useAuth();

  const version = useSelector(selectVersion);
  const { items, status, error, totalPages } = useSelector(selectList);

  // UI-only state stays local
  const [filter, setFilter] = useState("all");
  const [page, setPage] = useState(0);
  const [markingAll, setMarkingAll] = useState(false);

  const versionRef = useRef(version);

  // Load on mount, on page or filter change, and again whenever something changed elsewhere
  useEffect(() => {
    const silent = versionRef.current !== version; // a change: refill without the skeleton
    versionRef.current = version;
    dispatch(fetchList({ page, filter, silent }));
  }, [dispatch, page, filter, version]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, [page, filter]);

  // the last page emptied out (after a delete or a clear): step back one page
  useEffect(() => {
    if (
      status === "succeeded" &&
      items.length === 0 &&
      page > 0 &&
      totalPages > 0
    ) {
      setPage(totalPages - 1);
    }
  }, [status, items.length, page, totalPages]);

  const handleSelect = async (n) => {
    if (!n.read) dispatch(markNotificationRead(n));
    await openNotification(n, { navigate, refreshUser, isManager });
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

  const loading = status === "loading";
  const firstLoad = loading && items.length === 0;
  const empty = status === "succeeded" && items.length === 0 && page === 0;

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
            <NotificationMenu disabled={empty && filter === "all"} />
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

        {status === "failed" && (
          <div className="py-16 text-center">
            <h2 className="font-display text-xl font-semibold">
              Couldn't load notifications
            </h2>
            <p className="mt-2 text-subtext">{error}</p>
            <button
              type="button"
              onClick={() => dispatch(fetchList({ page, filter }))}
              className={`mt-5 cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
            >
              Try again
            </button>
          </div>
        )}

        {empty && (
          <p className="py-16 text-center text-subtext">
            {filter === "unread"
              ? "No unread notifications."
              : "You have no notifications yet."}
          </p>
        )}

        {status !== "failed" && items.length > 0 && (
          <div
            className={loading ? "opacity-60 transition-opacity" : ""}
            aria-busy={loading}
          >
            <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border bg-surface">
              {items.map((n) => (
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
              totalPages={totalPages || 1}
              onChange={setPage}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;
