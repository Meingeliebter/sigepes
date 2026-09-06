import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { STORAGE_KEYS } from '@/shared/constants/app';
import type { Role } from '@/shared/permissions/permissions';

export interface AuthUser {
  id: string;
  fullName: string;
  email: string;
  roles: Role[];
}

interface SessionState {
  /** Access token en memoria (NO se persiste por seguridad). */
  accessToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setSession: (payload: { accessToken: string; user: AuthUser }) => void;
  setAccessToken: (token: string) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      isAuthenticated: false,
      setSession: ({ accessToken, user }) =>
        set({ accessToken, user, isAuthenticated: true }),
      setAccessToken: (token) => set({ accessToken: token }),
      clearSession: () =>
        set({ accessToken: null, user: null, isAuthenticated: false }),
    }),
    {
      name: STORAGE_KEYS.session,
      // Solo persistimos datos NO sensibles del usuario.
      // El access token permanece en memoria; se restaura vía /auth/refresh.
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);

/** Acceso al token fuera de React (p.ej. interceptores Axios). */
export const getAccessToken = (): string | null =>
  useSessionStore.getState().accessToken;
