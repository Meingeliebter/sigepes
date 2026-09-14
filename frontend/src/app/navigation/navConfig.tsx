import type { ReactNode } from 'react';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PhishingIcon from '@mui/icons-material/Phishing';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import GavelIcon from '@mui/icons-material/Gavel';
import SetMealIcon from '@mui/icons-material/SetMeal';
import EventBusyIcon from '@mui/icons-material/EventBusy';
import NotificationsIcon from '@mui/icons-material/Notifications';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import PersonIcon from '@mui/icons-material/Person';
import type { Role } from '../../modules/auth/types/auth.types';

export interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
  roles: Role[];
  primary?: boolean;
}

const ALL: Role[] = ['FISHER', 'INSPECTOR', 'ADMIN'];

export const navItems: NavItem[] = [
  { label: 'Inicio', path: '/app/dashboard', icon: <DashboardIcon />, roles: ALL, primary: true },
  { label: 'Faenas', path: '/app/trips', icon: <PhishingIcon />, roles: ALL, primary: true },
  { label: 'Embarcaciones', path: '/app/vessels', icon: <DirectionsBoatIcon />, roles: ['FISHER', 'ADMIN'], primary: true },
  { label: 'Permisos', path: '/app/permits', icon: <GavelIcon />, roles: ALL, primary: true },
  { label: 'Especies', path: '/app/species', icon: <SetMealIcon />, roles: ALL },
  { label: 'Vedas', path: '/app/closures', icon: <EventBusyIcon />, roles: ALL },
  { label: 'Notificaciones', path: '/app/notifications', icon: <NotificationsIcon />, roles: ALL },
  { label: 'Auditoría', path: '/app/audit', icon: <FactCheckIcon />, roles: ['ADMIN'] },
  { label: 'Perfil', path: '/app/profile', icon: <PersonIcon />, roles: ALL, primary: true },
];