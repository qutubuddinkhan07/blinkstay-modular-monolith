import React, { useState } from "react";
import Logo from "../common/Logo";
import { useTheme } from "../../context/ThemeContext";
import { Link, NavLink, replace, useNavigate } from "react-router-dom";
import { FaMoon } from "react-icons/fa";
import { IoSunny } from "react-icons/io5";
import { useAuth } from "../../context/AuthContext";

const Navbar = () => {
  const navigate = useNavigate();
  const { isDark, toggleTheme } = useTheme();

  const nav_list = [
    {
      name: "home",
      link: "/",
    },
    {
      name: "explore",
      link: "/explore",
    },
    {
      name: "about",
      link: "/about",
    },
  ];

  const { isAuthenticated, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    navigate("/explore", { replace: true });
  };

  return (
    <nav className="h-[78px] bg-surface w-full sticky top-0 left-0 z-50 flex items-center shadow-md px-8">
      {/* Logo */}
      <Logo isDark={isDark} />

      {/* Navigation */}
      <ul className="mx-auto flex items-center gap-3 capitalize font-semibold">
        {nav_list.map((item) => (
          <li key={item.link}>
            <NavLink
              to={item.link}
              className={({ isActive }) =>
                `relative px-3 py-2 text-text transition-colors duration-300
                 after:absolute after:left-0 after:right-0 after:-bottom-1
                 after:h-[2px] after:bg-primary
                 after:transition-transform after:duration-300
                 ${
                   isActive
                     ? "text-primary after:scale-x-100"
                     : "after:scale-x-0 hover:text-primary"
                 }`
              }
            >
              {item.name}
            </NavLink>
          </li>
        ))}
      </ul>

      {/* Right side */}
      <div className="flex items-center gap-4">
        {/* Theme toggle */}
        <button
          type="button"
          onClick={toggleTheme}
          aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
          className="w-10 h-10 flex items-center justify-center rounded-full text-text hover:text-primary hover:bg-primary/10 hover:scale-105 active:scale-95 transition-all duration-300 cursor-pointer"
        >
          {isDark ? <IoSunny size={20} /> : <FaMoon size={20} />}
        </button>

        {/* Authentication */}
        {isAuthenticated ? (
          <button
            type="button"
            onClick={handleLogout}
            className="text-white px-4 py-2 rounded-lg bg-primary hover:bg-primary/80 cursor-pointer transition-colors duration-300"
          >
            Logout
          </button>
        ) : (
          <Link
            to="/login"
            className="bg-primary px-4 py-2 text-white rounded-lg hover:bg-primary-hover transition-colors duration-300"
          >
            Login
          </Link>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
