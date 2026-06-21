import { expect, test } from '@playwright/test';
import { TicketListPage } from '../pages/TicketListPage';
import { TicketDetailPage } from '../pages/TicketDetailPage';
import { ACKNOWLEDGED_SERVICE_ID, uniqueExternalId } from '../support/data';

test('TC-UI-10 — an action after access-token expiry refreshes the session transparently', async ({ page }) => {
  await page.clock.install();
  await page.goto(TicketListPage.path);

  const listPage = new TicketListPage(page);
  await expect(listPage.newTicketButton).toBeVisible();

  let refreshCount = 0;
  await page.route('**/protocol/openid-connect/token', async (route) => {
    if ((route.request().postData() ?? '').includes('grant_type=refresh_token')) {
      refreshCount += 1;
    }
    await route.continue();
  });

  await page.clock.fastForward('01:00:30');

  const createPage = await listPage.openCreateForm();
  const externalId = uniqueExternalId();
  await createPage.fillForm({ externalId, serviceId: ACKNOWLEDGED_SERVICE_ID, description: 'Session refresh test ticket' });

  refreshCount = 0;
  await createPage.submit();

  const detailPage = new TicketDetailPage(page);
  await expect(detailPage.heading(externalId)).toBeVisible();
  expect(refreshCount).toBeGreaterThan(0);
});
