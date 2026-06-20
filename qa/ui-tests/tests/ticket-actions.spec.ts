import { expect, test } from '@playwright/test';
import { createTicketViaUi } from '../support/flows';
import { uniqueExternalId } from '../support/data';

test('TC-UI-07 — closing a ticket updates its status and hides status-gated actions', async ({ page }) => {
  const externalId = uniqueExternalId();

  const detailPage = await createTicketViaUi(page, externalId, 'Lifecycle test ticket');
  await expect(detailPage.heading(externalId)).toBeVisible();
  await expect(detailPage.closeButton).toBeVisible();

  await detailPage.close();

  await expect(detailPage.statusChip('Zamknięte')).toBeVisible();
  await expect(detailPage.closeButton).toBeHidden();
  await expect(detailPage.addNoteButton).toBeHidden();
});

test('TC-UI-06 — adding a note shows it in the ticket notes history', async ({ page }) => {
  const externalId = uniqueExternalId();
  const noteText = 'Follow-up note from UI test';

  const detailPage = await createTicketViaUi(page, externalId, 'Note history test ticket');
  await expect(detailPage.heading(externalId)).toBeVisible();

  await detailPage.addNote(noteText);

  await expect(detailPage.note(noteText)).toBeVisible();
});
