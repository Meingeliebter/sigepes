import { Navigate, createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from './guards/ProtectedRoute';
import { RoleRoute } from './guards/RoleRoute';
import { AppLayout } from './layouts/AppLayout';
import { AccessDeniedPage } from './pages/AccessDeniedPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { HomePage } from '../modules/home/pages/HomePage';
import { LoginPage } from '../modules/auth/pages/LoginPage';
import { RegisterPage } from '../modules/auth/pages/RegisterPage';
import { DashboardPage } from '../modules/dashboard/pages/DashboardPage';
import { VesselsPage } from '../modules/vessels/pages/VesselsPage';
import { TripsPage } from '../modules/trips/pages/TripsPage';
import { NewTripPage } from '../modules/trips/pages/NewTripPage';
import { SpeciesPage } from '../modules/species/pages/SpeciesPage';
import { ClosuresPage } from '../modules/closures/pages/ClosuresPage';
import { PermitsPage } from '../modules/permits/pages/PermitsPage';
import { NotificationsPage } from '../modules/notifications/pages/NotificationsPage';
import { UsersPage } from '../modules/users/pages/UsersPage';
import { AuditPage } from '../modules/audit/pages/AuditPage';
import { VesselAuditPage } from '../modules/inspections/pages/VesselAuditPage';
import { PermitVerificationPage } from '../modules/permits/pages/PermitVerificationPage';
import { QuotasPage } from '../modules/quotas/pages/QuotasPage';
import { ZonesPage } from '../modules/zones/pages/ZonesPage';
import { PublicReportPage } from '../modules/reports/pages/PublicReportPage';
import { NewReportPage } from '../modules/reports/pages/NewReportPage';
import { ReportsQueuePage } from '../modules/reports/pages/ReportsQueuePage';

export const router = createBrowserRouter([
  { path: '/', element: <HomePage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  // RF-10: la denuncia ciudadana no exige cuenta.
  { path: '/denunciar', element: <PublicReportPage /> },
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
          { path: 'trips/new', element: <NewTripPage /> },
          { path: 'species', element: <SpeciesPage /> },
          { path: 'closures', element: <ClosuresPage /> },
          { path: 'quotas', element: <QuotasPage /> },
          { path: 'zones', element: <ZonesPage /> },
          { path: 'reports/new', element: <NewReportPage /> },
          { path: 'permits', element: <PermitsPage /> },
          { path: 'notifications', element: <NotificationsPage /> },
          { path: 'access-denied', element: <AccessDeniedPage /> },
          { element: <RoleRoute allow={['ADMIN']} />, children: [{ path: 'users', element: <UsersPage /> }] },
          { element: <RoleRoute allow={['INSPECTOR', 'ADMIN']} />, children: [{ path: 'audit', element: <AuditPage /> }] },
          { element: <RoleRoute allow={['INSPECTOR', 'ADMIN']} />, children: [{ path: 'reports', element: <ReportsQueuePage /> }] },
          {
            element: <RoleRoute allow={['INSPECTOR', 'ADMIN']} />,
            children: [
              { path: 'inspections/scan', element: <VesselAuditPage /> },
              { path: 'permits/verify', element: <PermitVerificationPage /> },
            ],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);
