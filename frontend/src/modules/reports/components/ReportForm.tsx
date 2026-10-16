import AddPhotoAlternateIcon from '@mui/icons-material/AddPhotoAlternate';
import CloseIcon from '@mui/icons-material/Close';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import MyLocationIcon from '@mui/icons-material/MyLocation';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import {
  Alert, Box, Button, Checkbox, FormControlLabel, Grid, IconButton, MenuItem, Stack, TextField, Tooltip, Typography,
} from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { GeoMap } from '../../../shared/components/GeoMap';
import { domainService, type ReportInput, type ReportReceipt, type ReportType } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useInteractiveMap } from '../../../shared/hooks/useInteractiveMap';
import { ACCEPTED_PHOTO_TYPES, MAX_PHOTO_BYTES, stripImage } from '../../../shared/media/stripImage';
import { enqueueReport, saveReceipt } from '../../../shared/storage/reportOutbox';
import { getCachedZones } from '../../../shared/storage/zoneOfflineStore';
import { REPORT_TYPE_LABEL } from '../reportLabels';

const MAX_PHOTOS = 3;

function newClientId(): string {
  const random = typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID().replace(/-/g, '')
    : Math.random().toString(36).slice(2) + Date.now().toString(36);
  return `rep-${random.slice(0, 24)}`;
}

function today(): string {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

type Outcome = { kind: 'sent'; receipt: ReportReceipt } | { kind: 'queued' };

interface ReportFormProps {
  /** Hay sesión iniciada: se puede elegir entre denuncia identificada o anónima. */
  signedIn: boolean;
}

const EMPTY = { reportType: 'CLOSURE_FISHING' as ReportType, description: '', occurredOn: today(), latitude: '', longitude: '', locationText: '' };

/**
 * AC-10.1 · Formulario de denuncia. Sin conexión, la denuncia queda en el dispositivo y se envía
 * sola al volver la señal (AC-10.6).
 */
export function ReportForm({ signedIn }: ReportFormProps) {
  const [form, setForm] = useState(EMPTY);
  const [anonymous, setAnonymous] = useState(true);
  const [photos, setPhotos] = useState<File[]>([]);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const [gpsMessage, setGpsMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [copied, setCopied] = useState(false);

  const previews = useMemo(() => photos.map((photo) => URL.createObjectURL(photo)), [photos]);
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews]);

  const latitude = form.latitude.trim() === '' ? undefined : Number(form.latitude);
  const longitude = form.longitude.trim() === '' ? undefined : Number(form.longitude);
  const latInvalid = latitude !== undefined && (Number.isNaN(latitude) || latitude < -90 || latitude > 90);
  const lonInvalid = longitude !== undefined && (Number.isNaN(longitude) || longitude < -180 || longitude > 180);
  const hasPoint = latitude !== undefined && longitude !== undefined && !latInvalid && !lonInvalid;
  const hasPlace = form.locationText.trim().length > 0;
  const descriptionShort = form.description.trim().length > 0 && form.description.trim().length < 10;
  const halfPoint = (latitude === undefined) !== (longitude === undefined);
  const canSubmit = form.description.trim().length >= 10 && (hasPoint || hasPlace) && !latInvalid && !lonInvalid && !halfPoint && !submitting;

  const zones = useMemo(() => getCachedZones() ?? [], []);
  const interactiveMap = useInteractiveMap();
  const effectiveAnonymous = !signedIn || anonymous;

  const addPhotos = (files: FileList | null) => {
    setPhotoError(null);
    if (!files) return;
    const incoming = Array.from(files);
    const invalid = incoming.find((file) => !ACCEPTED_PHOTO_TYPES.includes(file.type));
    const tooBig = incoming.find((file) => file.size > MAX_PHOTO_BYTES);
    if (invalid) return setPhotoError('Solo se admiten fotos JPG o PNG.');
    if (tooBig) return setPhotoError('Cada foto puede pesar como máximo 5 MB.');
    if (photos.length + incoming.length > MAX_PHOTOS) return setPhotoError('Puedes adjuntar hasta 3 fotos.');
    setPhotos([...photos, ...incoming]);
  };

  const locateWithGps = () => {
    if (!('geolocation' in navigator)) {
      setGpsMessage('Este dispositivo no tiene GPS disponible. Escribe las coordenadas o describe el lugar.');
      return;
    }
    setGpsMessage('Obteniendo tu posición…');
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setForm((current) => ({
          ...current,
          latitude: position.coords.latitude.toFixed(5),
          longitude: position.coords.longitude.toFixed(5),
        }));
        setGpsMessage(`Posición tomada con una precisión de ${Math.round(position.coords.accuracy)} m.`);
      },
      () => setGpsMessage('No fue posible leer el GPS. Revisa el permiso de ubicación o describe el lugar.'),
      { enableHighAccuracy: true, timeout: 15000 },
    );
  };

  const reset = () => {
    setForm({ ...EMPTY, occurredOn: today() });
    setPhotos([]);
    setOutcome(null);
    setError(null);
    setCopied(false);
    setGpsMessage(null);
  };

  const submit = async () => {
    setSubmitting(true);
    setError(null);
    const payload: ReportInput = {
      clientId: newClientId(),
      reportType: form.reportType,
      description: form.description.trim(),
      occurredOn: form.occurredOn,
      latitude: hasPoint ? latitude : undefined,
      longitude: hasPoint ? longitude : undefined,
      locationText: hasPlace ? form.locationText.trim() : undefined,
      anonymous: effectiveAnonymous,
    };
    const cleanPhotos = await Promise.all(photos.map(stripImage));
    const queue = async () => {
      await enqueueReport({ clientId: payload.clientId, payload, photos: cleanPhotos, queuedAt: new Date().toISOString() });
      setOutcome({ kind: 'queued' });
    };
    try {
      if (!navigator.onLine) {
        await queue();
      } else {
        const receipt = await domainService.submitReport(payload, cleanPhotos);
        saveReceipt(receipt);
        setOutcome({ kind: 'sent', receipt });
      }
    } catch (caught) {
      const appError = toAppError(caught);
      // Sin respuesta del servidor (señal intermitente): se guarda para reenviar, no se pierde.
      if (appError.status === undefined) {
        try {
          await queue();
        } catch {
          setError('No hay conexión y este navegador no permite guardar la denuncia. Inténtalo de nuevo con señal.');
        }
      } else {
        setError(appError.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (outcome?.kind === 'sent') {
    const code = outcome.receipt.trackingCode;
    return (
      <Stack spacing={2.5} role="status" aria-live="polite">
        <Alert severity="success">Recibimos tu denuncia. La autoridad pesquera la revisará.</Alert>
        <Box sx={{ p: 3, borderRadius: 4, textAlign: 'center', bgcolor: 'rgba(14,124,134,.07)', border: '1px dashed', borderColor: 'secondary.light' }}>
          <Typography variant="overline" color="text.secondary">Código de seguimiento</Typography>
          <Stack direction="row" spacing={1} alignItems="center" justifyContent="center">
            <Typography sx={{ fontFamily: 'ui-monospace, Consolas, monospace', fontSize: { xs: '1.6rem', sm: '2rem' }, fontWeight: 700, letterSpacing: '0.06em' }}>
              {code}
            </Typography>
            <Tooltip title={copied ? 'Copiado' : 'Copiar código'}>
              <IconButton
                aria-label="Copiar código de seguimiento"
                onClick={() => void navigator.clipboard?.writeText(code).then(() => setCopied(true))}
              >
                <ContentCopyIcon />
              </IconButton>
            </Tooltip>
          </Stack>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            {outcome.receipt.anonymous
              ? 'Es anónima: este código es la única forma de consultar su estado. Lo guardamos solo en este dispositivo; anótalo.'
              : 'Quedó asociada a tu cuenta. También puedes consultarla con este código.'}
          </Typography>
        </Box>
        <Button variant="outlined" onClick={reset} sx={{ alignSelf: 'flex-start' }}>Radicar otra denuncia</Button>
      </Stack>
    );
  }

  if (outcome?.kind === 'queued') {
    return (
      <Stack spacing={2.5} role="status" aria-live="polite">
        <Alert severity="info">
          No hay señal: la denuncia quedó guardada en este dispositivo y se enviará sola cuando vuelva la conexión.
          El código de seguimiento aparecerá en «Consultar estado».
        </Alert>
        <Button variant="outlined" onClick={reset} sx={{ alignSelf: 'flex-start' }}>Radicar otra denuncia</Button>
      </Stack>
    );
  }

  return (
    <Stack spacing={2.5} component="form" noValidate onSubmit={(event) => { event.preventDefault(); if (canSubmit) void submit(); }}>
      {error && <Alert severity="error" role="alert">{error}</Alert>}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 7 }}>
          <TextField
            select fullWidth required label="¿Qué observaste?" value={form.reportType}
            onChange={(e) => setForm({ ...form, reportType: e.target.value as ReportType })}
          >
            {(Object.keys(REPORT_TYPE_LABEL) as ReportType[]).map((type) => (
              <MenuItem key={type} value={type}>{REPORT_TYPE_LABEL[type]}</MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid size={{ xs: 12, sm: 5 }}>
          <TextField
            fullWidth required type="date" label="Fecha del hecho" value={form.occurredOn}
            onChange={(e) => setForm({ ...form, occurredOn: e.target.value })}
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: today() } }}
          />
        </Grid>
      </Grid>

      <TextField
        required multiline minRows={4} label="Descripción" value={form.description}
        onChange={(e) => setForm({ ...form, description: e.target.value })}
        error={descriptionShort}
        helperText={descriptionShort ? 'Cuéntanos un poco más (mínimo 10 caracteres).' : 'Qué pasó, embarcación, arte de pesca o especie si los viste. No escribas datos tuyos si quieres seguir anónimo.'}
        slotProps={{ htmlInput: { maxLength: 2000 } }}
      />

      <Box>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>¿Dónde ocurrió?</Typography>
        {interactiveMap && (
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: -0.5, mb: 1.5 }}>
            Marca el punto en el mapa, usa tu GPS o escribe las coordenadas.
          </Typography>
        )}
        <Grid container spacing={2} alignItems="flex-start">
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField
              fullWidth label="Latitud" value={form.latitude} inputMode="decimal" placeholder="6.21"
              onChange={(e) => setForm({ ...form, latitude: e.target.value })}
              error={latInvalid || halfPoint} helperText={latInvalid ? 'Entre -90 y 90' : undefined}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField
              fullWidth label="Longitud" value={form.longitude} inputMode="decimal" placeholder="-77.50"
              onChange={(e) => setForm({ ...form, longitude: e.target.value })}
              error={lonInvalid || halfPoint} helperText={lonInvalid ? 'Entre -180 y 180' : undefined}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <Button fullWidth variant="outlined" startIcon={<MyLocationIcon />} onClick={locateWithGps} sx={{ minHeight: 56 }}>
              Usar mi GPS
            </Button>
          </Grid>
        </Grid>
        {gpsMessage && <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }} role="status">{gpsMessage}</Typography>}
        <TextField
          fullWidth label="Referencia del lugar" value={form.locationText} sx={{ mt: 2 }}
          onChange={(e) => setForm({ ...form, locationText: e.target.value })}
          helperText="Opcional si diste coordenadas. Ej.: muelle de Bahía Solano, frente a Punta Huina."
          slotProps={{ htmlInput: { maxLength: 200 } }}
        />
        {(hasPoint || interactiveMap) && (
          <Box sx={{ mt: 2 }}>
            <GeoMap
              title="Ubicación de la denuncia"
              zones={zones}
              track={hasPoint ? [{ id: 'report', latitude: latitude!, longitude: longitude! }] : []}
              height={interactiveMap ? 320 : 240}
              onPick={(lat, lon) => setForm((current) => ({ ...current, latitude: String(lat), longitude: String(lon) }))}
            />
          </Box>
        )}
      </Box>

      <Box>
        <Typography variant="subtitle2">Fotos (opcional, hasta 3)</Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
          Se les quita la ubicación y el modelo del teléfono antes de enviarlas.
        </Typography>
        <Stack direction="row" spacing={1.5} useFlexGap flexWrap="wrap">
          {previews.map((url, index) => (
            <Box key={url} sx={{ position: 'relative', width: 96, height: 96, borderRadius: 3, overflow: 'hidden', boxShadow: 1 }}>
              <img src={url} alt={`Foto ${index + 1} adjunta`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              <IconButton
                size="small" aria-label={`Quitar foto ${index + 1}`}
                onClick={() => setPhotos(photos.filter((_, i) => i !== index))}
                sx={{ position: 'absolute', top: 4, right: 4, bgcolor: 'rgba(7,30,48,.66)', color: '#fff', '&:hover': { bgcolor: 'rgba(7,30,48,.85)' } }}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>
          ))}
          {photos.length < MAX_PHOTOS && (
            <Button
              component="label" variant="outlined" startIcon={<AddPhotoAlternateIcon />}
              sx={{ width: 96, height: 96, borderStyle: 'dashed', flexDirection: 'column', gap: 0.5, '& .MuiButton-startIcon': { m: 0 } }}
            >
              Añadir
              <input hidden type="file" accept="image/jpeg,image/png" multiple onChange={(e) => { addPhotos(e.target.files); e.target.value = ''; }} />
            </Button>
          )}
        </Stack>
        {photoError && <Typography variant="caption" color="error" sx={{ display: 'block', mt: 0.75 }} role="alert">{photoError}</Typography>}
      </Box>

      <Box sx={{ p: 2, borderRadius: 3, bgcolor: 'rgba(11,59,92,.05)' }}>
        <FormControlLabel
          control={<Checkbox checked={effectiveAnonymous} disabled={!signedIn} onChange={(e) => setAnonymous(e.target.checked)} />}
          label={<Typography fontWeight={700}>Denuncia anónima</Typography>}
        />
        <Stack direction="row" spacing={1} alignItems="flex-start" sx={{ pl: 4 }}>
          <ShieldOutlinedIcon fontSize="small" sx={{ color: 'secondary.main', mt: 0.25 }} />
          <Typography variant="body2" color="text.secondary">
            {effectiveAnonymous
              ? 'No se guarda tu cuenta, tu correo ni tu dirección IP; ni siquiera la autoridad puede saber quién denunció.'
              : 'La denuncia quedará asociada a tu cuenta para que la autoridad pueda contactarte si lo necesita.'}
            {!signedIn && ' Sin sesión iniciada, toda denuncia es anónima.'}
          </Typography>
        </Stack>
      </Box>

      <Button type="submit" variant="contained" color="secondary" size="large" disabled={!canSubmit} sx={{ alignSelf: { sm: 'flex-start' } }}>
        {submitting ? 'Enviando…' : 'Radicar denuncia'}
      </Button>
    </Stack>
  );
}
