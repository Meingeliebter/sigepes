import AddIcon from '@mui/icons-material/Add';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import EditIcon from '@mui/icons-material/Edit';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import {
  Alert, Button, Card, CardContent, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  IconButton, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type Trip } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useOnlineStatus } from '../../../shared/hooks/useOnlineStatus';
import { getPendingTrips } from '../../../shared/storage/tripOfflineStore';
import { syncPendingTrips } from '../../../shared/sync/tripSync';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';

const EMPTY_CORRECTION = { zone: '', arrivalTime: '', notes: '', reason: '' };

export function TripsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const online = useOnlineStatus();
  const user = useAuthStore((s) => s.user);
  const canCorrect = isAuthority(user?.role);
  const tripsQuery = useQuery({ queryKey: ['trips'], queryFn: domainService.trips });
  const [pendingTrips, setPendingTrips] = useState(() => getPendingTrips());
  const [syncError, setSyncError] = useState('');
  const [syncing, setSyncing] = useState(false);
  const [correctingTrip, setCorrectingTrip] = useState<Trip | null>(null);
  const [correction, setCorrection] = useState(EMPTY_CORRECTION);

  const trips = tripsQuery.data ?? [];
  const visibleTrips = [...trips, ...pendingTrips.filter((pending) => !trips.some((trip) => trip.id === pending.id))];

  const correctMutation = useMutation({
    mutationFn: () =>
      domainService.correctTrip(correctingTrip!.id, {
        zone: correction.zone || undefined,
        arrivalTime: correction.arrivalTime || undefined,
        notes: correction.notes || undefined,
        reason: correction.reason || undefined,
      }),
    onSuccess: async () => {
      setCorrectingTrip(null);
      setCorrection(EMPTY_CORRECTION);
      await queryClient.invalidateQueries({ queryKey: ['trips'] });
    },
  });
  const correctError = correctMutation.error ? toAppError(correctMutation.error) : null;

  function openCorrection(trip: Trip) {
    setCorrectingTrip(trip);
    setCorrection({ zone: trip.zone, arrivalTime: trip.arrivalTime ?? '', notes: trip.notes ?? '', reason: '' });
  }

  const reasonMissing = correctingTrip?.status === 'CLOSED' && !correction.reason.trim();

  async function synchronize() {
    setSyncError('');
    setSyncing(true);
    const result = await syncPendingTrips(queryClient);
    if (!result.ok) setSyncError(result.error ?? 'No fue posible sincronizar.');
    setPendingTrips(getPendingTrips());
    await tripsQuery.refetch();
    setSyncing(false);
  }

  return (
    <>
      <PageHeader title="Bitácora de faenas" subtitle="Registro de faenas y capturas con soporte offline" actionLabel="Nueva faena" actionIcon={<AddIcon />} onAction={() => navigate('/app/trips/new')} />
      {(!online || pendingTrips.length > 0) && (
        <Alert
          severity="warning"
          icon={<CloudOffIcon />}
          action={pendingTrips.length > 0 ? <Button size="small" onClick={() => void synchronize()} disabled={!online || syncing}>{syncing ? 'Sincronizando...' : 'Sincronizar'}</Button> : undefined}
          sx={{ mb: 2 }}
        >
          {pendingTrips.length > 0 ? `${pendingTrips.length} faena(s) pendiente(s) de sincronizar.` : 'Sin conexión. Las faenas nuevas se guardarán localmente.'}
        </Alert>
      )}
      {syncError && <Alert severity="error" sx={{ mb: 2 }}>{syncError}</Alert>}
      <Card>
        <CardContent>
          {tripsQuery.isLoading && <CircularProgress size={28} />}
          {tripsQuery.isError && <Alert severity="error">No fue posible cargar las faenas.</Alert>}
          {!tripsQuery.isLoading && !tripsQuery.isError && visibleTrips.length === 0 ? (
            <EmptyState
              icon={<MenuBookIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
              title="Aún no hay faenas registradas"
              description="Registra tu primera faena para comenzar a llevar la bitácora."
              actionLabel="Nueva faena"
              onAction={() => navigate('/app/trips/new')}
            />
          ) : (
            <>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Folio</TableCell>
                      <TableCell>Embarcación</TableCell>
                      <TableCell>Zona</TableCell>
                      <TableCell>Fecha</TableCell>
                      <TableCell align="right">Captura (kg)</TableCell>
                      <TableCell>Estado</TableCell>
                      {canCorrect && <TableCell align="right">Acciones</TableCell>}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {visibleTrips.map((trip) => (
                      <TableRow key={trip.id} hover>
                        <TableCell><Typography fontWeight={700}>{trip.id}</Typography></TableCell>
                        <TableCell>{trip.vessel}</TableCell>
                        <TableCell>{trip.zone}</TableCell>
                        <TableCell>{trip.date}</TableCell>
                        <TableCell align="right">{trip.captures.reduce((total, capture) => total + capture.weightKg, 0)}</TableCell>
                        <TableCell><StatusChip status={trip.status} /></TableCell>
                        {canCorrect && (
                          <TableCell align="right">
                            {!trip.id.startsWith('LOCAL-') && (
                              <Tooltip title="Corregir (queda auditado)">
                                <IconButton size="small" onClick={() => openCorrection(trip)} aria-label="Corregir faena">
                                  <EditIcon fontSize="small" />
                                </IconButton>
                              </Tooltip>
                            )}
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <Stack sx={{ mt: 1 }}>
                <Typography variant="caption" color="text.secondary">Mostrando {visibleTrips.length} faenas recientes</Typography>
              </Stack>
            </>
          )}
        </CardContent>
      </Card>

      <Dialog open={!!correctingTrip} onClose={() => setCorrectingTrip(null)} fullWidth maxWidth="sm">
        <DialogTitle>Corregir faena {correctingTrip?.id}</DialogTitle>
        <DialogContent>
          {correctError && <Alert severity="error" sx={{ mb: 2 }}>{correctError.message}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            {correctingTrip?.status === 'CLOSED' && (
              <Alert severity="info">
                Esta faena ya está cerrada: la corrección quedará registrada en la auditoría con tu usuario, la
                fecha y el motivo que indiques.
              </Alert>
            )}
            <TextField label="Zona" value={correction.zone} onChange={(e) => setCorrection({ ...correction, zone: e.target.value })} />
            <TextField
              label="Hora de llegada"
              type="time"
              value={correction.arrivalTime}
              onChange={(e) => setCorrection({ ...correction, arrivalTime: e.target.value })}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField label="Notas" value={correction.notes} onChange={(e) => setCorrection({ ...correction, notes: e.target.value })} multiline minRows={2} />
            <TextField
              label="Motivo del cambio"
              value={correction.reason}
              onChange={(e) => setCorrection({ ...correction, reason: e.target.value })}
              required={correctingTrip?.status === 'CLOSED'}
              error={reasonMissing}
              helperText={reasonMissing ? 'Obligatorio para corregir una faena cerrada.' : ' '}
              multiline
              minRows={2}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCorrectingTrip(null)}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={correctMutation.isPending || reasonMissing}
            onClick={() => correctMutation.mutate()}
          >
            {correctMutation.isPending ? 'Guardando...' : 'Guardar corrección'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
