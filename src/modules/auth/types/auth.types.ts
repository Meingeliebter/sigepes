/**
 * Roles del sistema. El formulario de registro solo ofrece los tres primeros, pero el backend
 * también emite SHIPOWNER (armador) y ANALYST (analista de sostenibilidad), así que la interfaz
 * debe saber etiquetarlos y enrutarlos.
 */
export type Role = 'FISHER' | 'SHIPOWNER' | 'INSPECTOR' | 'ANALYST' | 'ADMIN';

export interface User {
  id: string;
  nationalId: string;
  fullName: string;
  email: string;
  role: Role;
  status?: string;
}

export interface AuthSession {
  accessToken: string;
  tokenType?: string;
  expiresIn?: number;
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
  SHIPOWNER: 'Armador',
  INSPECTOR: 'Inspector AUNAP',
  ANALYST: 'Analista de sostenibilidad',
  ADMIN: 'Administrador',
};

/** Roles con potestad de control: ven todo y resuelven permisos. */
export const AUTHORITY_ROLES: Role[] = ['INSPECTOR', 'ANALYST', 'ADMIN'];

export const isAuthority = (role?: Role): boolean =>
  role !== undefined && AUTHORITY_ROLES.includes(role);
