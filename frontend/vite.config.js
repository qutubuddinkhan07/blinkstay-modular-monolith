import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";
import tailwindcss from "@tailwindcss/vite";

//! Configuration of development with cloudflare tunnel & localhost backend
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 1. Load environment variables into 'env' based on the current mode and working directory
  const env = loadEnv(mode, process.cwd(), "");

  return {
    plugins: [react(), tailwindcss()],

    server: {
      host: "0.0.0.0",
      allowedHosts: [".ngrok-free.app", ".trycloudflare.com"],

      proxy: {
        "/api": {
          // 2. Uses your Cloudflare tunnel from .env, or falls back to localhost
          target: env.VITE_API_CLOUDFLARED_TUNNEL || "http://localhost:8090",
          changeOrigin: true,
          // 3. Required for HTTPS Cloudflare tunnels to prevent SSL rejection errors
          secure: false,
        },
      },
    },
  };
});
