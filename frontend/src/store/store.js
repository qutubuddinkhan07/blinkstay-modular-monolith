import { configureStore } from "@reduxjs/toolkit";
import notificationsReducer from "../features/notifications/notificationsSlice";
import { api } from "./api";

export const store = configureStore({
  reducer: {
    notifications: notificationsReducer,
    [api.reducerPath]: api.reducer,
  },
  // the api middleware handles caching, de-duplication and cache expiry
  middleware: (getDefault) => getDefault().concat(api.middleware),
});
