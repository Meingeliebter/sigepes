import BlockIcon from '@mui/icons-material/Block';
import CloseIcon from '@mui/icons-material/Close';
import InsightsIcon from '@mui/icons-material/Insights';
import FolderOpenOutlinedIcon from '@mui/icons-material/FolderOpenOutlined';
import SummarizeOutlinedIcon from '@mui/icons-material/SummarizeOutlined';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import MarkEmailUnreadOutlinedIcon from '@mui/icons-material/MarkEmailUnreadOutlined';
import DashboardIcon from '@mui/icons-material/Dashboard';
import DonutLargeIcon from '@mui/icons-material/DonutLarge';
import WrongLocationIcon from '@mui/icons-material/WrongLocation';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import GavelIcon from '@mui/icons-material/Gavel';
import HistoryIcon from '@mui/icons-material/History';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import MenuIcon from '@mui/icons-material/Menu';
import NotificationsIcon from '@mui/icons-material/Notifications';
import PeopleIcon from '@mui/icons-material/People';
import PhishingIcon from '@mui/icons-material/Phishing';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import WifiOffIcon from '@mui/icons-material/WifiOff';
import {
  Alert, AppBar, Avatar, Badge, Box, Button, Drawer, IconButton, List, ListItemButton, Snackbar,
  ListItemIcon, ListItemText, ListSubheader, Stack, Toolbar, Tooltip, Typography, useMediaQuery, useTheme,
  BottomNavigation, BottomNavigationAction,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useEffect, useRef, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authService } from '../../modules/auth/api/auth.service';
import { useAuthStore } from '../../modules/auth/store/authStore';
import { ROLE_LABEL } from '../../modules/auth/types/auth.types';
import { AppBreadcrumbs } from '../../shared/components/AppBreadcrumbs';
import { Footer } from '../../shared/components/Footer';
import { Logotype } from '../../shared/components/Logotype';
import { SkipLink } from '../../shared/components/SkipLink';
import { TopUtilityBar } from '../../shared/components/TopUtilityBar';
import { useOnlineStatus } from '../../shared/hooks/useOnlineStatus';
import { NAV_GROUPS, NAV_ITEMS, matchNavItem } from '../../shared/permissions/navigation';
import { pendingTripCount } from '../../shared/storage/tripOfflineStore';
import { syncPendingTrips } from '../../shared/sync/tripSync';
import { flushReportOutbox } from '../../shared/sync/reportSync';
import { glass } from '../../shared/theme/theme';
import { useAlertStream } from '../../shared/alerts/useAlertStream';
import { domainService, type AppAlert } from '../../shared/api/domain.service';

const WIDTH = 264;
const BAR_HEIGHT = 64;
const GUTTER = 16;

const ICONS: Record<string, ReactNode> = {
  dashboard: <DashboardIcon />,
  boat: <DirectionsBoatIcon />,
  book: <MenuBookIcon />,
  fish: <PhishingIcon />,
  block: <BlockIcon />,
  gavel: <GavelIcon />,
  bell: <NotificationsIcon />,
  people: <PeopleIcon />,
  history: <HistoryIcon />,
  qrcode: <QrCode2Icon />,
  quota: <DonutLargeIcon />,
  zone: <WrongLocationIcon />,
  report: <CampaignOutlinedIcon />,
  insights: <InsightsIcon />,
  folder: <FolderOpenOutlinedIcon />,
  summarize: <SummarizeOutlinedIcon />,
  inbox: <MarkEmailUnreadOutlinedIcon />,
};

function initials(name?: string) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

export function AppLayout() {
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up('md'));
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const online = useOnlineStatus();
  // AC-11.2: la campana muestra las no leídas. El canal en vivo la actualiza al instante y esta
  // consulta es el respaldo si el canal se cae (AC-11.5).
  const unreadQuery = useQuery({
    queryKey: ['alerts', 'unread'],
    queryFn: domainService.unreadAlerts,
    refetchInterval: 60_000,
    enabled: !!user,
  });
  const unread = unreadQuery.data ?? 0;
  const [liveAlert, setLiveAlert] = useState<AppAlert | null>(null);
  // AC-11.6: una alerta crítica o de atención se muestra en cuanto llega, esté donde esté el usuario.
  useAlertStream((alert) => {
    if (alert.severity !== 'INFO') setLiveAlert(alert);
  });
  const queryClient = useQueryClient();
  const [pending, setPending] = useState(() => pendingTripCount());
  const wasOnline = useRef(online);

  const items = NAV_ITEMS.filter((item) => (user ? item.roles.includes(user.role) : false));
  const mobileItems = items.slice(0, 5);
  const activePath = matchNavItem(location.pathname, items)?.path;

  useEffect(() => {
    const refreshPending = () => setPending(pendingTripCount());
    window.addEventListener('storage', refreshPending);
    return () => window.removeEventListener('storage', refreshPending);
  }, []);

  // AC-03.1: al recuperar conexión, sincroniza automáticamente las faenas pendientes sin
  // esperar a que el usuario esté en la pantalla de bitácora ni pulse el botón manual.
  useEffect(() => {
    if (!wasOnline.current && online && pendingTripCount() > 0) {
      void syncPendingTrips(queryClient).then(() => setPending(pendingTripCount()));
    }
    // AC-10.6: igual con las denuncias redactadas sin señal.
    if (online) void flushReportOutbox();
    wasOnline.current = online;
  }, [online, queryClient]);

  const go = (path: string) => {
    void navigate(path);
    setOpen(false);
  };

  const signOut = () => {
    void authService.logout().finally(() => {
      logout();
      void navigate('/login');
    });
  };

  const navList = (
    <List sx={{ px: 1.5, py: 1, flex: 1, overflowY: 'auto' }} aria-label="Módulos del sistema">
      {NAV_GROUPS.map((group) => {
        const groupItems = items.filter((item) => item.group === group);
        if (groupItems.length === 0) return null;
        return (
          <li key={group}>
            <ul style={{ padding: 0, margin: 0, listStyle: 'none' }}>
              <ListSubheader disableSticky sx={{ px: 1.5, mt: 1 }}>{group}</ListSubheader>
              {groupItems.map((item) => {
                const active = item.path === activePath;
                return (
                  <ListItemButton
                    key={item.path}
                    selected={active}
                    aria-current={active ? 'page' : undefined}
                    onClick={() => go(item.path)}
                    sx={{ mb: 0.5, minHeight: 44, px: 1.5 }}
                  >
                    <ListItemIcon sx={{ minWidth: 36 }}>{ICONS[item.icon]}</ListItemIcon>
                    <ListItemText primary={item.label} slotProps={{ primary: { variant: 'body2', fontWeight: 600 } }} />
                  </ListItemButton>
                );
              })}
            </ul>
          </li>
        );
      })}
    </List>
  );

  /** Identidad del usuario al pie del panel lateral: quién opera y con qué rol. */
  const identity = user && (
    <Stack
      direction="row"
      spacing={1.5}
      alignItems="center"
      sx={{ m: 1.5, p: 1.25, borderRadius: 3, bgcolor: alpha(theme.palette.primary.main, 0.05) }}
    >
      <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '.85rem', fontWeight: 700 }}>
        {initials(user.fullName)}
      </Avatar>
      <Box sx={{ minWidth: 0, flex: 1 }}>
        <Typography variant="body2" fontWeight={700} noWrap>{user.fullName}</Typography>
        <Typography variant="caption" color="secondary.dark" fontWeight={650}>{ROLE_LABEL[user.role]}</Typography>
      </Box>
      <Tooltip title="Cerrar sesión">
        <IconButton size="small" onClick={signOut} aria-label="Cerrar sesión">
          <LogoutIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </Stack>
  );

  const connection = (
    <Stack
      direction="row"
      spacing={1}
      alignItems="center"
      role="status"
      sx={{
        px: 1.5,
        height: 32,
        borderRadius: 999,
        border: '1px solid rgba(255,255,255,.16)',
        bgcolor: 'rgba(255,255,255,.06)',
        display: { xs: 'none', sm: 'flex' },
      }}
    >
      <Box
        sx={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          bgcolor: online ? '#5FD39A' : '#FFB454',
          boxShadow: online ? '0 0 0 3px rgba(95,211,154,.22)' : '0 0 0 3px rgba(255,180,84,.22)',
        }}
      />
      <Typography variant="caption" fontWeight={650} sx={{ color: 'rgba(255,255,255,.9)' }}>
        {online ? 'En línea' : 'Sin conexión'}
      </Typography>
    </Stack>
  );

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <SkipLink />
      <TopUtilityBar />

      <AppBar position="sticky" sx={{ top: 0 }}>
        <Toolbar sx={{ minHeight: BAR_HEIGHT, gap: 1.5 }}>
          {!desktop && (
            <IconButton color="inherit" edge="start" onClick={() => setOpen(true)} aria-label="Abrir menú de navegación">
              <MenuIcon />
            </IconButton>
          )}
          <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center' }}>
            <Logotype variant="light" size={32} showTagline={desktop} compact={!desktop} />
          </Box>
          {connection}
          <Tooltip title="Notificaciones">
            <IconButton
              color="inherit"
              aria-label={unread > 0 ? `Ver notificaciones, ${unread} sin leer` : 'Ver notificaciones'}
              onClick={() => go('/app/notifications')}
            >
              <Badge badgeContent={unread} color="error" max={99}>
                <NotificationsIcon />
              </Badge>
            </IconButton>
          </Tooltip>
          {!desktop && (
            <Button color="inherit" startIcon={<LogoutIcon />} onClick={signOut}>
              Salir
            </Button>
          )}
        </Toolbar>
      </AppBar>

      <Box sx={{ display: 'flex', flex: 1, alignItems: 'flex-start' }}>
        {desktop ? (
          <Box
            component="nav"
            aria-label="Navegación principal"
            sx={{
              ...glass.paneStrong,
              width: WIDTH,
              flexShrink: 0,
              position: 'sticky',
              top: BAR_HEIGHT + GUTTER,
              m: `${GUTTER}px`,
              mr: 0,
              height: `calc(100vh - ${BAR_HEIGHT + GUTTER * 2}px)`,
              borderRadius: '20px',
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}
          >
            {navList}
            {identity}
          </Box>
        ) : (
          <Drawer open={open} onClose={() => setOpen(false)} slotProps={{ paper: { sx: { borderRadius: '0 20px 20px 0' } } }}>
            <Box sx={{ width: WIDTH + 16, height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ p: 2.5, pb: 1 }}>
                <Logotype size={36} />
              </Box>
              {navList}
              {identity}
            </Box>
          </Drawer>
        )}

        <Box component="main" id="main-content" sx={{ flexGrow: 1, minWidth: 0, display: 'flex', flexDirection: 'column', minHeight: `calc(100vh - ${BAR_HEIGHT}px)` }}>
          <Box sx={{ flex: 1, width: '100%', maxWidth: 1440, mx: 'auto', p: { xs: 2, md: 4 }, pt: { xs: 2, md: 3 }, pb: { xs: 13, md: 6 } }}>
            <AppBreadcrumbs />
            {(!online || pending > 0) && (
              <Alert
                severity="warning"
                icon={!online ? <WifiOffIcon fontSize="inherit" /> : undefined}
                sx={{ mb: 2 }}
              >
                {!online
                  ? 'Sin conexión — los registros se guardan en este dispositivo.'
                  : `${pending} faena(s) pendiente(s) de sincronización.`}
              </Alert>
            )}
            <Outlet />
            <Snackbar
              open={!!liveAlert}
              onClose={(_, reason) => reason !== 'clickaway' && setLiveAlert(null)}
              autoHideDuration={liveAlert?.severity === 'CRITICAL' ? null : 8000}
              anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
              sx={{ top: { xs: BAR_HEIGHT + 8, sm: BAR_HEIGHT + 12 } }}
            >
              {liveAlert ? (
                <Alert
                  severity={liveAlert.severity === 'CRITICAL' ? 'error' : 'warning'}
                  variant="filled"
                  onClose={() => setLiveAlert(null)}
                  action={
                    <>
                      {liveAlert.link && (
                        <Button color="inherit" size="small" onClick={() => { go(liveAlert.link!); setLiveAlert(null); }}>
                          Ver
                        </Button>
                      )}
                      <IconButton color="inherit" size="small" aria-label="Cerrar aviso" onClick={() => setLiveAlert(null)}>
                        <CloseIcon fontSize="small" />
                      </IconButton>
                    </>
                  }
                  sx={{ maxWidth: 560, boxShadow: 6, alignItems: 'center' }}
                >
                  <strong>{liveAlert.title}</strong> — {liveAlert.message}
                </Alert>
              ) : undefined}
            </Snackbar>
          </Box>
          {desktop && <Footer compact />}
        </Box>
      </Box>

      {!desktop && (
        <BottomNavigation
          value={mobileItems.findIndex((item) => item.path === activePath)}
          onChange={(_, index) => go(mobileItems[index].path)}
          sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: theme.zIndex.appBar }}
        >
          {mobileItems.map((item) => (
            <BottomNavigationAction key={item.path} label={item.label.split(' ')[0]} aria-label={item.label} icon={ICONS[item.icon]} />
          ))}
        </BottomNavigation>
      )}
    </Box>
  );
}
