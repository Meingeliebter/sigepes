import EmailIcon from '@mui/icons-material/Email';
import FacebookIcon from '@mui/icons-material/Facebook';
import InstagramIcon from '@mui/icons-material/Instagram';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import PhoneIcon from '@mui/icons-material/Phone';
import XIcon from '@mui/icons-material/Close';
import YouTubeIcon from '@mui/icons-material/YouTube';
import { Box, Divider, IconButton, Link, Stack, Tooltip, Typography } from '@mui/material';
import { Logotype } from './Logotype';

const SITE_LINKS = [
  { label: 'Inicio', href: '/#inicio' },
  { label: 'Nuestra misión', href: '/#nosotros' },
  { label: 'Trámites y accesos', href: '/#accesos' },
  { label: 'Normatividad', href: '/#normatividad' },
  { label: 'Programas', href: '/#programas' },
  { label: 'Atención al ciudadano', href: '/#contacto' },
];

const LEGAL_LINKS = [
  { label: 'Ley 13 de 1990', href: '/#normatividad' },
  { label: 'Ley 1851 de 2017', href: '/#normatividad' },
  { label: 'Política de privacidad', href: '/#privacidad' },
  { label: 'Términos y condiciones', href: '/#terminos' },
];

const SOCIAL = [
  { label: 'Facebook', icon: <FacebookIcon fontSize="small" /> },
  { label: 'X (Twitter)', icon: <XIcon fontSize="small" /> },
  { label: 'Instagram', icon: <InstagramIcon fontSize="small" /> },
  { label: 'YouTube', icon: <YouTubeIcon fontSize="small" /> },
];

interface FooterProps {
  /** Versión reducida para no competir con la barra de navegación inferior en móvil. */
  compact?: boolean;
}

/** Pie de página institucional, común a la página pública y al panel autenticado. */
export function Footer({ compact = false }: FooterProps) {
  const year = new Date().getFullYear();

  if (compact) {
    return (
      <Box component="footer" sx={{ bgcolor: '#071E30', color: 'rgba(255,255,255,.75)', px: 2, py: 1.5 }}>
        <Typography variant="caption" display="block" textAlign="center">
          © {year} SIGEPES · Proyecto académico — Universidad Manuela Beltrán
        </Typography>
      </Box>
    );
  }

  return (
    <Box component="footer" sx={{ bgcolor: '#071E30', color: 'rgba(255,255,255,.82)' }}>
      <Box sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 3, md: 4 }, py: { xs: 5, md: 6 } }}>
        <Box
          sx={{
            display: 'grid',
            gap: 4,
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1.4fr 1fr 1fr 1.2fr' },
          }}
        >
          <Stack spacing={2}>
            <Logotype variant="light" size={40} />
            <Typography variant="body2" sx={{ color: 'rgba(255,255,255,.7)', maxWidth: 320 }}>
              Sistema de información para el registro, control y trazabilidad de la actividad
              pesquera, en apoyo a la reducción de la pesca ilegal y al aprovechamiento sostenible
              del recurso.
            </Typography>
            <Stack direction="row" spacing={1}>
              {SOCIAL.map((network) => (
                <Tooltip key={network.label} title={`${network.label} — próximamente`}>
                  <span>
                    <IconButton
                      size="small"
                      disabled
                      aria-label={`${network.label} (próximamente)`}
                      sx={{ color: 'rgba(255,255,255,.55)', border: '1px solid rgba(255,255,255,.18)' }}
                    >
                      {network.icon}
                    </IconButton>
                  </span>
                </Tooltip>
              ))}
            </Stack>
          </Stack>

          <Stack spacing={1.25} id="mapa-del-sitio">
            <Typography variant="overline" sx={{ color: 'rgba(255,255,255,.55)' }}>
              Mapa del sitio
            </Typography>
            {SITE_LINKS.map((link) => (
              <Link key={link.label} href={link.href} color="inherit" underline="hover" variant="body2">
                {link.label}
              </Link>
            ))}
          </Stack>

          <Stack spacing={1.25}>
            <Typography variant="overline" sx={{ color: 'rgba(255,255,255,.55)' }}>
              Normatividad y transparencia
            </Typography>
            {LEGAL_LINKS.map((link) => (
              <Link key={link.label} href={link.href} color="inherit" underline="hover" variant="body2">
                {link.label}
              </Link>
            ))}
          </Stack>

          <Stack spacing={1.5}>
            <Typography variant="overline" sx={{ color: 'rgba(255,255,255,.55)' }}>
              Atención al ciudadano
            </Typography>
            <Stack direction="row" spacing={1.25} alignItems="flex-start">
              <LocationOnIcon fontSize="small" sx={{ mt: 0.25, color: 'rgba(255,255,255,.55)' }} />
              <Typography variant="body2">Bogotá D.C., Colombia</Typography>
            </Stack>
            <Stack direction="row" spacing={1.25} alignItems="center">
              <PhoneIcon fontSize="small" sx={{ color: 'rgba(255,255,255,.55)' }} />
              <Typography variant="body2">Línea de atención: 01 8000 000 000</Typography>
            </Stack>
            <Stack direction="row" spacing={1.25} alignItems="center">
              <EmailIcon fontSize="small" sx={{ color: 'rgba(255,255,255,.55)' }} />
              <Link href="mailto:contacto@sigepes.gov.co" color="inherit" underline="hover" variant="body2">
                contacto@sigepes.gov.co
              </Link>
            </Stack>
            <Typography variant="caption" sx={{ color: 'rgba(255,255,255,.5)' }}>
              Canales de ejemplo para este proyecto académico.
            </Typography>
          </Stack>
        </Box>

        <Divider sx={{ my: 4, borderColor: 'rgba(255,255,255,.1)' }} />

        <Stack spacing={1.5} id="accesibilidad" sx={{ maxWidth: 760 }}>
          <Typography variant="subtitle2" sx={{ color: '#fff' }}>
            Compromiso de accesibilidad
          </Typography>
          <Typography variant="body2" sx={{ color: 'rgba(255,255,255,.65)' }}>
            Este sitio prioriza el contraste adecuado, la navegación completa por teclado, el
            enfoque visible y etiquetas descriptivas en formularios y controles. Si encuentras una
            barrera de acceso, escríbenos a{' '}
            <Link href="mailto:accesibilidad@sigepes.gov.co" color="inherit" underline="always">
              accesibilidad@sigepes.gov.co
            </Link>
            .
          </Typography>
        </Stack>

        <Box
          sx={{
            display: 'grid',
            gap: 3,
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
            mt: 3,
          }}
        >
          <Stack spacing={0.75} id="privacidad">
            <Typography variant="subtitle2" sx={{ color: '#fff' }}>
              Política de privacidad
            </Typography>
            <Typography variant="body2" sx={{ color: 'rgba(255,255,255,.6)' }}>
              El registro solicita únicamente identificación, nombre, correo y rol. Las
              contraseñas se almacenan cifradas y nunca se comparten con terceros; los datos se
              usan exclusivamente para operar el sistema y su bitácora de auditoría.
            </Typography>
          </Stack>
          <Stack spacing={0.75} id="terminos">
            <Typography variant="subtitle2" sx={{ color: '#fff' }}>
              Términos y condiciones
            </Typography>
            <Typography variant="body2" sx={{ color: 'rgba(255,255,255,.6)' }}>
              El uso del sistema implica el registro veraz de la actividad pesquera. La
              información declarada en faenas y capturas es responsabilidad de quien la registra
              y puede ser verificada por la autoridad competente.
            </Typography>
          </Stack>
        </Box>

        <Divider sx={{ my: 4, borderColor: 'rgba(255,255,255,.1)' }} />

        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ xs: 'flex-start', sm: 'center' }}
          spacing={1}
        >
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,.55)' }}>
            © {year} SIGEPES · Proyecto académico — Universidad Manuela Beltrán. Todos los derechos reservados.
          </Typography>
        </Stack>
      </Box>
    </Box>
  );
}
