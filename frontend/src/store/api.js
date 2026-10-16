import { createApi } from "@reduxjs/toolkit/query/react";
import axiosInstance from "../api/axiosInstance";
import { handleApiError } from "../api/errors/handleApiError";

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

export const api = createApi({
  reducerPath: "api",
  baseQuery: axiosBaseQuery(),
  tagTypes: ["Listing"], // used later so a publish or a pause can refresh the lists
  endpoints: (builder) => ({
    // data: { content, totalPages, ... }, the same shape Explore used before
    getListings: builder.query({
      query: (params) => ({
        url: "/v3/listings/all",
        params: clean(params),
      }),
      providesTags: ["Listing"],
      keepUnusedDataFor: 30, // seconds a result stays cached after the page is left
    }),
  }),
});

export const { useGetListingsQuery } = api;
