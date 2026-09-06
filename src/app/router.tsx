import { Navigate, createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from './guards/ProtectedRoute';
import { RoleRoute } from './guards/RoleRoute';
import { AppLayout } from './layouts/AppLayout';
import { AccessDeniedPage } from './pages/AccessDeniedPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { LoginPage } from '../modules/auth/pages/LoginPage';
import { RegisterPage } from '../modules/auth/pages/RegisterPage';
import { DashboardPage } from '../modules/dashboard/pages/DashboardPage';
import { VesselsPage } from '../modules/vessels/pages/VesselsPage';
import { TripsPage } from '../modules/trips/pages/TripsPage';
import { SpeciesPage } from '../modules/species/pages/SpeciesPage';
import { ClosuresPage } from '../modules/closures/pages/ClosuresPage';
import { PermitsPage } from '../modules/permits/pages/PermitsPage';
import { NotificationsPage } from '../modules/notifications/pages/NotificationsPage';
import { UsersPage } from '../modules/users/pages/UsersPage';
import { AuditPage } from '../modules/audit/pages/AuditPage';

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/app/dashboard" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/app',
        element: <AppLayout />,
        children: [
          { index: true, element: <Navigate to="dashboard" replace /> },
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'vessels', element: <VesselsPage /> },
          { path: 'trips', element: <TripsPage /> },
          { path: 'species', element: <SpeciesPage /> },
          { path: 'closures', element: <ClosuresPage /> },
          { path: 'permits', element: <PermitsPage /> },
          { path: 'notifications', element: <NotificationsPage /> },
          { path: 'access-denied', element: <AccessDeniedPage /> },
          { element: <RoleRoute allow={['ADMIN']} />, children: [{ path: 'users', element: <UsersPage /> }] },
          { element: <RoleRoute allow={['INSPECTOR', 'ADMIN']} />, children: [{ path: 'audit', element: <AuditPage /> }] },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);
