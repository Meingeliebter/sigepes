import ArchiveOutlinedIcon from '@mui/icons-material/ArchiveOutlined';
import BlockIcon from '@mui/icons-material/Block';
import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DonutLargeIcon from '@mui/icons-material/DonutLarge';
import GavelIcon from '@mui/icons-material/Gavel';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import WrongLocationIcon from '@mui/icons-material/WrongLocation';
import {
  Alert, Avatar, Box, Button, Card, Chip, CircularProgress, IconButton, List, ListItem, ListItemAvatar,
  ListItemButton, ListItemText, Stack, Tab, Tabs, Tooltip, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { ALERT_SEVERITY, ALERT_TYPE_LABEL } from '../../../shared/alerts/alertLabels';
import { domainService, type AlertType, type AppAlert } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';

const ICON_BY_TYPE: Record<AlertType, ReactNode> = {
  CLOSURE: <BlockIcon />,
  QUOTA: <DonutLargeIcon />,
  PERMIT: <GavelIcon />,
  ZONE: <WrongLocationIcon />,
  REPORT: <CampaignOutlinedIcon />,
  SECURITY: <ShieldOutlinedIcon />,
  DOCUMENT: <DescriptionOutlinedIcon />,
};

const AVATAR_COLOR = { INFO: 'info.main', WARNING: 'warning.main', CRITICAL: 'error.main' } as const;

const time = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

type Filter = 'all' | 'unread' | 'critical';

/** RF-11 · Buzón de alertas de cumplimiento (AC-11.2, AC-11.3). */
export function NotificationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const inboxQuery = useQuery({ queryKey: ['alerts'], queryFn: domainService.alerts });
  const [filter, setFilter] = useState<Filter>('all');
  const alerts = inboxQuery.data?.alerts ?? [];
  const unread = inboxQuery.data?.unread ?? 0;

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['alerts'] });
  const markRead = useMutation({ mutationFn: domainService.markAlertRead, onSuccess: refresh });
  const markAll = useMutation({ mutationFn: domainService.markAllAlertsRead, onSuccess: refresh });
  const archive = useMutation({ mutationFn: domainService.archiveAlert, onSuccess: refresh });
  const actionError = [markRead.error, markAll.error, archive.error].find(Boolean);

  const visible = alerts.filter((alert) =>
    filter === 'all' || (filter === 'unread' ? !alert.read : alert.severity === 'CRITICAL'));
  const criticalCount = alerts.filter((alert) => alert.severity === 'CRITICAL').length;

  const open = (alert: AppAlert) => {
    if (!alert.read) markRead.mutate(alert.id);
    if (alert.link) void navigate(alert.link);
  };

  return (
    <>
      <PageHeader
        title="Notificaciones"
        subtitle="Vedas, cuotas, permisos, zonas restringidas y denuncias que requieren tu atención."
        secondaryAction={
          unread > 0 ? (
            <Button variant="outlined" startIcon={<DoneAllIcon />} onClick={() => markAll.mutate()} disabled={markAll.isPending}>
              Marcar todas como leídas
            </Button>
          ) : undefined
        }
      />

      <Tabs value={filter} onChange={(_, value) => setFilter(value)} sx={{ mb: 2 }} variant="scrollable" allowScrollButtonsMobile aria-label="Filtrar alertas">
        <Tab value="all" label={`Todas (${alerts.length})`} />
        <Tab value="unread" label={`Sin leer (${unread})`} />
        <Tab value="critical" label={`Críticas (${criticalCount})`} />
      </Tabs>

      {inboxQuery.isLoading && <CircularProgress size={28} />}
      {inboxQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar las notificaciones.</Alert>}
      {actionError && <Alert severity="error" sx={{ mb: 2 }}>{toAppError(actionError).message}</Alert>}

      {!inboxQuery.isLoading && !inboxQuery.isError && visible.length === 0 && (
        <EmptyState
          icon={<NotificationsNoneIcon sx={{ fontSize: 48, color: 'secondary.light' }} />}
          title={filter === 'all' ? 'Sin notificaciones' : 'Nada por aquí'}
          description={filter === 'all'
            ? 'Cuando haya vedas, cuotas o permisos que te afecten, te avisaremos aquí.'
            : 'No hay alertas con este filtro.'}
        />
      )}

      {visible.length > 0 && (
        <Card>
          <List disablePadding aria-label="Alertas">
            {visible.map((alert, index) => {
              const severity = ALERT_SEVERITY[alert.severity];
              return (
                <ListItem
                  key={alert.id}
                  divider={index < visible.length - 1}
                  disablePadding
                  secondaryAction={
                    alert.archivable ? (
                      <Tooltip title="Archivar">
                        <IconButton edge="end" aria-label={`Archivar «${alert.title}»`} onClick={() => archive.mutate(alert.id)} disabled={archive.isPending}>
                          <ArchiveOutlinedIcon />
                        </IconButton>
                      </Tooltip>
                    ) : (
                      <Tooltip title="Las alertas críticas de cumplimiento no se pueden archivar">
                        <LockOutlinedIcon fontSize="small" sx={{ color: 'text.disabled', mr: 1 }} aria-label="No archivable" />
                      </Tooltip>
                    )
                  }
                  sx={{
                    bgcolor: alert.severity === 'CRITICAL' ? 'rgba(179,38,30,.06)' : alert.read ? undefined : 'rgba(14,124,134,.05)',
                  }}
                >
                  <ListItemButton onClick={() => open(alert)} alignItems="flex-start" sx={{ py: 1.75, pr: 8 }}>
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: AVATAR_COLOR[alert.severity] }}>{ICON_BY_TYPE[alert.type]}</Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      disableTypography
                      primary={
                        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                          {!alert.read && (
                            <Box component="span" sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: 'secondary.main' }} aria-label="Sin leer" />
                          )}
                          <Typography component="span" fontWeight={alert.read ? 600 : 800}>{alert.title}</Typography>
                          <Chip size="small" label={severity.label} color={severity.color} />
                          <Chip size="small" variant="outlined" label={ALERT_TYPE_LABEL[alert.type]} />
                        </Stack>
                      }
                      secondary={
                        <>
                          <Typography variant="body2" color="text.primary" sx={{ mt: 0.5 }}>{alert.message}</Typography>
                          <Typography variant="caption" color="text.secondary">{time.format(new Date(alert.createdAt))}</Typography>
                        </>
                      }
                    />
                  </ListItemButton>
                </ListItem>
              );
            })}
          </List>
        </Card>
      )}
    </>
  );
}
