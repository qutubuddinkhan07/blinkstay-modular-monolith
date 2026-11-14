// Pages a user must never be sent back to after logging in
const AUTH_PAGES = ["/login", "/signup", "/forgot-password", "/blocked"];

// Where to go after login. Accepts a location object ({ pathname, search }) or a string.
// Only same-site paths are allowed, and never the pages above: anything else goes to Explore.
export const safeReturnPath = (from, fallback = "/explore") => {
  const raw =
    typeof from === "string"
      ? from
      : `${from?.pathname ?? ""}${from?.search ?? ""}`;

  // same-site paths only: blocks "https://elsewhere" and "//elsewhere"
  if (!raw.startsWith("/") || raw.startsWith("//")) return fallback;

  const pathOnly = raw.split(/[?#]/)[0];
  const isAuthPage = AUTH_PAGES.some(
    (p) => pathOnly === p || pathOnly.startsWith(`${p}/`),
  );

  return isAuthPage ? fallback : raw;
};
