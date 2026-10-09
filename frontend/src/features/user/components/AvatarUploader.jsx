import { useEffect, useRef, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { removeAvatar, uploadAvatar } from "../userService";
import { validateImage } from "../userValidation";
import { errCls, ghostBtn, hintCls, primaryBtn } from "../formStyles";
import { handleApiError } from "../../../api/errors/handleApiError";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";

const AvatarUploader = ({ onDirtyChange }) => {
  const { user, updateUser } = useAuth();
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [confirmRemove, setConfirmRemove] = useState(false);

  useEffect(() => () => preview && URL.revokeObjectURL(preview), [preview]);
  useEffect(() => {
    onDirtyChange?.(Boolean(file));
  }, [file, onDirtyChange]);

  const reset = () => {
    setFile(null);
    setPreview(null);
    if (inputRef.current) inputRef.current.value = "";
  };

  const onPick = (e) => {
    const picked = e.target.files?.[0];
    if (!picked) return;
    const problem = validateImage(picked);
    setError(problem);
    if (problem) return reset();
    setFile(picked);
    setPreview(URL.createObjectURL(picked));
  };

  const onUpload = async () => {
    setBusy(true);
    const request = uploadAvatar(file);

    notify.promise(request, {
      loading: "Uploading photo",
      success: (res) => res.data?.message || "Photo updated",
      error: (err) => handleApiError(err),
    });

    try {
      const res = await request;
      updateUser(res.data.data);
      reset();
    } catch {
      // the toast already shows the error; keep the preview so they can retry
    } finally {
      setBusy(false);
    }
  };

  const onRemove = async () => {
    setBusy(true);
    const request = removeAvatar();

    notify.promise(request, {
      loading: "Removing photo…",
      success: (res) => res.data?.message || "Photo removed",
      error: (err) => handleApiError(err),
    });

    try {
      const res = await request;
      updateUser(res.data.data);
    } catch {
      // handled by the toast
    } finally {
      setBusy(false);
      setConfirmRemove(false);
    }
  };

  const src = preview ?? user?.profileImgUrl;
  const initial = user?.username?.[0]?.toUpperCase() ?? "U";

  return (
    <div className="flex flex-col items-center text-center">
      {src ? (
        <img
          src={src}
          alt=""
          referrerPolicy="no-referrer"
          className="h-28 w-28 rounded-full object-cover ring-4 ring-bg"
        />
      ) : (
        <span
          className="flex h-28 w-28 items-center justify-center rounded-full bg-text font-display text-4xl font-semibold text-surface ring-4 ring-bg"
          aria-hidden="true"
        >
          {initial}
        </span>
      )}

      <input
        ref={inputRef}
        id="avatar-input"
        type="file"
        accept="image/*"
        onChange={onPick}
        disabled={busy}
        className="sr-only"
      />

      <div className="mt-4 flex flex-wrap justify-center gap-2">
        {file ? (
          <>
            <button
              type="button"
              onClick={onUpload}
              disabled={busy}
              className={primaryBtn}
            >
              {busy ? "Uploading…" : "Save photo"}
            </button>
            <button
              type="button"
              onClick={reset}
              disabled={busy}
              className={ghostBtn}
            >
              Cancel
            </button>
          </>
        ) : (
          <>
            <label
              htmlFor="avatar-input"
              className={`${ghostBtn} cursor-pointer`}
            >
              {user?.profileImgUrl ? "Change photo" : "Add photo"}
            </label>
            {user?.profileImgUrl && (
              <button
                type="button"
                onClick={() => setConfirmRemove(true)}
                disabled={busy}
                className={ghostBtn}
              >
                {busy ? "Removing…" : "Remove"}
              </button>
            )}
          </>
        )}
      </div>

      <p className={hintCls}>
        {file
          ? "Previewing your new photo. Save to apply it."
          : user?.profileImgUrl
            ? "JPG, PNG or WebP, up to 5 MB."
            : "No photo yet. Add one so people recognise you."}
      </p>
      {error && (
        <p className={errCls} role="alert">
          {error}
        </p>
      )}

      <ConfirmDialog
        open={confirmRemove}
        title="Remove your profile photo?"
        description="Your photo will be deleted and replaced with your initial. You can add a new one any time."
        confirmLabel="Remove photo"
        busy={busy}
        onConfirm={onRemove}
        onCancel={() => setConfirmRemove(false)}
      />
    </div>
  );
};

export default AvatarUploader;
