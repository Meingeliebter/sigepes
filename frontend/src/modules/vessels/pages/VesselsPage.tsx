import AddIcon from '@mui/icons-material/Add';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import {
  Alert, Button, Card, CardContent, Checkbox, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControlLabel, Grid, Stack, TextField, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';

const EMPTY = { name: '', registration: '', port: '' };

export function VesselsPage() {
  const queryClient = useQueryClient();
  const vesselsQuery = useQuery({ queryKey: ['vessels'], queryFn: domainService.vessels });
  const gearsQuery = useQuery({ queryKey: ['gears'], queryFn: domainService.gears });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(EMPTY);
  const [selectedGears, setSelectedGears] = useState<string[]>([]);

  const createMutation = useMutation({
    mutationFn: domainService.createVessel,
    onSuccess: async () => {
      setOpen(false);
      setForm(EMPTY);
      setSelectedGears([]);
      await queryClient.invalidateQueries({ queryKey: ['vessels'] });
    },
  });

  const vessels = vesselsQuery.data ?? [];
  const gears = gearsQuery.data ?? [];
  const error = createMutation.error ? toAppError(createMutation.error) : null;

  const toggleGear = (id: string) =>
    setSelectedGears((current) =>
      current.includes(id) ? current.filter((gear) => gear !== id) : [...current, id],
    );

  return (
    <>
      <PageHeader
        title="Embarcaciones"
        subtitle="Registro y estado de tus embarcaciones y artes de pesca"
        actionLabel="Nueva embarcación"
        actionIcon={<AddIcon />}
        onAction={() => setOpen(true)}
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
                  <StatusChip status={vessel.status} />
                </Stack>
                <Typography color="text.secondary" variant="body2">Matrícula {vessel.registration}</Typography>
                <Typography color="text.secondary" variant="body2" sx={{ mb: 1.5 }}>Puerto base: {vessel.port}</Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
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
            <TextField label="Matrícula" value={form.registration} onChange={(e) => setForm({ ...form, registration: e.target.value })} />
            <TextField label="Puerto base" value={form.port} onChange={(e) => setForm({ ...form, port: e.target.value })} />
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
            disabled={createMutation.isPending || !form.name || !form.registration || !form.port}
            onClick={() => createMutation.mutate({ ...form, gears: selectedGears })}
          >
            {createMutation.isPending ? 'Guardando...' : 'Registrar'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
