import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../modules/auth/store/authStore';

export function ProtectedRoute() {
  const user = useAuthStore((s) => s.user);
  const location = useLocation();
  return user ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />;
}
