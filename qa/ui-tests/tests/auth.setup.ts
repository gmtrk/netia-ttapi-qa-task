import { test as setup, expect } from '@playwright/test';
import { alpha, storageStatePath } from '../support/config';
import { KeycloakLoginPage } from '../pages/KeycloakLoginPage';

setup('authenticate as alpha', async ({ page }) => {
  await page.goto('/');

  const loginPage = new KeycloakLoginPage(page);
  const listPage = await loginPage.signIn(alpha);
  await expect(listPage.newTicketButton).toBeVisible();

  await page.context().storageState({ path: storageStatePath });
});
