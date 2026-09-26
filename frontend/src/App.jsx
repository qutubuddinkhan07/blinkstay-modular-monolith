import { RouterProvider } from "react-router-dom";
import route from "./routes/AppRoutes";
import { ToastContainer } from "react-toastify";
import { Suspense } from "react";
import Loading from "./components/common/Loading";
import { ThemeProvider } from "./context/ThemeContext";

const App = () => {
  return (
    <>
      <ThemeProvider>
        <Suspense fallback={<Loading />}>
          <RouterProvider router={route} />
        </Suspense>
        <ToastContainer />
      </ThemeProvider>
    </>
  );
};

export default App;
