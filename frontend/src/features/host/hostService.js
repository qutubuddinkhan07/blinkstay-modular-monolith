import axiosInstance from "../../api/axiosInstance";

const REQUESTS = "/v2/host-requests";
const PROFILE = "/v2/hosts/me";

// data is null when the user has never applied
export const fetchMyHostRequest = () => axiosInstance.get(`${REQUESTS}/me`);

// body: { businessName, address, description, website? }
export const applyForHost = (body) => axiosInstance.post(REQUESTS, body);

export const cancelMyHostRequest = () => axiosInstance.delete(`${REQUESTS}/me`);

// data can be null for hosts that were added by hand
export const fetchHostProfile = () => axiosInstance.get(PROFILE);

export const updateHostProfile = (changes) =>
  axiosInstance.patch(PROFILE, changes);
