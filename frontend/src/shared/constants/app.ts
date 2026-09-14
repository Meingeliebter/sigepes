export const APP_NAME = 'SIGEPES';
export const APP_FULL_NAME = 'Sistema de Gestión de Pesca Sostenible';

export const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api/v1';

export const STORAGE_KEYS = {
  session: 'sigepes.session',
  preferences: 'sigepes.preferences',
} as const;

export const HTTP_TIMEOUT_MS = 15000;
