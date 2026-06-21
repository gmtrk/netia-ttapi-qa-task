import { Locator, Page } from '@playwright/test';
import { BaseScreen } from './BaseScreen';

export interface NewTicketInput {
  externalId: string;
  serviceId: number;
  description: string;
  note?: string;
}

export class CreateTicketPage extends BaseScreen {
  static readonly path = '/tickets/new';

  readonly externalIdInput: Locator;
  readonly serviceIdInput: Locator;
  readonly descriptionInput: Locator;
  readonly noteInput: Locator;
  readonly submitButton: Locator;
  readonly requiredFieldError: Locator;

  constructor(page: Page) {
    super(page);
    this.externalIdInput = page.getByLabel('ID zewnętrzny');
    this.serviceIdInput = page.getByLabel('ID usługi');
    this.descriptionInput = page.getByLabel('Opis');
    this.noteInput = page.getByLabel('Notatka inicjalna');
    this.submitButton = page.getByRole('button', { name: 'Utwórz zgłoszenie' });
    this.requiredFieldError = page.getByText('Pole wymagane');
  }

  static async open(page: Page): Promise<CreateTicketPage> {
    const createPage = new CreateTicketPage(page);
    await page.goto(CreateTicketPage.path);
    return createPage;
  }

  async fillForm(input: NewTicketInput): Promise<void> {
    await this.externalIdInput.fill(input.externalId);
    await this.serviceIdInput.fill(String(input.serviceId));
    await this.descriptionInput.fill(input.description);
    if (input.note !== undefined) {
      await this.noteInput.fill(input.note);
    }
  }

  async submit(): Promise<void> {
    await this.submitButton.click();
  }
}
