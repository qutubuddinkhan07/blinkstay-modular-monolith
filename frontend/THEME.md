# ThemeContext — how it works

`src/context/ThemeContext.jsx` gives every page in the app access to one
shared `isDark` value and a `toggleTheme()` function, without prop-drilling
or duplicating `useState` in each page.

## The pieces

### 1. The context itself

```jsx
const ThemeContext = createContext(undefined);
```

`createContext` makes a "channel" that any component below it in the tree
can tune into with `useContext`. It starts as `undefined` on purpose — that
default is only ever seen if someone calls `useTheme()` outside of a
`ThemeProvider`, which is how we catch that mistake early (see point 5).

### 2. Picking the starting theme — `getInitialTheme()`

```jsx
function getInitialTheme() {
  if (typeof window === "undefined") return "light";

  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored === "light" || stored === "dark") {
    return stored;
  }

  const prefersDark = window.matchMedia?.(
    "(prefers-color-scheme: dark)",
  ).matches;
  return prefersDark ? "dark" : "light";
}
```

This runs once, the moment the app first loads, and decides which theme to
start in:

1. **Saved choice wins.** If the user has toggled the theme before,
   `localStorage.getItem("theme")` returns `"light"` or `"dark"`, and that's
   used — this is what makes the setting "stick" across page reloads and
   between pages.
2. **No saved choice → ask the OS.** `window.matchMedia("(prefers-color-scheme: dark)")`
   is a browser API that reports whether the user's operating system (or
   browser) is set to dark mode. If they've never touched the toggle in your
   app, you start them in whatever mode their system already uses — most
   people expect this.
3. **Fallback.** If neither is available (e.g. rendered outside a browser),
   default to `"light"`.

The `typeof window === "undefined"` check is a safety net for
server-side rendering, where there is no `window` object yet.

### 3. The provider — `ThemeProvider`

```jsx
export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(getInitialTheme);
  ...
  return (
    <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
  );
};
```

This is the component you wrap your app in (once, in `App.jsx`). It:

- Holds the actual `theme` state (`"light"` or `"dark"`).
- Passes `getInitialTheme` (not `getInitialTheme()`) to `useState` — passing
  the function itself means React only calls it once, on the first render,
  instead of recalculating it on every re-render.
- Wraps `children` in `ThemeContext.Provider`, which is what makes `theme`
  available to every component nested inside it — in your case, that's the
  whole app, including anything loaded lazily through your router.

#### Following the system live

```jsx
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
```

If the user has **never** manually toggled the theme in your app (no
`localStorage` entry yet), this keeps listening for OS-level changes — so if
they switch their laptop to dark mode at night, your app follows along live,
without needing a refresh. The moment they use your toggle, a value gets
saved to `localStorage`, and this listener backs off and leaves their choice
alone.

#### Persisting every change

```jsx
useEffect(() => {
  window.localStorage.setItem(STORAGE_KEY, theme);
  document.documentElement.setAttribute("data-theme", theme);
}, [theme]);
```

Every time `theme` changes (including the very first render), this:

- Saves it to `localStorage` under the key `"theme"`, which is what
  `getInitialTheme()` reads back on the next visit.
- Sets `data-theme="light"` / `data-theme="dark"` on the `<html>` element.
  Your pages don't currently rely on this (they use inline `style` objects
  driven by the `isDark` boolean), but it's there for free if you ever want
  to theme something with plain CSS, like
  `[data-theme="dark"] ::-webkit-scrollbar { ... }` or native form control
  colors, without touching React.

#### The toggle function

```jsx
const toggleTheme = () =>
  setTheme((prev) => (prev === "dark" ? "light" : "dark"));
```

Flips between the two values. Using the updater form (`prev => ...`) rather
than reading `theme` directly avoids stale-state bugs if `toggleTheme` is
ever called rapidly.

#### What gets shared

```jsx
const value = {
  theme, // "light" | "dark"
  isDark: theme === "dark", // convenience boolean, matches your existing code
  setTheme, // escape hatch to set an exact value, not just toggle
  toggleTheme,
};
```

`isDark` exists purely so your existing `Login.jsx` / `Signup.jsx` code
(`const theme = { bg: isDark ? ... : ... }`) didn't need to change at all —
just swap where `isDark` comes from.

### 4. The hook — `useTheme()`

```jsx
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }
  return context;
};
```

This is the only thing your pages ever import. `useContext(ThemeContext)`
reads whatever `value` the nearest `ThemeProvider` above it is passing down.
The `undefined` check exists so that if someone forgets to wrap the app in
`<ThemeProvider>`, they get a clear error immediately instead of a silent
`isDark is undefined` bug three components deep.

## Using it in a page

```jsx
import { useTheme } from "../../../context/ThemeContext";

const SomePage = () => {
  const { isDark, toggleTheme } = useTheme();

  const theme = {
    bg: isDark ? "#002642" : "#f8f9fc",
    // ...
  };

  return <button onClick={toggleTheme}>Toggle theme</button>;
};
```

No local `useState`, no prop drilling — every page reads and writes the
same shared value.

## Where it plugs into your app

```jsx
// App.jsx
import { ThemeProvider } from "./context/ThemeContext";

const App = () => (
  <ThemeProvider>
    <Suspense fallback={<Loading />}>
      <RouterProvider router={route} />
    </Suspense>
    <ToastContainer />
  </ThemeProvider>
);
```

`ThemeProvider` sits above the router, so every route — including ones
loaded lazily with `React.lazy` — is inside it and can call `useTheme()`.
