import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    host: "0.0.0.0",
    port: 8080,
    hmr: {
      overlay: false,
    },
    proxy: {
      "/api": {
        // Dev: SSH tunnel → ssh -L 8000:localhost:8000 photogeniusai@34.134.231.111
        // Then set target to http://localhost:8000
        // Prod VM: http://34.134.231.111:8000
        target: process.env.VITE_API_TARGET || "http://34.134.231.111:8000",
        changeOrigin: true,
      },
    },
  },
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
