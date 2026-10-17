import CampaignOutlinedIcon from '@mui/icons-material/CampaignOutlined';
import PhotoLibraryOutlinedIcon from '@mui/icons-material/PhotoLibraryOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Stack, Tab, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tabs, TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { GeoMap } from '../../../shared/components/GeoMap';
import { PageHeader } from '../../../shared/components/PageHeader';
import { domainService, type ReportStatus, type ReportSummary } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { NEXT_STATUSES, REPORT_STATUS_COLOR, REPORT_STATUS_LABEL, REPORT_TYPE_LABEL } from '../reportLabels';

const date = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });
const dateTime = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

type Filter = 'ALL' | 'RECEIVED' | 'INVESTIGATING' | 'CLOSED';

function StatusBadge({ status }: { status: ReportStatus }) {
  const color = REPORT_STATUS_COLOR[status];
  return <Chip size="small" color={color === 'default' ? undefined : color} variant={color === 'default' ? 'outlined' : 'filled'} label={REPORT_STATUS_LABEL[status]} />;
}

function place(report: Pick<ReportSummary, 'latitude' | 'longitude' | 'locationText'>) {
  if (report.locationText) return report.locationText;
  if (report.latitude !== undefined && report.latitude !== null && report.longitude !== undefined && report.longitude !== null) {
    return `${Number(report.latitude).toFixed(4)}, ${Number(report.longitude).toFixed(4)}`;
  }
  return '—';
}

/** Fotos protegidas por token: se descargan como blob y se liberan al cerrar. */
function ReportPhotos({ reportId, photoIds }: { reportId: string; photoIds: string[] }) {
  const [urls, setUrls] = useState<string[]>([]);
  useEffect(() => {
    let cancelled = false;
    const created: string[] = [];
    void Promise.all(photoIds.map((photoId) => domainService.reportPhoto(reportId, photoId)))
      .then((blobs) => {
        if (cancelled) return;
        blobs.forEach((blob) => created.push(URL.createObjectURL(blob)));
        setUrls(created);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
      created.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [reportId, photoIds]);

  if (photoIds.length === 0) return <Typography variant="body2" color="text.secondary">Sin fotos adjuntas.</Typography>;
  return (
    <Stack direction="row" spacing={1.5} useFlexGap flexWrap="wrap">
      {photoIds.map((photoId, index) => (
        <Box key={photoId} component={urls[index] ? 'a' : 'div'} href={urls[index]} target="_blank" rel="noreferrer"
          sx={{ width: 120, height: 120, borderRadius: 3, overflow: 'hidden', bgcolor: 'rgba(11,59,92,.06)', display: 'grid', placeItems: 'center' }}>
          {urls[index]
            ? <img src={urls[index]} alt={`Evidencia ${index + 1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            : <CircularProgress size={20} />}
        </Box>
      ))}
    </Stack>
  );
}

function ReportDialog({ reportId, onClose }: { reportId: string; onClose: () => void }) {
  const queryClient = useQueryClient();
  const detail = useQuery({ queryKey: ['report', reportId], queryFn: () => domainService.reportDetail(reportId) });
  const zones = useQuery({ queryKey: ['zones'], queryFn: domainService.zones });
  const [next, setNext] = useState<ReportStatus | ''>('');
  const [note, setNote] = useState('');

  const change = useMutation({
    mutationFn: () => domainService.changeReportStatus(reportId, next as ReportStatus, note.trim()),
    onSuccess: async (updated) => {
      queryClient.setQueryData(['report', reportId], updated);
      setNext('');
      setNote('');
      await queryClient.invalidateQueries({ queryKey: ['reports'] });
    },
  });
  const changeError = change.error ? toAppError(change.error) : null;
  const report = detail.data;
  const options = report ? NEXT_STATUSES[report.status] : [];
  const hasPoint = report?.latitude != null && report?.longitude != null;

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="md" aria-labelledby="report-dialog-title">
      <DialogTitle id="report-dialog-title">
        {report ? (
          <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
            <Box component="span" sx={{ fontFamily: 'ui-monospace, Consolas, monospace' }}>{report.trackingCode}</Box>
            <StatusBadge status={report.status} />
          </Stack>
        ) : 'Denuncia'}
      </DialogTitle>
      <DialogContent dividers>
        {detail.isLoading && <CircularProgress size={28} />}
        {detail.isError && <Alert severity="error">No fue posible cargar la denuncia.</Alert>}
        {report && (
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 7 }}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="overline" color="text.secondary">{REPORT_TYPE_LABEL[report.reportType]}</Typography>
                  <Typography sx={{ whiteSpace: 'pre-wrap' }}>{report.description}</Typography>
                </Box>
                <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                  <Chip size="small" variant="outlined" label={`Hecho: ${date.format(new Date(`${report.occurredOn}T12:00:00`))}`} />
                  <Chip size="small" variant="outlined" label={`Radicada: ${dateTime.format(new Date(report.createdAt))}`} />
                  {report.anonymous
                    ? <Chip size="small" icon={<VisibilityOffOutlinedIcon />} label="Anónima · sin datos del denunciante" />
                    : <Chip size="small" color="info" label={`Denunciante: ${report.reporterName}`} />}
                </Stack>
                <Box>
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Evidencia</Typography>
                  <ReportPhotos reportId={report.id} photoIds={report.photoIds} />
                </Box>
                <Box>
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Historial de gestión</Typography>
                  {report.history.length === 0 && <Typography variant="body2" color="text.secondary">Aún sin gestión.</Typography>}
                  <Stack component="ol" spacing={1.25} sx={{ m: 0, pl: 2.5 }}>
                    {report.history.map((step) => (
                      <li key={`${step.changedAt}-${step.toStatus}`}>
                        <Typography variant="body2" fontWeight={700}>
                          {REPORT_STATUS_LABEL[step.toStatus]} · {step.changedBy}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">{dateTime.format(new Date(step.changedAt))}</Typography>
                        <Typography variant="body2">{step.note}</Typography>
                      </li>
                    ))}
                  </Stack>
                </Box>
              </Stack>
            </Grid>
            <Grid size={{ xs: 12, md: 5 }}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Ubicación</Typography>
              {report.locationText && <Typography variant="body2" sx={{ mb: 1 }}>{report.locationText}</Typography>}
              {hasPoint ? (
                <GeoMap
                  title="Ubicación de la denuncia"
                  zones={zones.data ?? []}
                  track={[{ id: report.id, latitude: Number(report.latitude), longitude: Number(report.longitude) }]}
                  height={260}
                />
              ) : (
                <Typography variant="body2" color="text.secondary">Sin coordenadas: solo referencia escrita.</Typography>
              )}
            </Grid>
          </Grid>
        )}
      </DialogContent>
      {report && options.length > 0 && (
        <Box sx={{ px: 3, pt: 2 }}>
          {changeError && <Alert severity="error" sx={{ mb: 2 }}>{changeError.message}</Alert>}
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField select fullWidth label="Mover a" value={next} onChange={(e) => setNext(e.target.value as ReportStatus)}>
                {options.map((status) => <MenuItem key={status} value={status}>{REPORT_STATUS_LABEL[status]}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 8 }}>
              <TextField
                fullWidth required multiline minRows={2} label={next.startsWith('CLOSED') ? 'Nota de resolución' : 'Nota de gestión'}
                value={note} onChange={(e) => setNote(e.target.value)}
                helperText="Obligatoria. Queda en el historial y en la auditoría."
                slotProps={{ htmlInput: { maxLength: 1000 } }}
              />
            </Grid>
          </Grid>
        </Box>
      )}
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Cerrar</Button>
        {report && options.length > 0 && (
          <Button variant="contained" onClick={() => change.mutate()} disabled={!next || note.trim().length < 5 || change.isPending}>
            {change.isPending ? 'Guardando…' : 'Registrar gestión'}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}

/** AC-10.4 · Bandeja de denuncias para INSPECTOR y ADMIN. */
export function ReportsQueuePage() {
  const reportsQuery = useQuery({ queryKey: ['reports'], queryFn: () => domainService.reports() });
  const [filter, setFilter] = useState<Filter>('ALL');
  const [openId, setOpenId] = useState<string | null>(null);
  const reports = reportsQuery.data ?? [];

  const count = (value: Filter) =>
    value === 'ALL' ? reports.length
      : value === 'CLOSED' ? reports.filter((r) => r.status.startsWith('CLOSED')).length
        : reports.filter((r) => r.status === value).length;
  const visible = reports.filter((report) =>
    filter === 'ALL' || (filter === 'CLOSED' ? report.status.startsWith('CLOSED') : report.status === filter));

  return (
    <>
      <PageHeader
        title="Denuncias"
        subtitle="Reportes ciudadanos de pesca ilegal. Gestiónalos de Recibida a En investigación y ciérralos con una nota de resolución."
      />

      <Tabs value={filter} onChange={(_, value) => setFilter(value)} sx={{ mb: 2 }} variant="scrollable" allowScrollButtonsMobile aria-label="Filtrar por estado">
        <Tab value="ALL" label={`Todas (${count('ALL')})`} />
        <Tab value="RECEIVED" label={`Recibidas (${count('RECEIVED')})`} />
        <Tab value="INVESTIGATING" label={`En investigación (${count('INVESTIGATING')})`} />
        <Tab value="CLOSED" label={`Cerradas (${count('CLOSED')})`} />
      </Tabs>

      {reportsQuery.isLoading && <CircularProgress size={28} />}
      {reportsQuery.isError && <Alert severity="error">No fue posible cargar las denuncias.</Alert>}

      {!reportsQuery.isLoading && !reportsQuery.isError && (
        visible.length === 0 ? (
          <EmptyState
            icon={<CampaignOutlinedIcon sx={{ fontSize: 48, color: 'secondary.light' }} />}
            title="Sin denuncias en esta bandeja"
            description="Cuando un ciudadano o un pescador reporte pesca ilegal, aparecerá aquí."
          />
        ) : (
          <Card>
            <CardContent>
              <TableContainer>
                <Table aria-label="Bandeja de denuncias">
                  <TableHead>
                    <TableRow>
                      <TableCell>Código</TableCell>
                      <TableCell>Tipo</TableCell>
                      <TableCell>Hecho</TableCell>
                      <TableCell>Lugar</TableCell>
                      <TableCell align="center">Fotos</TableCell>
                      <TableCell>Estado</TableCell>
                      <TableCell align="right">Acción</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {visible.map((report) => (
                      <TableRow key={report.id} hover>
                        <TableCell sx={{ fontFamily: 'ui-monospace, Consolas, monospace', whiteSpace: 'nowrap' }}>
                          {report.trackingCode}
                          {report.anonymous && (
                            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', fontFamily: 'inherit' }}>anónima</Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Typography fontWeight={700} variant="body2">{REPORT_TYPE_LABEL[report.reportType]}</Typography>
                          <Typography variant="caption" color="text.secondary" sx={{ display: '-webkit-box', WebkitLineClamp: 1, WebkitBoxOrient: 'vertical', overflow: 'hidden', maxWidth: 320 }}>
                            {report.description}
                          </Typography>
                        </TableCell>
                        <TableCell sx={{ whiteSpace: 'nowrap' }}>{date.format(new Date(`${report.occurredOn}T12:00:00`))}</TableCell>
                        <TableCell>{place(report)}</TableCell>
                        <TableCell align="center">
                          {report.photoCount > 0
                            ? <Chip size="small" variant="outlined" icon={<PhotoLibraryOutlinedIcon />} label={report.photoCount} />
                            : '—'}
                        </TableCell>
                        <TableCell><StatusBadge status={report.status} /></TableCell>
                        <TableCell align="right">
                          <Button size="small" onClick={() => setOpenId(report.id)}>
                            {report.status.startsWith('CLOSED') ? 'Ver' : 'Gestionar'}
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        )
      )}

      {openId && <ReportDialog reportId={openId} onClose={() => setOpenId(null)} />}
    </>
  );
}
