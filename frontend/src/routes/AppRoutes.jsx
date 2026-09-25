import { createBrowserRouter } from "react-router-dom";
import Home from "../components/common/Home";
import Signup from "../features/auth/pages/Signup";
import Login from "../features/auth/pages/Login";

const route = createBrowserRouter([
  {
    path: "/",
    element: <Home />,
  },
  {
    path: "login",
    element: <Login />,
  },
  {
    path: "signup",
    element: <Signup />,
  },
]);

export default route;
