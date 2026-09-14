import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useSessionStore } from '@/shared/storage/sessionStore';
import {
  hasPermission,
  type Permission,
} from '@/shared/permissions/permissions';

interface RequireAuthProps {
  children: ReactNode;
  /** Permiso opcional requerido para acceder a la ruta. */
  permission?: Permission;
}

export function RequireAuth({ children, permission }: RequireAuthProps) {
  const location = useLocation();
  const { isAuthenticated, user } = useSessionStore();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (permission && !hasPermission(user.roles, permission)) {
    return <Navigate to="/app/access-denied" replace />;
  }

  return <>{children}</>;
}
