import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { handleApiError } from "../../../api/errors/handleApiError";
import { login } from "../authService";
import { notify } from "../../../utils/notify";
import Logo from "../../../components/common/Logo";

/* ---------- Icons (inline, no extra dependency) ---------- */

const EyeIcon = ({ className }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
);

const EyeOffIcon = ({ className }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1 5.06-5.94" />
    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
    <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
    <line x1="1" y1="1" x2="23" y2="23" />
  </svg>
);

const MoonIcon = ({ className }) => (
  <svg viewBox="0 0 24 24" fill="currentColor" className={className}>
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
  </svg>
);

const SunIcon = ({ className }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    className={className}
  >
    <circle cx="12" cy="12" r="4" fill="currentColor" stroke="none" />
    <line x1="12" y1="1.5" x2="12" y2="4" />
    <line x1="12" y1="20" x2="12" y2="22.5" />
    <line x1="4.2" y1="4.2" x2="6" y2="6" />
    <line x1="18" y1="18" x2="19.8" y2="19.8" />
    <line x1="1.5" y1="12" x2="4" y2="12" />
    <line x1="20" y1="12" x2="22.5" y2="12" />
    <line x1="4.2" y1="19.8" x2="6" y2="18" />
    <line x1="18" y1="6" x2="19.8" y2="4.2" />
  </svg>
);

/* ---------- Login ---------- */

const Login = () => {
  const navigate = useNavigate();
  const [isDark, setIsDark] = useState(false);
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const validateForm = () => {
    const newErrors = {};
    if (!formData.email) {
      newErrors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = "Email is invalid";
    }
    if (!formData.password) {
      newErrors.password = "Password is required";
    }
    return newErrors;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: "",
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validateForm();
    if (Object.keys(validationErrors).length === 0) {
      setIsLoading(true);
      setErrors({});
      try {
        const res = await login(formData.email, formData.password);
        localStorage.setItem("token", res.data.data);
        notify.success("Login successful!");
        navigate("/");
      } catch (error) {
        setErrors({ api: handleApiError(error, "login") });
      } finally {
        setIsLoading(false);
      }
    } else {
      setErrors(validationErrors);
    }
  };

  const theme = {
    bg: isDark ? "#002642" : "#f8f9fc",
    card: isDark ? "#03314f" : "#ffffff",
    text: isDark ? "#f8f9fc" : "#002642",
    subtext: isDark ? "#a9c1d1" : "#5b6b78",
    primary: isDark ? "#ff2768" : "#710019",
    primaryHover: isDark ? "#e01f59" : "#8a0020",
    border: isDark ? "#0d4568" : "#e3e1e6",
    inputBg: isDark ? "#04263d" : "#f8f9fc",
    danger: "#ff2768",
  };

  return (
    <div
      className="min-h-screen flex items-center justify-center p-6 relative overflow-hidden transition-colors duration-500"
      style={{ backgroundColor: theme.bg }}
    >
      {/* ambient glow, swaps role with theme */}
      <div
        className="pointer-events-none absolute -top-32 -right-24 w-96 h-96 rounded-full blur-3xl opacity-30 transition-colors duration-500"
        style={{ backgroundColor: isDark ? "#ff2768" : "#710019" }}
      />
      <div
        className="pointer-events-none absolute -bottom-32 -left-24 w-96 h-96 rounded-full blur-3xl opacity-20 transition-colors duration-500"
        style={{ backgroundColor: isDark ? "#710019" : "#ff2768" }}
      />

      {/* Blinkstay logo, top-left */}
      <Logo isDark={isDark} />

      {/* dark mode toggle */}
      <button
        type="button"
        onClick={() => setIsDark((d) => !d)}
        aria-label="Toggle dark mode"
        className="absolute top-6 right-6 flex items-center w-14 h-8 rounded-full p-1 transition-colors duration-300 z-10"
        style={{ backgroundColor: isDark ? "#04263d" : "#e9e4e8" }}
      >
        <span
          className="flex items-center justify-center w-6 h-6 rounded-full shadow-md transition-transform duration-300"
          style={{
            backgroundColor: isDark ? "#ff2768" : "#710019",
            transform: isDark ? "translateX(24px)" : "translateX(0)",
          }}
        >
          {isDark ? (
            <MoonIcon className="w-3.5 h-3.5 text-white" />
          ) : (
            <SunIcon className="w-3.5 h-3.5 text-white" />
          )}
        </span>
      </button>

      <div
        className="relative z-10 rounded-3xl shadow-2xl max-w-md w-full p-8 sm:p-10 transition-colors duration-500"
        style={{
          backgroundColor: theme.card,
          border: `1px solid ${theme.border}`,
        }}
      >
        <div className="mb-8">
          <h1
            className="font-display text-3xl sm:text-4xl mb-2"
            style={{ color: theme.text }}
          >
            Welcome back
          </h1>
          <p className="font-body text-sm" style={{ color: theme.subtext }}>
            Log in to pick up where you left off.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 font-body">
          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium mb-1.5"
              style={{ color: theme.text }}
            >
              Email address
            </label>
            <input
              id="email"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="you@example.com"
              className="w-full px-4 py-2.5 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2"
              style={{
                backgroundColor: theme.inputBg,
                color: theme.text,
                border: `1px solid ${errors.email ? theme.danger : theme.border}`,
                "--tw-ring-color": `${theme.primary}55`,
              }}
            />
            {errors.email && (
              <p className="text-xs mt-1.5" style={{ color: theme.danger }}>
                {errors.email}
              </p>
            )}
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label
                htmlFor="password"
                className="text-sm font-medium"
                style={{ color: theme.text }}
              >
                Password
              </label>
              <a
                href="#"
                className="text-xs font-medium hover:underline"
                style={{ color: theme.primary }}
              >
                Forgot password?
              </a>
            </div>
            <div className="relative">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Enter your password"
                className="w-full px-4 py-2.5 pr-11 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2"
                style={{
                  backgroundColor: theme.inputBg,
                  color: theme.text,
                  border: `1px solid ${errors.password ? theme.danger : theme.border}`,
                  "--tw-ring-color": `${theme.primary}55`,
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Hide password" : "Show password"}
                className="absolute right-3 top-1/2 -translate-y-1/2"
                style={{ color: theme.subtext }}
              >
                {showPassword ? (
                  <EyeOffIcon className="w-4.5 h-4.5" />
                ) : (
                  <EyeIcon className="w-4.5 h-4.5" />
                )}
              </button>
            </div>
            {errors.password && (
              <p className="text-xs mt-1.5" style={{ color: theme.danger }}>
                {errors.password}
              </p>
            )}
          </div>

          {/* <div className="flex items-center justify-between">
            <label
              className="flex items-center gap-2 text-sm cursor-pointer"
              style={{ color: theme.subtext }}
            >
              <input
                type="checkbox"
                className="w-4 h-4 rounded"
                style={{ accentColor: theme.primary }}
              />
              Remember me
            </label>
          </div> */}

          {errors.api && (
            <p className="text-sm text-center" style={{ color: theme.danger }}>
              {errors.api}
            </p>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 rounded-full text-sm font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
            style={{ backgroundColor: theme.primary, color: "#f8f9fc" }}
            onMouseEnter={(e) => {
              if (!isLoading)
                e.currentTarget.style.backgroundColor = theme.primaryHover;
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = theme.primary;
            }}
          >
            {isLoading ? "Logging in…" : "Log in"}
          </button>
        </form>

        <p
          className="text-center text-sm mt-6 font-body"
          style={{ color: theme.subtext }}
        >
          Don't have an account?
          <Link
            to="/signup"
            className="font-semibold hover:underline"
            style={{ color: theme.primary }}
          >
            Sign up
          </Link>
        </p>

        <div
          className="mt-8 pt-5 text-center text-xs font-body"
          style={{
            borderTop: `1px solid ${theme.border}`,
            color: theme.subtext,
          }}
        >
          Test credentials: boruto123@example.com / password-123456789
        </div>
      </div>
    </div>
  );
};

export default Login;
