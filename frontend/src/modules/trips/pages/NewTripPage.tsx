import GpsFixedIcon from '@mui/icons-material/GpsFixed';
import MyLocationIcon from '@mui/icons-material/MyLocation';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import { PageContainer } from '@/shared/components/PageContainer';
import { Alert, AlertTitle, Button, Checkbox, CircularProgress, FormControl, FormControlLabel, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { domainService } from '@/shared/api/domain.service';
import { toAppError } from '@/shared/errors/AppError';
import { useOnlineStatus } from '@/shared/hooks/useOnlineStatus';
import { useAuthStore } from '../../auth/store/authStore';
import { savePendingTrip } from '@/shared/storage/tripOfflineStore';
import { findApplicableQuota } from '@/shared/storage/quotaOfflineStore';
import { useTripTracker } from '@/shared/hooks/useTripTracker';
import { ZONE_TYPE_LABEL } from '@/shared/geo/geo';

/** Bloque del formulario con su título; agrupa los campos como los apartados de la bitácora en papel. */
function FormSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Stack component="fieldset" spacing={2} sx={{ border: 0, p: 0, m: 0, minWidth: 0 }}>
      <Typography component="legend" variant="subtitle1" sx={{ fontWeight: 750, color: 'primary.dark', mb: 2, p: 0 }}>
        {title}
      </Typography>
      {children}
    </Stack>
  );
}

export function NewTripPage() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const online = useOnlineStatus();
  const vesselsQuery = useQuery({ queryKey: ['vessels'], queryFn: domainService.vessels });
  const speciesQuery = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  const closuresQuery = useQuery({ queryKey: ['closures', 'active'], queryFn: domainService.activeClosures });
  // AC-07.6: la lista de cuotas se cachea en el dispositivo; sin señal se valida con el último saldo.
  const quotasQuery = useQuery({ queryKey: ['quotas'], queryFn: domainService.quotas });
  // AC-04.1 / AC-04.6: zonas cacheadas y seguimiento GPS mientras la faena está abierta.
  const zonesQuery = useQuery({ queryKey: ['zones'], queryFn: domainService.zones });
  const tracker = useTripTracker(zonesQuery.data, true);
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
  // RN-14 / AC-07.3: la captura no puede superar el saldo de la cuota que le aplica.
  const quota = findApplicableQuota(quotasQuery.data, speciesId, zone, vesselId);
  const blockedByQuota = quota !== undefined && Number(weightKg) > 0 && Number(weightKg) > quota.remainingKg;
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
      (!nullDeclaration && (!speciesId || !quantity || !weightKg || !averageSizeCm || blockedByClosure || blockedBySize || blockedByQuota))
    ) {
      return;
    }

    setSaving(true);
    // AC-04.1: además del muestreo periódico, se registra la posición en el momento de la captura.
    const atCatch = await tracker.capture();
    const positions = atCatch && !tracker.positions.some((item) => item.id === atCatch.id)
      ? [...tracker.positions, atCatch]
      : tracker.positions;
    try {
      const trip = await domainService.startTrip({
        vesselId,
        zone,
        originPort,
        date: new Date().toISOString().slice(0, 10),
      });
      if (positions.length > 0) {
        await domainService.postPositions(trip.id, positions);
      }
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
        positions,
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
    (!nullDeclaration && (!speciesId || !quantity || !weightKg || !averageSizeCm || Boolean(blockedByClosure || blockedBySize || blockedByQuota)));

  return (
    <PageContainer title="Nueva faena" subtitle="Registro con validación de permiso, veda y talla mínima" maxWidth={880}>
      <Stack spacing={3}>
        {!online && <Alert severity="info">Modo offline activo. La sincronización se habilitará al recuperar conexión.</Alert>}
        {error && <Alert severity="error">{error}</Alert>}
        {info && <Alert severity="warning">{info}</Alert>}
        {tracker.currentZone && (
          <Alert severity="error" variant="filled" icon={<ReportProblemIcon fontSize="large" />} role="alert">
            <AlertTitle sx={{ fontWeight: 700 }}>Estás dentro de una zona restringida</AlertTitle>
            <Typography variant="body2">
              <strong>{tracker.currentZone.name}</strong> · {ZONE_TYPE_LABEL[tracker.currentZone.zoneType]} (
              {tracker.currentZone.resolutionNumber}). La extracción aquí no está autorizada: el ingreso queda
              registrado para la autoridad.
            </Typography>
          </Alert>
        )}
        <Stack direction="row" spacing={1} alignItems="center" sx={{ color: 'text.secondary' }}>
          <GpsFixedIcon fontSize="small" color={tracker.status === 'tracking' ? 'secondary' : 'disabled'} />
          <Typography variant="body2">
            {tracker.status === 'tracking' && `Seguimiento GPS activo · ${tracker.positions.length} posición(es) registradas`}
            {tracker.status === 'idle' && 'Activando el seguimiento GPS…'}
            {tracker.status === 'denied' && 'Sin permiso de ubicación: la faena se registra sin recorrido. Actívalo para recibir alertas de zonas restringidas.'}
            {tracker.status === 'unavailable' && 'Ubicación no disponible en este momento; se reintentará en 5 minutos.'}
          </Typography>
        </Stack>
        {vesselsQuery.isLoading && <CircularProgress size={24} />}
        {!vesselsQuery.isLoading && (vesselsQuery.data?.length ?? 0) === 0 && (
          <Alert severity="warning">Necesitas registrar una embarcación antes de abrir una faena.</Alert>
        )}

        <FormSection title="Embarcación y zona">
          <FormControl fullWidth>
            <InputLabel id="trip-vessel-label">Embarcación</InputLabel>
            <Select labelId="trip-vessel-label" value={vesselId} label="Embarcación" onChange={(event) => selectVessel(event.target.value)}>
              {vesselsQuery.data?.map((vessel) => <MenuItem key={vessel.id} value={vessel.id}>{vessel.name} · {vessel.registration}</MenuItem>)}
            </Select>
          </FormControl>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField label="Puerto de origen" value={originPort} onChange={(event) => setOriginPort(event.target.value)} />
            <TextField label="Zona de faena" value={zone} onChange={(event) => setZone(event.target.value)} />
          </Stack>
        </FormSection>

        <FormSection title="Captura">
          <FormControlLabel control={<Checkbox checked={nullDeclaration} onChange={(event) => setNullDeclaration(event.target.checked)} />} label="Declaración de captura nula" />
          {nullDeclaration && <Alert severity="info">La faena quedará cerrada sin capturas con declaración nula.</Alert>}
          {!nullDeclaration && <>
          <FormControl fullWidth>
            <InputLabel id="trip-species-label">Especie</InputLabel>
            <Select labelId="trip-species-label" value={speciesId} label="Especie" onChange={(event) => setSpeciesId(event.target.value)}>
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
          {blockedByQuota && quota && (
            <Alert severity="error">
              <AlertTitle sx={{ fontWeight: 700 }}>Cuota agotada</AlertTitle>
              La cuota de <strong>{quota.species}</strong> en {quota.zone === 'TODAS' ? 'todas las zonas' : quota.zone} tiene{' '}
              {quota.remainingKg.toLocaleString('es-CO')} kg disponibles de {quota.totalKg.toLocaleString('es-CO')} kg.
              {!online && ' Validado con el último saldo guardado en el dispositivo.'}
            </Alert>
          )}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField label="Conteo individual" type="number" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
            <TextField
              label="Peso (kg)"
              type="number"
              value={weightKg}
              onChange={(event) => setWeightKg(event.target.value)}
              error={blockedByQuota}
              helperText={quota ? `Saldo de cuota: ${quota.remainingKg.toLocaleString('es-CO')} kg` : ' '}
            />
            <TextField
              label="Talla promedio (cm)"
              type="number"
              value={averageSizeCm}
              onChange={(event) => setAverageSizeCm(event.target.value)}
              error={blockedBySize}
              helperText={selectedSpecies ? `Mínimo legal: ${selectedSpecies.minSizeCm} cm` : ' '}
            />
          </Stack>
          </>}
        </FormSection>

        <FormSection title="Desembarco">
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'flex-start' }}>
            <TextField label="Latitud" type="number" value={landingLatitude} onChange={(event) => setLandingLatitude(event.target.value)} />
            <TextField label="Longitud" type="number" value={landingLongitude} onChange={(event) => setLandingLongitude(event.target.value)} />
            <Button
              variant="outlined"
              onClick={useMyLocation}
              disabled={locating}
              startIcon={locating ? <CircularProgress size={18} /> : <MyLocationIcon />}
              sx={{ flexShrink: 0, whiteSpace: 'nowrap', minHeight: 56 }}
            >
              Usar mi ubicación
            </Button>
          </Stack>
        </FormSection>

        <Stack
          direction={{ xs: 'column-reverse', sm: 'row' }}
          spacing={1.5}
          justifyContent="space-between"
          alignItems={{ sm: 'center' }}
          sx={{ pt: 2.5, borderTop: '1px solid', borderColor: 'divider' }}
        >
          <Typography variant="caption" color="text.secondary">El backend también valida permiso vigente, veda activa y talla mínima.</Typography>
          <Stack direction="row" spacing={1.5} justifyContent="flex-end">
            <Button variant="text" onClick={() => navigate('/app/trips')}>Cancelar</Button>
            <Button variant="contained" onClick={saveTrip} disabled={disabled}>
              {saving ? 'Guardando...' : 'Guardar faena'}
            </Button>
          </Stack>
        </Stack>
      </Stack>
    </PageContainer>
  );
}
