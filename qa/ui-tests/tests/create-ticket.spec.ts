import { expect, test } from '@playwright/test';
import { TicketListPage } from '../pages/TicketListPage';
import { CreateTicketPage } from '../pages/CreateTicketPage';
import { createTicketViaUi } from '../support/flows';
import { uniqueExternalId } from '../support/data';

test('TC-UI-04 — creating a ticket shows it on the list with its status', async ({ page }) => {
  const externalId = uniqueExternalId();

  const detailPage = await createTicketViaUi(page, externalId, 'Created by UI E2E test');
  await expect(detailPage.heading(externalId)).toBeVisible();

  const listPage = await TicketListPage.open(page);
  await expect(listPage.row(externalId)).toBeVisible();
  await expect(listPage.row(externalId)).toContainText('Przyjęte');
});

test('TC-UI-05 — empty create form submission shows client-side validation', async ({ page }) => {
  const createPage = await CreateTicketPage.open(page);

  await createPage.submit();

  await expect(createPage.requiredFieldError.first()).toBeVisible();
  await expect(page).toHaveURL(/\/tickets\/new$/);
});

test('TC-UI-09 — re-creating with an existing externalId shows the original ticket without duplicating it', async ({ page }) => {
  const externalId = uniqueExternalId();
  const originalDescription = 'Original ticket description';
  const ignoredDescription = 'Changed description that must be ignored';

  const firstDetail = await createTicketViaUi(page, externalId, originalDescription);
  await expect(firstDetail.heading(externalId)).toBeVisible();

  const secondDetail = await createTicketViaUi(page, externalId, ignoredDescription);
  await expect(secondDetail.heading(externalId)).toBeVisible();
  await expect(secondDetail.description(originalDescription)).toBeVisible();
  await expect(secondDetail.description(ignoredDescription)).toBeHidden();

  const listPage = await TicketListPage.open(page);
  await expect(listPage.row(externalId)).toHaveCount(1);
});
