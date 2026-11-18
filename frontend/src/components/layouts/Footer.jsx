import React from "react";
import { Link } from "react-router-dom";
import { FiHome, FiGlobe } from "react-icons/fi";
import { SOCIAL_LINKS } from "../../utils/support";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const FOOTER_SECTIONS = [
  {
    title: "Explore",
    links: [
      { to: "/explore", label: "Stays" },
      { to: "/explore?category=TRENDING", label: "Trending destinations" },
      { to: "/explore?category=BEACH", label: "Beachfront" },
      { to: "/explore?category=MOUNTAINS", label: "Mountain getaways" },
    ],
  },
  {
    title: "Support & company",
    links: [
      { to: "/help", label: "Help Center" },
      { to: "/about", label: "About us" },
      { to: "/careers", label: "Careers" },
      { to: "/contact", label: "Contact us" },
    ],
  },
];

const LEGAL_LINKS = [
  { to: "/legal#privacy", label: "Privacy Policy" },
  { to: "/legal#terms", label: "Terms of Service" },
  { to: "/legal#cookies", label: "Cookie Preferences" },
];

const linkCls = `rounded transition-colors hover:text-primary ${focusRing}`;

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-surface text-text transition-colors duration-300">
      <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-10 lg:py-16">
        <div className="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-5">
          {/* Brand */}
          <div className="lg:col-span-2">
            <Link
              to="/"
              className={`inline-flex items-center gap-2 rounded font-display text-xl font-bold tracking-tight text-primary ${focusRing}`}
            >
              <FiHome className="h-6 w-6 text-primary" aria-hidden="true" />
              <span>BlinkStay</span>
            </Link>
            <p className="mt-3 max-w-sm text-sm leading-relaxed text-subtext">
              Find a place that feels like home. Discover unique stays, local
              experiences, and unforgettable destinations around the world.
            </p>
          </div>

          {/* Link columns */}
          {FOOTER_SECTIONS.map((section) => (
            <nav key={section.title} aria-label={section.title}>
              <h3 className="font-display text-sm font-semibold uppercase tracking-wider text-text">
                {section.title}
              </h3>
              <ul className="mt-4 space-y-3 text-sm text-subtext">
                {section.links.map((link) => (
                  <li key={link.label}>
                    <Link to={link.to} className={linkCls}>
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          ))}

          {/* Social */}
          <div>
            <h3 className="font-display text-sm font-semibold uppercase tracking-wider text-text">
              Follow us
            </h3>
            <div className="mt-4 flex items-center gap-4 text-subtext">
              {SOCIAL_LINKS.map(({ label, href, icon: Icon }) => (
                <a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={`${label} (opens in a new tab)`}
                  className={`rounded-full p-2 transition-colors hover:bg-bg hover:text-primary ${focusRing}`}
                >
                  <Icon size={20} aria-hidden="true" />
                </a>
              ))}
            </div>
            <div className="mt-6 flex items-center gap-2 text-xs text-subtext">
              <FiGlobe size={14} aria-hidden="true" />
              <span>English (IN) · ₹ INR</span>
            </div>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-border pt-8 text-xs text-subtext sm:flex-row">
          <p className="text-center sm:text-left">
            © {currentYear} BlinkStay · A portfolio project by Qutubuddin Khan
          </p>

          <nav
            aria-label="Legal"
            className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2"
          >
            {LEGAL_LINKS.map((legal) => (
              <Link
                key={legal.label}
                to={legal.to}
                className={`${linkCls} hover:underline`}
              >
                {legal.label}
              </Link>
            ))}
          </nav>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
