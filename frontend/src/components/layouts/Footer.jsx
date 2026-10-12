import React from "react";
import { Link } from "react-router-dom";
import {
  FiHome,
  FiInstagram,
  FiLinkedin,
  FiGithub,
  FiGlobe,
} from "react-icons/fi";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const FOOTER_SECTIONS = [
  {
    title: "Explore",
    links: [
      {
        to: "/explore",
        label: "stays",
        external: false,
      },
      {
        to: "/explore?category=TRENDING",
        label: "trending destinations",
        external: false,
      },
      {
        to: "/explore?category=BEACH",
        label: "beachfront",
        external: false,
      },
      {
        to: "/explore?category=MOUNTAINS",
        label: "mountain gateways",
        external: false,
      },
    ],
  },
  {
    title: "support & company",
    links: [
      {
        label: "help center",
        href: "#help",
        external: true,
      },
      {
        label: "about us",
        href: "#about",
        external: true,
      },
      {
        label: "careers",
        href: "#careers",
        external: true,
      },
      {
        label: "contact us",
        href: "#contact",
        external: true,
      },
    ],
  },
];

const SOCIAL_LINKS = [
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

const LEGAL_LINKS = [
  { label: "Privacy Policy", href: "#privacy" },
  { label: "Terms of Service", href: "#terms" },
  { label: "Cookie Preferences", href: "#cookies" },
];

const Footer = () => {
  const currentYear = new Date().getFullYear();
  return (
    <footer className="border-t border-border bg-surface text-text transition-colors duration-300">
      <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-10 lg:py-16">
        {/* Main Footer Grid */}
        <div className="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-5">
          {/* Brand Info Section (Spans 2 columns) */}
          <div className="lg:col-span-2">
            <Link
              to="/explore"
              className={`inline-flex items-center gap-2 font-display text-xl font-bold tracking-tight text-primary ${focusRing}`}
            >
              <FiHome className="h-6 w-6 text-primary" />
              <span>BlinkStay</span>
            </Link>
            <p className="mt-3 max-w-sm text-sm text-subtext leading-relaxed">
              Find a place that feels like home. Discover unique stays, local
              experiences, and unforgettable destinations around the world.
            </p>
          </div>

          {/* Dynamic Navigation Columns */}
          {FOOTER_SECTIONS.map((section) => (
            <div key={section.title}>
              <h3 className="font-display text-sm font-semibold uppercase tracking-wider text-text">
                {section.title}
              </h3>
              <ul className="mt-4 space-y-3 text-sm text-subtext">
                {section.links.map((link) => (
                  <li key={link.label}>
                    {link.external ? (
                      <a
                        href={link.href}
                        className={`transition-colors capitalize hover:text-primary ${focusRing}`}
                      >
                        {link.label}
                      </a>
                    ) : (
                      <Link
                        to={link.to}
                        className={`transition-colors capitalize hover:text-primary ${focusRing}`}
                      >
                        {link.label}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}

          {/* Follow Us & Socials Column */}
          <div>
            <h3 className="font-display text-sm font-semibold uppercase tracking-wider text-text">
              Follow Us
            </h3>
            <div className="mt-4 flex items-center gap-4 text-subtext">
              {SOCIAL_LINKS.map(({ label, href, icon: Icon }) => (
                <a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noreferrer"
                  aria-label={label}
                  className={`rounded-full p-2 transition-colors hover:bg-bg hover:text-primary ${focusRing}`}
                >
                  <Icon size={20} />
                </a>
              ))}
            </div>
            <div className="mt-6 flex items-center gap-2 text-xs text-subtext">
              <FiGlobe size={14} />
              <span>English (IN) • ₹ INR</span>
            </div>
          </div>
        </div>

        {/* Bottom Legal Section */}
        <div className="mt-12 border-t border-border pt-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-subtext">
          <p>© {currentYear} BlinkStay, Inc. All rights reserved.</p>

          <div className="flex items-center gap-6">
            {LEGAL_LINKS.map((legal) => (
              <a
                key={legal.label}
                href={legal.href}
                className="transition-colors hover:text-primary hover:underline"
              >
                {legal.label}
              </a>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
