import { useEffect, useState } from "react";
import { useAuth } from "../../../context/AuthContext";
import { notify } from "../../../utils/notify";
import { updateProfile } from "../userService";
import {
  validateBio,
  validatePhone,
  validateUsername,
} from "../userValidation";
import {
  errCls,
  ghostBtn,
  hintCls,
  inputCls,
  labelCls,
  primaryBtn,
} from "../formStyles";
import { handleApiError } from "../../../api/errors/handleApiError";

const EditProfileForm = ({ onDirtyChange }) => {
  const { user, updateUser } = useAuth();
  const initial = {
    username: user.username ?? "",
    phone: user.phone ?? "",
    bio: user.bio ?? "",
  };
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const changes = {};
  if (form.username.trim() !== initial.username)
    changes.username = form.username.trim();
  if (form.phone.trim() !== initial.phone) changes.phone = form.phone.trim();
  if (form.bio !== initial.bio) changes.bio = form.bio;
  const dirty = Object.keys(changes).length > 0;

  useEffect(() => {
    onDirtyChange?.(dirty);
  }, [dirty, onDirtyChange]);

  const onSubmit = async (e) => {
    e.preventDefault();
    const next = {
      username: validateUsername(form.username),
      phone: validatePhone(form.phone.trim()),
      bio: validateBio(form.bio),
    };
    setErrors(next);
    if (Object.values(next).some(Boolean) || !dirty) return;

    setSaving(true);
    try {
      const res = await updateProfile(changes); // only changed fields
      const saved = res.data.data;
      updateUser(saved);
      // sync the form to what the server stored so "dirty" clears
      setForm({
        username: saved.username ?? "",
        phone: saved.phone ?? "",
        bio: saved.bio ?? "",
      });
      notify.success(res.data.message || "Profile updated");
    } catch (err) {
      notify.error(handleApiError(err));
    } finally {
      setSaving(false);
    }
  };

  const onDiscard = () => {
    setForm(initial);
    setErrors({});
  };

  const phoneEmpty = !initial.phone && !form.phone;
  const bioEmpty = !initial.bio && !form.bio;

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-5">
      <div>
        <label htmlFor="email" className={labelCls}>
          Email
        </label>
        <input
          id="email"
          value={user.email}
          readOnly
          aria-readonly="true"
          className={inputCls}
        />
        <p className={hintCls}>Your email can't be changed.</p>
      </div>

      <div className="grid gap-5 sm:grid-cols-2">
        <div>
          <label htmlFor="username" className={labelCls}>
            Username
          </label>
          <input
            id="username"
            value={form.username}
            onChange={set("username")}
            autoComplete="username"
            className={inputCls}
            aria-invalid={Boolean(errors.username)}
          />
          {errors.username ? (
            <p className={errCls}>{errors.username}</p>
          ) : (
            <p className={hintCls}>2 to 50 characters.</p>
          )}
        </div>

        <div>
          <label htmlFor="phone" className={labelCls}>
            Phone <span className="font-normal text-subtext">(optional)</span>
          </label>
          <input
            id="phone"
            value={form.phone}
            onChange={set("phone")}
            inputMode="tel"
            autoComplete="tel"
            placeholder="+919876543210"
            className={inputCls}
            aria-invalid={Boolean(errors.phone)}
          />
          {errors.phone ? (
            <p className={errCls}>{errors.phone}</p>
          ) : (
            <p className={hintCls}>
              {phoneEmpty
                ? "No number added yet. It helps hosts reach you about a booking."
                : "7 to 15 digits, optional +."}
            </p>
          )}
        </div>
      </div>

      <div>
        <label htmlFor="bio" className={labelCls}>
          Bio <span className="font-normal text-subtext">(optional)</span>
        </label>
        <textarea
          id="bio"
          rows={4}
          value={form.bio}
          onChange={set("bio")}
          placeholder="Tell hosts and guests a little about yourself"
          className={inputCls}
          aria-invalid={Boolean(errors.bio)}
        />
        <div className="flex justify-between gap-4">
          {errors.bio ? (
            <p className={errCls}>{errors.bio}</p>
          ) : (
            <p className={hintCls}>
              {bioEmpty ? "No bio yet. A short intro goes a long way." : " "}
            </p>
          )}
          <span className="mt-1.5 shrink-0 text-xs text-subtext">
            {form.bio.length}/300
          </span>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3 border-t border-border pt-4">
        <button
          type="submit"
          disabled={!dirty || saving}
          className={primaryBtn}
        >
          {saving ? "Saving…" : "Save changes"}
        </button>
        {dirty && (
          <>
            <button
              type="button"
              onClick={onDiscard}
              disabled={saving}
              className={ghostBtn}
            >
              Discard
            </button>
            <span className="text-xs text-subtext">
              You have unsaved changes
            </span>
          </>
        )}
      </div>
    </form>
  );
};

export default EditProfileForm;
