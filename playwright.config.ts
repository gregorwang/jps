import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 45_000,
  fullyParallel: false,
  reporter: 'list',
  use: {
    ...devices['iPad (gen 7) landscape'],
    baseURL: 'http://127.0.0.1:5173',
    serviceWorkers: 'allow',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'pnpm dev -- --port 5173',
    url: 'http://127.0.0.1:5173',
    timeout: 60_000,
    reuseExistingServer: !process.env.CI,
  },
})
