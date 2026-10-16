import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import {
  Alert, Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, List, ListItem,
  ListItemIcon, ListItemText, Stack, Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { GeoMap } from '../../../shared/components/GeoMap';
import { domainService, type Trip } from '../../../shared/api/domain.service';
import { ZONE_TYPE_LABEL } from '../../../shared/geo/geo';

const time = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

/** AC-04.4: recorrido de la faena sobre las zonas restringidas, con sus ingresos (RN-08). */
export function TripRouteDialog({ trip, onClose }: { trip: Trip | null; onClose: () => void }) {
  const tripId = trip?.id;
  const positionsQuery = useQuery({
    queryKey: ['trip-positions', tripId],
    queryFn: () => domainService.tripPositions(tripId!),
    enabled: !!tripId,
  });
  const eventsQuery = useQuery({
    queryKey: ['trip-zone-events', tripId],
    queryFn: () => domainService.tripZoneEvents(tripId!),
    enabled: !!tripId,
  });
  const zonesQuery = useQuery({ queryKey: ['zones'], queryFn: domainService.zones, enabled: !!tripId });

  const positions = positionsQuery.data ?? [];
  const events = eventsQuery.data ?? [];
  const loading = positionsQuery.isLoading || zonesQuery.isLoading;

  return (
    <Dialog open={!!trip} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Recorrido de la faena {trip?.id}</DialogTitle>
      <DialogContent>
        {loading && <CircularProgress size={28} />}
        {positionsQuery.isError && <Alert severity="error">No fue posible cargar el recorrido.</Alert>}
        {!loading && !positionsQuery.isError && (
          <Stack spacing={2.5}>
            {positions.length === 0 ? (
              <Alert severity="info">
                Esta faena no tiene posiciones GPS registradas (el dispositivo no compartió su ubicación).
              </Alert>
            ) : (
              <Typography color="text.secondary">
                {positions.length} posición(es) entre {time.format(new Date(positions[0].recordedAt))} y{' '}
                {time.format(new Date(positions[positions.length - 1].recordedAt))}.
              </Typography>
            )}
            <GeoMap
              title={`Recorrido de la faena ${trip?.id ?? ''}`}
              zones={zonesQuery.data ?? []}
              track={positions.map((position) => ({
                id: position.id,
                latitude: position.latitude,
                longitude: position.longitude,
                intrusion: !!position.insideZoneId,
              }))}
            />
            {events.length > 0 && (
              <>
                <Typography variant="subtitle1" fontWeight={750} color="error.main">
                  Ingresos a zonas restringidas
                </Typography>
                <List dense disablePadding>
                  {events.map((event) => (
                    <ListItem key={event.id} disableGutters>
                      <ListItemIcon sx={{ minWidth: 36 }}>
                        <ReportProblemIcon color="error" />
                      </ListItemIcon>
                      <ListItemText
                        primary={`${event.zone} · ${ZONE_TYPE_LABEL[event.zoneType as keyof typeof ZONE_TYPE_LABEL] ?? event.zoneType}`}
                        secondary={`${time.format(new Date(event.occurredAt))} · ${event.latitude.toFixed(4)}, ${event.longitude.toFixed(4)} · ${event.status === 'REVIEWED' ? 'Revisado por la autoridad' : 'Pendiente de revisión'}`}
                      />
                    </ListItem>
                  ))}
                </List>
              </>
            )}
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}
