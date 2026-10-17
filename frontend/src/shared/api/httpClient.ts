import axios from 'axios';
import { useAuthStore } from '../../modules/auth/store/authStore';

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: Number(import.meta.env.VITE_REQUEST_TIMEOUT_MS ?? 15000),
  headers: { 'Content-Type': 'application/json' },
});

httpClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/**
 * Cliente sin token, para endpoints públicos que no deben saber quién llama: la denuncia anónima
 * (RN-20) se envía por aquí aunque haya sesión iniciada, de modo que el servidor nunca ve la cuenta.
 */
export const publicClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: Number(import.meta.env.VITE_REQUEST_TIMEOUT_MS ?? 15000),
});

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  },
);
