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
import {
  AppBar, Avatar, Box, Button, Chip, Drawer, IconButton, List, ListItemButton,
  ListItemIcon, ListItemText, Toolbar, Typography, useMediaQuery, useTheme,
} from '@mui/material';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../modules/auth/store/authStore';
import { ROLE_LABEL } from '../../modules/auth/types/auth.types';
import { NAV_ITEMS } from '../../shared/permissions/navigation';

const WIDTH = 268;

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

  const items = NAV_ITEMS.filter((item) => (user ? item.roles.includes(user.role) : false));

  const go = (path: string) => {
    void navigate(path);
    setOpen(false);
  };

  const drawer = (
    <Box sx={{ width: WIDTH }}>
      <Box sx={{ p: 2.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <Avatar sx={{ bgcolor: 'secondary.main' }}>
          <PhishingIcon />
        </Avatar>
        <Box>
          <Typography variant="h6" color="primary" lineHeight={1}>SIGEPES</Typography>
          <Typography variant="caption" color="text.secondary">Gestión pesquera</Typography>
        </Box>
      </Box>
      <List sx={{ px: 1 }}>
        {items.map((item) => {
          const active = location.pathname.startsWith(item.path);
          return (
            <ListItemButton
              key={item.path}
              selected={active}
              onClick={() => go(item.path)}
              sx={{ borderRadius: 2, mb: 0.5, '&.Mui-selected': { bgcolor: 'primary.main', color: '#fff', '& .MuiListItemIcon-root': { color: '#fff' } } }}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>{ICONS[item.icon]}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          );
        })}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: theme.zIndex.drawer + 1 }}>
        <Toolbar>
          {!desktop && (
            <IconButton color="inherit" edge="start" onClick={() => setOpen(true)} aria-label="Abrir menú" sx={{ mr: 1 }}>
              <MenuIcon />
            </IconButton>
          )}
          <Typography sx={{ flexGrow: 1, fontWeight: 800 }}>{import.meta.env.VITE_APP_NAME ?? 'SIGEPES'}</Typography>
          {user && <Chip label={ROLE_LABEL[user.role]} color="secondary" size="small" sx={{ mr: 2, display: { xs: 'none', sm: 'inline-flex' } }} />}
          <Typography sx={{ mr: 2, display: { xs: 'none', md: 'block' } }}>{user?.fullName}</Typography>
          <Button
            color="inherit"
            startIcon={<LogoutIcon />}
            onClick={() => {
              logout();
              void navigate('/login');
            }}
          >
            Salir
          </Button>
        </Toolbar>
      </AppBar>

      {desktop ? (
        <Drawer variant="permanent" sx={{ width: WIDTH, flexShrink: 0, '& .MuiDrawer-paper': { width: WIDTH, pt: 8, border: 'none', boxShadow: '2px 0 12px rgba(10,37,64,.05)' } }}>
          {drawer}
        </Drawer>
      ) : (
        <Drawer open={open} onClose={() => setOpen(false)}>
          {drawer}
        </Drawer>
      )}

      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 4 }, pt: { xs: 9, md: 12 }, minWidth: 0 }}>
        <Outlet />
      </Box>
    </Box>
  );
}
