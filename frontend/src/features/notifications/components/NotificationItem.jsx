import { getNotificationIcon, timeAgo } from "../notificationMeta";

const NotificationItem = ({ notification: n, onSelect, compact = false }) => {
  const Icon = getNotificationIcon(n.type);

  return (
    <li>
      <button
        type="button"
        onClick={() => onSelect(n)}
        className={`flex w-full cursor-pointer gap-3 px-4 py-3 text-left transition-colors hover:bg-bg focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary ${
          n.read ? "" : "bg-primary/5"
        }`}
      >
        <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-muted text-subtext">
          <Icon size={16} aria-hidden="true" />
        </span>

        <span className="min-w-0 flex-1">
          <span
            className={`block text-sm text-text ${n.read ? "font-medium" : "font-semibold"}`}
          >
            {!n.read && <span className="sr-only">Unread: </span>}
            {n.title}
          </span>
          <span
            className={`mt-0.5 block break-words text-sm text-subtext ${compact ? "line-clamp-2" : ""}`}
          >
            {n.message}
          </span>
          <span className="mt-1 block text-xs text-subtext">
            {timeAgo(n.createdAt)}
          </span>
        </span>

        {!n.read && (
          <span
            className="mt-2 h-2.5 w-2.5 shrink-0 rounded-full bg-primary"
            aria-hidden="true"
          />
        )}
      </button>
    </li>
  );
};

export default NotificationItem;
