import { createApi } from "@reduxjs/toolkit/query/react";
import axiosInstance from "../api/axiosInstance";
import { handleApiError } from "../api/errors/handleApiError";
import { toListingRow } from "../features/listings/listingMappers";

const axiosBaseQuery =
  () =>
  async ({ url, method = "get", data, params }) => {
    try {
      const res = await axiosInstance({ url, method, data, params });
      return { data: res.data };
    } catch (err) {
      return {
        error: {
          status: err.response?.status ?? null,
          message: handleApiError(err),
        },
      };
    }
  };

// leaves out empty values so the URL stays clean (?category= is dropped)
const clean = (params) =>
  Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== "" && v != null),
  );

const LISTINGS = "/v3/listings";

export const api = createApi({
  reducerPath: "api",
  baseQuery: axiosBaseQuery(),
  // "Listing": the public lists (Explore). "MyListing": the host's own list.
  tagTypes: ["Listing", "MyListing"],
  endpoints: (builder) => {
    // POST /{id}/publish | pause | resume: the row updates at once, then the list is refetched to confirm
    const statusMutation = (action, nextStatus) =>
      builder.mutation({
        query: (id) => ({ url: `${LISTINGS}/${id}/${action}`, method: "post" }),
        invalidatesTags: (result, error, id) => [
          { type: "MyListing", id },
          "Listing",
        ],
        async onQueryStarted(id, { dispatch, queryFulfilled }) {
          try {
            await queryFulfilled;
            dispatch(
              api.util.updateQueryData("getMyListings", undefined, (draft) => {
                const row = draft.find((l) => l.id === id);
                if (!row) return;
                row.status = nextStatus;
                row.suspensionReason = null;
                row.suspendedAt = null;
              }),
            );
          } catch {
            // the caller shows the error
          }
        },
      });

    return {
      // data: { content, totalPages, ... } (Explore)
      getListings: builder.query({
        query: (params) => ({ url: `${LISTINGS}/all`, params: clean(params) }),
        providesTags: ["Listing"],
        keepUnusedDataFor: 30, // seconds a result stays cached after the page is left
      }),

      // data: the host's listings, shaped for the cards (needs HOTEL_MANAGER)
      getMyListings: builder.query({
        query: () => ({ url: `${LISTINGS}/my-listings` }),
        transformResponse: (res) => (res?.data ?? []).map(toListingRow),
        providesTags: (result) => [
          { type: "MyListing", id: "LIST" },
          ...(result ?? []).map((l) => ({ type: "MyListing", id: l.id })),
        ],
        keepUnusedDataFor: 30,
      }),

      publishListing: statusMutation("publish", "PUBLISHED"),
      pauseListing: statusMutation("pause", "PAUSED"),
      resumeListing: statusMutation("resume", "PUBLISHED"),

      // DELETE /{id}
      deleteListing: builder.mutation({
        query: (id) => ({ url: `${LISTINGS}/${id}`, method: "delete" }),
        invalidatesTags: [{ type: "MyListing", id: "LIST" }, "Listing"],
        async onQueryStarted(id, { dispatch, queryFulfilled }) {
          try {
            await queryFulfilled;
            dispatch(
              api.util.updateQueryData("getMyListings", undefined, (draft) =>
                draft.filter((l) => l.id !== id),
              ),
            );
          } catch {
            // the caller shows the error
          }
        },
      }),
    };
  },
});

export const {
  useGetListingsQuery,
  useGetMyListingsQuery,
  usePublishListingMutation,
  usePauseListingMutation,
  useResumeListingMutation,
  useDeleteListingMutation,
} = api;
