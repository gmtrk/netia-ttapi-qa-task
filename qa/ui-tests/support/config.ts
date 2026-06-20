import { fileURLToPath } from 'node:url';
import dotenv from 'dotenv';

dotenv.config({ path: fileURLToPath(new URL('../../credentials.env', import.meta.url)) });

export const uiBaseUrl = process.env.TTAPI_UI_URL ?? 'http://localhost:3000';

export interface TenantUser {
  username: string;
  password: string;
}

export const alpha: TenantUser = {
  username: process.env.TTAPI_ALPHA_USER ?? 'alpha',
  password: requireEnv('TTAPI_PASSWORD'),
};

export const storageStatePath = 'playwright/.auth/alpha.json';

function requireEnv(name: string): string {
  const value = process.env[name];
  if (value === undefined || value === '') {
    throw new Error(`Missing required environment variable ${name} (set it in qa/credentials.env)`);
  }
  return value;
}
