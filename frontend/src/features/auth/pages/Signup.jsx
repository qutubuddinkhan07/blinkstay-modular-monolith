import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import {
  FiEye,
  FiEyeOff,
  FiMoon,
  FiSun,
  FiCamera,
  FiUser,
  FiMail,
} from "react-icons/fi";
import { registerInit, verifyOtp } from "../authService";
import { handleApiError } from "../../../api/errors/handleApiError";
import { notify } from "../../../utils/notify";
import Logo from "../../../components/common/Logo";
import { useTheme } from "../../../context/ThemeContext";

/* ---------- Signup ---------- */

const Signup = () => {
  const navigate = useNavigate();
  const { isDark, toggleTheme } = useTheme();
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
        notify.error("File size must be less than 5MB");
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
      notify.error("Please fix the errors in the form");
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

      notify.success("OTP sent to your email!");
      setShowOtpModal(true);
    } catch (err) {
      const errorMsg = handleApiError(err);
      setErrors({ api: errorMsg });
      notify.error(errorMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (!otp.trim()) {
      setOtpError("Please enter the OTP");
      notify.error("Please enter the OTP");
      return;
    }

    setIsVerifying(true);
    setOtpError("");
    try {
      await verifyOtp(formData.email, otp.trim());

      setShowOtpModal(false);
      notify.success("Account verified successfully! Please login.");
      navigate("/login");
    } catch (err) {
      const errorMsg = handleApiError(err);
      setOtpError(errorMsg);
      notify.error(errorMsg);
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6 py-12 relative overflow-hidden transition-colors duration-500 bg-bg">
      {/* ambient glow */}
      <div className="pointer-events-none absolute -top-32 -right-24 w-96 h-96 rounded-full blur-3xl opacity-30 transition-colors duration-500 bg-primary" />
      <div className="pointer-events-none absolute -bottom-32 -left-24 w-96 h-96 rounded-full blur-3xl opacity-20 transition-colors duration-500 bg-danger" />

      {/* Blinkstay logo, top-left */}
      <Logo isDark={isDark} />

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
            Create your account
          </h1>
          <p className="font-body text-sm text-subtext">
            Join Blinkstay and get started in a minute.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 font-body">
          {/* Profile Picture Upload */}
          <div className="flex justify-center">
            <div className="relative">
              <div
                className={`w-24 h-24 rounded-full flex items-center justify-center overflow-hidden transition-colors duration-500 bg-input ${
                  errors.profilePicture
                    ? "ring-2 ring-danger"
                    : "ring-1 ring-border"
                }`}
              >
                {previewUrl ? (
                  <img
                    src={previewUrl}
                    alt="Profile preview"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <FiUser className="w-11 h-11 text-subtext" />
                )}
              </div>
              <label className="absolute bottom-0 right-0 rounded-full p-1.5 cursor-pointer transition-colors duration-200 bg-primary">
                <FiCamera className="w-4 h-4 text-white" />
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
            <p className="text-xs text-center text-danger">
              {errors.profilePicture}
            </p>
          )}

          {errors.api && (
            <div className="text-sm rounded-lg px-4 py-3 bg-danger/10 border border-danger text-danger">
              {errors.api}
            </div>
          )}

          {/* Username */}
          <div>
            <label className="block text-sm font-medium mb-1.5 text-text">
              Username
            </label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Choose a username"
              className={`w-full px-4 py-2.5 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2 focus:ring-primary/30 bg-input text-text border ${
                errors.username ? "border-danger" : "border-border"
              }`}
            />
            {errors.username && (
              <p className="text-xs mt-1.5 text-danger">{errors.username}</p>
            )}
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-medium mb-1.5 text-text">
              Email address
            </label>
            <input
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

          {/* Password */}
          <div>
            <label className="block text-sm font-medium mb-1.5 text-text">
              Password
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Create a password"
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

          {/* Confirm Password */}
          <div>
            <label className="block text-sm font-medium mb-1.5 text-text">
              Confirm password
            </label>
            <input
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Confirm your password"
              className={`w-full px-4 py-2.5 rounded-lg text-sm outline-none transition-all duration-200 focus:ring-2 focus:ring-primary/30 bg-input text-text border ${
                errors.confirmPassword ? "border-danger" : "border-border"
              }`}
            />
            {errors.confirmPassword && (
              <p className="text-xs mt-1.5 text-danger">
                {errors.confirmPassword}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 rounded-full text-sm font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed mt-2 !bg-primary hover:!bg-primary-hover text-white cursor-pointer"
          >
            {isSubmitting ? "Sending OTP…" : "Sign up"}
          </button>
        </form>

        <p className="text-center text-sm mt-6 font-body text-subtext">
          Already have an account?{" "}
          <Link
            to="/login"
            className="font-semibold hover:underline text-primary"
          >
            Log in
          </Link>
        </p>

        <div className="mt-6 pt-5 text-center text-xs font-body border-t border-border text-subtext">
          By signing up, you agree to our Terms of Service and Privacy Policy
        </div>
      </div>

      {/* OTP Modal */}
      {showOtpModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="rounded-3xl shadow-2xl w-full max-w-sm p-8 font-body transition-colors duration-500 bg-surface border border-border">
            <div className="text-center mb-6">
              <div className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 bg-primary/10">
                <FiMail className="w-8 h-8 text-primary" />
              </div>
              <h3 className="font-display text-2xl mb-1 text-text">
                Verify your email
              </h3>
              <p className="text-sm text-subtext">
                We sent a code to{" "}
                <span className="font-semibold text-text">
                  {formData.email}
                </span>{" "}
                valid for 3 minutes
              </p>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1.5 text-text">
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
                  className={`w-full px-4 py-3 rounded-lg text-center text-xl tracking-[0.5em] font-mono outline-none transition-all duration-200 focus:ring-2 focus:ring-primary/30 bg-input text-text border ${
                    otpError ? "border-danger" : "border-border"
                  }`}
                  onKeyDown={(e) => e.key === "Enter" && handleVerifyOtp()}
                />
                {otpError && (
                  <p className="text-xs mt-1.5 text-center text-danger">
                    {otpError}
                  </p>
                )}
              </div>

              <button
                onClick={handleVerifyOtp}
                disabled={isVerifying}
                className="w-full py-3 rounded-full text-sm font-semibold transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed bg-primary hover:bg-primary-hover text-white"
              >
                {isVerifying ? "Verifying…" : "Verify & create account"}
              </button>

              <button
                onClick={() => setShowOtpModal(false)}
                className="w-full text-sm py-1 hover:underline transition-colors text-subtext"
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
