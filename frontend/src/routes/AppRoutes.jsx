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
  lazy(() => import("../features/user/pages/ProfilePage")),
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

const GuestRoute = Loadable(lazy(() => import("./GuestRoute")));

const ComingSoon = Loadable(
  lazy(() => import("../components/common/ComingSoon")),
);

const BlockedPage = Loadable(
  lazy(() => import("../features/auth/pages/BlockedPage")),
);

const AdminUsers = Loadable(
  lazy(() => import("../features/admin/pages/AdminUsers")),
);

const ManagerListings = Loadable(
  lazy(() => import("../features/admin/pages/ManagerListings")),
);

const BecomeHost = Loadable(
  lazy(() => import("../features/host/pages/BecomeHost")),
);

const BusinessProfile = Loadable(
  lazy(() => import("../features/host/pages/BusinessProfile")),
);

const AdminHostRequests = Loadable(
  lazy(() => import("../features/admin/pages/AdminHostRequests")),
);

const ForgotPassword = Loadable(
  lazy(() => import("../features/auth/pages/ForgotPassword")),
);

const NotificationsPage = Loadable(
  lazy(() => import("../features/notifications/pages/NotificationsPage")),
);

const Home = Loadable(lazy(() => import("../features/home/pages/Home")));

const About = Loadable(lazy(() => import("../features/about/pages/About")));

const FavoritePage = Loadable(
  lazy(() => import("../features/favorites/pages/FavoritePage")),
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
            element: <Home />,
          },
          {
            path: "/explore",
            element: <ExplorePage />,
          },
          {
            path: "/listings/:id",
            element: <Listing />,
          },
          {
            path: "/about",
            element: <About />,
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
              {
                path: "/bookings",
                element: <ComingSoon title="My Bookings" />,
              },
              {
                path: "/favorites",
                element: <FavoritePage />,
              },
              {
                path: "/become-host",
                element: <BecomeHost />,
              },
              {
                path: "/notifications",
                element: <NotificationsPage />,
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
              {
                path: "/manager/reservations",
                element: <ComingSoon title="Reservations" />,
              },
              { path: "/host/business", element: <BusinessProfile /> },
            ],
          },

          //? ADMIN only
          {
            element: <ProtectedRoute allowedRoles={["ADMIN"]} />,
            children: [
              {
                path: "/admin",
                element: <ComingSoon title="Admin dashboard" />,
              },
              {
                path: "/admin/users",
                element: <AdminUsers />,
              },
              {
                path: "/admin/users/:managerId/listings",
                element: <ManagerListings />,
              },
              {
                path: "/admin/host-requests",
                element: <AdminHostRequests />,
              },
            ],
          },
        ],
      },

      // Unprotected standalone pages (without Navbar/Layout)
      {
        element: <GuestRoute />,
        children: [
          { path: "login", element: <Login /> },
          { path: "signup", element: <Signup /> },
          { path: "forgot-password", element: <ForgotPassword /> },
        ],
      },
      { path: "blocked", element: <BlockedPage /> },
      {
        path: "*", // wildcard route
        element: <PageNotFound />,
      },
    ],
  },
]);

export default route;
