import { expect, test } from '@playwright/test';
import { KeycloakLoginPage } from '../pages/KeycloakLoginPage';
import { alpha } from '../support/config';

const invalidLogins = [
  { label: 'wrong password', username: alpha.username, password: 'definitely-wrong', expectsGenericError: true },
  { label: 'unknown user', username: 'no-such-user', password: 'Whatever1234!', expectsGenericError: true },
  { label: 'empty password', username: alpha.username, password: '', expectsGenericError: false },
  { label: 'empty username', username: '', password: 'Whatever1234!', expectsGenericError: false },
];

test.describe('Login', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('TC-UI-01 — valid credentials log in and show the ticket list', async ({ page }) => {
    await page.goto('/');

    const loginPage = new KeycloakLoginPage(page);
    const listPage = await loginPage.signIn(alpha);

    await expect(listPage.heading).toBeVisible();
    await expect(listPage.newTicketButton).toBeVisible();
  });

  for (const credentials of invalidLogins) {
    test(`TC-UI-03 — invalid credentials are rejected: ${credentials.label}`, async ({ page }) => {
      await page.goto('/');

      const loginPage = new KeycloakLoginPage(page);
      const listPage = await loginPage.signIn(credentials);

      if (credentials.expectsGenericError) {
        await expect(loginPage.errorMessage).toBeVisible();
      }
      await expect(loginPage.passwordInput).toBeVisible();
      await expect(listPage.newTicketButton).toBeHidden();
    });
  }
});
