import { httpClient } from '../../../shared/api/httpClient';
import type { AuthSession, LoginRequest, RegisterRequest } from '../types/auth.types';

export const authService = {
  async login(payload: LoginRequest) {
    const { data } = await httpClient.post<AuthSession>('/auth/login', payload);
    return data;
  },
  async register(payload: RegisterRequest) {
    const { confirmPassword: _confirm, ...body } = payload;
    void _confirm;
    const { data } = await httpClient.post<AuthSession>('/auth/register', body);
    return data;
  },
  async me() {
    const { data } = await httpClient.get<AuthSession>('/auth/me');
    return data;
  },
  /**
   * Cierra la sesión en el servidor para que quede constancia en la auditoría.
   * El fallo no debe impedir salir: la sesión local se limpia igualmente.
   */
  async logout() {
    try {
      await httpClient.post('/auth/logout');
    } catch {
      // Sin conexión o token ya caducado: se ignora a propósito.
    }
  },
};
