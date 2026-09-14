import AccessibilityNewIcon from '@mui/icons-material/AccessibilityNew';
import MapIcon from '@mui/icons-material/Map';
import { Box, Link, Stack } from '@mui/material';

const LINKS = [
  { label: 'Atención al ciudadano', href: '/#contacto' },
  { label: 'Transparencia', href: '/#normatividad' },
  { label: 'PQRS', href: '/#contacto' },
  { label: 'Contacto', href: '/#contacto' },
];

/**
 * Barra utilitaria institucional (franja superior angosta, común en portales de gobierno) con
 * accesos a atención ciudadana, transparencia y accesibilidad. Los enlaces apuntan a secciones
 * reales de la página de inicio pública; no son botones decorativos sin destino.
 */
export function TopUtilityBar() {
  return (
    <Box
      sx={{
        display: { xs: 'none', md: 'block' },
        bgcolor: '#071E30',
        color: 'rgba(255,255,255,0.85)',
        borderBottom: '1px solid rgba(255,255,255,0.08)',
      }}
    >
      <Stack
        direction="row"
        spacing={3}
        alignItems="center"
        justifyContent="flex-end"
        sx={{ maxWidth: 1280, mx: 'auto', px: 3, py: 0.5, fontSize: '.8rem' }}
      >
        {LINKS.map((link) => (
          <Link key={link.label} href={link.href} color="inherit" underline="hover" variant="caption">
            {link.label}
          </Link>
        ))}
        <Stack direction="row" spacing={0.5} alignItems="center">
          <AccessibilityNewIcon sx={{ fontSize: 16 }} />
          <Link href="/#accesibilidad" color="inherit" underline="hover" variant="caption">
            Accesibilidad
          </Link>
        </Stack>
        <Stack direction="row" spacing={0.5} alignItems="center">
          <MapIcon sx={{ fontSize: 16 }} />
          <Link href="/#mapa-del-sitio" color="inherit" underline="hover" variant="caption">
            Mapa del sitio
          </Link>
        </Stack>
      </Stack>
    </Box>
  );
}
