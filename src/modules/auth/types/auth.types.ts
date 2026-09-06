export type Role = 'FISHER' | 'INSPECTOR' | 'ADMIN';

export interface User {
  id: string;
  nationalId: string;
  fullName: string;
  email: string;
  role: Role;
}

export interface AuthSession {
  accessToken: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nationalId: string;
  fullName: string;
  email: string;
  role: Role;
  password: string;
  confirmPassword: string;
}

export const ROLE_LABEL: Record<Role, string> = {
  FISHER: 'Pescador',
  INSPECTOR: 'Inspector AUNAP',
  ADMIN: 'Administrador',
};
