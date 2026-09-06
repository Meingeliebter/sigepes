import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../modules/auth/store/authStore';
import type { Role } from '../../modules/auth/types/auth.types';

export function RoleRoute({ allow }: { allow: Role[] }) {
  const user = useAuthStore((s) => s.user);
  if (!user) return <Navigate to="/login" replace />;
  return allow.includes(user.role) ? <Outlet /> : <Navigate to="/app/access-denied" replace />;
}
