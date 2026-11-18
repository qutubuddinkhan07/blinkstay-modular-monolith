import { createApi, retry } from "@reduxjs/toolkit/query/react";
import { toListingRow } from "../features/listings/listingMappers";

// Render's free plan sleeps; while it starts, requests fail with no response or 502/503/504.
// Retry reads only (never mutations), waiting longer each time: about 3 minutes in total.
const WAKE_STATUSES = [null, 502, 503, 504];
const MAX_WAKE_RETRIES = 10;

const baseQueryWithWake = retry(axiosBaseQuery(), {
  maxRetries: MAX_WAKE_RETRIES,
  retryCondition: (error, _args, { attempt, baseQueryApi }) =>
    baseQueryApi.type === "query" &&
    WAKE_STATUSES.includes(error?.status) &&
    attempt <= MAX_WAKE_RETRIES,
  // 2s, 4s, 8s, then every 20s
  backoff: (attempt) =>
    new Promise((resolve) =>
      setTimeout(resolve, Math.min(2000 * 2 ** (attempt - 1), 20000)),
    ),
});

// leaves out empty values so the URL stays clean (?category= is dropped)
const clean = (params) =>
  Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== "" && v != null),
  );

const LISTINGS = "/v3/listings";

// everything that can show a listing: refetched whenever one changes
const listingChangedTags = (id) => [
  { type: "ListingDetail", id },
  { type: "MyListing", id },
  "Listing",
  "ManagerListings",
];

export const api = createApi({
  reducerPath: "api",
  baseQuery: baseQueryWithWake,
  // "Listing": the public lists (Explore). "MyListing": the host's own list.
  tagTypes: ["Listing", "MyListing", "ListingDetail", "ManagerListings"],
  endpoints: (builder) => {
    // POST /{id}/publish | pause | resume: the row updates at once, then the list is refetched to confirm
    const statusMutation = (action, nextStatus) =>
      builder.mutation({
        query: (id) => ({ url: `${LISTINGS}/${id}/${action}`, method: "post" }),
        invalidatesTags: (result, error, id) => [
          ...listingChangedTags(id),
          { type: "MyListing", id: "LIST" },
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

      // One listing. A non-published one only comes back for its owner and admins (404 for others)
      getListingById: builder.query({
        query: (id) => ({ url: `${LISTINGS}/${id}` }),
        transformResponse: (res) => res?.data,
        providesTags: (result, error, id) => [{ type: "ListingDetail", id }],
        keepUnusedDataFor: 30,
      }),

      // GET /manager/{managerId} (ADMIN)
      getListingsByManager: builder.query({
        query: (managerId) => ({ url: `${LISTINGS}/manager/${managerId}` }),
        transformResponse: (res) => (res?.data ?? []).map(toListingRow),
        providesTags: (result, error, managerId) => [
          { type: "ManagerListings", id: managerId },
        ],
        keepUnusedDataFor: 30,
      }),

      // POST /api/v3/admin/listings/{id}/suspend  (reason required, max 500)
      suspendListing: builder.mutation({
        query: ({ id, reason }) => ({
          url: `/v3/admin/listings/${id}/suspend`,
          method: "post",
          data: { reason },
        }),
        invalidatesTags: (result, error, { id }) => listingChangedTags(id),
      }),

      // POST /api/v3/admin/listings/{id}/unsuspend  (409 when the owner is blocked)
      unsuspendListing: builder.mutation({
        query: (id) => ({
          url: `/v3/admin/listings/${id}/unsuspend`,
          method: "post",
        }),
        invalidatesTags: (result, error, id) => listingChangedTags(id),
      }),

      publishListing: statusMutation("publish", "PUBLISHED"),
      pauseListing: statusMutation("pause", "PAUSED"),
      resumeListing: statusMutation("resume", "PUBLISHED"),

      // DELETE /{id}
      deleteListing: builder.mutation({
        query: (id) => ({ url: `${LISTINGS}/${id}`, method: "delete" }),
        invalidatesTags: (result, error, id) => [
          ...listingChangedTags(id),
          { type: "MyListing", id: "LIST" },
        ],
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
  useGetListingByIdQuery,
  useGetListingsByManagerQuery,
  usePublishListingMutation,
  usePauseListingMutation,
  useResumeListingMutation,
  useDeleteListingMutation,
  useSuspendListingMutation,
  useUnsuspendListingMutation,
} = api;
