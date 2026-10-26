import React from "react";

// The same mark as public/favicon.svg, drawn as inline SVG so it follows the theme
const Logo = ({ isDark = false, positioning = "", showText = true }) => {
  const color = {
    text: isDark ? "#f8f9fc" : "#002642",
    tile: isDark ? "#ff2768" : "#710019",
    spark: isDark ? "#ffffff" : "#ff7a9c",
  };

  return (
    <div className={`${positioning} flex items-center gap-2.5`}>
      <svg
        viewBox="0 0 64 64"
        className="h-9 w-9 shrink-0"
        aria-hidden="true"
        focusable="false"
      >
        <rect
          width="64"
          height="64"
          rx="15"
          style={{ fill: color.tile, transition: "fill 500ms" }}
        />
        {/* the "B" */}
        <path
          d="M22 16V48M22 16H33a8 8 0 0 1 0 16H22M22 32H35a8 8 0 0 1 0 16H22"
          fill="none"
          stroke="#ffffff"
          strokeWidth="6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* the "blink" spark */}
        <path
          d="M51 8l1.7 4.3L57 14l-4.3 1.7L51 20l-1.7-4.3L45 14l4.3-1.7z"
          style={{ fill: color.spark, transition: "fill 500ms" }}
        />
      </svg>

      {showText ? (
        <span
          className="font-display text-lg tracking-tight transition-colors duration-500"
          style={{ color: color.text }}
        >
          BlinkStay
        </span>
      ) : (
        <span className="sr-only">BlinkStay</span>
      )}
    </div>
  );
};

export default Logo;
