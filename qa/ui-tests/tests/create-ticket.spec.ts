import { expect, test } from '@playwright/test';
import { TicketListPage } from '../pages/TicketListPage';
import { CreateTicketPage } from '../pages/CreateTicketPage';
import { createTicketViaUi } from '../support/flows';
import { uniqueExternalId } from '../support/data';

test('TC-UI-04 — creating a ticket shows it on the list with its status', async ({ page }) => {
  const externalId = uniqueExternalId();

  const detailPage = await createTicketViaUi(page, externalId, 'Created by UI E2E test');
  await expect(detailPage.heading(externalId)).toBeVisible();

  const listPage = new TicketListPage(page);
  await listPage.goto();
  await expect(listPage.row(externalId)).toBeVisible();
  await expect(listPage.row(externalId)).toContainText('Przyjęte');
});

test('TC-UI-05 — empty create form submission shows client-side validation', async ({ page }) => {
  const createPage = new CreateTicketPage(page);
  await createPage.goto();

  await createPage.submit();

  await expect(createPage.requiredFieldError.first()).toBeVisible();
  await expect(page).toHaveURL(/\/tickets\/new$/);
});
