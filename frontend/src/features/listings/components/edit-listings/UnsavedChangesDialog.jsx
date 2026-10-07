import { useEffect, useRef } from "react";
import { focusRing } from "../create-listing/formStyles";

const UnsavedChangesDialog = ({ onStay, onLeave }) => {
  const stayRef = useRef(null);

  // Focus the safe option and let Escape mean "stay"
  useEffect(() => {
    stayRef.current?.focus();

    const onKeyDown = (e) => {
      if (e.key === "Escape") onStay();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onStay]);

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-text/50 px-4 backdrop-blur-sm"
      onClick={onStay}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="unsaved-title"
        aria-describedby="unsaved-desc"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 shadow-xl"
      >
        <h2
          id="unsaved-title"
          className="font-display text-xl font-medium text-text"
        >
          Discard unsaved changes?
        </h2>
        <p id="unsaved-desc" className="mt-2 text-sm text-subtext">
          You have edits that haven't been saved. If you leave now, they'll be
          lost.
        </p>

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onLeave}
            className={`cursor-pointer rounded-lg border border-border bg-bg px-5 py-2.5 text-sm font-semibold text-danger transition-colors hover:bg-danger/10 ${focusRing}`}
          >
            Discard changes
          </button>
          <button
            ref={stayRef}
            type="button"
            onClick={onStay}
            className={`cursor-pointer rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-hover ${focusRing}`}
          >
            Keep editing
          </button>
        </div>
      </div>
    </div>
  );
};

export default UnsavedChangesDialog;
