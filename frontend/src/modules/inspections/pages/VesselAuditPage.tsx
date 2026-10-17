import CameraAltIcon from '@mui/icons-material/CameraAlt';
import DirectionsBoatIcon from '@mui/icons-material/DirectionsBoat';
import SearchIcon from '@mui/icons-material/Search';
import StopCircleIcon from '@mui/icons-material/StopCircle';
import {
  Alert, Button, Card, CardContent, Chip, Divider, Stack, TextField, Typography,
} from '@mui/material';
import { Html5Qrcode } from 'html5-qrcode';
import { useEffect, useRef, useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type Vessel } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';

const SCANNER_ELEMENT_ID = 'vessel-qr-scanner';

/** AC-02.5: perfil de artes de pesca de una embarcación, visto por un inspector en campo. */
export function VesselAuditPage() {
  const [scanning, setScanning] = useState(false);
  const [manualCode, setManualCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [vessel, setVessel] = useState<Vessel | null>(null);
  const scannerRef = useRef<Html5Qrcode | null>(null);

  const lookup = async (code: string) => {
    const value = code.trim();
    if (!value) return;
    setLoading(true);
    setError(null);
    try {
      const found = await domainService.vesselDetail(value);
      setVessel(found);
    } catch (err) {
      setVessel(null);
      setError(toAppError(err).message);
    } finally {
      setLoading(false);
    }
  };

  const stopScanning = async () => {
    const scanner = scannerRef.current;
    if (scanner) {
      try {
        await scanner.stop();
      } catch {
        // ya estaba detenido
      }
      scannerRef.current = null;
    }
    setScanning(false);
  };

  const startScanning = async () => {
    setError(null);
    setScanning(true);
    const scanner = new Html5Qrcode(SCANNER_ELEMENT_ID);
    scannerRef.current = scanner;
    try {
      await scanner.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: 240 },
        (decodedText) => {
          void stopScanning();
          void lookup(decodedText);
        },
        undefined,
      );
    } catch {
      setError('No fue posible acceder a la cámara. Usa el código manual como respaldo.');
      setScanning(false);
      scannerRef.current = null;
    }
  };

  useEffect(() => () => {
    scannerRef.current?.stop().catch(() => undefined);
  }, []);

  return (
    <>
      <PageHeader
        title="Auditoría de campo"
        subtitle="Escanea el código QR de una embarcación para verificar su perfil de artes de pesca autorizadas"
      />

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
              {!scanning ? (
                <Button variant="contained" startIcon={<CameraAltIcon />} onClick={() => void startScanning()} sx={{ flexShrink: 0, whiteSpace: 'nowrap' }}>
                  Escanear QR
                </Button>
              ) : (
                <Button variant="outlined" color="error" startIcon={<StopCircleIcon />} onClick={() => void stopScanning()} sx={{ flexShrink: 0, whiteSpace: 'nowrap' }}>
                  Detener cámara
                </Button>
              )}
              <TextField
                label="Código de la embarcación (manual)"
                size="small"
                fullWidth
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && void lookup(manualCode)}
              />
              <Button variant="outlined" startIcon={<SearchIcon />} onClick={() => void lookup(manualCode)} disabled={loading} sx={{ flexShrink: 0 }}>
                Buscar
              </Button>
            </Stack>
            <div id={SCANNER_ELEMENT_ID} style={{ width: '100%', maxWidth: 360 }} />
            {error && <Alert severity="error">{error}</Alert>}
          </Stack>
        </CardContent>
      </Card>

      {!vessel && !error && (
        <EmptyState
          icon={<DirectionsBoatIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
          title="Sin embarcación seleccionada"
          description="Escanea un código QR o escribe el identificador de la embarcación para ver su perfil."
        />
      )}

      {vessel && (
        <Card>
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <DirectionsBoatIcon color="primary" />
                <Typography variant="h5">{vessel.name}</Typography>
              </Stack>
              <StatusChip status={vessel.status} />
            </Stack>
            <Typography color="text.secondary">Matrícula DIMAR: {vessel.registration}</Typography>
            <Typography color="text.secondary">Propietario: {vessel.ownerName ?? vessel.ownerId}</Typography>
            <Typography color="text.secondary">Puerto base: {vessel.port}</Typography>
            <Typography color="text.secondary">
              Casco: {vessel.hullType} · Motor: {vessel.enginePower} HP · Capacidad: {vessel.capacityTons} t
            </Typography>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Artes de pesca autorizadas</Typography>
            {vessel.gears.length === 0 ? (
              <Typography color="text.secondary" variant="body2">Esta embarcación no tiene artes asociadas.</Typography>
            ) : (
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {vessel.gears.map((gear) => (
                  <Chip key={gear} label={gear} size="small" color="success" variant="outlined" />
                ))}
              </Stack>
            )}
          </CardContent>
        </Card>
      )}
    </>
  );
}
