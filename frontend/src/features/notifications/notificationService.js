import axiosInstance from "../../api/axiosInstance";

const BASE = "/v2/notifications";

// Fired whenever something is marked read, so the bell and the full page stay in step
export const NOTIFICATIONS_CHANGED = "notifications:changed";

// data: { content, page, size, totalElements, totalPages }, newest first
export const fetchNotifications = ({
  page = 0,
  size = 5,
  unreadOnly = false,
} = {}) =>
  axiosInstance.get(BASE, {
    params: { page, size, ...(unreadOnly && { unreadOnly: true }) },
  });

// data: a number
export const fetchUnreadCount = () => axiosInstance.get(`${BASE}/unread-count`);

export const markRead = (id) => axiosInstance.patch(`${BASE}/${id}/read`);

// data: how many were updated
export const markAllRead = () => axiosInstance.patch(`${BASE}/read-all`);

export const deleteNotification = (id) => axiosInstance.delete(`${BASE}/${id}`);

// deletes every read notification (no parameter)
export const clearReadNotifications = () => axiosInstance.delete(BASE);

// deletes everything, unread included
export const clearAllNotifications = () =>
  axiosInstance.delete(BASE, { params: { scope: "all" } });
