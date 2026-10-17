import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import UndoIcon from '@mui/icons-material/Undo';
import WrongLocationIcon from '@mui/icons-material/WrongLocation';
import {
  Alert, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, Grid, List, ListItemButton, ListItemText, MenuItem, Stack, Switch, Tab, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Tabs, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { GeoMap } from '../../../shared/components/GeoMap';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type RestrictedZone } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useInteractiveMap } from '../../../shared/hooks/useInteractiveMap';
import { parseVertices, ZONE_TYPE_LABEL } from '../../../shared/geo/geo';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';

const ZONE_MANAGERS = ['ADMIN', 'INSPECTOR'];
const time = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

const EMPTY_FORM = { name: '', zoneType: 'CLOSURE_ZONE' as RestrictedZone['zoneType'], vertices: '', resolutionNumber: '', description: '' };

export function ZonesPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const canManage = !!user && ZONE_MANAGERS.includes(user.role);
  const authority = isAuthority(user?.role);
  const interactiveMap = useInteractiveMap();

  const zonesQuery = useQuery({ queryKey: ['zones'], queryFn: domainService.zones });
  const eventsQuery = useQuery({ queryKey: ['zone-events'], queryFn: domainService.zoneEvents, enabled: authority });
  const zones = zonesQuery.data ?? [];
  const events = eventsQuery.data ?? [];

  const [tab, setTab] = useState<'zones' | 'events'>('zones');
  const [selectedId, setSelectedId] = useState<string | undefined>();
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);

  const vertices = useMemo(() => parseVertices(form.vertices), [form.vertices]);
  const verticesInvalid = form.vertices.trim() !== '' && (vertices === null || vertices.length < 3);
  const preview: RestrictedZone | undefined =
    vertices && vertices.length >= 3
      ? { id: 'preview', name: form.name || 'Zona nueva', zoneType: form.zoneType, polygon: vertices, resolutionNumber: form.resolutionNumber, active: true }
      : undefined;

  const createMutation = useMutation({
    mutationFn: () =>
      domainService.createZone({
        name: form.name.trim(),
        zoneType: form.zoneType,
        polygon: vertices ?? [],
        resolutionNumber: form.resolutionNumber.trim(),
        description: form.description || undefined,
      }),
    onSuccess: async (zone) => {
      setFormOpen(false);
      setForm(EMPTY_FORM);
      setSelectedId(zone.id);
      await queryClient.invalidateQueries({ queryKey: ['zones'] });
    },
  });
  const toggleMutation = useMutation({
    mutationFn: (zone: RestrictedZone) => domainService.updateZone(zone.id, { active: !zone.active }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['zones'] }),
  });
  const reviewMutation = useMutation({
    mutationFn: (id: string) => domainService.reviewZoneEvent(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['zone-events'] }),
  });
  const createError = createMutation.error ? toAppError(createMutation.error) : null;
  const canSubmit = !!form.name.trim() && !!form.resolutionNumber.trim() && !!vertices && vertices.length >= 3;
  const openEvents = events.filter((event) => event.status === 'OPEN').length;

  return (
    <>
      <PageHeader
        title="Zonas restringidas"
        subtitle="Áreas Marinas Protegidas y zonas en veda. Al entrar en una, el pescador recibe una alerta y la autoridad un evento de control."
        actionLabel={canManage ? 'Nueva zona' : undefined}
        actionIcon={canManage ? <AddIcon /> : undefined}
        onAction={canManage ? () => setFormOpen(true) : undefined}
      />

      {authority && (
        <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }} aria-label="Vista de zonas">
          <Tab value="zones" label="Mapa de zonas" />
          <Tab value="events" label={openEvents > 0 ? `Eventos de control (${openEvents})` : 'Eventos de control'} />
        </Tabs>
      )}

      {zonesQuery.isLoading && <CircularProgress size={28} />}
      {zonesQuery.isError && <Alert severity="error">No fue posible cargar las zonas restringidas.</Alert>}

      {tab === 'zones' && !zonesQuery.isLoading && !zonesQuery.isError && (
        zones.length === 0 ? (
          <EmptyState
            icon={<WrongLocationIcon sx={{ fontSize: 48, color: 'secondary.light' }} />}
            title="Sin zonas restringidas"
            description="La autoridad todavía no ha delimitado Áreas Marinas Protegidas ni zonas en veda."
            actionLabel={canManage ? 'Delimitar la primera zona' : undefined}
            onAction={canManage ? () => setFormOpen(true) : undefined}
          />
        ) : (
          <Grid container spacing={2.5}>
            <Grid size={{ xs: 12, md: 8 }}>
              <Card>
                <CardContent>
                  <GeoMap title="Zonas restringidas" zones={zones} highlightZoneId={selectedId} height={420} />
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <List disablePadding aria-label="Listado de zonas">
                    {zones.map((zone) => (
                      <ListItemButton
                        key={zone.id}
                        selected={zone.id === selectedId}
                        onClick={() => setSelectedId(zone.id === selectedId ? undefined : zone.id)}
                        sx={{ borderRadius: 3, mb: 0.75, alignItems: 'flex-start', '&.Mui-selected .MuiTypography-root': { color: '#fff' } }}
                      >
                        <ListItemText
                          primary={zone.name}
                          secondary={`${ZONE_TYPE_LABEL[zone.zoneType]} · ${zone.resolutionNumber}${zone.active ? '' : ' · inactiva'}`}
                          slotProps={{ primary: { fontWeight: 700 } }}
                        />
                      </ListItemButton>
                    ))}
                  </List>
                  {canManage && selectedId && (() => {
                    const zone = zones.find((item) => item.id === selectedId);
                    return zone ? (
                      <Stack spacing={1} sx={{ mt: 1.5, pt: 1.5, borderTop: '1px solid', borderColor: 'divider' }}>
                        {zone.description && <Typography variant="body2" color="text.secondary">{zone.description}</Typography>}
                        <FormControlLabel
                          control={<Switch checked={zone.active} onChange={() => toggleMutation.mutate(zone)} disabled={toggleMutation.isPending} />}
                          label={zone.active ? 'Zona activa' : 'Zona inactiva'}
                        />
                      </Stack>
                    ) : null;
                  })()}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        )
      )}

      {tab === 'events' && authority && (
        <Card>
          <CardContent>
            {eventsQuery.isLoading && <CircularProgress size={28} />}
            {!eventsQuery.isLoading && events.length === 0 && (
              <Typography color="text.secondary">No se han registrado ingresos a zonas restringidas.</Typography>
            )}
            {events.length > 0 && (
              <TableContainer>
                <Table aria-label="Eventos de control por ingreso a zonas restringidas">
                  <TableHead>
                    <TableRow>
                      <TableCell>Fecha y hora</TableCell>
                      <TableCell>Zona</TableCell>
                      <TableCell>Pescador</TableCell>
                      <TableCell>Embarcación</TableCell>
                      <TableCell>Posición</TableCell>
                      <TableCell>Estado</TableCell>
                      {canManage && <TableCell align="right">Acción</TableCell>}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {events.map((event) => (
                      <TableRow key={event.id}>
                        <TableCell>{time.format(new Date(event.occurredAt))}</TableCell>
                        <TableCell>
                          <Typography fontWeight={700}>{event.zone}</Typography>
                          <Typography variant="caption" color="text.secondary">Faena {event.tripId}</Typography>
                        </TableCell>
                        <TableCell>{event.ownerName}</TableCell>
                        <TableCell>{event.vessel}</TableCell>
                        <TableCell>{event.latitude.toFixed(4)}, {event.longitude.toFixed(4)}</TableCell>
                        <TableCell>
                          {event.status === 'OPEN' ? <Chip size="small" color="error" label="Pendiente" /> : <StatusChip status="APPROVED" />}
                        </TableCell>
                        {canManage && (
                          <TableCell align="right">
                            {event.status === 'OPEN' && (
                              <Button size="small" startIcon={<CheckIcon />} onClick={() => reviewMutation.mutate(event.id)} disabled={reviewMutation.isPending}>
                                Marcar revisado
                              </Button>
                            )}
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </CardContent>
        </Card>
      )}

      <Dialog open={formOpen} onClose={() => setFormOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>Nueva zona restringida</DialogTitle>
        <DialogContent>
          <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
            <Grid size={{ xs: 12, md: 5 }}>
              <Stack spacing={2}>
                {createError && <Alert severity="error">{createError.message}</Alert>}
                <TextField label="Nombre" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                <TextField select label="Tipo" value={form.zoneType} onChange={(e) => setForm({ ...form, zoneType: e.target.value as RestrictedZone['zoneType'] })}>
                  {(Object.keys(ZONE_TYPE_LABEL) as RestrictedZone['zoneType'][]).map((type) => (
                    <MenuItem key={type} value={type}>{ZONE_TYPE_LABEL[type]}</MenuItem>
                  ))}
                </TextField>
                <TextField label="Referencia legal (resolución)" value={form.resolutionNumber} onChange={(e) => setForm({ ...form, resolutionNumber: e.target.value })} />
                <TextField
                  label="Vértices (latitud, longitud)"
                  multiline
                  minRows={5}
                  value={form.vertices}
                  onChange={(e) => setForm({ ...form, vertices: e.target.value })}
                  error={verticesInvalid}
                  helperText={verticesInvalid ? 'Escribe al menos 3 vértices válidos, uno por línea: 6.05, -77.62' : 'Uno por línea, en orden, sin repetir el primero.'}
                  slotProps={{ htmlInput: { style: { fontFamily: 'ui-monospace, Consolas, monospace' } } }}
                />
                <TextField label="Descripción" multiline minRows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </Stack>
            </Grid>
            <Grid size={{ xs: 12, md: 7 }}>
              <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
                <Typography variant="subtitle2">Vista previa</Typography>
                {interactiveMap && form.vertices.trim() !== '' && (
                  <Button size="small" startIcon={<UndoIcon />} onClick={() => setForm({ ...form, vertices: form.vertices.trim().split('\n').slice(0, -1).join('\n') })}>
                    Quitar último vértice
                  </Button>
                )}
              </Stack>
              {interactiveMap && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                  Haz clic en el mapa para añadir los vértices en orden; también puedes escribirlos a mano.
                </Typography>
              )}
              <GeoMap
                title="Vista previa de la zona"
                zones={preview ? [...zones, preview] : zones}
                // Mientras no hay polígono (menos de 3 vértices), los puntos marcados se ven como trazo.
                track={!preview && vertices ? vertices.map(([latitude, longitude], index) => ({ id: `v${index}`, latitude, longitude })) : []}
                highlightZoneId="preview"
                height={360}
                onPick={(lat, lon) => setForm((current) => ({
                  ...current,
                  vertices: `${current.vertices.trim()}${current.vertices.trim() ? '\n' : ''}${lat}, ${lon}`,
                }))}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={() => createMutation.mutate()} disabled={!canSubmit || createMutation.isPending}>
            {createMutation.isPending ? 'Guardando...' : 'Registrar zona'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
