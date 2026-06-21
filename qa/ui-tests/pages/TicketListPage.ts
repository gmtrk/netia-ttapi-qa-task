import { Locator, Page } from '@playwright/test';
import { BaseScreen } from './BaseScreen';
import { CreateTicketPage } from './CreateTicketPage';

export class TicketListPage extends BaseScreen {
  static readonly path = '/';

  readonly heading: Locator;
  readonly newTicketButton: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Zgłoszenia' });
    this.newTicketButton = page.getByRole('button', { name: 'Nowe zgłoszenie' });
  }

  static async open(page: Page): Promise<TicketListPage> {
    const listPage = new TicketListPage(page);
    await page.goto(TicketListPage.path);
    return listPage;
  }

  async openCreateForm(): Promise<CreateTicketPage> {
    await this.newTicketButton.click();
    return new CreateTicketPage(this.page);
  }

  row(externalId: string): Locator {
    return this.page.getByRole('row').filter({ hasText: externalId });
  }

  columnHeader(name: string): Locator {
    return this.page.getByRole('columnheader', { name });
  }
}
