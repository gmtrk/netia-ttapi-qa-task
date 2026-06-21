import { Locator, Page } from '@playwright/test';
import { BaseScreen } from './BaseScreen';

export class TicketDetailPage extends BaseScreen {
  readonly closeButton: Locator;
  readonly noteInput: Locator;
  readonly addNoteButton: Locator;
  readonly errorAlert: Locator;

  constructor(page: Page) {
    super(page);
    this.closeButton = page.getByRole('button', { name: 'Zamknij zgłoszenie' });
    this.noteInput = page.getByLabel('Treść notatki');
    this.addNoteButton = page.getByRole('button', { name: 'Dodaj notatkę' });
    this.errorAlert = page.getByRole('alert');
  }

  heading(externalId: string): Locator {
    return this.page.getByRole('heading', { name: externalId });
  }

  description(text: string): Locator {
    return this.note(text);
  }

  statusChip(label: string): Locator {
    return this.page.getByText(label, { exact: true });
  }

  note(text: string): Locator {
    return this.page.getByText(text);
  }

  async close(): Promise<void> {
    await this.closeButton.click();
  }

  async addNote(text: string): Promise<void> {
    await this.noteInput.fill(text);
    await this.addNoteButton.click();
  }
}
