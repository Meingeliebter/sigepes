import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CloseIcon from '@mui/icons-material/Close';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import EmailIcon from '@mui/icons-material/Email';
import GavelIcon from '@mui/icons-material/Gavel';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import MenuIcon from '@mui/icons-material/Menu';
import NotificationsIcon from '@mui/icons-material/Notifications';
import PhishingIcon from '@mui/icons-material/Phishing';
import PhoneIcon from '@mui/icons-material/Phone';
import PublicIcon from '@mui/icons-material/Public';
import RocketLaunchIcon from '@mui/icons-material/RocketLaunch';
import ScheduleIcon from '@mui/icons-material/Schedule';
import {
  Box, Button, Card, CardActionArea, CardContent, Chip, Drawer, IconButton, Link, List, ListItem,
  Stack, Toolbar, Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Footer } from '../../../shared/components/Footer';
import { Logotype } from '../../../shared/components/Logotype';
import { SkipLink } from '../../../shared/components/SkipLink';
import { TopUtilityBar } from '../../../shared/components/TopUtilityBar';
import { WaveDivider } from '../../../shared/illustrations/WaveDivider';
import { useAuthStore } from '../../auth/store/authStore';

const NAV_LINKS = [
  { label: 'Inicio', href: '#inicio' },
  { label: 'Nuestra misión', href: '#nosotros' },
  { label: 'Trámites', href: '#accesos' },
  { label: 'Normatividad', href: '#normatividad' },
  { label: 'Programas', href: '#programas' },
  { label: 'Contacto', href: '#contacto' },
];

const QUICK_ACCESS = [
  { label: 'Bitácora de faenas', description: 'Registra faenas y capturas', icon: <MenuBookIcon />, to: '/app/trips' },
  { label: 'Embarcaciones', description: 'Registro de embarcaciones y artes', icon: <DirectionsBoatIcon />, to: '/app/vessels' },
  { label: 'Permisos de pesca', description: 'Solicita y consulta tu licencia', icon: <GavelIcon />, to: '/app/permits' },
  { label: 'Catálogo de especies', description: 'Tallas mínimas legales', icon: <PhishingIcon />, to: '/app/species' },
  { label: 'Vedas vigentes', description: 'Zonas y periodos restringidos', icon: <BlockIcon />, to: '/app/closures' },
  { label: 'Notificaciones', description: 'Alertas regulatorias y de faenas', icon: <NotificationsIcon />, to: '/app/notifications' },
];

const SCOPE_BADGES = [
  { value: '20', label: 'Requerimientos funcionales' },
  { value: '6', label: 'Épicas de historias de usuario' },
  { value: '3', label: 'Incrementos del producto (MVP)' },
];

const NORMATIVIDAD = [
  {
    icon: <GavelIcon />,
    title: 'Ley 13 de 1990',
    body: 'Estatuto General de Pesca. Regula el manejo integral y la explotación racional de los recursos pesqueros para su aprovechamiento sostenido.',
  },
  {
    icon: <GavelIcon />,
    title: 'Ley 1851 de 2017',
    body: 'Establece medidas contra la pesca ilegal y el delito de ejercicio ilícito de la actividad pesquera; ordena la formalización de los pescadores artesanales.',
  },
  {
    icon: <AccountBalanceIcon />,
    title: 'AUNAP',
    body: 'Autoridad Nacional de Acuicultura y Pesca: titular de la potestad de administrar, controlar y sancionar la actividad pesquera.',
  },
  {
    icon: <PublicIcon />,
    title: 'ODS 14 — Vida submarina',
    body: 'Meta 14.4: reglamentar la explotación pesquera y poner fin a la pesca ilegal, no declarada y no reglamentada, y a la sobrepesca.',
  },
];

const ROADMAP = [
  {
    icon: <CheckCircleIcon color="success" />,
    version: 'MVP v1.0',
    title: 'Núcleo de control y legalidad',
    items: ['Registro y autenticación', 'Permisos y licencias', 'Embarcaciones y artes', 'Bitácora de faenas', 'Especies y tallas mínimas', 'Vedas', 'Administración y auditoría'],
    status: 'Disponible',
    statusColor: 'success' as const,
  },
  {
    icon: <ScheduleIcon color="warning" />,
    version: 'MVP v1.1',
    title: 'Control avanzado y trazabilidad',
    items: ['Control de cuotas', 'Geolocalización de zona', 'Trazabilidad de la cadena', 'Alertas y notificaciones', 'Tablero de indicadores'],
    status: 'En planeación',
    statusColor: 'warning' as const,
  },
  {
    icon: <RocketLaunchIcon color="info" />,
    version: 'Versión 2.0',
    title: 'Sostenibilidad avanzada y mercado',
    items: ['Rendimiento Máximo Sostenible', 'Áreas Marinas Protegidas', 'Datos ambientales', 'Comercialización sostenible', 'Educación pesquera'],
    status: 'Hoja de ruta',
    statusColor: 'info' as const,
  },
];

export function HomePage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const primaryCta = user
    ? { label: 'Ir a mi panel', onClick: () => void navigate('/app/dashboard') }
    : { label: 'Solicitar acceso', onClick: () => void navigate('/register') };

  const mobileDrawer = (
    <Box sx={{ width: 280, p: 2 }} role="presentation">
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Logotype compact size={32} />
        <IconButton aria-label="Cerrar menú" onClick={() => setMobileNavOpen(false)}>
          <CloseIcon />
        </IconButton>
      </Stack>
      <List>
        {NAV_LINKS.map((link) => (
          <ListItem key={link.label} disablePadding>
            <Link
              href={link.href}
              underline="none"
              color="inherit"
              onClick={() => setMobileNavOpen(false)}
              sx={{ display: 'block', width: '100%', py: 1.25, px: 1, fontWeight: 600 }}
            >
              {link.label}
            </Link>
          </ListItem>
        ))}
      </List>
      <Stack spacing={1.5} sx={{ mt: 2 }}>
        <Button component={RouterLink} to="/login" variant="outlined" fullWidth>Iniciar sesión</Button>
        <Button component={RouterLink} to="/register" variant="contained" fullWidth>Registrarse</Button>
      </Stack>
    </Box>
  );

  return (
    <Box sx={{ bgcolor: 'background.default' }}>
      <SkipLink />
      <TopUtilityBar />

      {/* Encabezado público */}
      <Box sx={{ position: 'sticky', top: 0, zIndex: 1100, bgcolor: '#071E30', borderBottom: '1px solid rgba(255,255,255,.08)' }}>
        <Toolbar sx={{ maxWidth: 1280, mx: 'auto', width: '100%' }}>
          <Box sx={{ flexGrow: 1 }}>
            <Logotype variant="light" size={34} showTagline={false} />
          </Box>
          <Stack direction="row" spacing={3} sx={{ display: { xs: 'none', lg: 'flex' }, mr: 3 }}>
            {NAV_LINKS.map((link) => (
              <Link key={link.label} href={link.href} color="inherit" underline="hover" sx={{ fontWeight: 600, color: 'rgba(255,255,255,.85)' }}>
                {link.label}
              </Link>
            ))}
          </Stack>
          <Stack direction="row" spacing={1.5} sx={{ display: { xs: 'none', sm: 'flex' } }}>
            {user ? (
              <Button component={RouterLink} to="/app/dashboard" variant="contained" color="secondary">
                Ir a mi panel
              </Button>
            ) : (
              <>
                <Button component={RouterLink} to="/login" variant="outlined" sx={{ color: '#fff', borderColor: 'rgba(255,255,255,.5)' }}>
                  Iniciar sesión
                </Button>
                <Button component={RouterLink} to="/register" variant="contained" color="secondary">
                  Registrarse
                </Button>
              </>
            )}
          </Stack>
          <IconButton
            color="inherit"
            aria-label="Abrir menú"
            onClick={() => setMobileNavOpen(true)}
            sx={{ display: { xs: 'inline-flex', lg: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
        </Toolbar>
      </Box>
      <Drawer anchor="right" open={mobileNavOpen} onClose={() => setMobileNavOpen(false)}>
        {mobileDrawer}
      </Drawer>

      <Box component="main" id="main-content">
        {/* Hero */}
        <Box id="inicio" component="section" sx={{ bgcolor: '#071E30', color: '#fff', pt: { xs: 6, md: 9 } }}>
          <Box sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 3, md: 4 } }}>
            <Grid container spacing={6} alignItems="center">
              <Grid size={{ xs: 12, md: 6 }}>
                <Chip
                  label="Portal institucional"
                  size="small"
                  sx={{ bgcolor: 'rgba(255,255,255,.1)', color: '#fff', fontWeight: 700, mb: 2 }}
                />
                <Typography variant="h2" component="h1" sx={{ fontSize: { xs: '2.1rem', md: '2.75rem' }, mb: 2 }}>
                  Sistema de Gestión de Pesca Sostenible
                </Typography>
                <Typography variant="h6" component="p" sx={{ color: 'rgba(255,255,255,.8)', fontWeight: 400, mb: 4, maxWidth: 560 }}>
                  Registro digital de faenas, validación de permisos y cuotas, trazabilidad de la
                  cadena de valor y monitoreo de vedas, para reducir la pesca ilegal y fortalecer el
                  aprovechamiento sostenible del recurso.
                </Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <Button
                    size="large"
                    variant="contained"
                    color="secondary"
                    endIcon={<ArrowForwardIcon />}
                    onClick={primaryCta.onClick}
                  >
                    {primaryCta.label}
                  </Button>
                  <Button
                    size="large"
                    variant="outlined"
                    href="#accesos"
                    sx={{ color: '#fff', borderColor: 'rgba(255,255,255,.5)' }}
                  >
                    Consultar trámites
                  </Button>
                </Stack>
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <Box sx={{ position: 'relative', borderRadius: 3, overflow: 'hidden', boxShadow: '0 12px 32px rgba(0,0,0,.35)' }}>
                  <Box
                    component="img"
                    src="/images/hero-pescador.jpg"
                    alt="Pescadores artesanales lanzando una atarraya desde una canoa al amanecer"
                    sx={{ width: '100%', aspectRatio: '4 / 3', objectFit: 'cover', display: 'block' }}
                  />
                  <Typography
                    variant="caption"
                    sx={{
                      position: 'absolute',
                      right: 8,
                      bottom: 8,
                      px: 1,
                      py: 0.25,
                      borderRadius: 1,
                      bgcolor: 'rgba(7,30,48,.65)',
                      color: 'rgba(255,255,255,.85)',
                    }}
                  >
                    Foto: Sahaj Patel / Unsplash
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </Box>
          <WaveDivider fill="#F6F8FA" />
        </Box>

        {/* Accesos rápidos */}
        <Box id="accesos" component="section" sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 3, md: 4 }, py: { xs: 6, md: 8 } }}>
          <Typography variant="overline" color="secondary.dark">Trámites y accesos</Typography>
          <Typography variant="h4" sx={{ mb: 1 }}>Accesos rápidos</Typography>
          <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 640 }}>
            Inicia sesión para operar cada módulo. Si aún no tienes cuenta, el sistema te guiará a
            crearla antes de continuar.
          </Typography>
          <Grid container spacing={2.5}>
            {QUICK_ACCESS.map((item) => (
              <Grid key={item.label} size={{ xs: 12, sm: 6, md: 4 }}>
                <Card sx={{ height: '100%' }}>
                  <CardActionArea
                    component={RouterLink}
                    to={item.to}
                    sx={{ height: '100%', p: 2.5, display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}
                  >
                    <Box sx={{ color: 'secondary.main', mb: 1.5 }}>{item.icon}</Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{item.label}</Typography>
                    <Typography variant="body2" color="text.secondary">{item.description}</Typography>
                  </CardActionArea>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Box>

        {/* Misión */}
        <Box id="nosotros" component="section" sx={{ bgcolor: 'primary.dark', color: '#fff', py: { xs: 6, md: 8 } }}>
          <Box sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 3, md: 4 } }}>
            <Grid container spacing={5} alignItems="center">
              <Grid size={{ xs: 12, md: 7 }}>
                <Typography variant="overline" sx={{ color: 'secondary.light' }}>Nuestra misión</Typography>
                <Typography variant="h4" sx={{ mb: 2 }}>
                  Pesca legal, trazable y sostenible
                </Typography>
                <Typography sx={{ color: 'rgba(255,255,255,.82)', mb: 2 }}>
                  Diseñamos y documentamos la base de ingeniería de requerimientos y de diseño de
                  interfaces de un sistema que permite el registro, control, trazabilidad y
                  monitoreo de la actividad pesquera, contribuyendo a la reducción de la pesca
                  ilegal y al aprovechamiento sostenible del recurso.
                </Typography>
                <Typography sx={{ color: 'rgba(255,255,255,.82)' }}>
                  Si se implementa un sistema que integre el registro digital de faenas, la
                  validación automática de permisos y cuotas, la trazabilidad de la cadena de valor
                  y el monitoreo de vedas y zonas protegidas, se reducirá la pesca ilegal y se
                  fortalecerá el aprovechamiento sostenible del recurso — en alineación con el ODS
                  14 y la legislación pesquera colombiana.
                </Typography>
              </Grid>
              <Grid size={{ xs: 12, md: 5 }}>
                <Stack spacing={2}>
                  {SCOPE_BADGES.map((badge) => (
                    <Card key={badge.label} sx={{ bgcolor: 'rgba(255,255,255,.06)', border: '1px solid rgba(255,255,255,.14)' }}>
                      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Typography variant="h3" sx={{ color: 'secondary.light', fontWeight: 800 }}>{badge.value}</Typography>
                        <Typography sx={{ color: 'rgba(255,255,255,.85)' }}>{badge.label}</Typography>
                      </CardContent>
                    </Card>
                  ))}
                </Stack>
              </Grid>
            </Grid>
          </Box>
        </Box>

        {/* Normatividad */}
        <Box id="normatividad" component="section" sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 3, md: 4 }, py: { xs: 6, md: 8 } }}>
          <Typography variant="overline" color="secondary.dark">Marco legal</Typography>
          <Typography variant="h4" sx={{ mb: 4 }}>Normatividad</Typography>
          <Grid container spacing={2.5}>
            {NORMATIVIDAD.map((item) => (
              <Grid key={item.title} size={{ xs: 12, sm: 6, md: 3 }}>
                <Card sx={{ height: '100%' }}>
                  <CardContent>
                    <Box sx={{ color: 'primary.main', mb: 1.5 }}>{item.icon}</Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 0.5 }}>{item.title}</Typography>
                    <Typography variant="body2" color="text.secondary">{item.body}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Box>

        {/* Programas / hoja de ruta */}
        <Box id="programas" component="section" sx={{ bgcolor: '#EEF2F5', py: { xs: 6, md: 8 } }}>
          <Box sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 3, md: 4 } }}>
            <Typography variant="overline" color="secondary.dark">Programas y proyectos</Typography>
            <Typography variant="h4" sx={{ mb: 1 }}>Hoja de ruta del sistema</Typography>
            <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 640 }}>
              Construcción incremental del producto, priorizando primero el control legal y el
              núcleo de operación.
            </Typography>
            <Grid container spacing={2.5}>
              {ROADMAP.map((phase) => (
                <Grid key={phase.version} size={{ xs: 12, md: 4 }}>
                  <Card sx={{ height: '100%' }}>
                    <CardContent>
                      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                        {phase.icon}
                        <Chip label={phase.status} color={phase.statusColor} size="small" />
                      </Stack>
                      <Typography variant="overline" color="text.secondary">{phase.version}</Typography>
                      <Typography variant="h6" sx={{ fontWeight: 700, mb: 1.5 }}>{phase.title}</Typography>
                      <Stack spacing={0.75}>
                        {phase.items.map((line) => (
                          <Typography key={line} variant="body2" color="text.secondary">• {line}</Typography>
                        ))}
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Box>
        </Box>

        {/* Contacto */}
        <Box id="contacto" component="section" sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 3, md: 4 }, py: { xs: 6, md: 8 } }}>
          <Typography variant="overline" color="secondary.dark">Atención al ciudadano</Typography>
          <Typography variant="h4" sx={{ mb: 1 }}>¿Necesitas ayuda?</Typography>
          <Typography color="text.secondary" sx={{ mb: 4, maxWidth: 640 }}>
            Nuestros canales de atención están disponibles para pescadores, armadores, compradores y
            entidades de control.
          </Typography>
          <Grid container spacing={2.5}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <PhoneIcon color="secondary" sx={{ mb: 1 }} />
                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>Línea de atención</Typography>
                  <Typography color="text.secondary">01 8000 000 000</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <EmailIcon color="secondary" sx={{ mb: 1 }} />
                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>Correo institucional</Typography>
                  <Typography color="text.secondary">contacto@sigepes.gov.co</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <AccountBalanceIcon color="secondary" sx={{ mb: 1 }} />
                  <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>PQRS</Typography>
                  <Typography color="text.secondary">Radica tu solicitud por correo institucional</Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Box>
      </Box>

      <Footer />
    </Box>
  );
}
