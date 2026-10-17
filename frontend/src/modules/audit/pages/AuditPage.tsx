import HistoryIcon from '@mui/icons-material/History';
import VisibilityIcon from '@mui/icons-material/Visibility';
import {
  Alert, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  IconButton, MenuItem, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { PageHeader } from '../../../shared/components/PageHeader';
import { EmptyState } from '../../../shared/components/EmptyState';
import { domainService, type AuditEvent } from '../../../shared/api/domain.service';
import { useAuthStore } from '../../auth/store/authStore';

const ACTION_TYPE_COLOR: Record<string, 'success' | 'info' | 'error' | 'warning'> = {
  CREATE: 'success',
  UPDATE: 'info',
  DELETE: 'error',
  SECURITY: 'warning',
};

const EMPTY_FILTERS = { actionType: '', actorId: '', from: '', to: '' };

export function AuditPage() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN';

  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const auditQuery = useQuery({
    queryKey: ['audit', filters],
    queryFn: () =>
      domainService.audit({
        actionType: filters.actionType || undefined,
        actorId: filters.actorId || undefined,
        from: filters.from || undefined,
        to: filters.to || undefined,
      }),
  });
  // Solo el ADMIN puede listar usuarios (GET /usuarios es admin-only); para INSPECTOR el filtro
  // de usuario sigue funcionando como campo de texto libre por ID.
  const usersQuery = useQuery({ queryKey: ['users'], queryFn: domainService.users, enabled: isAdmin });

  const [detail, setDetail] = useState<AuditEvent | null>(null);

  const events = auditQuery.data ?? [];

  return (
    <>
      <PageHeader title="Auditoría" subtitle="Registro inmutable de eventos sensibles" />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
        <TextField
          select
          label="Tipo de acción"
          size="small"
          value={filters.actionType}
          onChange={(e) => setFilters({ ...filters, actionType: e.target.value })}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="">Todos</MenuItem>
          <MenuItem value="CREATE">Creación</MenuItem>
          <MenuItem value="UPDATE">Actualización</MenuItem>
          <MenuItem value="DELETE">Eliminación</MenuItem>
          <MenuItem value="SECURITY">Seguridad</MenuItem>
        </TextField>
        {isAdmin && usersQuery.data ? (
          <TextField
            select
            label="Usuario"
            size="small"
            value={filters.actorId}
            onChange={(e) => setFilters({ ...filters, actorId: e.target.value })}
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="">Todos</MenuItem>
            {usersQuery.data.map((u) => (
              <MenuItem key={u.id} value={u.id}>{u.fullName}</MenuItem>
            ))}
          </TextField>
        ) : (
          <TextField
            label="ID de usuario"
            size="small"
            value={filters.actorId}
            onChange={(e) => setFilters({ ...filters, actorId: e.target.value })}
            sx={{ minWidth: 200 }}
          />
        )}
        <TextField
          label="Desde"
          type="date"
          size="small"
          value={filters.from}
          onChange={(e) => setFilters({ ...filters, from: e.target.value })}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <TextField
          label="Hasta"
          type="date"
          size="small"
          value={filters.to}
          onChange={(e) => setFilters({ ...filters, to: e.target.value })}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <Button onClick={() => setFilters(EMPTY_FILTERS)} disabled={JSON.stringify(filters) === JSON.stringify(EMPTY_FILTERS)}>
          Limpiar filtros
        </Button>
      </Stack>

      <Card>
        <CardContent>
          {auditQuery.isLoading && <CircularProgress size={28} />}
          {auditQuery.isError && <Alert severity="error">No fue posible cargar la bitácora de auditoría.</Alert>}
          {!auditQuery.isLoading && !auditQuery.isError && events.length === 0 ? (
            <EmptyState
              icon={<HistoryIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
              title="Sin eventos registrados"
              description="Ningún evento coincide con los filtros actuales."
            />
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Fecha y hora</TableCell>
                    <TableCell>Responsable</TableCell>
                    <TableCell>Tipo</TableCell>
                    <TableCell>Acción</TableCell>
                    <TableCell>Entidad</TableCell>
                    <TableCell>IP</TableCell>
                    <TableCell align="right">Detalle</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {events.map((event) => (
                    <TableRow key={event.id} hover>
                      <TableCell><Typography variant="body2" color="text.secondary">{event.timestamp}</Typography></TableCell>
                      <TableCell>{event.actor}</TableCell>
                      <TableCell>
                        <Chip label={event.actionType} size="small" color={ACTION_TYPE_COLOR[event.actionType]} variant="outlined" />
                      </TableCell>
                      <TableCell><Chip label={event.action} size="small" variant="outlined" /></TableCell>
                      <TableCell><Typography fontWeight={700}>{event.entity}</Typography></TableCell>
                      <TableCell><Typography variant="body2" color="text.secondary">{event.ip ?? '—'}</Typography></TableCell>
                      <TableCell align="right">
                        {(event.beforeState || event.afterState) && (
                          <Tooltip title="Ver estado antes/después">
                            <IconButton size="small" onClick={() => setDetail(event)} aria-label="Ver detalle">
                              <VisibilityIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={!!detail} onClose={() => setDetail(null)} fullWidth maxWidth="sm">
        <DialogTitle>Detalle del evento — {detail?.action}</DialogTitle>
        <DialogContent>
          {detail?.beforeState && (
            <>
              <Typography variant="subtitle2" sx={{ mt: 1 }}>Estado anterior</Typography>
              <Typography component="pre" variant="body2" sx={{ whiteSpace: 'pre-wrap', bgcolor: 'grey.100', p: 1.5, borderRadius: 1 }}>
                {formatJson(detail.beforeState)}
              </Typography>
            </>
          )}
          {detail?.afterState && (
            <>
              <Typography variant="subtitle2" sx={{ mt: 2 }}>Estado posterior</Typography>
              <Typography component="pre" variant="body2" sx={{ whiteSpace: 'pre-wrap', bgcolor: 'grey.100', p: 1.5, borderRadius: 1 }}>
                {formatJson(detail.afterState)}
              </Typography>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetail(null)}>Cerrar</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

function formatJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}
