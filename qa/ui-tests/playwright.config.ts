import { defineConfig, devices } from '@playwright/test';
import { uiBaseUrl, storageStatePath } from './support/config';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['allure-playwright', { resultsDir: 'allure-results' }],
  ],
  use: {
    baseURL: uiBaseUrl,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
    },
    {
      name: 'UI',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        storageState: storageStatePath,
      },
      dependencies: ['setup'],
    },
  ],
});
