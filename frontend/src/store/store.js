import { configureStore } from "@reduxjs/toolkit";
import notificationsReducer from "../features/notifications/notificationSlice";

export const store = configureStore({
  reducer: {
    notifications: notificationsReducer,
  },
});
