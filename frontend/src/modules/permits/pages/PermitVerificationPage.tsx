import CameraAltIcon from '@mui/icons-material/CameraAlt';
import GavelIcon from '@mui/icons-material/Gavel';
import SearchIcon from '@mui/icons-material/Search';
import StopCircleIcon from '@mui/icons-material/StopCircle';
import { Alert, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material';
import { Html5Qrcode } from 'html5-qrcode';
import { useEffect, useRef, useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type Permit } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';

const SCANNER_ELEMENT_ID = 'permit-qr-scanner';

/** AC-08.4: verificación en campo de un permiso, escaneando el QR de su certificado. */
export function PermitVerificationPage() {
  const [scanning, setScanning] = useState(false);
  const [manualCode, setManualCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [permit, setPermit] = useState<Permit | null>(null);
  const scannerRef = useRef<Html5Qrcode | null>(null);

  const lookup = async (code: string) => {
    const value = code.trim();
    if (!value) return;
    setLoading(true);
    setError(null);
    try {
      const found = await domainService.validatePermit(value);
      setPermit(found);
    } catch (err) {
      setPermit(null);
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
        title="Verificar permiso"
        subtitle="Escanea el QR del certificado de un permiso para comprobar su vigencia en campo"
      />

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
              {!scanning ? (
                <Button variant="contained" startIcon={<CameraAltIcon />} onClick={() => void startScanning()}>
                  Escanear QR
                </Button>
              ) : (
                <Button variant="outlined" color="error" startIcon={<StopCircleIcon />} onClick={() => void stopScanning()}>
                  Detener cámara
                </Button>
              )}
              <TextField
                label="Número o código de verificación (manual)"
                size="small"
                fullWidth
                value={manualCode}
                onChange={(e) => setManualCode(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && void lookup(manualCode)}
              />
              <Button variant="outlined" startIcon={<SearchIcon />} onClick={() => void lookup(manualCode)} disabled={loading}>
                Buscar
              </Button>
            </Stack>
            <div id={SCANNER_ELEMENT_ID} style={{ width: '100%', maxWidth: 360 }} />
            {error && <Alert severity="error">{error}</Alert>}
          </Stack>
        </CardContent>
      </Card>

      {!permit && !error && (
        <EmptyState
          icon={<GavelIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
          title="Sin permiso seleccionado"
          description="Escanea un código QR o escribe el número/código del permiso para verificarlo."
        />
      )}

      {permit && (
        <Card>
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <GavelIcon color="primary" />
                <Typography variant="h5">{permit.number}</Typography>
              </Stack>
              <StatusChip status={permit.status} />
            </Stack>
            <Typography color="text.secondary">Titular: {permit.holder}</Typography>
            <Typography color="text.secondary">Tipo: {permit.type}</Typography>
            <Typography color="text.secondary">Vigencia: {permit.issued} al {permit.expires}</Typography>
            {permit.status !== 'APPROVED' && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                Este permiso no está aprobado y vigente — no habilita la operación.
              </Alert>
            )}
          </CardContent>
        </Card>
      )}
    </>
  );
}
