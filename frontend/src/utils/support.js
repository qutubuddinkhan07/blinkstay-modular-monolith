// Put your address in .env as VITE_SUPPORT_EMAIL=you@example.com
export const SUPPORT_EMAIL =
  import.meta.env.VITE_SUPPORT_EMAIL || "support@blinkstay.example";

export const supportMailto = (subject = "BlinkStay support") =>
  `mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent(subject)}`;
