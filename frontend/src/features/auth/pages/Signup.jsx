import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { registerInit, verifyOtp } from "../authService";
import { handleApiError } from "../../../api/errors/handleApiError";

/* ---------- Icons (inline, shared visual language with Login) ---------- */

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

const CameraIcon = ({ className }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <path d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
    <path d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
  </svg>
);

const PersonIcon = ({ className }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
  </svg>
);

const MailIcon = ({ className }) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <path d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
  </svg>
);

/* ---------- Signup ---------- */

const Signup = () => {
  const navigate = useNavigate();
  const [isDark, setIsDark] = useState(false);
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
    profilePicture: null,
  });
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(null);

  // OTP modal state
  const [showOtpModal, setShowOtpModal] = useState(false);
  const [otp, setOtp] = useState("");
  const [otpError, setOtpError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);

  const validateForm = () => {
    const newErrors = {};

    if (!formData.username) {
      newErrors.username = "Username is required";
    } else if (formData.username.length < 3) {
      newErrors.username = "Username must be at least 3 characters";
    }

    if (!formData.email) {
      newErrors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = "Email is invalid";
    }

    if (!formData.password) {
      newErrors.password = "Password is required";
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = "Please confirm your password";
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = "Passwords do not match";
    }

    if (!formData.profilePicture) {
      newErrors.profilePicture = "Profile picture is required";
    } else if (formData.profilePicture.size > 5 * 1024 * 1024) {
      newErrors.profilePicture = "File size must be less than 5MB";
    }

    return newErrors;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        setErrors((prev) => ({
          ...prev,
          profilePicture: "File size must be less than 5MB",
        }));
        toast.error("File size must be less than 5MB");
        return;
      }

      setFormData((prev) => ({ ...prev, profilePicture: file }));
      const reader = new FileReader();
      reader.onloadend = () => setPreviewUrl(reader.result);
      reader.readAsDataURL(file);
      if (errors.profilePicture) {
        setErrors((prev) => ({ ...prev, profilePicture: "" }));
      }
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validateForm();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      toast.error("Please fix the errors in the form");
      return;
    }

    setIsSubmitting(true);
    try {
      await registerInit(
        {
          username: formData.username,
          email: formData.email,
          password: formData.password,
        },
        formData.profilePicture,
      );

      localStorage.setItem(
        "user",
        JSON.stringify({
          username: formData.username,
          email: formData.email,
          profilePicture: previewUrl,
        }),
      );

      toast.success("OTP sent to your email!");
      setShowOtpModal(true);
    } catch (err) {
      const errorMsg = handleApiError(err);
      setErrors({ api: errorMsg });
      toast.error(errorMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (!otp.trim()) {
      setOtpError("Please enter the OTP");
      toast.error("Please enter the OTP");
      return;
    }

    setIsVerifying(true);
    setOtpError("");
    try {
      await verifyOtp(formData.email, otp.trim());

      setShowOtpModal(false);
      toast.success("Account verified successfully! Please login.");
      navigate("/login");
    } catch (err) {
      const errorMsg = handleApiError(err);
      setOtpError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setIsVerifying(false);
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
      className="min-h-screen flex items-center justify-center p-6 py-12 relative overflow-hidden transition-colors duration-500"
      style={{
        backgroundColor: theme.bg,
        "--toastify-color-success": theme.primary,
        "--toastify-color-error": theme.danger,
        "--toastify-color-progress-success": theme.primary,
        "--toastify-color-progress-error": theme.danger,
      }}
    >
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,400;9..144,500;9..144,600&family=Manrope:wght@400;500;600;700&display=swap');
        .font-display { font-family: 'Fraunces', serif; font-optical-sizing: auto; }
        .font-body { font-family: 'Manrope', sans-serif; }
      `}</style>

      <ToastContainer
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="colored"
      />

      {/* ambient glow */}
      <div
        className="pointer-events-none absolute -top-32 -right-24 w-96 h-96 rounded-full blur-3xl opacity-30 transition-colors duration-500"
        style={{ backgroundColor: isDark ? "#ff2768" : "#710019" }}
      />
      <div
        className="pointer-events-none absolute -bottom-32 -left-24 w-96 h-96 rounded-full blur-3xl opacity-20 transition-colors duration-500"
        style={{ backgroundColor: isDark ? "#710019" : "#ff2768" }}
      />

      {/* Blinkstay logo, top-left */}
      <div className="absolute top-6 left-6 z-10 flex items-center gap-2.5">
        <div
          className="w-9 h-9 rounded-xl flex items-center justify-center font-display font-semibold text-lg transition-colors duration-500"
          style={{ backgroundColor: theme.primary, color: "#f8f9fc" }}
        >
          B
        </div>
        <span
          className="font-display text-lg tracking-tight transition-colors duration-500"
          style={{ color: theme.text }}
        >
          Blinkstay
        </span>
      </div>

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
            Create your account
          </h1>
          <p className="font-body text-sm" style={{ color: theme.subtext }}>
            Join Blinkstay and get started in a minute.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 font-body">
          {/* Profile Picture Upload */}
          <div className="flex justify-center">
            <div className="relative">
              <div
                className="w-24 h-24 rounded-full flex items-center justify-center overflow-hidden transition-colors duration-500"
                style={{
                  backgroundColor: theme.inputBg,
                  boxShadow: errors.profilePicture
                    ? `0 0 0 2px ${theme.danger}`
                    : `0 0 0 1px ${theme.border}`,
                }}
              >
                {previewUrl ? (
                  <img
                    src={previewUrl}
                    alt="Profile preview"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <PersonIcon
                    className="w-11 h-11"
                    style={{ color: theme.subtext }}
                  />
                )}
              </div>
              <label
                className="absolute bottom-0 right-0 rounded-full p-1.5 cursor-pointer transition-colors duration-200"
                style={{ backgroundColor: theme.primary }}
              >
                <CameraIcon className="w-4 h-4 text-white" />
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  className="hidden"
                />
              </label>
            </div>
          </div>
          {errors.profilePicture && (
            <p className="text-xs text-center" style={{ color: theme.danger }}>
              {errors.profilePicture}
            </p>
          )}

          {errors.api && (
            <div
              className="text-sm rounded-lg px-4 py-3"
              style={{
                backgroundColor: isDark
                  ? "rgba(255,39,104,0.1)"
                  : "rgba(113,0,25,0.06)",
                border: `1px solid ${theme.danger}`,
                color: isDark ? theme.danger : theme.primary,
              }}
            >
              {errors.api}
            </div>
          )}

          {/* Username */}
          <div>
            <label
              className="block text-sm font-medium mb-1.5"
              style={{ color: theme.text }}
            >
              Username
            </label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Choose a username"
              className="w-full px-4 py-2.5 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2"
              style={{
                backgroundColor: theme.inputBg,
                color: theme.text,
                border: `1px solid ${errors.username ? theme.danger : theme.border}`,
                "--tw-ring-color": `${theme.primary}55`,
              }}
            />
            {errors.username && (
              <p className="text-xs mt-1.5" style={{ color: theme.danger }}>
                {errors.username}
              </p>
            )}
          </div>

          {/* Email */}
          <div>
            <label
              className="block text-sm font-medium mb-1.5"
              style={{ color: theme.text }}
            >
              Email address
            </label>
            <input
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

          {/* Password */}
          <div>
            <label
              className="block text-sm font-medium mb-1.5"
              style={{ color: theme.text }}
            >
              Password
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Create a password"
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

          {/* Confirm Password */}
          <div>
            <label
              className="block text-sm font-medium mb-1.5"
              style={{ color: theme.text }}
            >
              Confirm password
            </label>
            <input
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Confirm your password"
              className="w-full px-4 py-2.5 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2"
              style={{
                backgroundColor: theme.inputBg,
                color: theme.text,
                border: `1px solid ${errors.confirmPassword ? theme.danger : theme.border}`,
                "--tw-ring-color": `${theme.primary}55`,
              }}
            />
            {errors.confirmPassword && (
              <p className="text-xs mt-1.5" style={{ color: theme.danger }}>
                {errors.confirmPassword}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 rounded-full text-sm font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed mt-2"
            style={{ backgroundColor: theme.primary, color: "#f8f9fc" }}
            onMouseEnter={(e) => {
              if (!isSubmitting)
                e.currentTarget.style.backgroundColor = theme.primaryHover;
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = theme.primary;
            }}
          >
            {isSubmitting ? "Sending OTP…" : "Sign up"}
          </button>
        </form>

        <p
          className="text-center text-sm mt-6 font-body"
          style={{ color: theme.subtext }}
        >
          Already have an account?{" "}
          <Link
            to="/login"
            className="font-semibold hover:underline"
            style={{ color: theme.primary }}
          >
            Log in
          </Link>
        </p>

        <div
          className="mt-6 pt-5 text-center text-xs font-body"
          style={{
            borderTop: `1px solid ${theme.border}`,
            color: theme.subtext,
          }}
        >
          By signing up, you agree to our Terms of Service and Privacy Policy
        </div>
      </div>

      {/* OTP Modal */}
      {showOtpModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div
            className="rounded-3xl shadow-2xl w-full max-w-sm p-8 font-body transition-colors duration-500"
            style={{
              backgroundColor: theme.card,
              border: `1px solid ${theme.border}`,
            }}
          >
            <div className="text-center mb-6">
              <div
                className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
                style={{
                  backgroundColor: isDark
                    ? "rgba(255,39,104,0.12)"
                    : "rgba(113,0,25,0.08)",
                }}
              >
                <MailIcon
                  className="w-8 h-8"
                  style={{ color: theme.primary }}
                />
              </div>
              <h3
                className="font-display text-2xl mb-1"
                style={{ color: theme.text }}
              >
                Verify your email
              </h3>
              <p className="text-sm" style={{ color: theme.subtext }}>
                We sent a code to{" "}
                <span className="font-semibold" style={{ color: theme.text }}>
                  {formData.email}
                </span>{" "}
                valid for 3 minutes
              </p>
            </div>

            <div className="space-y-4">
              <div>
                <label
                  className="block text-sm font-medium mb-1.5"
                  style={{ color: theme.text }}
                >
                  Enter OTP
                </label>
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => {
                    setOtp(e.target.value);
                    if (otpError) setOtpError("");
                  }}
                  maxLength={6}
                  placeholder="······"
                  className="w-full px-4 py-3 rounded-lg text-center text-xl tracking-[0.5em] font-mono outline-none transition-all duration-200 focus:ring-2"
                  style={{
                    backgroundColor: theme.inputBg,
                    color: theme.text,
                    border: `1px solid ${otpError ? theme.danger : theme.border}`,
                    "--tw-ring-color": `${theme.primary}55`,
                  }}
                  onKeyDown={(e) => e.key === "Enter" && handleVerifyOtp()}
                />
                {otpError && (
                  <p
                    className="text-xs mt-1.5 text-center"
                    style={{ color: theme.danger }}
                  >
                    {otpError}
                  </p>
                )}
              </div>

              <button
                onClick={handleVerifyOtp}
                disabled={isVerifying}
                className="w-full py-3 rounded-full text-sm font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                style={{ backgroundColor: theme.primary, color: "#f8f9fc" }}
                onMouseEnter={(e) => {
                  if (!isVerifying)
                    e.currentTarget.style.backgroundColor = theme.primaryHover;
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = theme.primary;
                }}
              >
                {isVerifying ? "Verifying…" : "Verify & create account"}
              </button>

              <button
                onClick={() => setShowOtpModal(false)}
                className="w-full text-sm py-1 hover:underline transition-colors"
                style={{ color: theme.subtext }}
              >
                Go back
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Signup;
