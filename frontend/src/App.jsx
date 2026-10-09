import { RouterProvider } from "react-router-dom";
import route from "./routes/AppRoutes";
import { Suspense } from "react";
import Loading from "./components/common/Loading";
import { ThemeProvider } from "./context/ThemeContext";
import { AuthProvider } from "./context/AuthContext";
import ThemedToaster from "./components/common/ThemedToaster";

const App = () => {
  return (
    <>
      <ThemeProvider>
        <AuthProvider>
          <Suspense fallback={<Loading />}>
            <RouterProvider router={route} />
            <ThemedToaster />
          </Suspense>
        </AuthProvider>
      </ThemeProvider>
    </>
  );
};

export default App;
