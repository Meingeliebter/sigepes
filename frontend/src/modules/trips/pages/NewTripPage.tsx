import MyLocationIcon from '@mui/icons-material/MyLocation';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import { PageContainer } from '@/shared/components/PageContainer';
import { Alert, AlertTitle, Button, Checkbox, CircularProgress, FormControl, FormControlLabel, IconButton, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { domainService } from '@/shared/api/domain.service';
import { toAppError } from '@/shared/errors/AppError';
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus';
import { useAuthStore } from '../../auth/store/authStore';
import { savePendingTrip } from '@/shared/storage/tripOfflineStore';

export function NewTripPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const online = useOnlineStatus();
  const vesselsQuery = useQuery({ queryKey: ['vessels'], queryFn: domainService.vessels });
  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  const closuresQuery = useQuery({ queryKey: ['closures', 'active'], queryFn: domainService.activeClosures });
  const [vesselId, setVesselId] = useState('');
  const [originPort, setOriginPort] = useState('');
  const [zone, setZone] = useState('Pacífico Norte');
  const [speciesId, setSpeciesId] = useState('');
  const [quantity, setQuantity] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [averageSizeCm, setAverageSizeCm] = useState('');
  const [landingLatitude, setLandingLatitude] = useState('');
  const [landingLongitude, setLandingLongitude] = useState('');
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [saving, setSaving] = useState(false);
  const [nullDeclaration, setNullDeclaration] = useState(false);

  const selectedSpecies = speciesQuery.data?.find((item) => item.id === speciesId);
  // AC-05.3 / AC-05.5: se busca la veda concreta (no solo un booleano) para poder mostrar un
  // banner prominente con su alcance (total/parcial), zona, periodo y resolución.
  const activeClosure = closuresQuery.data?.find(
    (closure) =>
      closure.speciesId === speciesId &&
      (closure.zone.toLowerCase() === zone.trim().toLowerCase() || closure.zone.toUpperCase() === 'TODAS'),
  );
  // Total: bloquea el envío. Parcial: solo advierte — el backend decide según el permiso del
  // pescador (excepción de subsistencia), así que el cliente no puede negar el guardado de plano.
  const blockedByClosure = activeClosure?.closureType === 'TOTAL';
  const blockedBySize =
    selectedSpecies !== undefined && Number(averageSizeCm) > 0 && Number(averageSizeCm) < selectedSpecies.minSizeCm;
  const hasLandingLocation = landingLatitude !== '' && landingLongitude !== '';

  function selectVessel(id: string) {
    setVesselId(id);
    const vessel = vesselsQuery.data?.find((item) => item.id === id);
    if (vessel && !originPort) setOriginPort(vessel.port);
  }

  function useMyLocation() {
    if (!navigator.geolocation) {
      setError('Este dispositivo no admite geolocalización. Escribe las coordenadas manualmente.');
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLandingLatitude(position.coords.latitude.toFixed(6));
        setLandingLongitude(position.coords.longitude.toFixed(6));
        setLocating(false);
      },
      () => {
        setError('No fue posible obtener tu ubicación. Escribe las coordenadas manualmente.');
        setLocating(false);
      },
    );
  }

  async function saveTrip() {
    setError('');
    setInfo('');
    if (
      !vesselId ||
      !originPort ||
      !hasLandingLocation ||
      (!nullDeclaration && (!speciesId || !quantity || !weightKg || !averageSizeCm || blockedByClosure || blockedBySize))
    ) {
      return;
    }

    setSaving(true);
    try {
      const trip = await domainService.startTrip({
        vesselId,
        zone,
        originPort,
        date: new Date().toISOString().slice(0, 10),
      });
      if (!nullDeclaration) {
        await domainService.addCapture(trip.id, {
          speciesId,
          quantity: Number(quantity),
          weightKg: Number(weightKg),
          averageSizeCm: Number(averageSizeCm),
        });
      }
      await domainService.closeTrip(trip.id, {
        nullDeclaration,
        landingLatitude: Number(landingLatitude),
        landingLongitude: Number(landingLongitude),
      });
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
        originPort,
        date: new Date().toISOString().slice(0, 10),
        status: 'PENDING_SYNC',
        nullDeclaration,
        captures: nullDeclaration
          ? []
          : [{
              id: `LOCAL-CAP-${Date.now()}`,
              speciesId,
              species: selectedSpecies?.commonName ?? 'Especie',
              quantity: Number(quantity),
              weightKg: Number(weightKg),
              averageSizeCm: Number(averageSizeCm),
            }],
        landingLatitude: Number(landingLatitude),
        landingLongitude: Number(landingLongitude),
        ownerId: user?.id ?? '',
      });
      setInfo('Sin conexión: la faena quedó guardada en este dispositivo.');
      void navigate('/app/trips');
    } finally {
      setSaving(false);
    }
  }

  const disabled =
    saving ||
    !vesselId ||
    !originPort ||
    !hasLandingLocation ||
    (!nullDeclaration && (!speciesId || !quantity || !weightKg || !averageSizeCm || Boolean(blockedByClosure || blockedBySize)));

  return (
    <PageContainer title="Nueva faena" subtitle="Registro con validación de permiso, veda y talla mínima">
      <Stack spacing={2}>
        {!online && <Alert severity="info">Modo offline activo. La sincronización se habilitará al recuperar conexión.</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {info && <Alert severity="warning">{info}</Alert>}
        {vesselsQuery.isLoading && <CircularProgress size={24} />}
        {!vesselsQuery.isLoading && (vesselsQuery.data?.length ?? 0) === 0 && (
          <Alert severity="warning">Necesitas registrar una embarcación antes de abrir una faena.</Alert>
        )}
        <FormControl fullWidth>
          <InputLabel>Embarcación</InputLabel>
          <Select value={vesselId} label="Embarcación" onChange={(event) => selectVessel(event.target.value)}>
            {vesselsQuery.data?.map((vessel) => <MenuItem key={vessel.id} value={vessel.id}>{vessel.name} · {vessel.registration}</MenuItem>)}
          </Select>
        </FormControl>
        <TextField label="Puerto de origen" value={originPort} onChange={(event) => setOriginPort(event.target.value)} />
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
        {activeClosure && (
          <Alert severity="error" variant="filled" icon={<ReportProblemIcon fontSize="large" />} sx={{ '& .MuiAlert-message': { width: '100%' } }}>
            <AlertTitle sx={{ fontWeight: 700 }}>
              {activeClosure.closureType === 'TOTAL' ? 'Veda total activa' : 'Veda parcial activa'}
            </AlertTitle>
            <Typography variant="body2">
              <strong>{activeClosure.species}</strong> está en veda en {activeClosure.zone} del {activeClosure.from} al{' '}
              {activeClosure.to} (Resolución {activeClosure.resolutionNumber}).
            </Typography>
            <Typography variant="body2" sx={{ mt: 0.5 }}>
              {activeClosure.closureType === 'TOTAL'
                ? 'Prohibida para todo tipo de pesca: no puedes registrar esta captura.'
                : 'Permitida únicamente para pesca artesanal de subsistencia; el sistema validará tu permiso al guardar.'}
            </Typography>
          </Alert>
        )}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField label="Conteo individual" type="number" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
          <TextField label="Peso (kg)" type="number" value={weightKg} onChange={(event) => setWeightKg(event.target.value)} />
          <TextField label="Talla promedio (cm)" type="number" value={averageSizeCm} onChange={(event) => setAverageSizeCm(event.target.value)} error={blockedBySize} helperText={blockedBySize ? `Mínimo legal: ${selectedSpecies?.minSizeCm} cm` : ''} />
        </Stack>
        </>}
        <Typography variant="subtitle2">Ubicación GPS de desembarco</Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <TextField label="Latitud" type="number" value={landingLatitude} onChange={(event) => setLandingLatitude(event.target.value)} />
          <TextField label="Longitud" type="number" value={landingLongitude} onChange={(event) => setLandingLongitude(event.target.value)} />
          <IconButton onClick={useMyLocation} disabled={locating} aria-label="Usar mi ubicación" color="primary">
            {locating ? <CircularProgress size={20} /> : <MyLocationIcon />}
          </IconButton>
        </Stack>
        <Stack direction="row" spacing={2}>
          <Button variant="outlined" onClick={() => navigate('/app/trips')}>Atrás</Button>
          <Button variant="contained" onClick={saveTrip} disabled={disabled}>
            {saving ? 'Guardando...' : 'Guardar faena'}
          </Button>
        </Stack>
        <Typography variant="caption" color="text.secondary">El backend también valida permiso vigente, veda activa y talla mínima.</Typography>
      </Stack>
    </PageContainer>
  );
}
