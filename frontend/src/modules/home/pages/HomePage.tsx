import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import BlockIcon from '@mui/icons-material/Block';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import EmailIcon from '@mui/icons-material/Email';
import GavelIcon from '@mui/icons-material/Gavel';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import MenuIcon from '@mui/icons-material/Menu';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import PhoneIcon from '@mui/icons-material/Phone';
import PublicIcon from '@mui/icons-material/Public';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import {
  Box, Button, ButtonBase, Chip, Drawer, IconButton, Link, List, ListItem, Stack, Toolbar, Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { Footer } from '../../../shared/components/Footer';
import { Logotype } from '../../../shared/components/Logotype';
import { SkipLink } from '../../../shared/components/SkipLink';
import { TopUtilityBar } from '../../../shared/components/TopUtilityBar';
import { glass } from '../../../shared/theme/theme';
import { useAuthStore } from '../../auth/store/authStore';

const NAV_HEIGHT = 68;

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
  { label: 'Verificar un lote', description: 'Origen legal por código QR', icon: <QrCode2Icon />, to: '/verificar' },
  { label: 'Vedas vigentes', description: 'Zonas y periodos restringidos', icon: <BlockIcon />, to: '/app/closures' },
  { label: 'Denunciar pesca ilegal', description: 'Anónimo y sin cuenta', icon: <CampaignOutlinedIcon />, to: '/denunciar' },
];

const SCOPE_BADGES = [
  { value: '20', label: 'Requerimientos funcionales' },
  { value: '6', label: 'Épicas de historias de usuario' },
  { value: '3', label: 'Incrementos del producto (MVP)' },
];

const NORMATIVIDAD = [
  {
    title: 'Ley 13 de 1990',
    body: 'Estatuto General de Pesca. Regula el manejo integral y la explotación racional de los recursos pesqueros para su aprovechamiento sostenido.',
  },
  {
    title: 'Ley 1851 de 2017',
    body: 'Establece medidas contra la pesca ilegal y el delito de ejercicio ilícito de la actividad pesquera; ordena la formalización de los pescadores artesanales.',
  },
  {
    title: 'AUNAP',
    body: 'Autoridad Nacional de Acuicultura y Pesca: titular de la potestad de administrar, controlar y sancionar la actividad pesquera.',
  },
  {
    title: 'ODS 14 — Vida submarina',
    body: 'Meta 14.4: reglamentar la explotación pesquera y poner fin a la pesca ilegal, no declarada y no reglamentada, y a la sobrepesca.',
  },
];

const ROADMAP = [
  {
    version: 'MVP v1.0',
    title: 'Núcleo de control y legalidad',
    items: ['Registro y autenticación', 'Permisos y licencias', 'Embarcaciones y artes', 'Bitácora de faenas', 'Especies y tallas mínimas', 'Vedas', 'Administración y auditoría'],
    status: 'Disponible',
    statusColor: 'success' as const,
    current: false,
  },
  {
    version: 'MVP v1.1',
    title: 'Control avanzado y trazabilidad',
    items: ['Control de cuotas', 'Geolocalización de zona', 'Trazabilidad con QR', 'Denuncias ciudadanas', 'Alertas y notificaciones', 'Tablero de indicadores', 'Documentos y reportes oficiales'],
    status: 'Disponible',
    statusColor: 'success' as const,
    current: true,
  },
  {
    version: 'Versión 2.0',
    title: 'Sostenibilidad avanzada y mercado',
    items: ['Rendimiento Máximo Sostenible', 'Áreas Marinas Protegidas', 'Datos ambientales', 'Comercialización sostenible', 'Educación pesquera'],
    status: 'Hoja de ruta',
    statusColor: 'info' as const,
    current: false,
  },
];

/**
 * Ejemplo de la verificación que hace el sistema sobre una captura. Usa la especie y la talla
 * mínima de los datos de demostración del backend (Pargo rojo, 35 cm); se rotula como ejemplo.
 */
const LEGALITY_CHECKS = [
  { label: 'Permiso de pesca vigente', detail: 'PER-2026-0142' },
  { label: 'Talla sobre la mínima legal', detail: '41 cm ≥ 35 cm' },
  { label: 'Fuera de periodo de veda', detail: 'Caribe · sin veda activa' },
];

const SECTION_SX = { scrollMarginTop: NAV_HEIGHT + 16 } as const;

function SectionHeading({ title, lede }: { title: string; lede?: string }) {
  return (
    <Box sx={{ mb: { xs: 3, md: 4 }, maxWidth: 680 }}>
      <Typography variant="h3" component="h2" sx={{ fontSize: { xs: '1.75rem', md: '2.375rem' }, textWrap: 'balance' }}>
        {title}
      </Typography>
      {lede && (
        <Typography color="text.secondary" sx={{ mt: 1.5, fontSize: '1.0625rem' }}>
          {lede}
        </Typography>
      )}
    </Box>
  );
}

function Shell({ children, sx }: { children: ReactNode; sx?: object }) {
  return <Box sx={{ maxWidth: 1280, mx: 'auto', px: { xs: 2, sm: 3, md: 4 }, ...sx }}>{children}</Box>;
}

export function HomePage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const primaryCta = user
    ? { label: 'Ir a mi panel', onClick: () => void navigate('/app/dashboard') }
    : { label: 'Solicitar acceso', onClick: () => void navigate('/register') };

  const mobileDrawer = (
    <Box sx={{ width: 300, p: 2 }} role="presentation">
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
              sx={{ display: 'block', width: '100%', py: 1.25, px: 1.5, fontWeight: 600, borderRadius: 2, '&:hover': { bgcolor: 'rgba(11,59,92,.06)' } }}
            >
              {link.label}
            </Link>
          </ListItem>
        ))}
      </List>
      <Stack spacing={1.5} sx={{ mt: 2 }}>
        <Button component={RouterLink} to="/login" variant="outlined" fullWidth>Iniciar sesión</Button>
        <Button component={RouterLink} to="/register" variant="contained" color="secondary" fullWidth>Registrarse</Button>
      </Stack>
    </Box>
  );

  return (
    <Box>
      <SkipLink />
      <TopUtilityBar />

      {/* Encabezado público: vidrio ahumado que deja ver la foto del hero al pasar por encima */}
      <Box component="header" sx={{ ...glass.smoked, position: 'sticky', top: 0, zIndex: 1100, color: '#fff' }}>
        <Toolbar sx={{ maxWidth: 1280, mx: 'auto', width: '100%', minHeight: NAV_HEIGHT, px: { xs: 2, sm: 3, md: 4 } }} disableGutters>
          <Box sx={{ flexGrow: 1 }}>
            <Logotype variant="light" size={34} showTagline={false} />
          </Box>
          <Stack component="nav" aria-label="Secciones del portal" direction="row" spacing={0.5} sx={{ display: { xs: 'none', lg: 'flex' }, mr: 2 }}>
            {NAV_LINKS.map((link) => (
              <Link
                key={link.label}
                href={link.href}
                underline="none"
                sx={{
                  px: 1.5,
                  py: 0.75,
                  borderRadius: 999,
                  fontWeight: 600,
                  fontSize: '.9375rem',
                  color: 'rgba(255,255,255,.86)',
                  transition: 'background-color .2s, color .2s',
                  '&:hover': { color: '#fff', bgcolor: 'rgba(255,255,255,.1)' },
                }}
              >
                {link.label}
              </Link>
            ))}
          </Stack>
          <Stack direction="row" spacing={1.25} sx={{ display: { xs: 'none', sm: 'flex' } }}>
            {user ? (
              <Button component={RouterLink} to="/app/dashboard" variant="contained" color="secondary">
                Ir a mi panel
              </Button>
            ) : (
              <>
                <Button
                  component={RouterLink}
                  to="/login"
                  variant="outlined"
                  sx={{ color: '#fff', borderColor: 'rgba(255,255,255,.4)', bgcolor: 'rgba(255,255,255,.06)', '&:hover': { borderColor: '#fff', bgcolor: 'rgba(255,255,255,.14)' } }}
                >
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
            sx={{ display: { xs: 'inline-flex', lg: 'none' }, ml: 1 }}
          >
            <MenuIcon />
          </IconButton>
        </Toolbar>
      </Box>
      <Drawer anchor="right" open={mobileNavOpen} onClose={() => setMobileNavOpen(false)} slotProps={{ paper: { sx: { borderRadius: '20px 0 0 20px' } } }}>
        {mobileDrawer}
      </Drawer>

      <Box component="main" id="main-content">
        {/* Hero: la faena al amanecer, con el trámite flotando encima como vidrio */}
        <Box
          id="inicio"
          component="section"
          aria-labelledby="hero-title"
          sx={{
            ...SECTION_SX,
            position: 'relative',
            isolation: 'isolate',
            overflow: 'hidden',
            mt: `-${NAV_HEIGHT}px`,
            pt: `${NAV_HEIGHT}px`,
            minHeight: { xs: 'auto', md: 'min(860px, calc(100svh - 28px))' },
            display: 'flex',
            alignItems: 'flex-end',
            bgcolor: '#071E30',
          }}
        >
          <Box
            component="img"
            src="/images/hero-pescador.jpg"
            alt="Pescadores artesanales lanzando una atarraya desde una canoa al amanecer"
            sx={{
              position: 'absolute',
              zIndex: -2,
              top: 0,
              left: 0,
              height: { xs: 520, md: '100%' },
              width: { xs: '100%', md: '128%' },
              objectFit: 'cover',
              objectPosition: { xs: '72% center', md: 'left center' },
            }}
          />
          <Box
            aria-hidden
            sx={{
              position: 'absolute',
              inset: 0,
              zIndex: -1,
              background: {
                xs: 'linear-gradient(180deg, rgba(7,30,48,.5) 0px, rgba(7,30,48,0) 160px, rgba(7,30,48,0) 360px, #071E30 520px)',
                md: 'linear-gradient(90deg, rgba(7,30,48,.55) 0%, rgba(7,30,48,.1) 45%, rgba(7,30,48,0) 70%), linear-gradient(0deg, rgba(7,30,48,.35) 0%, rgba(7,30,48,0) 40%)',
              },
            }}
          />

          <Shell sx={{ width: '100%', pb: { xs: 3, md: 7 }, pt: { xs: 36, sm: 40, md: 10 } }}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={{ xs: 2, md: 4 }} alignItems={{ md: 'flex-end' }} justifyContent="space-between">
              <Box
                sx={{
                  ...glass.paneStrong,
                  backgroundColor: 'rgba(255,255,255,0.78)',
                  borderRadius: '28px',
                  p: { xs: 3, sm: 4, md: 5 },
                  maxWidth: 600,
                  animation: 'pane-rise .9s cubic-bezier(.16,1,.3,1) both',
                  '@keyframes pane-rise': {
                    from: { opacity: 0.001, transform: 'translateY(16px)', backdropFilter: 'blur(0px)' },
                    to: { opacity: 1, transform: 'none' },
                  },
                  '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
                }}
              >
                <Typography
                  id="hero-title"
                  variant="h1"
                  sx={{ fontSize: { xs: '2.125rem', sm: '2.75rem', md: '3.25rem' }, lineHeight: 1.05, color: 'primary.dark', textWrap: 'balance' }}
                >
                  Sistema de Gestión de Pesca Sostenible
                </Typography>
                <Typography sx={{ mt: 2.5, mb: 4, color: 'text.secondary', fontSize: { xs: '1rem', md: '1.125rem' }, lineHeight: 1.6 }}>
                  Registro digital de faenas, validación de permisos y cuotas, trazabilidad de la
                  cadena de valor y monitoreo de vedas, para reducir la pesca ilegal y fortalecer el
                  aprovechamiento sostenible del recurso.
                </Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                  <Button size="large" variant="contained" color="secondary" endIcon={<ArrowForwardIcon />} onClick={primaryCta.onClick}>
                    {primaryCta.label}
                  </Button>
                  <Button size="large" variant="outlined" href="#accesos">
                    Consultar trámites
                  </Button>
                </Stack>
              </Box>

              {/* Demostración del mecanismo: una captura verificada contra la norma */}
              <Box
                role="figure"
                aria-label="Ejemplo de verificación de legalidad de una captura"
                sx={{
                  ...glass.pane,
                  backgroundColor: 'rgba(255,255,255,0.7)',
                  borderRadius: '22px',
                  p: 2.5,
                  width: { xs: '100%', md: 340 },
                  flexShrink: 0,
                  animation: 'pane-rise .9s .25s cubic-bezier(.16,1,.3,1) both',
                  '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
                }}
              >
                <Stack direction="row" justifyContent="space-between" alignItems="baseline" sx={{ mb: 1.5 }}>
                  <Box>
                    <Typography fontWeight={800} color="primary.dark">Pargo rojo · 41 cm</Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                      Lutjanus purpureus
                    </Typography>
                  </Box>
                  <Chip label="Legal" color="success" size="small" />
                </Stack>
                <Stack spacing={1}>
                  {LEGALITY_CHECKS.map((check) => (
                    <Stack key={check.label} direction="row" spacing={1.25} alignItems="center">
                      <Box sx={{ width: 22, height: 22, borderRadius: '50%', bgcolor: 'success.main', color: '#fff', display: 'grid', placeItems: 'center', flexShrink: 0 }}>
                        <CheckIcon sx={{ fontSize: 15 }} />
                      </Box>
                      <Box sx={{ minWidth: 0 }}>
                        <Typography variant="body2" fontWeight={650} lineHeight={1.25}>{check.label}</Typography>
                        <Typography variant="caption" color="text.secondary">{check.detail}</Typography>
                      </Box>
                    </Stack>
                  ))}
                </Stack>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1.75, pt: 1.25, borderTop: '1px solid', borderColor: 'divider' }}>
                  Ejemplo ilustrativo con datos de demostración.
                </Typography>
              </Box>
            </Stack>
            <Typography variant="caption" sx={{ display: 'block', mt: 2, color: 'rgba(255,255,255,.75)', textAlign: { xs: 'left', md: 'right' } }}>
              Foto: Sahaj Patel / Unsplash
            </Typography>
          </Shell>
        </Box>

        {/* Trámites: una ventanilla única, no una cuadrícula de tarjetas */}
        <Box id="accesos" component="section" sx={{ ...SECTION_SX, py: { xs: 7, md: 11 } }}>
          <Shell>
            <SectionHeading
              title="Accesos rápidos"
              lede="Inicia sesión para operar cada módulo. Si aún no tienes cuenta, el sistema te guiará a crearla antes de continuar."
            />
            <Box
              component="ul"
              sx={{
                ...glass.pane,
                listStyle: 'none',
                m: 0,
                p: { xs: 1, md: 1.5 },
                borderRadius: '24px',
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
                columnGap: 1.5,
              }}
            >
              {QUICK_ACCESS.map((item) => (
                <Box component="li" key={item.label}>
                  <ButtonBase
                    component={RouterLink}
                    to={item.to}
                    sx={{
                      width: '100%',
                      justifyContent: 'flex-start',
                      textAlign: 'left',
                      gap: 2,
                      p: { xs: 1.75, md: 2.25 },
                      borderRadius: '16px',
                      transition: 'background-color .25s, box-shadow .25s',
                      '& .qa-arrow': { transition: 'transform .3s cubic-bezier(.16,1,.3,1), color .2s' },
                      '&:hover, &.Mui-focusVisible': {
                        bgcolor: 'rgba(255,255,255,.75)',
                        boxShadow: '0 8px 20px -10px rgba(11,59,92,.35)',
                        '& .qa-arrow': { transform: 'translateX(4px)', color: 'secondary.main' },
                      },
                      '&.Mui-focusVisible': { outline: '3px solid', outlineColor: 'secondary.main' },
                    }}
                  >
                    <Box
                      sx={(t) => ({
                        width: 48,
                        height: 48,
                        flexShrink: 0,
                        borderRadius: '14px',
                        display: 'grid',
                        placeItems: 'center',
                        color: 'secondary.dark',
                        bgcolor: alpha(t.palette.secondary.main, 0.12),
                        boxShadow: `inset 0 0 0 1px ${alpha(t.palette.secondary.main, 0.18)}`,
                      })}
                    >
                      {item.icon}
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography fontWeight={750} fontSize="1.0625rem" color="primary.dark">{item.label}</Typography>
                      <Typography variant="body2" color="text.secondary">{item.description}</Typography>
                    </Box>
                    <ArrowForwardIcon className="qa-arrow" sx={{ color: 'text.secondary' }} />
                  </ButtonBase>
                </Box>
              ))}
            </Box>
          </Shell>
        </Box>

        {/* Misión: banda de vidrio ahumado sobre la carta náutica */}
        <Box id="nosotros" component="section" sx={{ ...SECTION_SX, pb: { xs: 7, md: 11 } }}>
          <Shell>
            <Box
              sx={{
                ...glass.smoked,
                backgroundColor: 'rgba(7,30,48,0.84)',
                borderBottom: 'none',
                border: '1px solid rgba(255,255,255,.12)',
                borderRadius: '28px',
                color: '#fff',
                p: { xs: 3, sm: 5, md: 7 },
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: '1.5fr 1fr' },
                gap: { xs: 4, md: 8 },
                alignItems: 'center',
              }}
            >
              <Box>
                <Typography variant="h3" component="h2" sx={{ fontSize: { xs: '1.75rem', md: '2.375rem' }, mb: 2.5, textWrap: 'balance' }}>
                  Pesca legal, trazable y sostenible
                </Typography>
                <Typography sx={{ color: 'rgba(255,255,255,.84)', mb: 2, lineHeight: 1.7 }}>
                  Diseñamos y documentamos la base de ingeniería de requerimientos y de diseño de
                  interfaces de un sistema que permite el registro, control, trazabilidad y
                  monitoreo de la actividad pesquera, contribuyendo a la reducción de la pesca
                  ilegal y al aprovechamiento sostenible del recurso.
                </Typography>
                <Typography sx={{ color: 'rgba(255,255,255,.84)', lineHeight: 1.7 }}>
                  Si se implementa un sistema que integre el registro digital de faenas, la
                  validación automática de permisos y cuotas, la trazabilidad de la cadena de valor
                  y el monitoreo de vedas y zonas protegidas, se reducirá la pesca ilegal y se
                  fortalecerá el aprovechamiento sostenible del recurso — en alineación con el ODS
                  14 y la legislación pesquera colombiana.
                </Typography>
              </Box>
              <Box component="dl" sx={{ m: 0, display: 'grid', gap: 0 }}>
                {SCOPE_BADGES.map((badge, index) => (
                  <Stack
                    key={badge.label}
                    direction="row"
                    alignItems="center"
                    sx={{ gap: 2.5, py: 2.25, borderTop: index === 0 ? 'none' : '1px solid rgba(255,255,255,.14)' }}
                  >
                    <Typography component="dt" sx={{ order: 1, color: 'rgba(255,255,255,.88)', fontWeight: 600 }}>
                      {badge.label}
                    </Typography>
                    <Typography component="dd" sx={{ m: 0, order: 0, minWidth: 64, fontSize: '3rem', fontWeight: 800, lineHeight: 1, color: 'secondary.light' }}>
                      {badge.value}
                    </Typography>
                  </Stack>
                ))}
              </Box>
            </Box>
          </Shell>
        </Box>

        {/* Normatividad: un registro legal, cada norma con su cita */}
        <Box id="normatividad" component="section" sx={{ ...SECTION_SX, pb: { xs: 7, md: 11 } }}>
          <Shell>
            <SectionHeading title="Normatividad" lede="El sistema traduce estas obligaciones legales en registros verificables." />
            <Box
              sx={{
                ...glass.pane,
                borderRadius: '24px',
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(4, 1fr)' },
                overflow: 'hidden',
              }}
            >
              {NORMATIVIDAD.map((item, index) => (
                <Box
                  key={item.title}
                  component="article"
                  sx={{
                    p: { xs: 3, md: 3.5 },
                    borderTop: { xs: index === 0 ? 'none' : '1px solid', sm: index < 2 ? 'none' : '1px solid', lg: 'none' },
                    borderLeft: { xs: 'none', sm: index % 2 === 1 ? '1px solid' : 'none', lg: index === 0 ? 'none' : '1px solid' },
                    borderColor: { xs: 'divider', sm: 'divider', lg: 'divider' },
                  }}
                >
                  <Box sx={{ color: 'secondary.dark', mb: 2, display: 'flex' }}>
                    {item.title === 'AUNAP' ? <AccountBalanceIcon /> : item.title.startsWith('ODS') ? <PublicIcon /> : <GavelIcon />}
                  </Box>
                  <Typography variant="h6" component="h3" sx={{ fontWeight: 800, mb: 1, color: 'primary.dark' }}>{item.title}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.65 }}>{item.body}</Typography>
                </Box>
              ))}
            </Box>
          </Shell>
        </Box>

        {/* Hoja de ruta: una derrota con tres puntos, el primero ya alcanzado */}
        <Box id="programas" component="section" sx={{ ...SECTION_SX, pb: { xs: 7, md: 11 } }}>
          <Shell>
            <SectionHeading
              title="Hoja de ruta del sistema"
              lede="Construcción incremental del producto, priorizando primero el control legal y el núcleo de operación."
            />
            <Box
              component="ol"
              sx={{
                listStyle: 'none',
                m: 0,
                p: 0,
                position: 'relative',
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' },
                gap: { xs: 2, md: 2.5 },
              }}
            >
              <Box
                aria-hidden
                sx={{
                  display: { xs: 'none', md: 'block' },
                  position: 'absolute',
                  top: 31,
                  left: 32,
                  right: 32,
                  borderTop: '2px dashed',
                  borderColor: 'rgba(11,59,92,.28)',
                }}
              />
              {ROADMAP.map((phase) => (
                <Box component="li" key={phase.version} sx={{ position: 'relative' }}>
                  <Box
                    sx={{
                      width: 64,
                      height: 64,
                      borderRadius: '50%',
                      display: 'grid',
                      placeItems: 'center',
                      mb: 2,
                      position: 'relative',
                      ...(phase.current
                        ? { bgcolor: 'primary.dark', color: '#fff', boxShadow: '0 0 0 6px rgba(14,124,134,.22), 0 10px 24px -8px rgba(7,30,48,.6)' }
                        : { ...glass.paneStrong, color: 'primary.dark' }),
                    }}
                  >
                    <Typography fontWeight={800} fontSize=".8125rem" textAlign="center" lineHeight={1.1}>
                      {phase.version.replace(/^(MVP |Versión )v?/, 'v')}
                    </Typography>
                  </Box>
                  <Box sx={{ ...(phase.current ? glass.paneStrong : glass.pane), borderRadius: '20px', p: 3, height: { md: 'calc(100% - 80px)' } }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1} sx={{ mb: 1 }}>
                      <Typography variant="body2" color="text.secondary" fontWeight={650}>{phase.version}</Typography>
                      <Chip label={phase.status} color={phase.statusColor} size="small" />
                    </Stack>
                    <Typography variant="h6" component="h3" sx={{ fontWeight: 800, mb: 1.5, color: 'primary.dark' }}>{phase.title}</Typography>
                    <Box component="ul" sx={{ m: 0, pl: 2.25, color: 'text.secondary', '& li': { mb: 0.5, fontSize: '.875rem' }, '& li::marker': { color: 'secondary.main' } }}>
                      {phase.items.map((line) => (
                        <li key={line}>{line}</li>
                      ))}
                    </Box>
                  </Box>
                </Box>
              ))}
            </Box>
          </Shell>
        </Box>

        {/* Contacto */}
        <Box id="contacto" component="section" sx={{ ...SECTION_SX, pb: { xs: 8, md: 12 } }}>
          <Shell>
            <SectionHeading
              title="¿Necesitas ayuda?"
              lede="Nuestros canales de atención están disponibles para pescadores, armadores, compradores y entidades de control."
            />
            <Box
              sx={{
                ...glass.pane,
                borderRadius: '24px',
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' },
              }}
            >
              {[
                { icon: <PhoneIcon />, title: 'Línea de atención', body: '01 8000 000 000' },
                { icon: <EmailIcon />, title: 'Correo institucional', body: 'contacto@sigepes.gov.co' },
                { icon: <AccountBalanceIcon />, title: 'PQRS', body: 'Radica tu solicitud por correo institucional' },
              ].map((channel, index) => (
                <Stack
                  key={channel.title}
                  direction="row"
                  spacing={2}
                  alignItems="flex-start"
                  sx={{
                    p: { xs: 2.5, md: 3.5 },
                    borderTop: { xs: index === 0 ? 'none' : '1px solid', md: 'none' },
                    borderLeft: { xs: 'none', md: index === 0 ? 'none' : '1px solid' },
                    borderColor: { xs: 'divider', md: 'divider' },
                  }}
                >
                  <Box sx={{ color: 'secondary.dark', display: 'flex', mt: 0.25 }}>{channel.icon}</Box>
                  <Box>
                    <Typography fontWeight={750} color="primary.dark">{channel.title}</Typography>
                    <Typography color="text.secondary">{channel.body}</Typography>
                  </Box>
                </Stack>
              ))}
            </Box>
          </Shell>
        </Box>
      </Box>

      <Footer />
    </Box>
  );
}
