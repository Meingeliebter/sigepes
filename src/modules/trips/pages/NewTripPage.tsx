import { PageContainer } from '@/shared/components/PageContainer';
import { Alert, Button, Checkbox, CircularProgress, FormControl, FormControlLabel, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { domainService } from '@/shared/api/domain.service';
import { toAppError } from '@/shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';
import { savePendingTrip } from '@/shared/storage/tripOfflineStore';

export function NewTripPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const vesselsQuery = useQuery({ queryKey: ['vessels'], queryFn: domainService.vessels });
  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  const closuresQuery = useQuery({ queryKey: ['closures', 'active'], queryFn: domainService.activeClosures });
  const [vesselId, setVesselId] = useState('');
  const [zone, setZone] = useState('Pacífico Norte');
  const [speciesId, setSpeciesId] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [averageSizeCm, setAverageSizeCm] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [saving, setSaving] = useState(false);
  const [nullDeclaration, setNullDeclaration] = useState(false);

  const selectedSpecies = speciesQuery.data?.find((item) => item.id === speciesId);
  const blockedByClosure = closuresQuery.data?.some(
    (closure) =>
      closure.speciesId === speciesId &&
      (closure.zone.toLowerCase() === zone.trim().toLowerCase() || closure.zone.toUpperCase() === 'TODAS'),
  );
  const blockedBySize =
    selectedSpecies !== undefined && Number(averageSizeCm) > 0 && Number(averageSizeCm) < selectedSpecies.minSizeCm;

  async function saveTrip() {
    setError('');
    setInfo('');
    if (!vesselId || (!nullDeclaration && (!speciesId || !weightKg || !averageSizeCm || blockedByClosure || blockedBySize))) return;

    setSaving(true);
    try {
      const trip = await domainService.startTrip({ vesselId, zone, date: new Date().toISOString().slice(0, 10) });
      if (nullDeclaration) {
        await domainService.closeTrip(trip.id, true);
      } else {
        await domainService.addCapture(trip.id, {
          speciesId,
          weightKg: Number(weightKg),
          averageSizeCm: Number(averageSizeCm),
        });
        await domainService.closeTrip(trip.id, false);
      }
      void navigate('/app/trips');
    } catch (requestError) {
      const appError = toAppError(requestError);

      // Solo se guarda en el dispositivo si el servidor no respondió. Un rechazo por veda,
      // talla o permiso es una decisión del backend y debe mostrarse, no sincronizarse después.
      if (appError.code !== 'NETWORK_ERROR') {
        setError(appError.message);
        setSaving(false);
        return;
      }

      savePendingTrip({
        id: `LOCAL-${Date.now()}`,
        vesselId,
        vessel: vesselsQuery.data?.find((item) => item.id === vesselId)?.name ?? 'Embarcación',
        zone,
        date: new Date().toISOString().slice(0, 10),
        status: 'PENDING_SYNC',
        nullDeclaration,
        captures: nullDeclaration
          ? []
          : [{
              id: `LOCAL-CAP-${Date.now()}`,
              speciesId,
              species: selectedSpecies?.commonName ?? 'Especie',
              weightKg: Number(weightKg),
              averageSizeCm: Number(averageSizeCm),
            }],
        ownerId: user?.id ?? '',
      });
      setInfo('Sin conexión: la faena quedó guardada en este dispositivo.');
      void navigate('/app/trips');
    } finally {
      setSaving(false);
    }
  }

  return (
    <PageContainer title="Nueva faena" subtitle="Registro con validación de permiso, veda y talla mínima">
      <Stack spacing={2}>
        {!navigator.onLine && <Alert severity="info">Modo offline activo. La sincronización se habilitará al recuperar conexión.</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {info && <Alert severity="warning">{info}</Alert>}
        {vesselsQuery.isLoading && <CircularProgress size={24} />}
        {!vesselsQuery.isLoading && (vesselsQuery.data?.length ?? 0) === 0 && (
          <Alert severity="warning">Necesitas registrar una embarcación antes de abrir una faena.</Alert>
        )}
        <FormControl fullWidth>
          <InputLabel>Embarcación</InputLabel>
          <Select value={vesselId} label="Embarcación" onChange={(event) => setVesselId(event.target.value)}>
            {vesselsQuery.data?.map((vessel) => <MenuItem key={vessel.id} value={vessel.id}>{vessel.name} · {vessel.registration}</MenuItem>)}
          </Select>
        </FormControl>
        <TextField label="Zona de faena" value={zone} onChange={(event) => setZone(event.target.value)} />
        <FormControlLabel control={<Checkbox checked={nullDeclaration} onChange={(event) => setNullDeclaration(event.target.checked)} />} label="Declaración de captura nula" />
        {nullDeclaration && <Alert severity="info">La faena quedará cerrada sin capturas con declaración nula.</Alert>}
        {!nullDeclaration && <>
        <FormControl fullWidth>
          <InputLabel>Especie</InputLabel>
          <Select value={speciesId} label="Especie" onChange={(event) => setSpeciesId(event.target.value)}>
            {speciesQuery.data?.map((item) => <MenuItem key={item.id} value={item.id}>{item.commonName} · mínima {item.minSizeCm} cm</MenuItem>)}
          </Select>
        </FormControl>
        {blockedByClosure && <Alert severity="error">La especie está en veda activa para esta zona.</Alert>}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField label="Peso (kg)" type="number" value={weightKg} onChange={(event) => setWeightKg(event.target.value)} />
          <TextField label="Talla promedio (cm)" type="number" value={averageSizeCm} onChange={(event) => setAverageSizeCm(event.target.value)} error={blockedBySize} helperText={blockedBySize ? `Mínimo legal: ${selectedSpecies?.minSizeCm} cm` : ''} />
        </Stack>
        </>}
        <Stack direction="row" spacing={2}>
          <Button variant="outlined" onClick={() => navigate('/app/trips')}>Atrás</Button>
          <Button variant="contained" onClick={saveTrip} disabled={saving || !vesselId || (!nullDeclaration && (!speciesId || !weightKg || !averageSizeCm || Boolean(blockedByClosure || blockedBySize)))}>
            {saving ? 'Guardando...' : 'Guardar faena'}
          </Button>
        </Stack>
        <Typography variant="caption" color="text.secondary">El backend también valida permiso vigente, veda activa y talla mínima.</Typography>
      </Stack>
    </PageContainer>
  );
}
