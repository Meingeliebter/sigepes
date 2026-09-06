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
};
