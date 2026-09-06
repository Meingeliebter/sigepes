import BlockIcon from '@mui/icons-material/Block';
import CloudSyncIcon from '@mui/icons-material/CloudSync';
import GavelIcon from '@mui/icons-material/Gavel';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { Avatar, Card, List, ListItem, ListItemAvatar, ListItemText, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { PageHeader } from '../../../shared/components/PageHeader';

interface Notification {
  id: string;
  title: string;
  body: string;
  date: string;
  icon: ReactNode;
  color: string;
}

const NOTIFICATIONS: Notification[] = [
  { id: 'n1', title: 'Veda de camarón activada', body: 'Restricción vigente en el Pacífico Norte hasta el 30 de octubre.', date: 'Hace 2 h', icon: <BlockIcon />, color: '#C62828' },
  { id: 'n2', title: 'Permiso próximo a vencer', body: 'El permiso PES-0231 vence en 15 días.', date: 'Hace 1 día', icon: <GavelIcon />, color: '#ED6C02' },
  { id: 'n3', title: 'Faena sincronizada', body: 'La faena T-1042 se sincronizó correctamente.', date: 'Hace 2 días', icon: <CloudSyncIcon />, color: '#1E7A46' },
  { id: 'n4', title: 'Zona restringida cercana', body: 'Estás próximo a un Área Marina Protegida.', date: 'Hace 3 días', icon: <WarningAmberIcon />, color: '#0A6572' },
];

export function NotificationsPage() {
  return (
    <>
      <PageHeader title="Notificaciones" subtitle="Alertas regulatorias y operativas" />
      <Card>
        <List disablePadding>
          {NOTIFICATIONS.map((item, index) => (
            <ListItem key={item.id} divider={index < NOTIFICATIONS.length - 1} alignItems="flex-start">
              <ListItemAvatar>
                <Avatar sx={{ bgcolor: item.color }}>{item.icon}</Avatar>
              </ListItemAvatar>
              <ListItemText
                primary={<Typography fontWeight={700}>{item.title}</Typography>}
                secondary={
                  <>
                    <Typography component="span" variant="body2" color="text.primary">{item.body}</Typography>
                    <Typography component="span" variant="caption" color="text.secondary" display="block">{item.date}</Typography>
                  </>
                }
              />
            </ListItem>
          ))}
        </List>
      </Card>
    </>
  );
}
