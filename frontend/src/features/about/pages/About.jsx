import { Link } from "react-router-dom"; // use "react-router" if you're on v7 without react-router-dom

const ABOUT_IMAGE =
  "https://res.cloudinary.com/dps1vvchb/image/upload/v1791226999/image_1_ttkxki.avif"; // optional: a team / hotel photo. Empty = gradient placeholder.

const guests = [
  "Browse listings with photos, rooms and a map location",
  "Sign up with email verification and a secure session",
  "Manage your profile, photo and password in one place",
];

const hosts = [
  "Apply to become a host with your business details",
  "Add rooms and publish or pause your listing whenever you want",
  "Get clear status and reasons if a listing is ever suspended",
];

const stack = [
  { name: "React", role: "Frontend" },
  { name: "Spring Boot", role: "Backend API" },
  { name: "MySQL", role: "Database" },
  { name: "Mapbox", role: "Maps" },
  { name: "Cloudinary", role: "Image storage" },
  { name: "Caffeine", role: "Caching" },
];

const primaryBtn =
  "inline-block rounded-full bg-primary px-7 py-3.5 font-semibold text-on-primary transition hover:bg-primary-hover focus:outline-none focus-visible:ring-4 focus-visible:ring-primary/40";

function Check({ className = "text-primary" }) {
  return (
    <svg
      viewBox="0 0 20 20"
      className={`mt-1 h-5 w-5 shrink-0 ${className}`}
      fill="currentColor"
      aria-hidden="true"
    >
      <path d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.7-9.3l-4.5 4.5a1 1 0 01-1.4 0l-2-2 1.4-1.4 1.3 1.3 3.8-3.8 1.4 1.4z" />
    </svg>
  );
}

export default function About() {
  return (
    <div className="bg-bg font-body text-text">
      {/* INTRO */}
      <section className="bg-muted">
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-6 py-20 md:grid-cols-2">
          <div>
            <h1 className="font-display text-4xl font-medium leading-tight tracking-tight sm:text-5xl">
              Making it simple to find a place to stay.
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-relaxed text-subtext">
              BlinkStay is a hotel booking platform that connects travellers
              with approved hotel managers. We focus on clear listings, honest
              details and a process that is easy to follow from the first search
              to the final room choice.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <Link to="/explore" className={primaryBtn}>
                Explore stays
              </Link>
              <Link
                to="/"
                className="rounded-full border border-border bg-surface px-7 py-3.5 font-semibold text-text transition hover:bg-bg focus:outline-none focus-visible:ring-4 focus-visible:ring-primary/30"
              >
                Back to home
              </Link>
            </div>
          </div>
          {ABOUT_IMAGE ? (
            <img
              src={ABOUT_IMAGE}
              alt="BlinkStay"
              className="h-80 w-full rounded-3xl object-cover md:h-[26rem]"
            />
          ) : (
            <div
              role="img"
              aria-label="BlinkStay"
              className="h-80 w-full rounded-3xl bg-gradient-to-br from-[#002642] via-[#03314f] to-[#710019] md:h-[26rem]"
            />
          )}
        </div>
      </section>

      {/* MISSION */}
      <section className="mx-auto max-w-3xl px-6 py-20">
        <h2 className="font-display text-3xl font-medium tracking-tight">
          Why we built it
        </h2>
        <p className="mt-5 text-lg leading-relaxed text-subtext">
          Booking a room often means jumping between sites, unclear photos and
          unknown hosts. BlinkStay puts the important things in one place: where
          the hotel is, what the rooms look like, and who is behind the listing.
          Hosts are reviewed before they go live, and our admins can step in
          quickly when something is not right.
        </p>
      </section>

      {/* GUESTS & HOSTS */}
      <section className="mx-auto max-w-6xl px-6 pb-20">
        <div className="grid gap-8 md:grid-cols-2">
          <div className="rounded-3xl border border-border bg-surface p-8 sm:p-10">
            <h2 className="font-display text-2xl font-medium">For guests</h2>
            <ul className="mt-6 space-y-4">
              {guests.map((t) => (
                <li key={t} className="flex gap-3 leading-relaxed text-subtext">
                  <Check />
                  <span>{t}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="rounded-3xl bg-primary p-8 text-on-primary sm:p-10">
            <h2 className="font-display text-2xl font-medium">For hosts</h2>
            <ul className="mt-6 space-y-4">
              {hosts.map((t) => (
                <li
                  key={t}
                  className="flex gap-3 leading-relaxed text-on-primary/90"
                >
                  <Check className="text-on-primary" />
                  <span>{t}</span>
                </li>
              ))}
            </ul>
            <Link
              to="/become-host"
              className="mt-8 inline-block rounded-full bg-on-primary px-6 py-3 font-semibold text-primary transition hover:opacity-90 focus:outline-none focus-visible:ring-4 focus-visible:ring-white/50"
            >
              Become a host
            </Link>
          </div>
        </div>
      </section>

      {/* TECH */}
      <section className="bg-muted py-20">
        <div className="mx-auto max-w-6xl px-6">
          <h2 className="font-display text-3xl font-medium tracking-tight">
            Built with
          </h2>
          <p className="mt-4 max-w-2xl text-lg leading-relaxed text-subtext">
            BlinkStay is a full-stack project, designed so each part can grow on
            its own.
          </p>
          <dl className="mt-10 grid grid-cols-2 gap-6 sm:grid-cols-3 lg:grid-cols-6">
            {stack.map((s) => (
              <div
                key={s.name}
                className="rounded-2xl border border-border bg-surface p-5"
              >
                <dt className="font-semibold">{s.name}</dt>
                <dd className="mt-1 text-sm text-subtext">{s.role}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      {/* CLOSING CTA */}
      <section className="mx-auto max-w-3xl px-6 py-20 text-center">
        <h2 className="font-display text-3xl font-medium tracking-tight">
          Find your next stay
        </h2>
        <p className="mt-4 text-lg text-subtext">
          Browse listings and see what is available.
        </p>
        <Link to="/explore" className={`${primaryBtn} mt-8`}>
          Explore stays
        </Link>
      </section>
    </div>
  );
}
