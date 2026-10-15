import { FiTrash2 } from "react-icons/fi";
import { getNotificationIcon, timeAgo } from "../notificationMeta";

const NotificationItem = ({
  notification: n,
  onSelect,
  onDelete,
  compact = false,
}) => {
  const Icon = getNotificationIcon(n.type);

  return (
    <li className="group relative">
      <button
        type="button"
        onClick={() => onSelect(n)}
        className={`flex w-full cursor-pointer gap-3 py-3 pl-4 text-left transition-colors hover:bg-bg focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary ${
          onDelete ? "pr-12" : "pr-4"
        } ${n.read ? "" : "bg-primary/5"}`}
      >
        <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-muted text-subtext">
          <Icon size={16} aria-hidden="true" />
        </span>

        <span className="min-w-0 flex-1">
          <span
            className={`block text-sm text-text ${n.read ? "font-medium" : "font-semibold"}`}
          >
            {!n.read && (
              <>
                <span className="sr-only">Unread: </span>
                <span
                  className="mr-2 inline-block h-2 w-2 rounded-full bg-primary align-middle"
                  aria-hidden="true"
                />
              </>
            )}
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
      </button>

      {onDelete && (
        <button
          type="button"
          onClick={() => onDelete(n)}
          aria-label={`Delete notification: ${n.title}`}
          title="Delete"
          className="absolute right-2 top-2 flex h-8 w-8 cursor-pointer items-center justify-center rounded-full text-subtext transition hover:bg-danger/10 hover:text-danger focus-visible:opacity-100 focus-visible:outline-2 focus-visible:outline-danger md:opacity-0 md:group-focus-within:opacity-100 md:group-hover:opacity-100"
        >
          <FiTrash2 size={15} aria-hidden="true" />
        </button>
      )}
    </li>
  );
};

export default NotificationItem;
