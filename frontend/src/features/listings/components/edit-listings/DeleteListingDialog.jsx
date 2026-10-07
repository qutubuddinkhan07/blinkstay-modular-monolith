import { useEffect, useRef } from "react";
import { focusRing } from "../create-listing/formStyles";

const DeleteListingDialog = ({ title, deleting, onCancel, onConfirm }) => {
  const cancelRef = useRef(null);

  // Focus the safe option; Escape cancels (unless a request is in flight)
  useEffect(() => {
    cancelRef.current?.focus();

    const onKeyDown = (e) => {
      if (e.key === "Escape" && !deleting) onCancel();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onCancel, deleting]);

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-text/50 px-4 backdrop-blur-sm"
      onClick={() => !deleting && onCancel()}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="delete-title"
        aria-describedby="delete-desc"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 shadow-xl"
      >
        <h2
          id="delete-title"
          className="font-display text-xl font-medium text-text"
        >
          Delete this listing?
        </h2>
        <p id="delete-desc" className="mt-2 text-sm text-subtext">
          <span className="font-medium text-text">{title}</span> and all of its
          images will be permanently removed. This can't be undone.
        </p>

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            ref={cancelRef}
            type="button"
            onClick={onCancel}
            disabled={deleting}
            className={`cursor-pointer rounded-lg border border-border bg-bg px-5 py-2.5 text-sm font-semibold text-text transition-colors hover:bg-border/60 disabled:opacity-50 ${focusRing}`}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={deleting}
            className={`cursor-pointer rounded-lg bg-danger px-5 py-2.5 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:cursor-wait disabled:opacity-60 ${focusRing}`}
          >
            {deleting ? "Deleting..." : "Delete listing"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default DeleteListingDialog;
