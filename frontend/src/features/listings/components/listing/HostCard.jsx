import React from "react";
import ZoomImage from "../../../../components/common/ZoomImage";

// "Hinata Hyuga" -> "HH", "Hinata" -> "H"
const initials = (name = "") =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? "")
    .join("");

// "2026-10-10" -> "2026". Read straight from the string: no Date, no time zone shift.
const memberYear = (d) =>
  /^\d{4}-\d{2}-\d{2}$/.test(d ?? "") ? d.slice(0, 4) : null;

// null, "" and "   " all count as empty
const filled = (v) => (typeof v === "string" && v.trim() ? v.trim() : null);

// host can be null (the account no longer exists): render nothing, never a placeholder name.
// No contact details here on purpose; a Contact button is a later feature.
const HostCard = ({ host, className = "" }) => {
  const name = filled(host?.name);
  if (!name) return null;

  const year = memberYear(host.memberSince);
  const businessName = filled(host.businessName);
  const about = filled(host.about);
  const size = "h-16 w-16";

  return (
    <section
      aria-labelledby="host-heading"
      className={`rounded-2xl border border-border bg-surface p-6 ${className}`}
    >
      <div className="flex items-center gap-4">
        <ZoomImage
          src={host.photoUrl}
          alt={`${name}'s profile photo`}
          zoomable
          className={`${size} shrink-0 rounded-full`}
          fallback={
            <span
              aria-hidden="true"
              className={`${size} flex shrink-0 items-center justify-center rounded-full bg-text font-display text-xl font-semibold text-surface`}
            >
              {initials(name)}
            </span>
          }
        />

        <div className="min-w-0">
          <h2
            id="host-heading"
            className="font-display text-xl font-semibold text-text"
          >
            Hosted by {name}
          </h2>

          {businessName ? (
            <p className="truncate text-sm font-medium text-text">
              {businessName}
            </p>
          ) : (
            <p className="text-sm italic text-subtext">Independent host</p>
          )}

          {year && <p className="text-sm text-subtext">Member since {year}</p>}
        </div>
      </div>

      <div className="mt-4 border-t border-border pt-4">
        <h3 className="text-sm font-semibold text-text">About the host</h3>
        {about ? (
          <p className="mt-1 max-w-prose whitespace-pre-line text-sm leading-relaxed text-subtext">
            {about}
          </p>
        ) : (
          <p className="mt-1 text-sm italic text-subtext">
            This host hasn't added a bio yet.
          </p>
        )}
      </div>
    </section>
  );
};

export default HostCard;
