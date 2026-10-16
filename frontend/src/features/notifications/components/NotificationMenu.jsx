import { useEffect, useRef, useState } from "react";
import { FiMoreHorizontal } from "react-icons/fi";
import { useDispatch } from "react-redux";
import { notify } from "../../../utils/notify";
import { handleApiError } from "../../../api/errors/handleApiError";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";
import {
  clearAllNotifications,
  clearReadNotifications,
} from "../notificationService";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const itemCls =
  "block w-full cursor-pointer px-4 py-2.5 text-left text-sm font-medium transition-colors hover:bg-bg focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-50";

const NotificationMenu = ({ disabled = false }) => {
  const dispatch = useDispatch();
  const [open, setOpen] = useState(false);
  const [confirmAll, setConfirmAll] = useState(false);
  const [busy, setBusy] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!open) return;

    const onPointerDown = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    const onKeyDown = (e) => e.key === "Escape" && setOpen(false);

    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  const run = async (action, successMessage) => {
    setBusy(true);
    try {
      await dispatch(action).unwrap(); // the slice refreshes the bell and the list by itself
      notify.success(successMessage);
    } catch (err) {
      notify.error(err?.message || "Something went wrong.");
    } finally {
      setBusy(false);
    }
  };

  const handleClearRead = async () => {
    setOpen(false);
    await run(clearReadNotifications(), "Read notifications cleared.");
  };

  const handleClearAll = async () => {
    await run(clearAllNotifications(), "All notifications cleared.");
    setConfirmAll(false);
  };

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        disabled={disabled || busy}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="More notification actions"
        className={`flex h-8 w-8 cursor-pointer items-center justify-center rounded-full text-subtext transition-colors hover:bg-bg hover:text-text disabled:cursor-not-allowed disabled:opacity-50 ${focusRing}`}
      >
        <FiMoreHorizontal size={18} aria-hidden="true" />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 top-full z-20 mt-1 w-44 overflow-hidden rounded-xl border border-border bg-surface py-1 shadow-lg"
        >
          <button
            type="button"
            role="menuitem"
            onClick={handleClearRead}
            className={`${itemCls} text-text`}
          >
            Clear read
          </button>
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              setConfirmAll(true);
            }}
            className={`${itemCls} text-danger`}
          >
            Clear all
          </button>
        </div>
      )}

      <ConfirmDialog
        open={confirmAll}
        title="Clear all notifications?"
        description="This permanently deletes every notification, including the ones you haven't read."
        confirmLabel="Clear all"
        busyLabel="Clearing…"
        busy={busy}
        onConfirm={handleClearAll}
        onCancel={() => setConfirmAll(false)}
      />
    </div>
  );
};

export default NotificationMenu;
