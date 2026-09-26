import {
  Children,
  createContext,
  useContext,
  useEffect,
  useState,
} from "react";

const ThemeContext = createContext(undefined);

const STORAGE_KEY = "theme"; // stored value is "light" | "dark"

/**
 * Decide the theme to use on first render:
 * 1. If the user has explicitly chosen before (saved in localStorage), use that.
 * 2. Otherwise, fall back to their OS-level preference (prefers-color-scheme).
 * 3. Otherwise default to light.
 */
function getInitialTheme() {
  if (typeof window === "undefined") return "light";

  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored === "light" || stored === "dark") {
    return stored;
  }

  const preferDark = window.matchMedia?.(
    "(prefers-color-scheme: dark)",
  ).matches;

  return preferDark ? "dark" : "light";
}

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(getInitialTheme);

  // If the user never explicitly chose a theme on this device, keep following
  // their OS setting live (e.g. they switch their system to dark mode at night).
  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    const handleSystemChange = (e) => {
      const hasExplicitChoice = window.localStorage.getItem(STORAGE_KEY);

      if (!hasExplicitChoice) {
        setTheme(e.matches ? "dark" : "light");
      }
    };

    mediaQuery.addEventListener("change", handleSystemChange);
    return () => mediaQuery.removeEventListener("change", handleSystemChange);
  }, []);

  // Persist every change (this marks it as an "explicit choice" from now on).
  useEffect(() => {
    window.localStorage.setItem(STORAGE_KEY, theme);
    // Optional but handy for CSS/Tailwind dark: selectors or native form controls.
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);

  const toggleTheme = () =>
    setTheme((prev) => (prev === "dark" ? "light" : "dark"));

  const value = {
    theme,
    isDark: theme === "dark",
    setTheme,
    toggleTheme,
  };

  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }

  return context;
};
