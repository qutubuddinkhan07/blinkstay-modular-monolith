import { RouterProvider } from "react-router-dom";
import route from "./routes/AppRoutes";

const App = () => {
  return (
    <>
      <RouterProvider router={route} />
    </>
  );
};

export default App;
