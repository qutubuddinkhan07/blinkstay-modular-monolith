import { RouterProvider } from "react-router-dom";
import route from "./routes/AppRoutes";
import { ToastContainer } from "react-toastify";

const App = () => {
  return (
    <>
      <RouterProvider router={route} />
      <ToastContainer />
    </>
  );
};

export default App;
