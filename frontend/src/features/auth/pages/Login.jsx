import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { FiEye, FiEyeOff, FiMoon, FiSun } from "react-icons/fi";
import { handleApiError } from "../../../api/errors/handleApiError";
import Logo from "../../../components/common/Logo";
import { useTheme } from "../../../context/ThemeContext";
import { useAuth } from "../../../context/AuthContext";

/* ---------- Login ---------- */

const Login = () => {
  const navigate = useNavigate();
  const { loginUser } = useAuth(); // to handle login
  const { isDark, toggleTheme } = useTheme();
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
        const result = await loginUser(formData.email, formData.password);

        if (result.success) {
          navigate("/");
        } else {
          setErrors({ api: handleApiError(result.error, "login") });
        }
      } finally {
        setIsLoading(false);
      }
    } else {
      setErrors(validationErrors);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6 relative overflow-hidden transition-colors duration-500 bg-bg">
      {/* ambient glow */}
      <div className="pointer-events-none absolute -top-32 -right-24 w-96 h-96 rounded-full blur-3xl opacity-30 transition-colors duration-500 bg-primary" />
      <div className="pointer-events-none absolute -bottom-32 -left-24 w-96 h-96 rounded-full blur-3xl opacity-20 transition-colors duration-500 bg-danger" />

      {/* Blinkstay logo, top-left */}
      <Link to={"/"}>
        <Logo isDark={isDark} positioning={"absolute top-6 left-6 z-10"} />
      </Link>

      {/* dark mode toggle */}
      <button
        type="button"
        onClick={toggleTheme}
        aria-label="Toggle dark mode"
        className="absolute top-6 right-6 flex items-center w-14 h-8 rounded-full p-1 transition-colors duration-300 z-10 bg-border"
      >
        <span
          className="flex items-center justify-center w-6 h-6 rounded-full shadow-md transition-transform duration-300 bg-primary"
          style={{ transform: isDark ? "translateX(24px)" : "translateX(0)" }}
        >
          {isDark ? (
            <FiMoon className="w-3.5 h-3.5 text-white" />
          ) : (
            <FiSun className="w-3.5 h-3.5 text-white" />
          )}
        </span>
      </button>

      <div className="relative z-10 rounded-3xl shadow-2xl max-w-md w-full p-8 sm:p-10 transition-colors duration-500 bg-surface border border-border">
        <div className="mb-8">
          <h1 className="font-display text-3xl sm:text-4xl mb-2 text-text">
            Welcome back
          </h1>
          <p className="font-body text-sm text-subtext">
            Log in to pick up where you left off.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 font-body">
          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium mb-1.5 text-text"
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
              className={`w-full px-4 py-2.5 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2 focus:ring-primary/30 bg-input text-text border ${
                errors.email ? "border-danger" : "border-border"
              }`}
            />
            {errors.email && (
              <p className="text-xs mt-1.5 text-danger">{errors.email}</p>
            )}
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label
                htmlFor="password"
                className="text-sm font-medium text-text"
              >
                Password
              </label>
              <a
                href="#"
                className="text-xs font-medium hover:underline text-primary"
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
                className={`w-full px-4 py-2.5 pr-11 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2 focus:ring-primary/30 bg-input text-text border ${
                  errors.password ? "border-danger" : "border-border"
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Hide password" : "Show password"}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-subtext"
              >
                {showPassword ? (
                  <FiEyeOff className="w-4.5 h-4.5" />
                ) : (
                  <FiEye className="w-4.5 h-4.5" />
                )}
              </button>
            </div>
            {errors.password && (
              <p className="text-xs mt-1.5 text-danger">{errors.password}</p>
            )}
          </div>

          {errors.api && (
            <p className="text-sm text-center text-danger">{errors.api}</p>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full cursor-pointer py-3 rounded-full text-sm font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed !bg-primary hover:!bg-primary-hover text-white"
          >
            {isLoading ? "Logging in…" : "Log in"}
          </button>
        </form>

        <p className="text-center text-sm mt-6 font-body text-subtext">
          Don't have an account?{" "}
          <Link
            to="/signup"
            className="font-semibold hover:underline text-primary"
          >
            Sign up
          </Link>
        </p>

        <div className="mt-8 pt-5 text-center text-xs font-body border-t border-border text-subtext">
          Test credentials: boruto123@example.com / password-123456789
        </div>
      </div>
    </div>
  );
};

export default Login;
