import React from "react";
import Navbar from "./Navbar";
import { Outlet } from "react-router-dom";

const Layout = () => {
  return (
    <div className="relative">
      <Navbar />
      <div className="">
        <Outlet />
      </div>
    </div>
  );
};

export default Layout;
