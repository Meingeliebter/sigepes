import AddIcon from '@mui/icons-material/Add';
import DonutLargeIcon from '@mui/icons-material/DonutLarge';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import {
  Alert, Box, Button, Card, CardContent, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, IconButton, LinearProgress, MenuItem, Stack, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, TextField, Tooltip, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type Quota } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';

const QUOTA_MANAGERS = ['ADMIN', 'ANALYST'];

const kg = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });

const EMPTY_FORM = {
  speciesId: '',
  zone: '',
  from: '',
  to: '',
  totalKg: '',
  vesselId: '',
  resolutionNumber: '',
  notes: '',
};

/** AC-07.4: verde hasta el 80 %, ámbar hasta el 95 %, rojo a partir de ahí. */
function consumptionColor(percent: number): 'success' | 'warning' | 'error' {
  if (percent >= 95) return 'error';
  if (percent >= 80) return 'warning';
  return 'success';
}

function ConsumptionMeter({ quota }: { quota: Quota }) {
  const percent = Math.min(100, quota.consumedPercent);
  return (
    <Box sx={{ minWidth: 180 }}>
      <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
        <Typography variant="body2" fontWeight={650}>{kg.format(quota.consumedPercent)} %</Typography>
        <Typography variant="caption" color="text.secondary">
          {kg.format(quota.consumedKg)} / {kg.format(quota.totalKg)} kg
        </Typography>
      </Stack>
      <LinearProgress
        variant="determinate"
        value={percent}
        color={consumptionColor(quota.consumedPercent)}
        aria-label={`Consumo de la cuota: ${kg.format(quota.consumedPercent)} por ciento`}
        sx={{ height: 8 }}
      />
    </Box>
  );
}

export function QuotasPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const canManage = !!user && QUOTA_MANAGERS.includes(user.role);

  const quotasQuery = useQuery({ queryKey: ['quotas'], queryFn: domainService.quotas });
  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species, enabled: canManage });
  const vesselsQuery = useQuery({ queryKey: ['vessels'], queryFn: domainService.vessels, enabled: canManage });
  const quotas = quotasQuery.data ?? [];

  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);

  const createMutation = useMutation({
    mutationFn: () =>
      domainService.createQuota({
        speciesId: form.speciesId,
        zone: form.zone.trim(),
        from: form.from,
        to: form.to,
        totalKg: Number(form.totalKg),
        vesselId: form.vesselId || undefined,
        resolutionNumber: form.resolutionNumber || undefined,
        notes: form.notes || undefined,
      }),
    onSuccess: async () => {
      setFormOpen(false);
      setForm(EMPTY_FORM);
      await queryClient.invalidateQueries({ queryKey: ['quotas'] });
    },
  });

  const statusMutation = useMutation({
    mutationFn: (quota: Quota) =>
      domainService.updateQuota(quota.id, { status: quota.status === 'ACTIVE' ? 'CLOSED' : 'ACTIVE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['quotas'] }),
  });

  const createError = createMutation.error ? toAppError(createMutation.error) : null;
  const statusError = statusMutation.error ? toAppError(statusMutation.error) : null;
  const canSubmit =
    !!form.speciesId && !!form.zone.trim() && !!form.from && !!form.to && Number(form.totalKg) > 0 && form.to >= form.from;

  const active = quotas.filter((quota) => quota.status === 'ACTIVE');
  const nearLimit = active.filter((quota) => quota.consumedPercent >= 80).length;

  return (
    <>
      <PageHeader
        title="Cuotas de pesca"
        subtitle="Saldo disponible por especie, zona y periodo. Cada desembarque se descuenta al cerrar la faena."
        actionLabel={canManage ? 'Nueva cuota' : undefined}
        actionIcon={canManage ? <AddIcon /> : undefined}
        onAction={canManage ? () => setFormOpen(true) : undefined}
      />

      {nearLimit > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {nearLimit === 1
            ? '1 cuota activa supera el 80 % de consumo.'
            : `${nearLimit} cuotas activas superan el 80 % de consumo.`}
        </Alert>
      )}
      {statusError && <Alert severity="error" sx={{ mb: 2 }}>{statusError.message}</Alert>}
      {quotasQuery.isLoading && <CircularProgress size={28} />}
      {quotasQuery.isError && <Alert severity="error">No fue posible cargar las cuotas.</Alert>}

      {!quotasQuery.isLoading && !quotasQuery.isError && quotas.length === 0 && (
        <EmptyState
          icon={<DonutLargeIcon sx={{ fontSize: 48, color: 'secondary.light' }} />}
          title="Sin cuotas asignadas"
          description="La autoridad todavía no ha fijado cuotas de captura. Sin cuota, las especies solo se rigen por talla mínima y vedas."
          actionLabel={canManage ? 'Asignar la primera cuota' : undefined}
          onAction={canManage ? () => setFormOpen(true) : undefined}
        />
      )}

      {quotas.length > 0 && (
        <Card>
          <CardContent>
            <TableContainer>
              <Table aria-label="Cuotas de pesca">
                <TableHead>
                  <TableRow>
                    <TableCell>Especie</TableCell>
                    <TableCell>Zona</TableCell>
                    <TableCell>Periodo</TableCell>
                    <TableCell>Asignación</TableCell>
                    <TableCell>Consumo</TableCell>
                    <TableCell align="right">Saldo</TableCell>
                    <TableCell>Estado</TableCell>
                    {canManage && <TableCell align="right">Acción</TableCell>}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {quotas.map((quota) => (
                    <TableRow key={quota.id}>
                      <TableCell>
                        <Typography fontWeight={700}>{quota.species}</Typography>
                        {quota.resolutionNumber && (
                          <Typography variant="caption" color="text.secondary">{quota.resolutionNumber}</Typography>
                        )}
                      </TableCell>
                      <TableCell>{quota.zone === 'TODAS' ? 'Todas las zonas' : quota.zone}</TableCell>
                      <TableCell sx={{ whiteSpace: 'nowrap' }}>{quota.from} → {quota.to}</TableCell>
                      <TableCell>{quota.vessel ?? 'General'}</TableCell>
                      <TableCell><ConsumptionMeter quota={quota} /></TableCell>
                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                        <Typography fontWeight={800}>{kg.format(quota.remainingKg)} kg</Typography>
                      </TableCell>
                      <TableCell><StatusChip status={quota.status === 'ACTIVE' ? 'ACTIVE' : 'CLOSED'} /></TableCell>
                      {canManage && (
                        <TableCell align="right">
                          <Tooltip title={quota.status === 'ACTIVE' ? 'Cerrar cuota' : 'Reabrir cuota'}>
                            <IconButton
                              size="small"
                              onClick={() => statusMutation.mutate(quota)}
                              disabled={statusMutation.isPending}
                              aria-label={quota.status === 'ACTIVE' ? `Cerrar la cuota de ${quota.species}` : `Reabrir la cuota de ${quota.species}`}
                            >
                              {quota.status === 'ACTIVE' ? <LockIcon fontSize="small" /> : <LockOpenIcon fontSize="small" />}
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      )}

      <Dialog open={formOpen} onClose={() => setFormOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Nueva cuota</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {createError && <Alert severity="error">{createError.message}</Alert>}
            <TextField select label="Especie" value={form.speciesId} onChange={(e) => setForm({ ...form, speciesId: e.target.value })}>
              {(speciesQuery.data ?? []).map((sp) => (
                <MenuItem key={sp.id} value={sp.id}>{sp.commonName}</MenuItem>
              ))}
            </TextField>
            <TextField
              label="Zona"
              value={form.zone}
              onChange={(e) => setForm({ ...form, zone: e.target.value })}
              helperText="Escribe TODAS para que aplique en cualquier zona."
            />
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField label="Desde" type="date" value={form.from} onChange={(e) => setForm({ ...form, from: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Hasta"
                  type="date"
                  value={form.to}
                  onChange={(e) => setForm({ ...form, to: e.target.value })}
                  error={!!form.from && !!form.to && form.to < form.from}
                  helperText={!!form.from && !!form.to && form.to < form.from ? 'Debe ser posterior al inicio' : ' '}
                  slotProps={{ inputLabel: { shrink: true } }}
                />
              </Grid>
            </Grid>
            <TextField label="Cuota total (kg)" type="number" value={form.totalKg} onChange={(e) => setForm({ ...form, totalKg: e.target.value })} />
            <TextField
              select
              label="Asignar a una embarcación (opcional)"
              value={form.vesselId}
              onChange={(e) => setForm({ ...form, vesselId: e.target.value })}
            >
              <MenuItem value="">Cuota general</MenuItem>
              {(vesselsQuery.data ?? []).map((vessel) => (
                <MenuItem key={vessel.id} value={vessel.id}>{vessel.name} · {vessel.registration}</MenuItem>
              ))}
            </TextField>
            <TextField label="Resolución de respaldo" value={form.resolutionNumber} onChange={(e) => setForm({ ...form, resolutionNumber: e.target.value })} />
            <TextField label="Observaciones" multiline minRows={2} value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={() => createMutation.mutate()} disabled={!canSubmit || createMutation.isPending}>
            {createMutation.isPending ? 'Guardando...' : 'Asignar cuota'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
