import { RouterProvider } from "react-router-dom";
import route from "./routes/AppRoutes";
import { Suspense } from "react";
import Loading from "./components/common/Loading";
import { ThemeProvider } from "./context/ThemeContext";
import { AuthProvider } from "./context/AuthContext";
import ThemedToaster from "./components/common/ThemedToaster";
import { FavoritesProvider } from "./context/FavoriteContext";

const App = () => {
  return (
    <ThemeProvider>
      <ThemedToaster />
      <AuthProvider>
        <FavoritesProvider>
          <Suspense fallback={<Loading />}>
            <RouterProvider router={route} />
          </Suspense>
        </FavoritesProvider>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
