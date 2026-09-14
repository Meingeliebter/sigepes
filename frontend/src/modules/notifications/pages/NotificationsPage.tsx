import BlockIcon from '@mui/icons-material/Block';
import CloudSyncIcon from '@mui/icons-material/CloudSync';
import GavelIcon from '@mui/icons-material/Gavel';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { Alert, Avatar, Card, CircularProgress, List, ListItem, ListItemAvatar, ListItemText, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { domainService, type Notification } from '../../../shared/api/domain.service';

const ICON_BY_TYPE: Record<Notification['type'], ReactNode> = {
  CLOSURE: <BlockIcon />,
  PERMIT: <GavelIcon />,
  TRIP: <CloudSyncIcon />,
  INFO: <WarningAmberIcon />,
};

const COLOR_BY_TYPE: Record<Notification['type'], string> = {
  CLOSURE: 'error.main',
  PERMIT: 'warning.main',
  TRIP: 'success.main',
  INFO: 'info.main',
};

export function NotificationsPage() {
  const notificationsQuery = useQuery({ queryKey: ['notifications'], queryFn: domainService.notifications });
  const notifications = notificationsQuery.data ?? [];

  return (
    <>
      <PageHeader title="Notificaciones" subtitle="Alertas regulatorias y operativas" />
      {notificationsQuery.isLoading && <CircularProgress size={28} />}
      {notificationsQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar las notificaciones.</Alert>}
      <Card>
        <List disablePadding>
          {notifications.map((item, index) => (
            <ListItem key={item.id} divider={index < notifications.length - 1} alignItems="flex-start">
              <ListItemAvatar>
                <Avatar sx={{ bgcolor: COLOR_BY_TYPE[item.type] ?? COLOR_BY_TYPE.INFO }}>
                  {ICON_BY_TYPE[item.type] ?? ICON_BY_TYPE.INFO}
                </Avatar>
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
