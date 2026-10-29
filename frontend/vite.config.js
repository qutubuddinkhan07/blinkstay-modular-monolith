import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";

//! Configuration of devlopement with ngrok & localhost backend
// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],

  server: {
    host: "0.0.0.0",
    allowedHosts: [".ngrok-free.app", ".trycloudflare.com"],
    hmr: { clientPort: 443 },

    // proxy: {
    //   "/api": {
    //     target: "http://localhost:8080",
    //     changeOrigin: true,
    //   },
    // },
    proxy: {
      "/api": {
        target:
          "https://committee-likelihood-brings-remarkable.trycloudflare.com", // always changing

        changeOrigin: true,
      },
    },
  },
});
