import React, { useEffect } from "react";
import { logout } from "../../features/auth/authService";

const Home = () => {
  const logoutFn = async () => {
    try {
      const response = await logout();
      console.log(response);
    } catch (err) {
      console.error("Logout failed: ", err);
    } finally {
      localStorage.removeItem("token");
    }
  };

  return (
    <div>
      Home
      <button
        onClick={logoutFn}
        className="bg-blue-700 text-white rounded-2xl cursor-pointer px-3 py-1"
      >
        Logout
      </button>
    </div>
  );
};

export default Home;
