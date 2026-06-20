import { Locator, Page } from '@playwright/test';
import { BaseScreen } from './BaseScreen';
import { CreateTicketPage } from './CreateTicketPage';

export class TicketListPage extends BaseScreen {
  readonly heading: Locator;
  readonly newTicketButton: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Zgłoszenia' });
    this.newTicketButton = page.getByRole('button', { name: 'Nowe zgłoszenie' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/');
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
