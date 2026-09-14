import AddIcon from '@mui/icons-material/Add';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import { Alert, Button, Card, CardContent, CircularProgress, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { clearPendingTrips, getPendingTrips } from '../../../shared/storage/tripOfflineStore';

export function TripsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const tripsQuery = useQuery({ queryKey: ['trips'], queryFn: domainService.trips });
  const [pendingTrips, setPendingTrips] = useState(() => getPendingTrips());
  const [syncError, setSyncError] = useState('');
  const [syncing, setSyncing] = useState(false);

  const trips = tripsQuery.data ?? [];
  const visibleTrips = [...trips, ...pendingTrips.filter((pending) => !trips.some((trip) => trip.id === pending.id))];

  async function synchronize() {
    if (!pendingTrips.length) return;
    setSyncError('');
    setSyncing(true);
    try {
      await domainService.sync(
        pendingTrips.map((trip) => ({
          id: trip.id,
          vesselId: trip.vesselId,
          zone: trip.zone,
          date: trip.date,
          status: 'CLOSED',
          nullDeclaration: trip.nullDeclaration,
          captures: trip.captures.map((capture) => ({
            speciesId: capture.speciesId,
            weightKg: capture.weightKg,
            averageSizeCm: capture.averageSizeCm,
          })),
        })),
      );
      clearPendingTrips();
      setPendingTrips([]);
      await queryClient.invalidateQueries({ queryKey: ['summary'] });
      await tripsQuery.refetch();
    } catch (error) {
      // Si el servidor rechaza la faena (veda, talla, permiso) se conserva en el dispositivo
      // para que el usuario pueda corregirla en lugar de perder el registro.
      setSyncError(toAppError(error).message);
    } finally {
      setSyncing(false);
    }
  }

  return (
    <>
      <PageHeader title="Bitácora de faenas" subtitle="Registro de faenas y capturas con soporte offline" actionLabel="Nueva faena" actionIcon={<AddIcon />} onAction={() => navigate('/app/trips/new')} />
      {(!navigator.onLine || pendingTrips.length > 0) && (
        <Alert
          severity="warning"
          icon={<CloudOffIcon />}
          action={pendingTrips.length > 0 ? <Button size="small" onClick={() => void synchronize()} disabled={!navigator.onLine || syncing}>{syncing ? 'Sincronizando...' : 'Sincronizar'}</Button> : undefined}
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
    </>
  );
}
