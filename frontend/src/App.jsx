import { RouterProvider } from "react-router-dom";
import route from "./routes/AppRoutes";
import { ToastContainer } from "react-toastify";
import { Suspense } from "react";
import Loading from "./components/common/Loading";
import { ThemeProvider } from "./context/ThemeContext";
import { AuthProvider } from "./context/AuthContext";

const App = () => {
  return (
    <>
      <AuthProvider>
        <ThemeProvider>
          <Suspense fallback={<Loading />}>
            <RouterProvider router={route} />
          </Suspense>
          <ToastContainer />
        </ThemeProvider>
      </AuthProvider>
    </>
  );
};

export default App;
