import { expect, test } from '@playwright/test';
import { TicketListPage } from '../pages/TicketListPage';

test('TC-UI-02 — authenticated ticket list renders its columns', async ({ page }) => {
  const listPage = new TicketListPage(page);
  await listPage.goto();

  await expect(listPage.heading).toBeVisible();
  await expect(listPage.newTicketButton).toBeVisible();
  await expect(listPage.columnHeader('ID zewnętrzny')).toBeVisible();
  await expect(listPage.columnHeader('Status')).toBeVisible();
});
