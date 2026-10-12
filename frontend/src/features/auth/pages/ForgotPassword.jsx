import { useState, useRef } from "react";
import { Link, useNavigate, useBlocker } from "react-router-dom";
import { FiMoon, FiSun } from "react-icons/fi";
import { forgotPassword, resetPassword } from "../authService";
import { handleApiError } from "../../../api/errors/handleApiError";
import { notify } from "../../../utils/notify";
import { useCooldown } from "../../../utils/useCooldown";
import { useTheme } from "../../../context/ThemeContext";
import Logo from "../../../components/common/Logo";
import PasswordInput from "../../../components/common/PasswordInput";
import ConfirmDialog from "../../../components/common/ConfirmDialogBox";

const inputCls = (invalid) =>
  `w-full rounded-lg border bg-input px-4 py-2.5 text-sm text-text outline-none transition-all duration-200 focus:ring-2 focus:ring-primary/30 ${
    invalid ? "border-danger" : "border-border"
  }`;
const labelCls = "mb-1.5 block text-sm font-medium text-text";
const errCls = "mt-1.5 text-xs text-danger";
const btnCls =
  "w-full cursor-pointer rounded-full bg-primary py-3 text-sm font-semibold text-white transition-colors duration-200 hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50";

// Backend field names -> the names used in this form
const FIELD_ALIASES = { otp: "code", newPassword: "password" };

// Reads a { field: message } map from a 400 response, whichever key the backend puts it under
const fieldErrorsFrom = (err) => {
  const d = err.response?.data;
  const map =
    d?.errors ??
    d?.fieldErrors ??
    (d?.data && typeof d.data === "object" ? d.data : null);
  if (!map || Array.isArray(map)) return null;

  const out = {};
  Object.entries(map).forEach(([key, value]) => {
    out[FIELD_ALIASES[key] ?? key] = Array.isArray(value)
      ? value[0]
      : String(value);
  });
  return Object.keys(out).length ? out : null;
};

const Shell = ({ children }) => {
  const { isDark, toggleTheme } = useTheme();

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-bg p-6 py-12 transition-colors duration-500">
      <div className="pointer-events-none absolute -right-24 -top-32 h-96 w-96 rounded-full bg-primary opacity-30 blur-3xl transition-colors duration-500" />
      <div className="pointer-events-none absolute -bottom-32 -left-24 h-96 w-96 rounded-full bg-danger opacity-20 blur-3xl transition-colors duration-500" />

      <Link to="/">
        <Logo isDark={isDark} positioning="absolute top-6 left-6 z-10" />
      </Link>

      <button
        type="button"
        onClick={toggleTheme}
        aria-label="Toggle dark mode"
        className="absolute right-6 top-6 z-10 flex h-8 w-14 items-center rounded-full bg-border p-1 transition-colors duration-300"
      >
        <span
          className="flex h-6 w-6 items-center justify-center rounded-full bg-primary shadow-md transition-transform duration-300"
          style={{ transform: isDark ? "translateX(24px)" : "translateX(0)" }}
        >
          {isDark ? (
            <FiMoon className="h-3.5 w-3.5 text-white" />
          ) : (
            <FiSun className="h-3.5 w-3.5 text-white" />
          )}
        </span>
      </button>

      <div className="relative z-10 w-full max-w-md rounded-3xl border border-border bg-surface p-8 shadow-2xl transition-colors duration-500 sm:p-10">
        {children}
      </div>
    </div>
  );
};

const ForgotPassword = () => {
  const navigate = useNavigate();
  const cooldown = useCooldown();

  const [step, setStep] = useState(1); // 1: enter details, 2: enter code + new password
  const [details, setDetails] = useState({ username: "", email: "" });
  const [reset, setReset] = useState({ code: "", password: "", confirm: "" });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [resending, setResending] = useState(false);

  const leavingRef = useRef(false); // set true when navigating away on purpose (success)

  // Block in-app navigation (links, browser back/forward) when on step 2
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      step === 2 &&
      !leavingRef.current &&
      currentLocation.pathname !== nextLocation.pathname,
  );

  const trimmed = {
    username: details.username.trim(),
    email: details.email.trim(),
  };

  const clearError = (name) =>
    setErrors((e) => (e[name] || e.api ? { ...e, [name]: "", api: "" } : e));

  /* ---------- Step 1: request a code ---------- */
  const requestCode = async (e) => {
    e.preventDefault();
    if (busy) return;

    const next = {};
    if (!trimmed.username) next.username = "Username is required";
    if (!trimmed.email) next.email = "Email is required";
    else if (!/\S+@\S+\.\S+/.test(trimmed.email))
      next.email = "Email is invalid";
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setBusy(true);
    try {
      await forgotPassword(trimmed);
      notify.info("If the details match an account, we sent a 6-digit code.");
      cooldown.start(60);
      setReset({ code: "", password: "", confirm: "" });
      setStep(2);
    } catch (err) {
      setErrors(fieldErrorsFrom(err) ?? { api: handleApiError(err) });
    } finally {
      setBusy(false);
    }
  };

  /* ---------- Resend ---------- */
  const resend = async () => {
    if (cooldown.active || resending) return;

    setResending(true);
    try {
      await forgotPassword(trimmed);
      notify.info("If the details match an account, we sent a new code.");
      cooldown.start(60);
    } catch (err) {
      notify.error(handleApiError(err));
    } finally {
      setResending(false);
    }
  };

  /* ---------- Step 2: set the new password ---------- */
  const submitReset = async (e) => {
    e.preventDefault();
    if (busy) return;

    const code = reset.code.trim();
    const next = {};
    if (!/^\d{6}$/.test(code)) next.code = "Enter the 6-digit code";
    if (reset.password.length < 8 || reset.password.length > 72) {
      next.password = "Password must be 8 to 72 characters";
    }
    if (reset.confirm !== reset.password)
      next.confirm = "Passwords do not match";
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setBusy(true);
    try {
      await resetPassword({
        email: trimmed.email,
        otp: code,
        newPassword: reset.password,
      });
      notify.success("Password updated. Please log in with your new password.");
      leavingRef.current = true; // Mark as intended exit so blocker doesn't trigger
      navigate("/login", { replace: true });
    } catch (err) {
      const fields = fieldErrorsFrom(err);
      const message = handleApiError(err);

      if (fields) setErrors(fields);
      else if (/code|otp|expired/i.test(message)) setErrors({ code: message });
      else if (/password/i.test(message)) setErrors({ password: message });
      else setErrors({ api: message });
    } finally {
      setBusy(false);
    }
  };

  const backToDetails = () => {
    setStep(1);
    setErrors({});
    setReset({ code: "", password: "", confirm: "" });
  };

  /* ---------- Step 1 view ---------- */
  if (step === 1) {
    return (
      <Shell>
        <div className="mb-8">
          <h1 className="mb-2 font-display text-3xl text-text sm:text-4xl">
            Forgot your password?
          </h1>
          <p className="font-body text-sm text-subtext">
            Enter your username and email. If they match an account, we'll send
            you a 6-digit code.
          </p>
        </div>

        <form onSubmit={requestCode} noValidate className="space-y-5 font-body">
          {errors.api && (
            <div
              role="alert"
              className="rounded-lg border border-danger bg-danger/10 px-4 py-3 text-sm text-danger"
            >
              {errors.api}
            </div>
          )}

          <div>
            <label htmlFor="fp-username" className={labelCls}>
              Username
            </label>
            <input
              id="fp-username"
              value={details.username}
              onChange={(e) => {
                setDetails((d) => ({ ...d, username: e.target.value }));
                clearError("username");
              }}
              autoComplete="username"
              placeholder="Your username"
              className={inputCls(errors.username)}
              aria-invalid={Boolean(errors.username)}
            />
            {errors.username && <p className={errCls}>{errors.username}</p>}
          </div>

          <div>
            <label htmlFor="fp-email" className={labelCls}>
              Email address
            </label>
            <input
              id="fp-email"
              type="email"
              value={details.email}
              onChange={(e) => {
                setDetails((d) => ({ ...d, email: e.target.value }));
                clearError("email");
              }}
              autoComplete="email"
              placeholder="you@example.com"
              className={inputCls(errors.email)}
              aria-invalid={Boolean(errors.email)}
            />
            {errors.email && <p className={errCls}>{errors.email}</p>}
          </div>

          <button type="submit" disabled={busy} className={btnCls}>
            {busy ? "Sending…" : "Send code"}
          </button>
        </form>

        <p className="mt-6 text-center font-body text-sm text-subtext">
          Remembered it?{" "}
          <Link
            to="/login"
            className="font-semibold text-primary hover:underline"
          >
            Log in
          </Link>
        </p>
      </Shell>
    );
  }

  /* ---------- Step 2 view ---------- */
  return (
    <>
      <Shell>
        <div className="mb-8">
          <h1 className="mb-2 font-display text-3xl text-text sm:text-4xl">
            Set a new password
          </h1>
          <p className="font-body text-sm text-subtext">
            If the details match an account, we sent a 6-digit code to{" "}
            <span className="break-all font-semibold text-text">
              {trimmed.email}
            </span>
            .{" "}
            <button
              type="button"
              onClick={backToDetails}
              className="cursor-pointer font-semibold text-primary hover:underline"
            >
              Change details
            </button>
          </p>
        </div>

        <form onSubmit={submitReset} noValidate className="space-y-5 font-body">
          {errors.api && (
            <div
              role="alert"
              className="rounded-lg border border-danger bg-danger/10 px-4 py-3 text-sm text-danger"
            >
              {errors.api}
            </div>
          )}

          <div>
            <label htmlFor="fp-code" className={labelCls}>
              6-digit code
            </label>
            <input
              id="fp-code"
              value={reset.code}
              onChange={(e) => {
                setReset((r) => ({
                  ...r,
                  code: e.target.value.replace(/\D/g, "").slice(0, 6),
                }));
                clearError("code");
              }}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="······"
              className={`${inputCls(errors.code)} text-center font-mono text-xl tracking-[0.5em]`}
              aria-invalid={Boolean(errors.code)}
            />
            {errors.code && <p className={errCls}>{errors.code}</p>}

            <p className="mt-2 text-xs text-subtext">
              Didn't get it?{" "}
              <button
                type="button"
                onClick={resend}
                disabled={cooldown.active || resending}
                className="cursor-pointer font-semibold text-primary hover:underline disabled:cursor-not-allowed disabled:text-subtext disabled:no-underline"
              >
                {resending
                  ? "Sending…"
                  : cooldown.active
                    ? `Resend code in ${cooldown.secondsLeft}s`
                    : "Resend code"}
              </button>
            </p>
          </div>

          <div>
            <label htmlFor="fp-password" className={labelCls}>
              New password
            </label>
            <PasswordInput
              id="fp-password"
              value={reset.password}
              onChange={(e) => {
                setReset((r) => ({ ...r, password: e.target.value }));
                clearError("password");
              }}
              autoComplete="new-password"
              placeholder="8 to 72 characters"
              className={inputCls(errors.password)}
              invalid={Boolean(errors.password)}
            />
            {errors.password && <p className={errCls}>{errors.password}</p>}
          </div>

          <div>
            <label htmlFor="fp-confirm" className={labelCls}>
              Confirm new password
            </label>
            <PasswordInput
              id="fp-confirm"
              value={reset.confirm}
              onChange={(e) => {
                setReset((r) => ({ ...r, confirm: e.target.value }));
                clearError("confirm");
              }}
              autoComplete="new-password"
              placeholder="Repeat the new password"
              className={inputCls(errors.confirm)}
              invalid={Boolean(errors.confirm)}
            />
            {errors.confirm && <p className={errCls}>{errors.confirm}</p>}
          </div>

          <button type="submit" disabled={busy} className={btnCls}>
            {busy ? "Updating…" : "Update password"}
          </button>
        </form>

        <p className="mt-6 text-center font-body text-sm text-subtext">
          <Link
            to="/login"
            className="font-semibold text-primary hover:underline"
          >
            Back to log in
          </Link>
        </p>
      </Shell>

      <ConfirmDialog
        open={blocker.state === "blocked"}
        title="Leave password reset?"
        description="Your verification code session will be discarded, and you'll need to request a new code if you want to reset your password."
        confirmLabel="Leave page"
        cancelLabel="Stay here"
        busyLabel="Leaving…"
        onConfirm={() => blocker.proceed()}
        onCancel={() => blocker.reset()}
      />
    </>
  );
};

export default ForgotPassword;
