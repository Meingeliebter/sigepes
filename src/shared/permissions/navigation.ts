import type { Role } from '../../modules/auth/types/auth.types';

export interface NavItem {
  label: string;
  path: string;
  icon: string;
  roles: Role[];
}

export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard', roles: ['FISHER', 'INSPECTOR', 'ADMIN'] },
  { label: 'Embarcaciones', path: '/app/vessels', icon: 'boat', roles: ['FISHER', 'INSPECTOR', 'ADMIN'] },
  { label: 'Bitácora de faenas', path: '/app/trips', icon: 'book', roles: ['FISHER', 'INSPECTOR', 'ADMIN'] },
  { label: 'Especies', path: '/app/species', icon: 'fish', roles: ['FISHER', 'INSPECTOR', 'ADMIN'] },
  { label: 'Vedas', path: '/app/closures', icon: 'block', roles: ['FISHER', 'INSPECTOR', 'ADMIN'] },
  { label: 'Permisos', path: '/app/permits', icon: 'gavel', roles: ['FISHER', 'INSPECTOR', 'ADMIN'] },
  { label: 'Notificaciones', path: '/app/notifications', icon: 'bell', roles: ['FISHER', 'INSPECTOR', 'ADMIN'] },
  { label: 'Usuarios', path: '/app/users', icon: 'people', roles: ['ADMIN'] },
  { label: 'Auditoría', path: '/app/audit', icon: 'history', roles: ['INSPECTOR', 'ADMIN'] },
];
