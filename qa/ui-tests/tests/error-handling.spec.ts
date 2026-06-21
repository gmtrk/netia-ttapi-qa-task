import { expect, test } from '@playwright/test';
import { createTicketViaUi } from '../support/flows';
import { uniqueExternalId } from '../support/data';

test('TC-UI-08 — a failed close action shows a readable error and leaves the ticket state unchanged', async ({ page }) => {
  const externalId = uniqueExternalId();
  const errorMessage = 'Nie można zamknąć zgłoszenia w bieżącym statusie';

  const detailPage = await createTicketViaUi(page, externalId, 'API error handling test ticket');
  await expect(detailPage.heading(externalId)).toBeVisible();
  await expect(detailPage.statusChip('Przyjęte')).toBeVisible();

  await page.route(`**/troubleTicket/${externalId}`, async (route) => {
    if (route.request().method() === 'PATCH') {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'STATUS_TRANSITION_ERROR', message: errorMessage }),
      });
      return;
    }
    await route.fallback();
  });

  await detailPage.close();

  await expect(detailPage.errorAlert.filter({ hasText: errorMessage })).toBeVisible({ timeout: 10_000 });
  await expect(detailPage.statusChip('Przyjęte')).toBeVisible();
  await expect(detailPage.closeButton).toBeVisible();
});
