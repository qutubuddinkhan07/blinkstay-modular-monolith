import { FiGithub, FiInstagram, FiLinkedin } from "react-icons/fi";

// Put your address in .env as VITE_SUPPORT_EMAIL=you@example.com
export const SUPPORT_EMAIL =
  import.meta.env.VITE_SUPPORT_EMAIL || "support@blinkstay.example";

export const supportMailto = (subject = "BlinkStay support") =>
  `mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent(subject)}`;

// Shared by the footer and the careers page
export const SOCIAL_LINKS = [
  {
    label: "Instagram",
    href: "https://www.instagram.com/qutubuddin_khan",
    icon: FiInstagram,
  },
  {
    label: "LinkedIn",
    href: "https://www.linkedin.com/in/qutubuddin-khan-9b9049225",
    icon: FiLinkedin,
  },
  {
    label: "GitHub",
    href: "https://github.com/qutubuddinkhan07",
    icon: FiGithub,
  },
];
