import AddIcon from '@mui/icons-material/Add';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import EditIcon from '@mui/icons-material/Edit';
import HistoryIcon from '@mui/icons-material/History';
import PhishingIcon from '@mui/icons-material/Phishing';
import SearchIcon from '@mui/icons-material/Search';
import {
  Alert, Button, Card, CardContent, CardMedia, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, Grid, IconButton, InputAdornment, MenuItem, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, TextField, Tooltip, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { useOnlineStatus } from '../../../shared/hooks/useOnlineStatus';
import { domainService, type Species, type SpeciesVersion } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';

const SPECIES_MANAGER_ROLES = ['ADMIN', 'ANALYST'];

const REGION_LABEL: Record<string, string> = {
  PACIFICO: 'Pacífico',
  CARIBE: 'Caribe',
  AMBAS: 'Ambas costas',
};

const MEASUREMENT_LABEL: Record<string, string> = {
  TOTAL_LENGTH: 'Longitud total',
  FORK_LENGTH: 'Longitud a la horquilla',
};

const EMPTY_FORM = {
  commonName: '',
  scientificName: '',
  minSizeCm: '',
  measurementMethod: 'TOTAL_LENGTH',
  region: 'AMBAS',
  imageUrl: '',
};

export function SpeciesPage() {
  const queryClient = useQueryClient();
  const online = useOnlineStatus();
  const user = useAuthStore((s) => s.user);
  const canManage = !!user && SPECIES_MANAGER_ROLES.includes(user.role);
  const canSeeHistory = isAuthority(user?.role);

  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  const [search, setSearch] = useState('');
  const [regionFilter, setRegionFilter] = useState('ALL');

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const [historyId, setHistoryId] = useState<string | null>(null);
  const historyQuery = useQuery({
    queryKey: ['species', historyId, 'versions'],
    queryFn: () => domainService.speciesVersions(historyId!),
    enabled: !!historyId,
  });

  const species = speciesQuery.data ?? [];
  const filtered = useMemo(
    () =>
      species.filter((sp) => {
        const matchesSearch = sp.commonName.toLowerCase().includes(search.trim().toLowerCase());
        const matchesRegion = regionFilter === 'ALL' || sp.region === regionFilter;
        return matchesSearch && matchesRegion;
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- `speciesQuery.data` es la identidad estable; `species` solo agrega el fallback `[]`.
    [speciesQuery.data, search, regionFilter],
  );

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        commonName: form.commonName,
        scientificName: form.scientificName,
        minSizeCm: Number(form.minSizeCm),
        measurementMethod: form.measurementMethod,
        region: form.region,
        imageUrl: form.imageUrl,
      };
      return editingId ? domainService.updateSpecies(editingId, payload) : domainService.createSpecies(payload);
    },
    onSuccess: async () => {
      setFormOpen(false);
      setEditingId(null);
      setForm(EMPTY_FORM);
      await queryClient.invalidateQueries({ queryKey: ['species'] });
    },
  });
  const saveError = saveMutation.error ? toAppError(saveMutation.error) : null;

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
  }

  function openEdit(sp: Species) {
    setEditingId(sp.id);
    setForm({
      commonName: sp.commonName,
      scientificName: sp.scientificName,
      minSizeCm: String(sp.minSizeCm),
      measurementMethod: sp.measurementMethod,
      region: sp.region,
      imageUrl: sp.imageUrl,
    });
    setFormOpen(true);
  }

  const canSubmit = !!form.commonName && !!form.scientificName && !!form.minSizeCm && !!form.imageUrl;

  return (
    <>
      <PageHeader
        title="Especies"
        subtitle="Catálogo de especies con tallas mínimas y estado de veda"
        actionLabel={canManage ? 'Nueva especie' : undefined}
        actionIcon={canManage ? <AddIcon /> : undefined}
        onAction={canManage ? openCreate : undefined}
      />

      {!online && (
        <Alert severity="warning" icon={<CloudOffIcon />} sx={{ mb: 2 }}>
          Sin conexión — mostrando el último catálogo guardado en este dispositivo.
        </Alert>
      )}

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <TextField
          label="Buscar por nombre común"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          fullWidth
          slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> } }}
        />
        <TextField select label="Región" value={regionFilter} onChange={(e) => setRegionFilter(e.target.value)} sx={{ minWidth: 200 }}>
          <MenuItem value="ALL">Todas las regiones</MenuItem>
          <MenuItem value="PACIFICO">Pacífico</MenuItem>
          <MenuItem value="CARIBE">Caribe</MenuItem>
          <MenuItem value="AMBAS">Ambas costas</MenuItem>
        </TextField>
      </Stack>

      {speciesQuery.isLoading && <CircularProgress size={28} />}
      {speciesQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar el catálogo de especies.</Alert>}
      {!speciesQuery.isLoading && !speciesQuery.isError && filtered.length === 0 && (
        <EmptyState
          icon={<PhishingIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
          title={species.length === 0 ? 'El catálogo de especies está vacío' : 'Ningún resultado para el filtro actual'}
          description={
            species.length === 0
              ? 'La autoridad aún no ha registrado especies con su talla mínima legal.'
              : 'Ajusta la búsqueda o la región seleccionada.'
          }
        />
      )}
      <Grid container spacing={2}>
        {filtered.map((sp) => (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }} key={sp.id}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              {sp.imageUrl && <CardMedia component="img" height="140" image={sp.imageUrl} alt={sp.commonName} />}
              <CardContent sx={{ flex: 1 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Typography variant="h6">{sp.commonName}</Typography>
                  <StatusChip status={sp.status} />
                </Stack>
                <Typography variant="body2" color="text.secondary" fontStyle="italic" sx={{ mb: 1.5 }}>{sp.scientificName}</Typography>
                <Typography variant="body2">Talla mínima: {sp.minSizeCm} cm</Typography>
                <Typography variant="body2" color="text.secondary">{MEASUREMENT_LABEL[sp.measurementMethod] ?? sp.measurementMethod}</Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 1 }} alignItems="center" justifyContent="space-between">
                  <Chip size="small" label={REGION_LABEL[sp.region] ?? sp.region} variant="outlined" />
                  <Stack direction="row" spacing={0.5}>
                    {canSeeHistory && (
                      <Tooltip title="Ver historial de versiones">
                        <IconButton size="small" onClick={() => setHistoryId(sp.id)} aria-label="Ver historial">
                          <HistoryIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                    {canManage && (
                      <Tooltip title="Editar">
                        <IconButton size="small" onClick={() => openEdit(sp)} aria-label="Editar especie">
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </Stack>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={formOpen} onClose={() => setFormOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingId ? 'Editar especie' : 'Nueva especie'}</DialogTitle>
        <DialogContent>
          {saveError && <Alert severity="error" sx={{ mb: 2 }}>{saveError.message}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Nombre común" value={form.commonName} onChange={(e) => setForm({ ...form, commonName: e.target.value })} />
            <TextField label="Nombre científico" value={form.scientificName} onChange={(e) => setForm({ ...form, scientificName: e.target.value })} />
            <TextField label="Talla mínima (cm)" type="number" value={form.minSizeCm} onChange={(e) => setForm({ ...form, minSizeCm: e.target.value })} />
            <TextField
              select
              label="Método de medición"
              value={form.measurementMethod}
              onChange={(e) => setForm({ ...form, measurementMethod: e.target.value })}
            >
              <MenuItem value="TOTAL_LENGTH">Longitud total</MenuItem>
              <MenuItem value="FORK_LENGTH">Longitud a la horquilla</MenuItem>
            </TextField>
            <TextField select label="Región" value={form.region} onChange={(e) => setForm({ ...form, region: e.target.value })}>
              <MenuItem value="PACIFICO">Pacífico</MenuItem>
              <MenuItem value="CARIBE">Caribe</MenuItem>
              <MenuItem value="AMBAS">Ambas costas</MenuItem>
            </TextField>
            <TextField
              label="Imagen de referencia (URL)"
              value={form.imageUrl}
              onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
              helperText="Enlace a una imagen ya alojada (no se sube el archivo)"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>Cancelar</Button>
          <Button variant="contained" disabled={saveMutation.isPending || !canSubmit} onClick={() => saveMutation.mutate()}>
            {saveMutation.isPending ? 'Guardando...' : 'Guardar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!historyId} onClose={() => setHistoryId(null)} fullWidth maxWidth="sm">
        <DialogTitle>Historial de versiones</DialogTitle>
        <DialogContent>
          {historyQuery.isLoading && <CircularProgress size={24} />}
          {historyQuery.isError && <Alert severity="error">No fue posible cargar el historial.</Alert>}
          {historyQuery.data && (
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Versión</TableCell>
                    <TableCell>Talla mínima</TableCell>
                    <TableCell>Cambiado por</TableCell>
                    <TableCell>Fecha</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {historyQuery.data.map((v: SpeciesVersion) => (
                    <TableRow key={v.version}>
                      <TableCell>{v.version}</TableCell>
                      <TableCell>{v.minSizeCm} cm</TableCell>
                      <TableCell>{v.changedByLabel}</TableCell>
                      <TableCell>{new Date(v.changedAt).toLocaleString()}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setHistoryId(null)}>Cerrar</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
