import { createBrowserRouter } from "react-router-dom";
// import Signup from "../features/auth/pages/Signup";
// import Login from "../features/auth/pages/Login";
// import ListingHome from "../features/listings/ListingHome";
// import ErrorPage from "../components/common/ErrorPage";
// import PageNotFound from "../components/common/PageNotFound";
import { lazy } from "react";
import Layout from "../components/layouts/Layout";

const Signup = lazy(() => import("../features/auth/pages/Signup"));
const Login = lazy(() => import("../features/auth/pages/Login"));
const ExplorePage = lazy(
  () => import("../features/listings/pages/ExplorePage"),
);
const ErrorPage = lazy(() => import("../components/common/ErrorPage"));
const PageNotFound = lazy(() => import("../components/common/PageNotFound"));

const route = createBrowserRouter([
  {
    path: "/",
    errorElement: <ErrorPage />,
    children: [
      {
        // Pathless layout route: no URL segment of its own, just wraps
        // whichever child below matches, with Navbar + Outlet.
        element: <Layout />,
        children: [
          {
            index: true,
            element: <ExplorePage />,
          },
          {
            path: "/explore",
            element: <ExplorePage />,
          },
          {
            path: "/about",
            element: <ExplorePage />,
          },
          // Add more pages here later (e.g. listing details, user profile)
          // to give them the same Navbar automatically.
        ],
      },
      {
        path: "login",
        element: <Login />,
      },
      {
        path: "signup",
        element: <Signup />,
      },
      {
        path: "*", // wildcard route
        element: <PageNotFound />,
      },
    ],
  },
]);

export default route;
