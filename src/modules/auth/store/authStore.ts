import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthSession, User } from '../types/auth.types';

interface AuthState {
  accessToken: string | null;
  user: User | null;
  setSession: (session: AuthSession) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      setSession: (session) => set({ accessToken: session.accessToken, user: session.user }),
      logout: () => set({ accessToken: null, user: null }),
    }),
    { name: 'sigepes-auth' },
  ),
);
