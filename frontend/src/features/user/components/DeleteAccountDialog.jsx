import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { deleteAccount } from "../userService";
import { dangerBtn, ghostBtn, inputCls } from "../formStyles";
import { handleApiError } from "../../../api/errors/handleApiError";

const DeleteAccountDialog = ({ open, onClose, onDeleted }) => {
  const { clearSession } = useAuth();
  const navigate = useNavigate();
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open) return;
    const onKey = (e) => e.key === "Escape" && !busy && onClose();
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, busy, onClose]);

  useEffect(() => {
    if (!open) setText("");
  }, [open]);

  if (!open) return null;

  const onDelete = async () => {
    setBusy(true);
    try {
      await deleteAccount(); // backend also clears the cookie
      +onDeleted?.(); // lets the page skip the unsaved-changes prompt
      clearSession();
      notify.success("Your account has been deleted.");
      navigate("/login", { replace: true });
    } catch (err) {
      notify.error(handleApiError(err));
      setBusy(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4"
      onClick={() => !busy && onClose()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-title"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md rounded-2xl border border-border bg-surface p-6 text-text shadow-xl"
      >
        <h2 id="delete-title" className="font-display text-xl font-semibold">
          Delete your account?
        </h2>
        <p className="mt-2 text-sm text-subtext">
          This is permanent. Type <strong className="text-text">DELETE</strong>{" "}
          to confirm.
        </p>
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          autoFocus
          aria-label="Type DELETE to confirm"
          className={`${inputCls} mt-4`}
        />
        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className={ghostBtn}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onDelete}
            disabled={text !== "DELETE" || busy}
            className={dangerBtn}
          >
            {busy ? "Deleting…" : "Delete account"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default DeleteAccountDialog;
