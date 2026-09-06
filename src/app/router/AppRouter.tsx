import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { AppLayout } from '@/app/layouts/AppLayout';
import { RequireAuth } from '@/app/guards/RequireAuth';

// ---- Lazy loading por ruta (code splitting) ----
const LoginPage = lazy(() =>
  import('@/modules/auth/pages/LoginPage').then((m) => ({ default: m.LoginPage })),
);
const HomePage = lazy(() =>
  import('@/modules/home/pages/HomePage').then((m) => ({ default: m.HomePage })),
);
const TripsPage = lazy(() =>
  import('@/modules/trips/pages/TripsPage').then((m) => ({ default: m.TripsPage })),
);
const NewTripPage = lazy(() =>
  import('@/modules/trips/pages/NewTripPage').then((m) => ({ default: m.NewTripPage })),
);
const VesselsPage = lazy(() =>
  import('@/modules/vessels/pages/VesselsPage').then((m) => ({ default: m.VesselsPage })),
);
const PermitsPage = lazy(() =>
  import('@/modules/permits/pages/PermitsPage').then((m) => ({ default: m.PermitsPage })),
);
const MapPage = lazy(() =>
  import('@/modules/map/pages/MapPage').then((m) => ({ default: m.MapPage })),
);
const MarketplacePage = lazy(() =>
  import('@/modules/marketplace/pages/MarketplacePage').then((m) => ({
    default: m.MarketplacePage,
  })),
);
const EducationPage = lazy(() =>
  import('@/modules/education/pages/EducationPage').then((m) => ({
    default: m.EducationPage,
  })),
);
const NotificationsPage = lazy(() =>
  import('@/modules/notifications/pages/NotificationsPage').then((m) => ({
    default: m.NotificationsPage,
  })),
);
const DashboardPage = lazy(() =>
  import('@/modules/dashboard/pages/DashboardPage').then((m) => ({
    default: m.DashboardPage,
  })),
);
const AuditPage = lazy(() =>
  import('@/modules/audit/pages/AuditPage').then((m) => ({ default: m.AuditPage })),
);
const NotFoundPage = lazy(() =>
  import('@/modules/not-found/NotFoundPage').then((m) => ({ default: m.NotFoundPage })),
);
const AccessDeniedPage = lazy(() =>
  import('@/modules/not-found/AccessDeniedPage').then((m) => ({
    default: m.AccessDeniedPage,
  })),
);

function RouteFallback() {
  return (
    <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '60vh' }}>
      <CircularProgress />
    </Box>
  );
}

export function AppRouter() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        {/* Público */}
        <Route path="/login" element={<LoginPage />} />

        {/* Privado */}
        <Route
          path="/app"
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="home" replace />} />
          <Route path="home" element={<HomePage />} />

          <Route path="operations">
            <Route path="trips" element={<TripsPage />} />
            <Route
              path="trips/new"
              element={
                <RequireAuth permission="trip:create">
                  <NewTripPage />
                </RequireAuth>
              }
            />
            <Route path="vessels" element={<VesselsPage />} />
            <Route path="permits" element={<PermitsPage />} />
          </Route>

          <Route path="map" element={<MapPage />} />
          <Route path="marketplace" element={<MarketplacePage />} />
          <Route path="learn" element={<EducationPage />} />
          <Route path="notifications" element={<NotificationsPage />} />

          <Route
            path="dashboard"
            element={
              <RequireAuth permission="dashboard:read">
                <DashboardPage />
              </RequireAuth>
            }
          />
          <Route
            path="administration/audit-logs"
            element={
              <RequireAuth permission="audit:read">
                <AuditPage />
              </RequireAuth>
            }
          />

          <Route path="access-denied" element={<AccessDeniedPage />} />
        </Route>

        {/* Redirecciones */}
        <Route path="/" element={<Navigate to="/app/home" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
