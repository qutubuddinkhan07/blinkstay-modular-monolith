import { createBrowserRouter } from "react-router-dom";
import Signup from "../features/auth/pages/Signup";
import Login from "../features/auth/pages/Login";
import ListingHome from "../features/listings/ListingHome";

const route = createBrowserRouter([
  {
    path: "/",
    element: <ListingHome />,
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
