import path from "path";
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  return {
    plugins: [
      react({
        babel: {
          plugins: [["babel-plugin-react-compiler"]],
        },
      }),
      tailwindcss(),
    ],
    test: {
      globals: true,
      environment: "jsdom",
      setupFiles: "./src/__tests__/setup.ts",
    },
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    server: {
      proxy: {
        "/api": {
          target: `${env.VITE_API_DOMAIN}`,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  };
});
