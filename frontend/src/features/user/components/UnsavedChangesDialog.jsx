import { useEffect } from "react";
import { ghostBtn, dangerBtn } from "../formStyles";

const UnsavedChangesDialog = ({ open, onStay, onLeave }) => {
  useEffect(() => {
    if (!open) return;
    const onKey = (e) => e.key === "Escape" && onStay();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onStay]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4"
      onClick={onStay}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="unsaved-title"
        aria-describedby="unsaved-desc"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 text-text shadow-xl"
      >
        <h2 id="unsaved-title" className="font-display text-xl font-semibold">
          Leave without saving?
        </h2>
        <p id="unsaved-desc" className="mt-2 text-sm text-subtext">
          You have changes that haven't been saved. If you leave now, they'll be
          lost.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" onClick={onStay} autoFocus className={ghostBtn}>
            Keep editing
          </button>
          <button type="button" onClick={onLeave} className={dangerBtn}>
            Leave page
          </button>
        </div>
      </div>
    </div>
  );
};

export default UnsavedChangesDialog;
