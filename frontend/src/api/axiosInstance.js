import axios from "axios";

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // Crucial: Allows browser to send HttpOnly BLINKSTAY_TOKEN
  headers: {
    "ngrok-skip-browser-warning": "true",
  },
});

export default axiosInstance;
