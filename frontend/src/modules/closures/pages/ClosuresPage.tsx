import AddIcon from '@mui/icons-material/Add';
import BlockIcon from '@mui/icons-material/Block';
import EditIcon from '@mui/icons-material/Edit';
import {
  Alert, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, Grid, IconButton, InputLabel, MenuItem, Select, Stack, TextField, Tooltip, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type Closure } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';

const CLOSURE_TYPE_LABEL: Record<string, string> = {
  TOTAL: 'Total',
  PARTIAL: 'Parcial (subsistencia)',
};

const EMPTY_FORM = {
  speciesId: '',
  zone: '',
  from: '',
  to: '',
  resolutionNumber: '',
  closureType: 'TOTAL',
  reason: '',
  notes: '',
};

export function ClosuresPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const canManage = isAuthority(user?.role);

  const closuresQuery = useQuery({ queryKey: ['closures'], queryFn: domainService.closures });
  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  const closures = closuresQuery.data ?? [];

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);

  const saveMutation = useMutation({
    mutationFn: () => {
      if (editingId) {
        return domainService.updateClosure(editingId, {
          from: form.from,
          to: form.to,
          resolutionNumber: form.resolutionNumber,
          closureType: form.closureType,
          notes: form.notes,
        });
      }
      return domainService.createClosure({
        speciesId: form.speciesId,
        zone: form.zone,
        from: form.from,
        to: form.to,
        resolutionNumber: form.resolutionNumber,
        closureType: form.closureType,
        reason: form.reason,
        notes: form.notes,
      });
    },
    onSuccess: async () => {
      setFormOpen(false);
      setEditingId(null);
      setForm(EMPTY_FORM);
      await queryClient.invalidateQueries({ queryKey: ['closures'] });
      await queryClient.invalidateQueries({ queryKey: ['closures', 'active'] });
    },
  });
  const saveError = saveMutation.error ? toAppError(saveMutation.error) : null;

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormOpen(true);
  }

  function openEdit(closure: Closure) {
    setEditingId(closure.id);
    setForm({
      speciesId: closure.speciesId,
      zone: closure.zone,
      from: closure.from,
      to: closure.to,
      resolutionNumber: closure.resolutionNumber,
      closureType: closure.closureType,
      reason: closure.reason ?? '',
      notes: closure.notes ?? '',
    });
    setFormOpen(true);
  }

  const canSubmit =
    (editingId || !!form.speciesId) && (editingId || !!form.zone) && !!form.from && !!form.to && !!form.resolutionNumber;

  return (
    <>
      <PageHeader
        title="Vedas pesqueras"
        subtitle="Periodos de restricción por especie y zona"
        actionLabel={canManage ? 'Nueva veda' : undefined}
        actionIcon={canManage ? <AddIcon /> : undefined}
        onAction={canManage ? openCreate : undefined}
      />
      {closuresQuery.isLoading && <CircularProgress size={28} />}
      {closuresQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar las vedas.</Alert>}
      {!closuresQuery.isLoading && !closuresQuery.isError && closures.length === 0 && (
        <EmptyState
          icon={<BlockIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
          title="No hay vedas registradas"
          description="Actualmente no existen periodos de restricción declarados por la autoridad."
        />
      )}
      <Grid container spacing={2}>
        {closures.map((closure) => (
          <Grid size={{ xs: 12, md: 4 }} key={closure.id}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <BlockIcon color="error" />
                    <Typography variant="h6">{closure.species}</Typography>
                  </Stack>
                  <Stack direction="row" spacing={0.5} alignItems="center">
                    <StatusChip status={closure.status} />
                    {canManage && (
                      <Tooltip title="Editar">
                        <IconButton size="small" onClick={() => openEdit(closure)} aria-label="Editar veda">
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </Stack>
                </Stack>
                <Typography variant="body2" color="text.secondary">Zona: {closure.zone}</Typography>
                <Typography variant="body2" color="text.secondary">Del {closure.from} al {closure.to}</Typography>
                <Typography variant="body2" color="text.secondary">Resolución: {closure.resolutionNumber}</Typography>
                <Chip
                  size="small"
                  sx={{ mt: 1 }}
                  color={closure.closureType === 'TOTAL' ? 'error' : 'warning'}
                  label={CLOSURE_TYPE_LABEL[closure.closureType] ?? closure.closureType}
                />
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={formOpen} onClose={() => setFormOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editingId ? 'Editar veda' : 'Nueva veda'}</DialogTitle>
        <DialogContent>
          {saveError && <Alert severity="error" sx={{ mb: 2 }}>{saveError.message}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormControl fullWidth disabled={!!editingId}>
              <InputLabel>Especie</InputLabel>
              <Select value={form.speciesId} label="Especie" onChange={(e) => setForm({ ...form, speciesId: e.target.value })}>
                {speciesQuery.data?.map((sp) => <MenuItem key={sp.id} value={sp.id}>{sp.commonName}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField
              label="Zona (o «TODAS»)"
              value={form.zone}
              onChange={(e) => setForm({ ...form, zone: e.target.value })}
              disabled={!!editingId}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Fecha de inicio"
                type="date"
                value={form.from}
                onChange={(e) => setForm({ ...form, from: e.target.value })}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="Fecha de fin"
                type="date"
                value={form.to}
                onChange={(e) => setForm({ ...form, to: e.target.value })}
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
            </Stack>
            <TextField select label="Tipo de veda" value={form.closureType} onChange={(e) => setForm({ ...form, closureType: e.target.value })}>
              <MenuItem value="TOTAL">Total (prohibida para todos)</MenuItem>
              <MenuItem value="PARTIAL">Parcial (solo subsistencia)</MenuItem>
            </TextField>
            <TextField
              label="Número de resolución"
              value={form.resolutionNumber}
              onChange={(e) => setForm({ ...form, resolutionNumber: e.target.value })}
              helperText="Documento legal de respaldo, p. ej. Resolución AUNAP"
            />
            <TextField label="Motivo" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} multiline minRows={2} />
            <TextField label="Notas" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} multiline minRows={2} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>Cancelar</Button>
          <Button variant="contained" disabled={saveMutation.isPending || !canSubmit} onClick={() => saveMutation.mutate()}>
            {saveMutation.isPending ? 'Guardando...' : 'Guardar'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
