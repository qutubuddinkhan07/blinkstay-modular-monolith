import { createBrowserRouter } from "react-router-dom";
import NavigateSetter from "./NavigateSetter";
import Home from "../components/common/Home";
import Signup from "../features/auth/pages/Signup";
import Login from "../features/auth/pages/Login";

const route = createBrowserRouter([
  {
    index: true,
    element: (
      <>
        <NavigateSetter />
        <Home />
      </>
    ),
  },
  {
    path: "/login",
    element: <Login />,
  },
  {
    path: "/signup",
    element: <Signup />,
  },
]);

export default route;
