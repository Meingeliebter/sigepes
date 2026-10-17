import AddIcon from '@mui/icons-material/Add';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import {
  Alert, Button, Card, CardContent, Checkbox, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControlLabel, Grid, IconButton, MenuItem, Stack, TextField, Tooltip, Typography,
} from '@mui/material';
import { QRCodeSVG } from 'qrcode.react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type Vessel } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';

const EMPTY = { name: '', registration: '', port: '', hullType: '', enginePower: '', capacityTons: '' };
const EMPTY_GEAR = { name: '', description: '', destructive: false, status: 'ACTIVE' };
const GEAR_MANAGER_ROLES = ['ADMIN', 'ANALYST'];

export function VesselsPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const canManageGears = !!user && GEAR_MANAGER_ROLES.includes(user.role);

  const vesselsQuery = useQuery({ queryKey: ['vessels'], queryFn: domainService.vessels });
  const gearsQuery = useQuery({ queryKey: ['gears'], queryFn: domainService.gears });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(EMPTY);
  const [selectedGears, setSelectedGears] = useState<string[]>([]);
  const [qrVessel, setQrVessel] = useState<Vessel | null>(null);
  const [gearDialogOpen, setGearDialogOpen] = useState(false);
  const [gearForm, setGearForm] = useState(EMPTY_GEAR);

  const createMutation = useMutation({
    mutationFn: domainService.createVessel,
    onSuccess: async () => {
      setOpen(false);
      setForm(EMPTY);
      setSelectedGears([]);
      await queryClient.invalidateQueries({ queryKey: ['vessels'] });
    },
  });

  const createGearMutation = useMutation({
    mutationFn: domainService.createGear,
    onSuccess: async () => {
      setGearDialogOpen(false);
      setGearForm(EMPTY_GEAR);
      await queryClient.invalidateQueries({ queryKey: ['gears'] });
    },
  });

  const vessels = vesselsQuery.data ?? [];
  const gears = gearsQuery.data ?? [];
  const error = createMutation.error ? toAppError(createMutation.error) : null;
  const gearError = createGearMutation.error ? toAppError(createGearMutation.error) : null;

  const toggleGear = (id: string) =>
    setSelectedGears((current) =>
      current.includes(id) ? current.filter((gear) => gear !== id) : [...current, id],
    );

  const canSubmit =
    !!form.name && !!form.registration && !!form.port && !!form.hullType &&
    !!form.enginePower && Number(form.enginePower) > 0 &&
    !!form.capacityTons && Number(form.capacityTons) > 0;

  return (
    <>
      <PageHeader
        title="Embarcaciones"
        subtitle="Registro y estado de tus embarcaciones y artes de pesca"
        actionLabel="Nueva embarcación"
        actionIcon={<AddIcon />}
        onAction={() => setOpen(true)}
        secondaryAction={
          canManageGears ? (
            <Button variant="outlined" onClick={() => setGearDialogOpen(true)}>
              Nueva arte de pesca
            </Button>
          ) : undefined
        }
      />
      {vesselsQuery.isLoading && <CircularProgress size={28} />}
      {vesselsQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar las embarcaciones.</Alert>}
      {!vesselsQuery.isLoading && !vesselsQuery.isError && vessels.length === 0 && (
        <EmptyState
          icon={<DirectionsBoatIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
          title="Todavía no tienes embarcaciones registradas"
          description="Registra tu primera embarcación para poder abrir faenas y solicitar permisos."
          actionLabel="Nueva embarcación"
          onAction={() => setOpen(true)}
        />
      )}
      <Grid container spacing={2}>
        {vessels.map((vessel) => (
          <Grid size={{ xs: 12, md: 6 }} key={vessel.id}>
            <Card>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                  <Stack direction="row" spacing={1.5} alignItems="center">
                    <DirectionsBoatIcon color="primary" />
                    <Typography variant="h6">{vessel.name}</Typography>
                  </Stack>
                  <Stack direction="row" spacing={0.5} alignItems="center">
                    <StatusChip status={vessel.status} />
                    <Tooltip title="Ver código QR (identificación para inspección de campo)">
                      <IconButton size="small" onClick={() => setQrVessel(vessel)} aria-label="Ver código QR">
                        <QrCode2Icon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                </Stack>
                <Typography color="text.secondary" variant="body2">Matrícula DIMAR: {vessel.registration}</Typography>
                <Typography color="text.secondary" variant="body2">Puerto base: {vessel.port}</Typography>
                <Typography color="text.secondary" variant="body2">
                  Casco: {vessel.hullType} · Motor: {vessel.enginePower} HP · Capacidad: {vessel.capacityTons} t
                </Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1.5 }}>
                  {vessel.gears.map((gear) => (
                    <Chip key={gear} label={gear} size="small" variant="outlined" />
                  ))}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Nueva embarcación</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="Nombre" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField
              label="Matrícula DIMAR"
              value={form.registration}
              onChange={(e) => setForm({ ...form, registration: e.target.value })}
            />
            <TextField label="Puerto base" value={form.port} onChange={(e) => setForm({ ...form, port: e.target.value })} />
            <TextField
              label="Tipo de casco"
              value={form.hullType}
              onChange={(e) => setForm({ ...form, hullType: e.target.value })}
            />
            <TextField
              label="Potencia del motor (HP)"
              type="number"
              value={form.enginePower}
              onChange={(e) => setForm({ ...form, enginePower: e.target.value })}
            />
            <TextField
              label="Capacidad de bodega (t)"
              type="number"
              value={form.capacityTons}
              onChange={(e) => setForm({ ...form, capacityTons: e.target.value })}
            />
            <Typography variant="subtitle2">Artes de pesca</Typography>
            <Stack>
              {gears.map((gear) => (
                <FormControlLabel
                  key={gear.id}
                  control={<Checkbox checked={selectedGears.includes(gear.id)} onChange={() => toggleGear(gear.id)} disabled={gear.destructive} />}
                  label={gear.destructive ? `${gear.name} — prohibida` : gear.name}
                />
              ))}
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={createMutation.isPending || !canSubmit}
            onClick={() =>
              createMutation.mutate({
                ...form,
                enginePower: Number(form.enginePower),
                capacityTons: Number(form.capacityTons),
                gears: selectedGears,
              })
            }
          >
            {createMutation.isPending ? 'Guardando...' : 'Registrar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!qrVessel} onClose={() => setQrVessel(null)} fullWidth maxWidth="xs">
        <DialogTitle>Código QR — {qrVessel?.name}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} alignItems="center" sx={{ py: 2 }}>
            {qrVessel && <QRCodeSVG value={qrVessel.id} size={220} />}
            <Typography color="text.secondary" variant="body2" textAlign="center">
              Un inspector puede escanear este código en campo (módulo "Auditoría de campo") para ver el perfil
              de artes de pesca autorizadas de esta embarcación.
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setQrVessel(null)}>Cerrar</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={gearDialogOpen} onClose={() => setGearDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Nueva arte de pesca</DialogTitle>
        <DialogContent>
          {gearError && <Alert severity="error" sx={{ mb: 2 }}>{gearError.message}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Nombre"
              value={gearForm.name}
              onChange={(e) => setGearForm({ ...gearForm, name: e.target.value })}
            />
            <TextField
              label="Descripción"
              value={gearForm.description}
              onChange={(e) => setGearForm({ ...gearForm, description: e.target.value })}
              multiline
              minRows={2}
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={gearForm.destructive}
                  onChange={(e) =>
                    setGearForm({
                      ...gearForm,
                      destructive: e.target.checked,
                      status: e.target.checked ? 'BLOCKED' : 'ACTIVE',
                    })
                  }
                />
              }
              label="Destructiva o prohibida por regulación (AC-02.1: se rechazará al registrar embarcaciones)"
            />
            <TextField
              select
              label="Estado"
              value={gearForm.status}
              onChange={(e) => setGearForm({ ...gearForm, status: e.target.value })}
            >
              <MenuItem value="ACTIVE">Activa</MenuItem>
              <MenuItem value="BLOCKED">Bloqueada</MenuItem>
              <MenuItem value="INACTIVE">Inactiva</MenuItem>
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGearDialogOpen(false)}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={createGearMutation.isPending || !gearForm.name}
            onClick={() => createGearMutation.mutate(gearForm)}
          >
            {createGearMutation.isPending ? 'Guardando...' : 'Registrar arte'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
