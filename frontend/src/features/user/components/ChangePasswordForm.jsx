import { useEffect, useState } from "react";
import PasswordInput from "../../../components/common/PasswordInput";
import { notify } from "../../../utils/notify";
import { changePassword } from "../userService";
import { validateNewPassword } from "../userValidation";
import { errCls, hintCls, inputCls, labelCls, primaryBtn } from "../formStyles";
import { handleApiError } from "../../../api/errors/handleApiError";

const empty = { current: "", next: "", confirm: "" };

const ChangePasswordForm = ({ onDirtyChange }) => {
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const dirty = Object.values(form).some(Boolean);
  useEffect(() => {
    onDirtyChange?.(dirty);
  }, [dirty, onDirtyChange]);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    const next = {
      current: form.current ? "" : "Enter your current password",
      next: validateNewPassword(form.next),
      confirm: form.confirm === form.next ? "" : "Passwords do not match",
    };
    setErrors(next);
    if (Object.values(next).some(Boolean)) return;

    setSaving(true);
    try {
      const res = await changePassword(form.current, form.next);
      notify.success(res.data.message || "Password changed");
      setForm(empty);
      setErrors({});
    } catch (err) {
      const message = handleApiError(err);
      if (/current/i.test(message)) setErrors({ current: message });
      else notify.error(message);
    } finally {
      setSaving(false);
    }
  };

  const field = (id, label, key, autoComplete, hint) => (
    <div>
      <label htmlFor={id} className={labelCls}>
        {label}
      </label>
      <PasswordInput
        id={id}
        value={form[key]}
        onChange={set(key)}
        autoComplete={autoComplete}
        className={inputCls}
        invalid={Boolean(errors[key])}
      />
      {errors[key] ? (
        <p className={errCls}>{errors[key]}</p>
      ) : (
        hint && <p className={hintCls}>{hint}</p>
      )}
    </div>
  );

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-5">
      {field(
        "current-password",
        "Current password",
        "current",
        "current-password",
      )}
      <div className="grid gap-5 sm:grid-cols-2">
        {field(
          "new-password",
          "New password",
          "next",
          "new-password",
          "8 to 72 characters.",
        )}
        {field(
          "confirm-password",
          "Confirm new password",
          "confirm",
          "new-password",
        )}
      </div>
      <div className="border-t border-border pt-4">
        <button type="submit" disabled={saving} className={primaryBtn}>
          {saving ? "Updating…" : "Update password"}
        </button>
      </div>
    </form>
  );
};

export default ChangePasswordForm;
