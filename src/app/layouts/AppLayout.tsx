import BlockIcon from '@mui/icons-material/Block';
import DashboardIcon from '@mui/icons-material/Dashboard';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import GavelIcon from '@mui/icons-material/Gavel';
import HistoryIcon from '@mui/icons-material/History';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import MenuIcon from '@mui/icons-material/Menu';
import NotificationsIcon from '@mui/icons-material/Notifications';
import PeopleIcon from '@mui/icons-material/People';
import PhishingIcon from '@mui/icons-material/Phishing';
import WifiIcon from '@mui/icons-material/Wifi';
import WifiOffIcon from '@mui/icons-material/WifiOff';
import {
  Alert, AppBar, Box, Button, Chip, Drawer, IconButton, List, ListItemButton,
  ListItemIcon, ListItemText, ListSubheader, Toolbar, Typography, useMediaQuery, useTheme,
  BottomNavigation, BottomNavigationAction,
} from '@mui/material';
import type { ReactNode } from 'react';
import { useEffect, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { authService } from '../../modules/auth/api/auth.service';
import { useAuthStore } from '../../modules/auth/store/authStore';
import { ROLE_LABEL } from '../../modules/auth/types/auth.types';
import { AppBreadcrumbs } from '../../shared/components/AppBreadcrumbs';
import { Footer } from '../../shared/components/Footer';
import { Logotype } from '../../shared/components/Logotype';
import { SkipLink } from '../../shared/components/SkipLink';
import { TopUtilityBar } from '../../shared/components/TopUtilityBar';
import { NAV_GROUPS, NAV_ITEMS } from '../../shared/permissions/navigation';
import { pendingTripCount } from '../../shared/storage/tripOfflineStore';

const WIDTH = 272;

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
};

export function AppLayout() {
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up('md'));
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const [online, setOnline] = useState(() => navigator.onLine);
  const [pending, setPending] = useState(() => pendingTripCount());

  const items = NAV_ITEMS.filter((item) => (user ? item.roles.includes(user.role) : false));
  const mobileItems = items.slice(0, 5);

  useEffect(() => {
    const setOnlineState = () => setOnline(navigator.onLine);
    window.addEventListener('online', setOnlineState);
    window.addEventListener('offline', setOnlineState);
    const refreshPending = () => setPending(pendingTripCount());
    window.addEventListener('storage', refreshPending);
    return () => {
      window.removeEventListener('online', setOnlineState);
      window.removeEventListener('offline', setOnlineState);
      window.removeEventListener('storage', refreshPending);
    };
  }, []);

  const go = (path: string) => {
    void navigate(path);
    setOpen(false);
  };

  const drawer = (
    <Box sx={{ width: WIDTH, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ p: 2.5 }}>
        <Logotype size={36} />
      </Box>
      <List sx={{ px: 1.5, flex: 1, overflowY: 'auto' }}>
        {NAV_GROUPS.map((group) => {
          const groupItems = items.filter((item) => item.group === group);
          if (groupItems.length === 0) return null;
          return (
            <li key={group}>
              <ul style={{ padding: 0, margin: 0, listStyle: 'none' }}>
                <ListSubheader disableSticky>{group}</ListSubheader>
                {groupItems.map((item) => {
                  const active = location.pathname.startsWith(item.path);
                  return (
                    <ListItemButton key={item.path} selected={active} onClick={() => go(item.path)} sx={{ mb: 0.5 }}>
                      <ListItemIcon sx={{ minWidth: 40 }}>{ICONS[item.icon]}</ListItemIcon>
                      <ListItemText primary={item.label} />
                    </ListItemButton>
                  );
                })}
              </ul>
            </li>
          );
        })}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <SkipLink />

      <Box sx={{ position: 'sticky', top: 0, zIndex: theme.zIndex.appBar }}>
        <TopUtilityBar />
        <AppBar position="static">
          <Toolbar>
            {!desktop && (
              <IconButton color="inherit" edge="start" onClick={() => setOpen(true)} aria-label="Abrir menú de navegación" sx={{ mr: 1 }}>
                <MenuIcon />
              </IconButton>
            )}
            <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center' }}>
              <Logotype variant="light" size={32} showTagline={desktop} compact={!desktop} />
            </Box>
            <IconButton
              color="inherit"
              aria-label="Ver notificaciones"
              onClick={() => go('/app/notifications')}
              sx={{ display: { xs: 'none', sm: 'inline-flex' }, mr: 1 }}
            >
              <NotificationsIcon />
            </IconButton>
            <Chip
              icon={online ? <WifiIcon /> : <WifiOffIcon />}
              label={online ? 'En línea' : 'Sin conexión'}
              color={online ? 'success' : 'warning'}
              size="small"
              sx={{ mr: 1, display: { xs: 'none', sm: 'inline-flex' }, color: '#fff', '& .MuiChip-icon': { color: '#fff' } }}
            />
            {user && (
              <Chip
                label={ROLE_LABEL[user.role]}
                size="small"
                sx={{ mr: 2, display: { xs: 'none', sm: 'inline-flex' }, bgcolor: 'secondary.main', color: '#fff' }}
              />
            )}
            <Typography sx={{ mr: 2, display: { xs: 'none', md: 'block' } }}>{user?.fullName}</Typography>
            <Button
              color="inherit"
              startIcon={<LogoutIcon />}
              onClick={() => {
                void authService.logout().finally(() => {
                  logout();
                  void navigate('/login');
                });
              }}
            >
              Salir
            </Button>
          </Toolbar>
        </AppBar>
      </Box>

      <Box sx={{ display: 'flex', flex: 1 }}>
        {desktop ? (
          <Drawer
            variant="permanent"
            sx={{ width: WIDTH, flexShrink: 0, '& .MuiDrawer-paper': { width: WIDTH, position: 'sticky', top: 0, height: '100vh' } }}
          >
            {drawer}
          </Drawer>
        ) : (
          <Drawer open={open} onClose={() => setOpen(false)}>
            {drawer}
          </Drawer>
        )}

        <Box component="main" id="main-content" sx={{ flexGrow: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
          <Box sx={{ flex: 1, p: { xs: 2, md: 4 }, pb: { xs: 12, md: 4 } }}>
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
          </Box>
          {desktop && <Footer />}
        </Box>
      </Box>

      {!desktop && (
        <BottomNavigation
          value={mobileItems.findIndex((item) => location.pathname.startsWith(item.path))}
          onChange={(_, index) => go(mobileItems[index].path)}
          sx={{ position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: theme.zIndex.appBar }}
        >
          {mobileItems.map((item) => (
            <BottomNavigationAction key={item.path} label={item.label} icon={ICONS[item.icon]} />
          ))}
        </BottomNavigation>
      )}
    </Box>
  );
}
