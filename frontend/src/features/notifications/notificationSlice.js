import { createAsyncThunk, createSlice, isAnyOf } from "@reduxjs/toolkit";
import { handleApiError } from "../../api/errors/handleApiError";
import * as api from "./notificationService";

const PREVIEW_SIZE = 5;
export const LIST_PAGE_SIZE = 15;

// Every failure becomes a plain { message, status } payload
const fail = (err, rejectWithValue) =>
  rejectWithValue({
    message: handleApiError(err),
    status: err.response?.status ?? null,
  });

/* ------------------------------ thunks: reading ------------------------------ */

export const fetchUnreadCount = createAsyncThunk(
  "notifications/fetchUnreadCount",
  async (_, { getState, rejectWithValue }) => {
    const before = getState().notifications;
    try {
      const res = await api.fetchUnreadCount();
      const count = Number(res.data?.data) || 0;
      // `increased` tells the bell that something new arrived (roles may have changed)
      return {
        count,
        increased: before.countLoaded && count > before.unreadCount,
      };
    } catch (err) {
      return fail(err, rejectWithValue);
    }
  },
);

// The dropdown: the latest few
export const fetchPreview = createAsyncThunk(
  "notifications/fetchPreview",
  async (_, { rejectWithValue }) => {
    try {
      const res = await api.fetchNotifications({ page: 0, size: PREVIEW_SIZE });
      return res.data.data?.content ?? [];
    } catch (err) {
      return fail(err, rejectWithValue);
    }
  },
);

// The full page. silent = refill without showing the skeleton
export const fetchList = createAsyncThunk(
  "notifications/fetchList",
  async ({ page, filter }, { rejectWithValue }) => {
    try {
      const res = await api.fetchNotifications({
        page,
        size: LIST_PAGE_SIZE,
        unreadOnly: filter === "unread",
      });
      const d = res.data.data ?? {};
      return {
        items: d.content ?? [],
        totalPages: d.totalPages ?? 0,
        totalElements: d.totalElements ?? null,
      };
    } catch (err) {
      return fail(err, rejectWithValue);
    }
  },
);

/* ----------------------------- thunks: changing ----------------------------- */

export const markNotificationRead = createAsyncThunk(
  "notifications/markRead",
  async (n, { dispatch, rejectWithValue }) => {
    try {
      await api.markRead(n.id);
      dispatch(fetchUnreadCount()); // confirm the badge with the server
    } catch (err) {
      return fail(err, rejectWithValue);
    }
  },
);

export const markAllNotificationsRead = createAsyncThunk(
  "notifications/markAllRead",
  async (_, { dispatch, rejectWithValue }) => {
    try {
      await api.markAllRead();
      dispatch(fetchUnreadCount());
    } catch (err) {
      return fail(err, rejectWithValue);
    }
  },
);

export const deleteNotification = createAsyncThunk(
  "notifications/delete",
  async (n, { dispatch, rejectWithValue }) => {
    try {
      await api.deleteNotification(n.id);
    } catch (err) {
      // 404 means it was already gone (deleted in another tab): that counts as done
      if (err.response?.status !== 404) return fail(err, rejectWithValue);
    } finally {
      dispatch(fetchUnreadCount());
    }
  },
);

export const clearReadNotifications = createAsyncThunk(
  "notifications/clearRead",
  async (_, { dispatch, rejectWithValue }) => {
    try {
      await api.clearReadNotifications();
      dispatch(fetchUnreadCount());
    } catch (err) {
      return fail(err, rejectWithValue);
    }
  },
);

export const clearAllNotifications = createAsyncThunk(
  "notifications/clearAll",
  async (_, { dispatch, rejectWithValue }) => {
    try {
      await api.clearAllNotifications();
      dispatch(fetchUnreadCount());
    } catch (err) {
      return fail(err, rejectWithValue);
    }
  },
);

/* ---------------------------------- slice ---------------------------------- */

const initialState = {
  unreadCount: 0,
  countLoaded: false, // false until the first count arrives
  version: 0, // bumped after every successful change; mounted lists refetch when it moves
  preview: { items: null, status: "idle", error: null }, // items stays null until first load
  list: {
    items: [],
    status: "idle", // "idle" | "loading" | "succeeded" | "failed"
    error: null,
    totalPages: 0,
    totalElements: null,
    requestId: null, // only the latest request may write the list
  },
};

// the same notification can sit in both lists
const setRead = (state, id, read) => {
  [state.preview.items ?? [], state.list.items].forEach((items) => {
    const found = items.find((i) => i.id === id);
    if (found) found.read = read;
  });
};

const removeFromLists = (state, id) => {
  if (state.preview.items) {
    state.preview.items = state.preview.items.filter((i) => i.id !== id);
  }
  const before = state.list.items.length;
  state.list.items = state.list.items.filter((i) => i.id !== id);
  if (state.list.items.length < before && state.list.totalElements != null) {
    state.list.totalElements -= 1;
  }
};

const bump = (state) => {
  state.version += 1;
};

const notificationsSlice = createSlice({
  name: "notifications",
  initialState,
  reducers: {
    // called on logout so one user's notifications never show for the next login
    notificationsReset: () => initialState,
  },
  extraReducers: (builder) => {
    builder
      /* count */
      .addCase(fetchUnreadCount.fulfilled, (state, { payload }) => {
        state.unreadCount = payload.count;
        state.countLoaded = true;
      })

      /* dropdown */
      .addCase(fetchPreview.pending, (state) => {
        state.preview.status = "loading";
        state.preview.error = null;
      })
      .addCase(fetchPreview.fulfilled, (state, { payload }) => {
        state.preview.items = payload;
        state.preview.status = "succeeded";
      })
      .addCase(fetchPreview.rejected, (state, { payload }) => {
        state.preview.status = "failed";
        state.preview.error =
          payload?.message ?? "Couldn't load notifications.";
      })

      /* full page */
      .addCase(fetchList.pending, (state, { meta }) => {
        state.list.requestId = meta.requestId;
        state.list.error = null;
        if (!meta.arg.silent) state.list.status = "loading";
      })
      .addCase(fetchList.fulfilled, (state, { payload, meta }) => {
        if (state.list.requestId !== meta.requestId) return; // a newer request is in flight
        state.list.items = payload.items;
        state.list.totalPages = payload.totalPages;
        state.list.totalElements = payload.totalElements;
        state.list.status = "succeeded";
      })
      .addCase(fetchList.rejected, (state, { payload, meta }) => {
        if (state.list.requestId !== meta.requestId) return;
        if (meta.arg.silent && state.list.status !== "loading") return; // quiet refill: keep what's shown
        state.list.status = "failed";
        state.list.error = payload?.message ?? "Couldn't load notifications.";
      })

      /* mark one read: optimistic, rolled back on failure */
      .addCase(markNotificationRead.pending, (state, { meta }) => {
        setRead(state, meta.arg.id, true);
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      })
      .addCase(markNotificationRead.fulfilled, bump)
      .addCase(markNotificationRead.rejected, (state, { meta }) => {
        setRead(state, meta.arg.id, false);
        state.unreadCount += 1;
      })

      /* mark all read */
      .addCase(markAllNotificationsRead.fulfilled, (state) => {
        [state.preview.items ?? [], state.list.items].forEach((items) =>
          items.forEach((i) => {
            i.read = true;
          }),
        );
        state.unreadCount = 0;
        state.version += 1;
      })

      /* delete: optimistic; the refetch after it restores the row if it failed */
      .addCase(deleteNotification.pending, (state, { meta }) => {
        removeFromLists(state, meta.arg.id);
        if (!meta.arg.read)
          state.unreadCount = Math.max(0, state.unreadCount - 1);
      })

      /* clear */
      .addCase(clearReadNotifications.fulfilled, bump)
      .addCase(clearAllNotifications.fulfilled, (state) => {
        state.unreadCount = 0;
        if (state.preview.items) state.preview.items = [];
        state.list.items = [];
        state.list.totalElements = 0;
        state.list.totalPages = 0;
        state.version += 1;
      })

      // matchers must come after every addCase
      .addMatcher(
        isAnyOf(deleteNotification.fulfilled, deleteNotification.rejected),
        bump,
      );
  },
});

export const { notificationsReset } = notificationsSlice.actions;

export const selectUnreadCount = (state) => state.notifications.unreadCount;
export const selectVersion = (state) => state.notifications.version;
export const selectPreview = (state) => state.notifications.preview;
export const selectList = (state) => state.notifications.list;

export default notificationsSlice.reducer;
