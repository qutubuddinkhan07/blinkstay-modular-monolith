import React from "react";
import { useTheme } from "../../context/ThemeContext";
import { Toaster } from "sonner";

const ThemedToaster = () => {
  const { isDark } = useTheme();
  return (
    <Toaster
      theme={isDark ? "dakr" : "light"}
      position="top-center"
      offset={80}
      mobileOffset={80}
      closeButton
      toastOptions={{
        duration: 3500,
        style: {
          background: "var(--color-surface)",
          color: "var(--color-text)",
          border: "1px solid var(--color-border)",
        },
      }}
    />
  );
};

export default ThemedToaster;
