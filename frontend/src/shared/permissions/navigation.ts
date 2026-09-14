import type { Role } from '../../modules/auth/types/auth.types';

export interface NavItem {
  label: string;
  path: string;
  icon: string;
  roles: Role[];
  /** Sección bajo la que se agrupa en el menú institucional (opcional). */
  group?: string;
}

const ALL: Role[] = ['FISHER', 'SHIPOWNER', 'INSPECTOR', 'ANALYST', 'ADMIN'];

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard', roles: ALL, group: 'Operación' },
  { label: 'Embarcaciones', path: '/app/vessels', icon: 'boat', roles: ALL, group: 'Operación' },
  { label: 'Bitácora de faenas', path: '/app/trips', icon: 'book', roles: ALL, group: 'Operación' },
  { label: 'Permisos', path: '/app/permits', icon: 'gavel', roles: ALL, group: 'Operación' },
  { label: 'Especies', path: '/app/species', icon: 'fish', roles: ALL, group: 'Recursos y control' },
  { label: 'Vedas', path: '/app/closures', icon: 'block', roles: ALL, group: 'Recursos y control' },
  { label: 'Notificaciones', path: '/app/notifications', icon: 'bell', roles: ALL, group: 'Recursos y control' },
  { label: 'Usuarios', path: '/app/users', icon: 'people', roles: ['ADMIN'], group: 'Administración' },
  { label: 'Auditoría', path: '/app/audit', icon: 'history', roles: ['INSPECTOR', 'ADMIN'], group: 'Administración' },
];

/** Etiquetas de las secciones del menú, en el orden en que deben mostrarse. */
export const NAV_GROUPS = ['Operación', 'Recursos y control', 'Administración'] as const;
