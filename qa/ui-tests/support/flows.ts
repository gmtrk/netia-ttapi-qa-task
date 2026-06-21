import { Page } from '@playwright/test';
import { TicketListPage } from '../pages/TicketListPage';
import { TicketDetailPage } from '../pages/TicketDetailPage';
import { ACKNOWLEDGED_SERVICE_ID } from './data';

export async function createTicketViaUi(page: Page, externalId: string, description: string): Promise<TicketDetailPage> {
  const listPage = await TicketListPage.open(page);
  const createPage = await listPage.openCreateForm();
  await createPage.fillForm({ externalId, serviceId: ACKNOWLEDGED_SERVICE_ID, description });
  await createPage.submit();
  return new TicketDetailPage(page);
}
