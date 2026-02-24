import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E 测试配置
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './e2e',

  /* 并行运行测试 */
  fullyParallel: true,

  /* 失败时禁止并行 */
  forbidOnly: !!process.env.CI,

  /* 重试次数 */
  retries: process.env.CI ? 2 : 0,

  /* 并行 workers */
  workers: process.env.CI ? 1 : undefined,

  /* 报告器 */
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list']
  ],

  /* 共享配置 */
  use: {
    /* 基础 URL */
    baseURL: 'http://localhost:3002',

    /* 无头模式 */
    headless: true,

    /* 收集 trace */
    trace: 'on-first-retry',

    /* 截图 */
    screenshot: 'only-on-failure',

    /* 视频 */
    video: 'on-first-retry',
  },

  /* 项目配置 */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
  ],

  /* 本地开发服务器配置 */
  webServer: {
    command: 'npm run dev -- --port 3002',
    url: 'http://localhost:3002',
    reuseExistingServer: true,
    timeout: 120000,
  },
})
