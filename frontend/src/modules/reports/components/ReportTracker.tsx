import CloudOffIcon from '@mui/icons-material/CloudOff';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import SearchIcon from '@mui/icons-material/Search';
import {
  Alert, Box, Button, Chip, IconButton, Stack, Step, StepLabel, Stepper, TextField, Tooltip, Typography,
} from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useState } from 'react';
import { domainService, type ReportTracking } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import {
  forgetReceipt, OUTBOX_EVENT, queuedReports, removeQueuedReport, savedReceipts, type QueuedReport,
} from '../../../shared/storage/reportOutbox';
import { REPORT_STATUS_COLOR, REPORT_STATUS_LABEL, REPORT_TYPE_LABEL } from '../reportLabels';

const date = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });
const dateTime = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

function StatusTimeline({ report }: { report: ReportTracking }) {
  const closed = report.status === 'CLOSED_FOUNDED' || report.status === 'CLOSED_UNFOUNDED';
  const active = report.status === 'RECEIVED' ? 0 : report.status === 'INVESTIGATING' ? 1 : 3;
  return (
    <Stepper activeStep={active} alternativeLabel sx={{ my: 1 }} aria-label="Avance de la denuncia">
      <Step completed={active > 0}><StepLabel>Recibida</StepLabel></Step>
      <Step completed={active > 1}><StepLabel>En investigación</StepLabel></Step>
      <Step completed={closed}>
        <StepLabel>{closed ? REPORT_STATUS_LABEL[report.status] : 'Cerrada'}</StepLabel>
      </Step>
    </Stepper>
  );
}

/** AC-10.3 · Consulta del estado por código, más lo que este dispositivo recuerda. */
export function ReportTracker({ signedIn }: { signedIn: boolean }) {
  const [code, setCode] = useState('');
  const [receipts, setReceipts] = useState(savedReceipts);
  const [queued, setQueued] = useState<QueuedReport[]>([]);

  const refresh = useCallback(() => {
    setReceipts(savedReceipts());
    void queuedReports().then(setQueued);
  }, []);
  useEffect(() => {
    void queuedReports().then(setQueued);
    window.addEventListener(OUTBOX_EVENT, refresh);
    return () => window.removeEventListener(OUTBOX_EVENT, refresh);
  }, [refresh]);

  const lookup = useMutation({ mutationFn: (value: string) => domainService.trackReport(value) });
  const mine = useQuery({ queryKey: ['my-reports'], queryFn: domainService.myReports, enabled: signedIn });
  const lookupError = lookup.error ? toAppError(lookup.error) : null;

  const search = (value: string) => {
    setCode(value);
    if (value.trim()) lookup.mutate(value.trim());
  };

  const known = [
    ...receipts.map((receipt) => receipt.trackingCode),
    ...(mine.data ?? []).map((report) => report.trackingCode),
  ].filter((value, index, all) => all.indexOf(value) === index);

  return (
    <Stack spacing={3}>
      <Stack component="form" direction={{ xs: 'column', sm: 'row' }} spacing={1.5} onSubmit={(e) => { e.preventDefault(); search(code); }}>
        <TextField
          fullWidth label="Código de seguimiento" placeholder="DEN-XXXXXXXX" value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          slotProps={{ htmlInput: { style: { fontFamily: 'ui-monospace, Consolas, monospace', letterSpacing: '0.04em' }, maxLength: 20 } }}
        />
        <Button type="submit" variant="contained" startIcon={<SearchIcon />} disabled={!code.trim() || lookup.isPending} sx={{ minWidth: 140 }}>
          Consultar
        </Button>
      </Stack>

      {lookupError && (
        <Alert severity={lookupError.status === 404 ? 'warning' : 'error'} role="alert">
          {lookupError.status === 404 ? 'No existe una denuncia con ese código. Revisa que esté bien escrito.' : lookupError.message}
        </Alert>
      )}

      {lookup.data && (
        <Box sx={{ p: { xs: 2, sm: 3 }, borderRadius: 4, bgcolor: 'rgba(255,255,255,.6)', border: '1px solid', borderColor: 'divider' }} aria-live="polite">
          <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={1} sx={{ mb: 1 }}>
            <Box>
              <Typography sx={{ fontFamily: 'ui-monospace, Consolas, monospace', fontWeight: 700 }}>{lookup.data.trackingCode}</Typography>
              <Typography variant="body2" color="text.secondary">
                {REPORT_TYPE_LABEL[lookup.data.reportType]} · hecho del {date.format(new Date(`${lookup.data.occurredOn}T12:00:00`))}
              </Typography>
            </Box>
            <Chip color={REPORT_STATUS_COLOR[lookup.data.status] === 'default' ? undefined : REPORT_STATUS_COLOR[lookup.data.status]} label={REPORT_STATUS_LABEL[lookup.data.status]} sx={{ alignSelf: 'flex-start' }} />
          </Stack>
          <StatusTimeline report={lookup.data} />
          <Typography variant="caption" color="text.secondary">
            Radicada el {dateTime.format(new Date(lookup.data.createdAt))} · última actualización {dateTime.format(new Date(lookup.data.updatedAt))}.
            Por reserva de la investigación solo se muestra el estado.
          </Typography>
        </Box>
      )}

      {queued.length > 0 && (
        <Box>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Pendientes de envío en este dispositivo</Typography>
          <Stack spacing={1}>
            {queued.map((item) => (
              <Stack
                key={item.clientId} direction="row" spacing={1.5} alignItems="center"
                sx={{ p: 1.5, borderRadius: 3, bgcolor: item.rejection ? 'rgba(178,34,34,.06)' : 'rgba(168,90,0,.07)' }}
              >
                <CloudOffIcon fontSize="small" sx={{ color: item.rejection ? 'error.main' : 'warning.main' }} />
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography variant="body2" fontWeight={700}>{REPORT_TYPE_LABEL[item.payload.reportType]}</Typography>
                  <Typography variant="caption" color={item.rejection ? 'error' : 'text.secondary'}>
                    {item.rejection
                      ? `No se pudo radicar: ${item.rejection}`
                      : `Guardada el ${dateTime.format(new Date(item.queuedAt))}; se enviará al recuperar la señal.`}
                  </Typography>
                </Box>
                {item.rejection && (
                  <Tooltip title="Descartar">
                    <IconButton aria-label="Descartar denuncia pendiente" onClick={() => void removeQueuedReport(item.clientId)}>
                      <DeleteOutlineIcon />
                    </IconButton>
                  </Tooltip>
                )}
              </Stack>
            ))}
          </Stack>
        </Box>
      )}

      {known.length > 0 && (
        <Box>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Tus denuncias</Typography>
          <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
            {known.map((value) => (
              <Chip
                key={value}
                label={value}
                onClick={() => search(value)}
                onDelete={receipts.some((receipt) => receipt.trackingCode === value) ? () => forgetReceipt(value) : undefined}
                sx={{ fontFamily: 'ui-monospace, Consolas, monospace' }}
              />
            ))}
          </Stack>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
            Los códigos de denuncias anónimas solo se guardan en este dispositivo. Quitarlos de la lista no borra la denuncia.
          </Typography>
        </Box>
      )}
    </Stack>
  );
}
