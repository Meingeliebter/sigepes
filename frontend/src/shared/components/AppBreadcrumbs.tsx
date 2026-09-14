import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import { Breadcrumbs, Link, Typography } from '@mui/material';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import { NAV_ITEMS } from '../permissions/navigation';

/** Segundo nivel de una ruta que no tiene su propio ítem de navegación (p. ej. "Nueva faena"). */
const SUB_LABELS: Record<string, string> = {
  '/app/trips/new': 'Nueva faena',
};

/**
 * Migas de pan generadas a partir de la ruta activa y de {@link NAV_ITEMS}, para que la
 * navegación institucional siempre indique "dónde estoy" dentro del panel.
 */
export function AppBreadcrumbs() {
  const location = useLocation();
  const current = NAV_ITEMS.find((item) => location.pathname.startsWith(item.path));
  const subLabel = SUB_LABELS[location.pathname];

  if (!current) return null;

  return (
    <Breadcrumbs separator={<NavigateNextIcon fontSize="small" />} sx={{ mb: 2 }} aria-label="Ruta de navegación">
      <Link component={RouterLink} to="/app/dashboard" underline="hover" color="text.secondary" variant="body2">
        Inicio
      </Link>
      {subLabel ? (
        <>
          <Link component={RouterLink} to={current.path} underline="hover" color="text.secondary" variant="body2">
            {current.label}
          </Link>
          <Typography variant="body2" color="text.primary" fontWeight={600}>
            {subLabel}
          </Typography>
        </>
      ) : (
        <Typography variant="body2" color="text.primary" fontWeight={600}>
          {current.label}
        </Typography>
      )}
    </Breadcrumbs>
  );
}
