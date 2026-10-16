import { useState } from "react";
import { cld } from "../../../utils/cloudinary"; // adjust the path to where the page lives
import { Link } from "react-router-dom"; // use "react-router" if you're on v7 without react-router-dom
import { motion } from "motion/react";
import { useAuth } from "../../../context/AuthContext";

/* ------------------------------------------------------------------
   Put your images in /src/assets (or use Cloudinary URLs) and set them here.
   While a value is "", a clean gradient placeholder is shown instead.
------------------------------------------------------------------- */
const IMAGES = {
  hero: "https://res.cloudinary.com/dps1vvchb/image/upload/v1791228575/image_1_unskne.avif", // wide hotel / resort photo, ideally 1600x1000
  stay1:
    "https://res.cloudinary.com/dps1vvchb/image/upload/v1791228497/image_4_mchipd.jpg", // room interior
  stay2:
    "https://res.cloudinary.com/dps1vvchb/image/upload/v1791228571/image_5_v3nnzd.avif", // pool / view
  stay3:
    "https://res.cloudinary.com/dps1vvchb/image/upload/v1791228567/image_4_y2tlh9.avif", // lobby / exterior
  host: "https://res.cloudinary.com/dps1vvchb/image/upload/v1791228398/image_4_dlftst.avif", // host section photo
};

function Photo({ src, alt, className = "", eager = false, width }) {
  const [failed, setFailed] = useState(false);

  if (src && !failed) {
    return (
      <img
        src={cld(src, width)}
        alt={alt}
        loading={eager ? "eager" : "lazy"}
        fetchPriority={eager ? "high" : undefined} // React 19; on React 18 use lowercase fetchpriority
        decoding="async"
        onError={() => setFailed(true)}
        className={`object-cover ${className}`}
      />
    );
  }
  return (
    <div
      role={alt ? "img" : undefined}
      aria-label={alt || undefined}
      aria-hidden={alt ? undefined : true}
      className={`bg-gradient-to-br from-[#002642] via-[#03314f] to-[#710019] ${className}`}
    />
  );
}

function Icon({ children }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="currentColor"
      className="h-6 w-6"
      aria-hidden="true"
    >
      {children}
    </svg>
  );
}

const features = [
  {
    title: "Find stays on the map",
    text: "Browse listings by location and see exactly where each property sits before you decide.",
    icon: (
      <path d="M12 21s7-5.686 7-11a7 7 0 10-14 0c0 5.314 7 11 7 11zm0-8a3 3 0 110-6 3 3 0 010 6z" />
    ),
  },
  {
    title: "Real photos, clear details",
    text: "Every listing has a photo gallery, room options and plain information about what is included.",
    icon: (
      <path d="M4 5h16a1 1 0 011 1v12a1 1 0 01-1 1H4a1 1 0 01-1-1V6a1 1 0 011-1zm2 11l3.5-4.5 2.5 3 2-2.5L18 16H6zm2-6.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3z" />
    ),
  },
  {
    title: "Hosts you can trust",
    text: "Hotel managers are reviewed and approved by our team before their properties go live.",
    icon: (
      <path d="M12 3l8 3v6c0 4.5-3.2 8.2-8 9-4.8-.8-8-4.5-8-9V6l8-3zm-1 12l5-5-1.4-1.4L11 12.2 9.4 10.6 8 12l3 3z" />
    ),
  },
  {
    title: "Secure sign in",
    text: "Email verification and protected sessions keep your account and details safe.",
    icon: (
      <path d="M17 9V7a5 5 0 00-10 0v2H5v12h14V9h-2zm-8-2a3 3 0 016 0v2H9V7z" />
    ),
  },
];

const steps = [
  {
    title: "Explore",
    text: "Search the listings and filter until something fits your trip.",
  },
  {
    title: "Compare",
    text: "Open a listing to view photos, rooms and the location on the map.",
  },
  { title: "Stay", text: "Choose your room and get ready for the trip." },
];

const primaryBtn =
  "rounded-full bg-primary px-7 py-3.5 text-base font-semibold text-on-primary transition hover:bg-primary-hover focus:outline-none focus-visible:ring-4 focus-visible:ring-primary/40";

export default function Home() {
  const { isManager } = useAuth();

  return (
    <div className="bg-bg font-body text-text">
      {/* HERO */}
      <section className="relative isolate overflow-hidden bg-[#002642]">
        <Photo
          src={IMAGES.hero}
          alt="A bright hotel room overlooking the sea"
          className="absolute inset-0 -z-10 h-full w-full opacity-60"
        />
        <div className="absolute inset-0 -z-10 bg-gradient-to-r from-[#002642]/90 via-[#002642]/50 to-transparent" />

        <div className="mx-auto flex min-h-[78vh] max-w-6xl flex-col justify-center px-6 py-24">
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: "easeOut" }}
            className="max-w-2xl"
          >
            <h1 className="font-display text-4xl font-medium leading-[1.1] tracking-tight text-white sm:text-6xl">
              Book a stay that feels right, in a blink.
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-relaxed text-white/80">
              BlinkStay helps you find hotels and rooms from approved hosts, see
              them on the map, and choose with confidence.
            </p>
            <div className="mt-10 flex flex-wrap gap-4">
              <Link
                to="/explore"
                className={`${primaryBtn} shadow-lg ring-1 ring-white/25`}
              >
                Explore stays
              </Link>
              <Link
                to="/about"
                className="rounded-full border border-white/40 px-7 py-3.5 text-base font-semibold text-white transition hover:bg-white/10 focus:outline-none focus-visible:ring-4 focus-visible:ring-white/40"
              >
                About BlinkStay
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* FEATURES */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="max-w-2xl">
          <h2 className="font-display text-3xl font-medium tracking-tight sm:text-4xl">
            Everything you need to pick the right place
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-subtext">
            We keep the search simple and the information honest, so you spend
            less time comparing tabs.
          </p>
        </div>
        <div className="mt-12 grid gap-x-10 gap-y-12 sm:grid-cols-2">
          {features.map((f) => (
            <div key={f.title} className="flex gap-5">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-muted text-primary">
                <Icon>{f.icon}</Icon>
              </div>
              <div>
                <h3 className="text-lg font-semibold">{f.title}</h3>
                <p className="mt-2 leading-relaxed text-subtext">{f.text}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* PHOTO STRIP */}
      <section className="bg-muted py-20">
        <div className="mx-auto max-w-6xl px-6">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <h2 className="font-display text-3xl font-medium tracking-tight sm:text-4xl">
              Stays worth the trip
            </h2>
            <Link
              to="/explore"
              className="font-semibold text-primary underline-offset-4 hover:underline"
            >
              See all listings
            </Link>
          </div>
          <div className="mt-10 grid gap-5 md:grid-cols-3 md:grid-rows-2">
            <Photo
              src={IMAGES.stay1}
              alt="Comfortable guest room"
              className="h-64 w-full rounded-2xl md:row-span-2 md:h-full md:min-h-[26rem]"
            />
            <Photo
              src={IMAGES.stay2}
              alt="Hotel pool with a view"
              className="h-64 w-full rounded-2xl"
            />
            <Photo
              src={IMAGES.stay3}
              alt="Hotel lobby"
              className="h-64 w-full rounded-2xl"
            />
            <div className="flex flex-col justify-center rounded-2xl bg-primary p-8 text-on-primary md:col-span-2">
              <h3 className="font-display text-2xl font-medium">
                Ready to find yours?
              </h3>
              <p className="mt-2 max-w-md text-on-primary/85">
                Open the explore page and start with the destination you have in
                mind.
              </p>
              <div className="mt-6">
                <Link
                  to="/explore"
                  className="inline-block rounded-full bg-on-primary px-6 py-3 font-semibold text-primary transition hover:opacity-90 focus:outline-none focus-visible:ring-4 focus-visible:ring-white/50"
                >
                  Start exploring
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* HOW IT WORKS (a real sequence, so numbered) */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <h2 className="font-display text-3xl font-medium tracking-tight sm:text-4xl">
          How BlinkStay works
        </h2>
        <ol className="mt-12 grid gap-10 md:grid-cols-3">
          {steps.map((s, i) => (
            <li key={s.title} className="border-t-2 border-primary pt-5">
              <span className="text-sm font-semibold text-primary">
                Step {i + 1}
              </span>
              <h3 className="mt-1 text-xl font-semibold">{s.title}</h3>
              <p className="mt-2 leading-relaxed text-subtext">{s.text}</p>
            </li>
          ))}
        </ol>
      </section>

      {/* HOST CTA */}
      <section className="mx-auto max-w-6xl px-6 pb-24">
        <div className="grid overflow-hidden rounded-3xl border border-border bg-surface md:grid-cols-2">
          <div className="p-10 sm:p-14">
            <h2 className="font-display text-3xl font-medium tracking-tight">
              Own a hotel? Host on BlinkStay.
            </h2>
            <p className="mt-4 max-w-md leading-relaxed text-subtext">
              Apply to become a host, add your business details and rooms, and
              publish your listing once approved. You can pause it any time.
            </p>
            <Link
              to={isManager ? "/my-listings" : "/become-host"}
              className={`${primaryBtn} mt-8 inline-block`}
            >
              {isManager ? "Manage your listings" : "Become a host"}
            </Link>
          </div>
          <Photo
            src={IMAGES.host}
            alt="Hotel manager welcoming a guest"
            className="h-64 w-full md:h-full"
          />
        </div>
      </section>
    </div>
  );
}
