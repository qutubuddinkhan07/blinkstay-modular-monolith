import React from "react";
import Navbar from "./Navbar";
import Footer from "./Footer";
import { Outlet, ScrollRestoration } from "react-router-dom";
import { scrollKey } from "../../utils/scrollKey";
const Layout = () => {
  return (
    <div className="relative">
      <Navbar />
      <div className="">
        <Outlet />
        <ScrollRestoration getKey={scrollKey} />
      </div>
      <Footer />
    </div>
  );
};

export default Layout;
