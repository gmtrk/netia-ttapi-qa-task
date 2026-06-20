import { Page } from '@playwright/test';

export abstract class BaseScreen {
  protected readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }
}
