import { Link } from "react-router-dom";
import { usePageTitle } from "../../../utils/usePageTitle";
import { SOCIAL_LINKS } from "../../../utils/support";

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const CareersPage = () => {
  usePageTitle("Careers");
  const links = SOCIAL_LINKS.filter((l) => l.label !== "Instagram");

  return (
    <div className="flex min-h-[calc(100vh-72px)] items-center justify-center bg-bg px-4 py-10 text-text">
      <div className="max-w-xl text-center">
        <h1 className="font-display text-3xl font-semibold sm:text-4xl">
          Careers
        </h1>
        <p className="mt-4 text-subtext">
          BlinkStay is a portfolio project, built end to end by one developer,
          so there are no open roles here.
        </p>
        <p className="mt-2 text-subtext">
          If you're hiring, I'd like to hear from you. The code and my profile
          are linked below.
        </p>

        <div className="mt-6 flex flex-wrap justify-center gap-3">
          {links.map(({ label, href, icon: Icon }) => (
            <a
              key={label}
              href={href}
              target="_blank"
              rel="noopener noreferrer"
              className={`inline-flex items-center gap-2 rounded-lg border border-border bg-surface px-5 py-2.5 text-sm font-semibold text-text hover:bg-input ${focusRing}`}
            >
              <Icon size={16} aria-hidden="true" />
              {label}
            </a>
          ))}
          <Link
            to="/about"
            className={`inline-flex items-center rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary hover:bg-primary-hover ${focusRing}`}
          >
            About BlinkStay
          </Link>
        </div>
      </div>
    </div>
  );
};

export default CareersPage;
