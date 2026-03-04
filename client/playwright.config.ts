import { defineConfig, devices } from "@playwright/test";
import { loadEnv } from "vite";

const env = loadEnv("", process.cwd(), "");

/**
 * 실행 환경 판별
 * - SMOKE_TEST=true : 배포 후 테스트 서버 대상
 * - CI=true         : PR CI 파이프라인
 * - 기본값          : 로컬 dev 서버
 */
const isSmoke = !!process.env.SMOKE_TEST;
const isCI = !!process.env.CI;

const LOCAL_DEV_URL = "http://localhost:5173";
const CI_PREVIEW_URL = "http://localhost:4173";

const baseURL = isSmoke
  ? env.VITE_APP_DOMAIN // 배포된 테스트 서버
  : isCI
    ? CI_PREVIEW_URL // CI: 빌드 후 preview 서버
    : LOCAL_DEV_URL; // 로컬: dev 서버

export default defineConfig({
  testDir: ".//src/__tests__",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 1 : undefined,
  reporter: "html",
  use: {
    baseURL,
    trace: "on-first-retry",
  },

  webServer: isSmoke
    ? undefined
    : isCI
      ? {
          // CI: 빌드 결과물을 preview 서버로 기동
          command: "npm run build && npm run preview",
          url: CI_PREVIEW_URL,
          reuseExistingServer: false,
          timeout: 120_000,
        }
      : {
          // 로컬: dev 서버 기동 (이미 실행 중이면 재사용)
          command: "npm run dev",
          url: LOCAL_DEV_URL,
          reuseExistingServer: true,
        },

  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },

    // {
    //   name: "webkit",
    //   use: { ...devices["Desktop Safari"] },
    // },

    /* Test against mobile viewports. */
    // {
    //   name: "Mobile Chrome",
    //   use: { ...devices["Pixel 5"] },
    // },
    // {
    //   name: "Mobile Safari",
    //   use: { ...devices["iPhone 12"] },
    // },

    /* Test against branded browsers. */
    // {
    //   name: "Microsoft Edge",
    //   use: { ...devices["Desktop Edge"], channel: "msedge" },
    // },
    // {
    //   name: "Google Chrome",
    //   use: { ...devices["Desktop Chrome"], channel: "chrome" },
    // },
  ],
});
