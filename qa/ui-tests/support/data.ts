import { randomUUID } from 'node:crypto';

export const ACKNOWLEDGED_SERVICE_ID = 100002;

export function uniqueExternalId(): string {
  return `ui-${randomUUID()}`;
}
