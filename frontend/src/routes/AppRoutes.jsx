import { createBrowserRouter } from "react-router-dom";
// import Signup from "../features/auth/pages/Signup";
// import Login from "../features/auth/pages/Login";
// import ListingHome from "../features/listings/ListingHome";
// import ErrorPage from "../components/common/ErrorPage";
// import PageNotFound from "../components/common/PageNotFound";
import { lazy } from "react";
import Layout from "../components/layouts/Layout";
import ProtectedRoute from "./ProtectedRoute";

const Signup = lazy(() => import("../features/auth/pages/Signup"));
const Login = lazy(() => import("../features/auth/pages/Login"));
const ExplorePage = lazy(
  () => import("../features/listings/pages/ExplorePage"),
);
const ErrorPage = lazy(() => import("../components/common/ErrorPage"));
const PageNotFound = lazy(() => import("../components/common/PageNotFound"));

//! Protected pages (FUTURE & CURRENT)
const ProfilePage = lazy(() => import("../features/auth/pages/ProfilePage"));
const CreateListing = lazy(
  () => import("../features/listings/pages/CreateListing"),
);
const MyListings = lazy(() => import("../features/listings/pages/MyListings"));

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
          //? -----------------------------------------------------------
          //? PUBLIC ROUTES (Anyone can view)
          //? -----------------------------------------------------------
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

          //? -----------------------------------------------------------
          //? PROTECTED ROUTES (Requires active BLINKSTAY_TOKEN cookie)
          //? -----------------------------------------------------------
          {
            element: <ProtectedRoute />, //! All children inside this object are guarded
            children: [
              {
                path: "/profile",
                element: <ProfilePage />,
              },
              {
                path: "/my-listings",
                element: <MyListings />,
              },
              {
                path: "/create-listing",
                element: <CreateListing />,
              },
            ],
          },
        ],
      },

      // Unprotected standalone pages (without Navbar/Layout)
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
