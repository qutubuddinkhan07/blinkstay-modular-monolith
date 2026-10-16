import { RouterProvider } from "react-router-dom";
import route from "./routes/AppRoutes";
import { Suspense } from "react";
import Loading from "./components/common/Loading";
import { ThemeProvider } from "./context/ThemeContext";
import { AuthProvider } from "./context/AuthContext";
import ThemedToaster from "./components/common/ThemedToaster";
import { FavoritesProvider } from "./context/FavoriteContext";
import { Provider } from "react-redux";
import { store } from "./store/store";
import { MotionConfig } from "motion/react";

const App = () => {
  return (
    <Provider store={store}>
      <ThemeProvider>
        <ThemedToaster />
        <AuthProvider>
          <FavoritesProvider>
            <Suspense fallback={<Loading />}>
              <MotionConfig reducedMotion="user">
                <RouterProvider router={route} />
              </MotionConfig>
            </Suspense>
          </FavoritesProvider>
        </AuthProvider>
      </ThemeProvider>
    </Provider>
  );
};

export default App;
