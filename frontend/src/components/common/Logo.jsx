import React from "react";

const Logo = ({ isDark = false }) => {
  const theme = {
    text: isDark ? "#f8f9fc" : "#002642",
    primary: isDark ? "#ff2768" : "#710019",
  };
  return (
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
  );
};

export default Logo;
