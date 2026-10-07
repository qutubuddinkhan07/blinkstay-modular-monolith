import React, { useEffect, useRef, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import {
  AnimatePresence,
  MotionConfig,
  motion,
  useMotionValueEvent,
  useScroll,
} from "motion/react";
import { FaMoon } from "react-icons/fa";
import { IoSunny } from "react-icons/io5";
import {
  FiCalendar,
  FiChevronDown,
  FiGrid,
  FiHeart,
  FiHome,
  FiLogOut,
  FiMenu,
  FiPlusSquare,
  FiUser,
  FiUsers,
  FiX,
} from "react-icons/fi";
import Logo from "../common/Logo";
import { useTheme } from "../../context/ThemeContext";
import { useAuth } from "../../context/AuthContext";

/* ------------------------------------------------------------------ */
/* Config                                                              */
/* ------------------------------------------------------------------ */

const NAV_LIST = [
  { name: "home", link: "/" },
  { name: "explore", link: "/explore" },
  { name: "about", link: "/about" },
];

// Account links shown in the avatar menu / mobile panel.
// A link with `roles` only shows for users who have at least one of them.
// Some of these pages aren't built yet, so they will show your 404 page until you add the routes.
const ACCOUNT_SECTIONS = [
  {
    title: "My account",
    links: [
      { label: "My profile", to: "/profile", icon: FiUser },
      { label: "My bookings", to: "/bookings", icon: FiCalendar },
      { label: "Saved hotels", to: "/favorites", icon: FiHeart },
    ],
  },
  {
    title: "Hotel management",
    links: [
      {
        label: "My listings",
        to: "/my-listings",
        icon: FiHome,
        roles: ["HOTEL_MANAGER"], // GET /my-listings is manager-only on the backend
      },
      {
        label: "Add a listing",
        to: "/create-listing",
        icon: FiPlusSquare,
        roles: ["HOTEL_MANAGER", "ADMIN"],
      },
      {
        label: "Reservations",
        to: "/manager/reservations",
        icon: FiCalendar,
        roles: ["HOTEL_MANAGER"],
      },
    ],
  },
  {
    title: "Administration",
    links: [
      { label: "Dashboard", to: "/admin", icon: FiGrid, roles: ["ADMIN"] },
      {
        label: "Manage users",
        to: "/admin/users",
        icon: FiUsers,
        roles: ["ADMIN"],
      },
    ],
  },
];

const focusRing =
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary";

const roleLabel = (role) => role.replace(/_/g, " ").toLowerCase();

/* ------------------------------------------------------------------ */
/* Motion presets (kept short: 150–300ms, transform + opacity only)    */
/* ------------------------------------------------------------------ */

const MotionLink = motion.create(Link);

const spring = { type: "spring", stiffness: 380, damping: 30 };

const dropdownMotion = {
  initial: { opacity: 0, y: -6, scale: 0.96 },
  animate: { opacity: 1, y: 0, scale: 1, transition: { duration: 0.18 } },
  exit: { opacity: 0, y: -6, scale: 0.96, transition: { duration: 0.12 } },
};

const panelVariants = {
  hidden: { opacity: 0, y: -8, transition: { duration: 0.15 } },
  show: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.2,
      when: "beforeChildren",
      staggerChildren: 0.04,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, x: -8 },
  show: { opacity: 1, x: 0, transition: { duration: 0.18 } },
};

/* ------------------------------------------------------------------ */
/* Small pieces                                                        */
/* ------------------------------------------------------------------ */

// Cross-fades + rotates between two icons (theme toggle, hamburger)
const IconSwap = ({ swapKey, children }) => (
  <AnimatePresence mode="wait" initial={false}>
    <motion.span
      key={swapKey}
      className="flex"
      initial={{ opacity: 0, rotate: -80, scale: 0.6 }}
      animate={{ opacity: 1, rotate: 0, scale: 1 }}
      exit={{ opacity: 0, rotate: 80, scale: 0.6 }}
      transition={{ duration: 0.16 }}
    >
      {children}
    </motion.span>
  </AnimatePresence>
);

const Avatar = ({ user, className = "w-10 h-10" }) => {
  const [failed, setFailed] = useState(false);
  const initial = user?.username?.[0]?.toUpperCase() ?? "U";

  if (!user?.profileImgUrl || failed) {
    return (
      <span
        className={`${className} flex shrink-0 items-center justify-center rounded-full bg-text font-display font-semibold text-surface`}
        aria-hidden="true"
      >
        {initial}
      </span>
    );
  }

  return (
    <img
      src={user.profileImgUrl}
      alt=""
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className={`${className} shrink-0 rounded-full object-cover`}
    />
  );
};

const ThemeToggle = ({ isDark, onToggle, className = "" }) => (
  <motion.button
    type="button"
    onClick={onToggle}
    whileHover={{ scale: 1.08 }}
    whileTap={{ scale: 0.9 }}
    aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
    className={`flex h-10 w-10 cursor-pointer items-center justify-center rounded-full text-text transition-colors duration-300 hover:bg-bg ${focusRing} ${className}`}
  >
    <IconSwap swapKey={isDark ? "sun" : "moon"}>
      {isDark ? <IoSunny size={20} /> : <FaMoon size={18} />}
    </IconSwap>
  </motion.button>
);

const AccountSections = ({ sections, onNavigate }) => (
  <div className="divide-y divide-border">
    {sections.map((section) => (
      <div key={section.title} className="py-2">
        <p className="px-3 pb-1 text-xs font-semibold text-subtext">
          {section.title}
        </p>
        {section.links.map(({ label, to, icon: Icon }) => (
          <Link
            key={to}
            to={to}
            onClick={onNavigate}
            className={`group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-text transition-colors hover:bg-bg ${focusRing}`}
          >
            <Icon
              size={16}
              className="text-subtext transition-transform duration-200 group-hover:translate-x-0.5"
            />
            {label}
          </Link>
        ))}
      </div>
    ))}
  </div>
);

/* ------------------------------------------------------------------ */
/* Navbar                                                              */
/* ------------------------------------------------------------------ */

const Navbar = () => {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { isDark, toggleTheme } = useTheme();
  const { user, isAuthenticated, logout } = useAuth();

  const [scrolled, setScrolled] = useState(
    () => typeof window !== "undefined" && window.scrollY > 16,
  ); // becomes a floating pill
  const [hidden, setHidden] = useState(false); // slides away on scroll down
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userOpen, setUserOpen] = useState(false);

  const headerRef = useRef(null);
  const userRef = useRef(null);
  const lockRef = useRef(false);
  const lastY = useRef(0);

  const roles = user?.roles ?? [];
  const sections = ACCOUNT_SECTIONS.map((section) => ({
    ...section,
    links: section.links.filter(
      (link) => !link.roles || link.roles.some((r) => roles.includes(r)),
    ),
  })).filter((section) => section.links.length > 0);

  /* Don't hide the bar while a menu is open */
  useEffect(() => {
    lockRef.current = mobileOpen || userOpen;
    if (lockRef.current) setHidden(false);
  }, [mobileOpen, userOpen]);

  /* Scroll behaviour: pill after 16px, hide going down, show going up */
  const { scrollY } = useScroll();
  useMotionValueEvent(scrollY, "change", (y) => {
    setScrolled(y > 16);

    if (lockRef.current) {
      lastY.current = y;
      return;
    }
    if (Math.abs(y - lastY.current) < 8) return;

    setHidden(y > lastY.current && y > 160);
    lastY.current = y;
  });

  /* Close menus on outside click / Escape */
  useEffect(() => {
    const onPointerDown = (e) => {
      if (userRef.current && !userRef.current.contains(e.target)) {
        setUserOpen(false);
      }
      if (headerRef.current && !headerRef.current.contains(e.target)) {
        setMobileOpen(false);
      }
    };
    const onKeyDown = (e) => {
      if (e.key === "Escape") {
        setUserOpen(false);
        setMobileOpen(false);
      }
    };

    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, []);

  /* Close menus on route change */
  useEffect(() => {
    setMobileOpen(false);
    setUserOpen(false);
  }, [pathname]);

  const closeMenus = () => {
    setMobileOpen(false);
    setUserOpen(false);
  };

  const handleLogout = async () => {
    closeMenus();
    await logout();
    navigate("/explore", { replace: true });
  };

  return (
    // reducedMotion="user": people who turn on "reduce motion" in their OS
    // get fades only, no sliding / scaling / rotating.
    <MotionConfig reducedMotion="user">
      {/* Keeps the page content below the fixed bar */}
      <div className="h-[72px]" aria-hidden="true" />

      <motion.header
        ref={headerRef}
        initial={{ y: -80, opacity: 0 }}
        animate={{ y: hidden ? "-110%" : 0, opacity: 1 }}
        transition={{ type: "spring", stiffness: 260, damping: 26 }}
        className={`fixed inset-x-0 top-0 z-50 transition-[padding] duration-300 motion-reduce:transition-none ${
          scrolled ? "px-3 pt-3 sm:px-6" : "px-0 pt-0"
        }`}
      >
        <nav
          aria-label="Main"
          className={`mx-auto flex w-full items-center gap-4 px-4 transition-all duration-300 motion-reduce:transition-none sm:px-8 ${
            scrolled
              ? "h-16 max-w-5xl rounded-full border border-border bg-surface/90 shadow-lg backdrop-blur-md"
              : "h-[72px] max-w-full border-b border-border bg-surface shadow-sm"
          }`}
        >
          {/* Logo */}
          <Link
            to="/"
            aria-label="Blinkstay home"
            className={`rounded-lg ${focusRing}`}
          >
            <motion.div whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.97 }}>
              <Logo isDark={isDark} />
            </motion.div>
          </Link>

          {/* Desktop links */}
          <ul className="mx-auto hidden items-center gap-2 font-semibold capitalize md:flex">
            {NAV_LIST.map((item) => (
              <li key={item.link}>
                <NavLink
                  to={item.link}
                  end={item.link === "/"}
                  className={({ isActive }) =>
                    `relative block rounded-md px-3 py-2 transition-colors duration-300 ${focusRing} ${
                      isActive ? "text-text" : "text-subtext hover:text-text"
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      {item.name}
                      {isActive && (
                        // One underline that slides between links
                        <motion.span
                          layoutId="nav-underline"
                          transition={spring}
                          className="absolute inset-x-3 -bottom-0.5 h-0.5 rounded-full bg-primary"
                        />
                      )}
                    </>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>

          {/* Right side */}
          <div className="ml-auto flex items-center gap-2 sm:gap-3 md:ml-0">
            <ThemeToggle
              isDark={isDark}
              onToggle={toggleTheme}
              className="hidden md:flex"
            />

            {isAuthenticated ? (
              /* Desktop avatar menu */
              <div ref={userRef} className="relative hidden md:block">
                <motion.button
                  type="button"
                  onClick={() => setUserOpen((v) => !v)}
                  whileTap={{ scale: 0.96 }}
                  aria-expanded={userOpen}
                  aria-haspopup="true"
                  aria-label="Open account menu"
                  className={`flex cursor-pointer items-center gap-2 rounded-full p-1 pr-2 transition-colors hover:bg-bg ${focusRing}`}
                >
                  <span
                    className={`rounded-full ring-2 ring-offset-2 ring-offset-surface transition-all duration-200 ${
                      userOpen ? "ring-primary" : "ring-transparent"
                    }`}
                  >
                    <Avatar user={user} className="h-9 w-9" />
                  </span>
                  <span className="hidden max-w-[110px] truncate text-sm font-semibold text-text lg:block">
                    {user?.username}
                  </span>
                  <motion.span
                    animate={{ rotate: userOpen ? 180 : 0 }}
                    transition={{ duration: 0.2 }}
                    className="flex text-subtext"
                  >
                    <FiChevronDown size={16} />
                  </motion.span>
                </motion.button>

                <AnimatePresence>
                  {userOpen && (
                    <motion.div
                      {...dropdownMotion}
                      style={{ transformOrigin: "top right" }}
                      className="absolute right-0 top-full mt-3 w-72 rounded-2xl border border-border bg-surface p-2 shadow-xl"
                    >
                      <div className="flex items-center gap-3 px-3 py-3">
                        <Avatar user={user} className="h-11 w-11" />
                        <div className="min-w-0">
                          <p className="truncate font-display text-base font-medium text-text">
                            {user?.username}
                          </p>
                          <p className="truncate text-xs text-subtext">
                            {user?.email}
                          </p>
                        </div>
                      </div>

                      {roles.length > 0 && (
                        <div className="flex flex-wrap gap-1.5 px-3 pb-3">
                          {roles.map((r) => (
                            <span
                              key={r}
                              className="rounded-full border border-border px-2 py-0.5 text-[11px] font-medium capitalize text-subtext"
                            >
                              {roleLabel(r)}
                            </span>
                          ))}
                        </div>
                      )}

                      <div className="border-t border-border">
                        <AccountSections
                          sections={sections}
                          onNavigate={closeMenus}
                        />
                      </div>

                      <div className="border-t border-border pt-2">
                        <button
                          type="button"
                          onClick={handleLogout}
                          className={`flex w-full cursor-pointer items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-danger transition-colors hover:bg-danger/10 ${focusRing}`}
                        >
                          <FiLogOut size={16} />
                          Log out
                        </button>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            ) : (
              <MotionLink
                to="/login"
                whileHover={{ scale: 1.04 }}
                whileTap={{ scale: 0.95 }}
                className={`rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors duration-300 hover:bg-primary-hover ${focusRing}`}
              >
                Login
              </MotionLink>
            )}

            {/* Mobile menu button */}
            <motion.button
              type="button"
              onClick={() => setMobileOpen((v) => !v)}
              whileTap={{ scale: 0.9 }}
              aria-expanded={mobileOpen}
              aria-controls="mobile-menu"
              aria-label={mobileOpen ? "Close menu" : "Open menu"}
              className={`flex h-10 w-10 cursor-pointer items-center justify-center rounded-full text-text transition-colors hover:bg-bg md:hidden ${focusRing}`}
            >
              <IconSwap swapKey={mobileOpen ? "close" : "open"}>
                {mobileOpen ? <FiX size={22} /> : <FiMenu size={22} />}
              </IconSwap>
            </motion.button>
          </div>
        </nav>

        {/* Mobile panel */}
        <AnimatePresence>
          {mobileOpen && (
            <motion.div
              id="mobile-menu"
              variants={panelVariants}
              initial="hidden"
              animate="show"
              exit="hidden"
              className="absolute inset-x-3 top-full mt-2 max-h-[calc(100dvh-6rem)] overflow-y-auto rounded-2xl border border-border bg-surface p-2 shadow-xl sm:inset-x-6 md:hidden"
            >
              {isAuthenticated && (
                <motion.div
                  variants={itemVariants}
                  className="flex items-center gap-3 border-b border-border px-3 py-3"
                >
                  <Avatar user={user} className="h-12 w-12" />
                  <div className="min-w-0">
                    <p className="truncate font-display text-base font-medium text-text">
                      {user?.username}
                    </p>
                    <p className="truncate text-xs text-subtext">
                      {user?.email}
                    </p>
                  </div>
                </motion.div>
              )}

              <ul className="flex flex-col gap-1 py-2 font-semibold capitalize">
                {NAV_LIST.map((item) => (
                  <motion.li key={item.link} variants={itemVariants}>
                    <NavLink
                      to={item.link}
                      end={item.link === "/"}
                      onClick={closeMenus}
                      className={({ isActive }) =>
                        `relative block rounded-lg px-3 py-2.5 transition-colors ${focusRing} ${
                          isActive
                            ? "bg-bg text-text"
                            : "text-subtext hover:bg-bg hover:text-text"
                        }`
                      }
                    >
                      {({ isActive }) => (
                        <>
                          {item.name}
                          {isActive && (
                            <motion.span
                              layoutId="mobile-nav-indicator"
                              transition={spring}
                              className="absolute inset-y-2 left-0 w-0.5 rounded-full bg-primary"
                            />
                          )}
                        </>
                      )}
                    </NavLink>
                  </motion.li>
                ))}
              </ul>

              {isAuthenticated && sections.length > 0 && (
                <motion.div
                  variants={itemVariants}
                  className="border-t border-border"
                >
                  <AccountSections
                    sections={sections}
                    onNavigate={closeMenus}
                  />
                </motion.div>
              )}

              <motion.div
                variants={itemVariants}
                className="mt-1 flex items-center justify-between gap-2 border-t border-border px-1 pt-2"
              >
                <button
                  type="button"
                  onClick={toggleTheme}
                  className={`flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2 text-sm font-medium text-text transition-colors hover:bg-bg ${focusRing}`}
                >
                  <IconSwap swapKey={isDark ? "sun" : "moon"}>
                    {isDark ? <IoSunny size={18} /> : <FaMoon size={16} />}
                  </IconSwap>
                  {isDark ? "Light mode" : "Dark mode"}
                </button>

                {isAuthenticated && (
                  <button
                    type="button"
                    onClick={handleLogout}
                    className={`flex cursor-pointer items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-danger transition-colors hover:bg-danger/10 ${focusRing}`}
                  >
                    <FiLogOut size={16} />
                    Log out
                  </button>
                )}
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.header>
    </MotionConfig>
  );
};

export default Navbar;
