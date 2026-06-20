import { Locator, Page } from '@playwright/test';
import { BaseScreen } from './BaseScreen';
import { TenantUser } from '../support/config';
import { TicketListPage } from './TicketListPage';

export class KeycloakLoginPage extends BaseScreen {
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly signInButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    this.usernameInput = page.locator('#username');
    this.passwordInput = page.locator('#password');
    this.signInButton = page.locator('#kc-login');
    this.errorMessage = page.getByText(/invalid username or password/i);
  }

  async signIn(user: TenantUser): Promise<TicketListPage> {
    await this.usernameInput.fill(user.username);
    await this.passwordInput.fill(user.password);
    await this.signInButton.click();
    return new TicketListPage(this.page);
  }
}
