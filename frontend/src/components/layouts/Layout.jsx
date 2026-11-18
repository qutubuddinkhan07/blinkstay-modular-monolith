import React from "react";
import Navbar from "./Navbar";
import { Outlet, ScrollRestoration } from "react-router-dom";
import Footer from "./Footer";

const Layout = () => {
  return (
    <div className="relative">
      <Navbar />
      <div className="">
        <Outlet />
        <ScrollRestoration />
      </div>
      <Footer />
    </div>
  );
};

export default Layout;
