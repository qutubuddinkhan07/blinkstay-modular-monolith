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

const App = () => {
  return (
    <Provider store={store}>
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
    </Provider>
  );
};

export default App;
