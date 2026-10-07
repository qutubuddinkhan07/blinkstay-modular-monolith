import { createBrowserRouter } from "react-router-dom";
import { lazy, Suspense } from "react";
import Layout from "../components/layouts/Layout";
import ProtectedRoute from "./ProtectedRoute";
import Loading from "../components/common/Loading";

// 1. Declare Loadable FIRST before invoking it below
const Loadable = (Component) => (props) => (
  <Suspense fallback={<Loading />}>
    <Component {...props} />
  </Suspense>
);

// 2. Wrap all lazy-loaded pages consistently
const Signup = Loadable(lazy(() => import("../features/auth/pages/Signup")));
const Login = Loadable(lazy(() => import("../features/auth/pages/Login")));
const ExplorePage = Loadable(
  lazy(() => import("../features/listings/pages/ExplorePage")),
);
const ErrorPage = lazy(() => import("../components/common/ErrorPage"));
const PageNotFound = lazy(() => import("../components/common/PageNotFound"));
const Listing = Loadable(
  lazy(() => import("../features/listings/pages/Listing")),
);

//! Protected pages (FUTURE & CURRENT)
const ProfilePage = Loadable(
  lazy(() => import("../features/auth/pages/ProfilePage")),
);
const CreateListing = Loadable(
  lazy(() => import("../features/listings/pages/CreateListing")),
);
const MyListings = Loadable(
  lazy(() => import("../features/listings/pages/MyListings")),
);

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
            path: "/listings/:id",
            element: <Listing />,
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
