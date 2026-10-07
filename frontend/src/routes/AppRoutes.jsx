import { createBrowserRouter } from "react-router-dom";
import { lazy, Suspense } from "react";
import Layout from "../components/layouts/Layout";
import ProtectedRoute from "./ProtectedRoute";
import Loading from "../components/common/Loading";

const Loadable = (Component) => (props) => (
  <Suspense fallback={<Loading />}>
    <Component {...props} />
  </Suspense>
);

// Public
const Signup = Loadable(lazy(() => import("../features/auth/pages/Signup")));
const Login = Loadable(lazy(() => import("../features/auth/pages/Login")));
const ExplorePage = Loadable(
  lazy(() => import("../features/listings/pages/ExplorePage")),
);
const Listing = Loadable(
  lazy(() => import("../features/listings/pages/Listing")),
);

// Wrapped too: a lazy component rendered without <Suspense> can break navigation
const ErrorPage = Loadable(
  lazy(() => import("../components/common/ErrorPage")),
);
const PageNotFound = Loadable(
  lazy(() => import("../components/common/PageNotFound")),
);

// Logged-in users
const ProfilePage = Loadable(
  lazy(() => import("../features/auth/pages/ProfilePage")),
);

// Hotel manager / admin
const MyListings = Loadable(
  lazy(() => import("../features/listings/pages/MyListings")),
);
const CreateListing = Loadable(
  lazy(() => import("../features/listings/pages/CreateListing")),
);
const EditListing = Loadable(
  lazy(() => import("../features/listings/pages/EditListing")),
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
            element: <ProtectedRoute />,
            children: [
              {
                path: "/profile",
                element: <ProfilePage />,
              },
            ],
          },

          //? HOTEL MANAGER or ADMIN
          {
            element: (
              <ProtectedRoute allowedRoles={["HOTEL_MANAGER", "ADMIN"]} />
            ),
            children: [
              {
                path: "/create-listing",
                element: <CreateListing />,
              },
              // param is named listingId because EditListing reads useParams().listingId
              {
                path: "listings/:listingId/edit",
                element: <EditListing />,
              },
            ],
          },

          //? HOTEL MANAGER only (GET /my-listings is manager-only in the backend)
          {
            element: <ProtectedRoute allowedRoles={["HOTEL_MANAGER"]} />,
            children: [
              {
                path: "/my-listings",
                element: <MyListings />,
              },
            ],
          },

          //? ADMIN only (add admin pages here later)
          // {
          //   element: <ProtectedRoute allowedRoles={["ADMIN"]} />,
          //   children: [{ path: "/admin", element: <AdminDashboard /> }],
          // },
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
