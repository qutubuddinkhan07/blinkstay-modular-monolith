import { useEffect, useState } from "react";
import {
  errCls,
  ghostBtn,
  hintCls,
  inputCls,
  labelCls,
  primaryBtn,
} from "../../user/formStyles";

const MAX_DESCRIPTION = 1000;

const validate = (v) => {
  const e = {};
  if (!v.businessName.trim()) e.businessName = "Business name is required";
  if (!v.address.trim()) e.address = "Address is required";

  const len = v.description.trim().length;
  if (len < 20 || len > MAX_DESCRIPTION) {
    e.description = `Description must be 20 to ${MAX_DESCRIPTION} characters`;
  }

  const site = v.website.trim();
  if (site && !/^https?:\/\/\S+$/i.test(site)) {
    e.website = "Website must start with http:// or https://";
  }
  return e;
};

const HostDetailsForm = ({
  initial,
  submitLabel,
  busyLabel = "Saving…",
  requireChange = false, // true: Save is disabled until something changed
  onSubmit, // async (values, changes) => void; the parent shows success/error toasts
  onCancel, // optional: shows a Cancel button
  onDirtyChange,
}) => {
  const base = {
    businessName: initial?.businessName ?? "",
    address: initial?.address ?? "",
    description: initial?.description ?? "",
    website: initial?.website ?? "",
  };

  const [form, setForm] = useState(base);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const values = {
    businessName: form.businessName.trim(),
    address: form.address.trim(),
    description: form.description.trim(),
    website: form.website.trim(),
  };

  // only the fields that differ from the saved ones (used for PATCH)
  const changes = {};
  Object.keys(values).forEach((k) => {
    if (values[k] !== base[k].trim()) changes[k] = values[k];
  });
  const dirty = Object.keys(changes).length > 0;

  useEffect(() => {
    onDirtyChange?.(dirty);
  }, [dirty, onDirtyChange]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const next = validate(form);
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    if (requireChange && !dirty) return;

    setSaving(true);
    try {
      await onSubmit(values, changes);
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5">
      <div>
        <label htmlFor="businessName" className={labelCls}>
          Business name
        </label>
        <input
          id="businessName"
          value={form.businessName}
          onChange={set("businessName")}
          autoComplete="organization"
          placeholder="Sea View Stays"
          className={inputCls}
          aria-invalid={Boolean(errors.businessName)}
        />
        {errors.businessName && <p className={errCls}>{errors.businessName}</p>}
      </div>

      <div>
        <label htmlFor="address" className={labelCls}>
          Business address
        </label>
        <input
          id="address"
          value={form.address}
          onChange={set("address")}
          autoComplete="street-address"
          placeholder="12 Beach Road"
          className={inputCls}
          aria-invalid={Boolean(errors.address)}
        />
        {errors.address && <p className={errCls}>{errors.address}</p>}
      </div>

      <div>
        <label htmlFor="description" className={labelCls}>
          About your business
        </label>
        <textarea
          id="description"
          rows={5}
          maxLength={MAX_DESCRIPTION}
          value={form.description}
          onChange={set("description")}
          placeholder="What kind of properties do you run, and where?"
          className={inputCls}
          aria-invalid={Boolean(errors.description)}
        />
        <div className="flex justify-between gap-4">
          {errors.description ? (
            <p className={errCls}>{errors.description}</p>
          ) : (
            <p className={hintCls}>At least 20 characters.</p>
          )}
          <span className="mt-1.5 shrink-0 text-xs text-subtext">
            {form.description.length}/{MAX_DESCRIPTION}
          </span>
        </div>
      </div>

      <div>
        <label htmlFor="website" className={labelCls}>
          Website <span className="font-normal text-subtext">(optional)</span>
        </label>
        <input
          id="website"
          type="url"
          value={form.website}
          onChange={set("website")}
          inputMode="url"
          autoComplete="url"
          placeholder="https://example.com"
          className={inputCls}
          aria-invalid={Boolean(errors.website)}
        />
        {errors.website && <p className={errCls}>{errors.website}</p>}
      </div>

      <div className="flex flex-wrap items-center gap-3 border-t border-border pt-4">
        <button
          type="submit"
          disabled={saving || (requireChange && !dirty)}
          className={primaryBtn}
        >
          {saving ? busyLabel : submitLabel}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={saving}
            className={ghostBtn}
          >
            Cancel
          </button>
        )}
        {requireChange && dirty && (
          <span className="text-xs text-subtext">You have unsaved changes</span>
        )}
      </div>
    </form>
  );
};

export default HostDetailsForm;
