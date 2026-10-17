import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DownloadIcon from '@mui/icons-material/Download';
import HourglassTopIcon from '@mui/icons-material/HourglassTop';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import TableViewOutlinedIcon from '@mui/icons-material/TableViewOutlined';
import {
  Alert, Box, Button, Card, CardActionArea, CardContent, Chip, CircularProgress, Grid, IconButton, MenuItem, Stack,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { PageHeader } from '../../../shared/components/PageHeader';
import {
  domainService, type ExportFormat, type ExportJob, type IndicatorFilters, type OfficialReportKind, type Region,
} from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { saveBlob } from '../../../shared/files/saveBlob';

const DESCRIPTION: Record<OfficialReportKind, string> = {
  LANDINGS: 'Kilos validados por especie, zona y región.',
  QUOTA_COMPLIANCE: 'Asignado, consumido y estado de cada cuota.',
  PERMITS: 'Situación actual de los permisos, sin datos de titulares.',
  ILLEGAL_REPORTS: 'Denuncias radicadas, sin datos del denunciante.',
  ZONE_EVENTS: 'Ingresos a vedas y Áreas Marinas Protegidas.',
};

const JOB_STATUS: Record<ExportJob['status'], { label: string; color: 'default' | 'info' | 'success' | 'error' }> = {
  PENDING: { label: 'En cola', color: 'default' },
  RUNNING: { label: 'Generando', color: 'info' },
  DONE: { label: 'Listo', color: 'success' },
  FAILED: { label: 'Falló', color: 'error' },
};

const dateTime = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

function isoDay(offsetYears = 0) {
  const date = new Date();
  if (offsetYears) {
    date.setFullYear(date.getFullYear() + offsetYears);
    date.setDate(date.getDate() + 1);
  }
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

/** RF-18 · Reportes oficiales para inspectores, analistas y administradores. */
export function OfficialReportsPage() {
  const queryClient = useQueryClient();
  const catalog = useQuery({ queryKey: ['official-catalog'], queryFn: domainService.officialCatalog });
  const species = useQuery({ queryKey: ['species'], queryFn: domainService.species });
  const [kind, setKind] = useState<OfficialReportKind>('LANDINGS');
  const [filters, setFilters] = useState<IndicatorFilters>({ from: isoDay(-1), to: isoDay(), region: '', speciesId: '' });
  const [notice, setNotice] = useState<string | null>(null);
  const entry = catalog.data?.find((item) => item.kind === kind);
  const valid = !filters.from || !filters.to || filters.from <= filters.to;

  const preview = useQuery({
    queryKey: ['official-preview', kind, filters],
    queryFn: () => domainService.officialPreview(kind, filters),
    enabled: valid,
  });
  const jobs = useQuery({
    queryKey: ['export-jobs'],
    queryFn: domainService.exportJobs,
    // Mientras haya trabajos en curso se consulta su estado; la alerta del buzón también avisa.
    refetchInterval: (query) => (query.state.data?.some((job) => job.status === 'PENDING' || job.status === 'RUNNING') ? 5000 : false),
  });

  const exportMutation = useMutation({
    mutationFn: (format: ExportFormat) => domainService.officialExport(kind, format, filters),
    onSuccess: async (outcome) => {
      if (outcome.kind === 'file') {
        saveBlob(outcome.blob, outcome.filename);
        setNotice(null);
      } else {
        setNotice('El periodo supera un año: el reporte se está generando en segundo plano. Te avisaremos en Notificaciones cuando esté listo.');
        await queryClient.invalidateQueries({ queryKey: ['export-jobs'] });
      }
    },
  });
  const downloadJob = useMutation({
    mutationFn: domainService.exportJobFile,
    onSuccess: ({ blob, filename }) => saveBlob(blob, filename),
  });
  const error = [exportMutation.error, downloadJob.error, preview.error].find(Boolean);
  const table = preview.data?.table;

  return (
    <>
      <PageHeader
        title="Reportes oficiales"
        subtitle="Genera y exporta reportes de la actividad pesquera con datos validados, listos para entregar a la autoridad."
      />

      <Grid container spacing={1.5} sx={{ mb: 2.5 }} role="radiogroup" aria-label="Reporte">
        {(catalog.data ?? []).map((item) => {
          const selected = item.kind === kind;
          return (
            <Grid key={item.kind} size={{ xs: 12, sm: 6, lg: 2.4 }}>
              <Card sx={{ height: '100%', outline: selected ? '2px solid' : 'none', outlineColor: 'secondary.main' }}>
                <CardActionArea role="radio" aria-checked={selected} onClick={() => setKind(item.kind)} sx={{ height: '100%' }}>
                  <CardContent>
                    <Typography fontWeight={800} sx={{ mb: 0.5 }}>{item.title}</Typography>
                    <Typography variant="body2" color="text.secondary">{DESCRIPTION[item.kind]}</Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          );
        })}
      </Grid>

      <Card sx={{ mb: 2.5 }}>
        <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
          <Grid container spacing={1.5} alignItems="center">
            <Grid size={{ xs: 6, md: 3, xl: 2.5 }}>
              <TextField fullWidth size="small" type="date" label="Desde" value={filters.from} disabled={kind === 'PERMITS'}
                onChange={(e) => setFilters({ ...filters, from: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 6, md: 3, xl: 2.5 }}>
              <TextField fullWidth size="small" type="date" label="Hasta" value={filters.to} disabled={kind === 'PERMITS'}
                onChange={(e) => setFilters({ ...filters, to: e.target.value })} slotProps={{ inputLabel: { shrink: true } }} />
            </Grid>
            <Grid size={{ xs: 6, md: 3, xl: 2 }}>
              <TextField select fullWidth size="small" label="Región" value={filters.region ?? ''} disabled={!entry?.regionFilter}
                onChange={(e) => setFilters({ ...filters, region: e.target.value as Region | '' })}>
                <MenuItem value="">Todas</MenuItem>
                <MenuItem value="PACIFICO">Pacífico</MenuItem>
                <MenuItem value="CARIBE">Caribe</MenuItem>
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, md: 3, xl: 2 }}>
              <TextField select fullWidth size="small" label="Especie" value={filters.speciesId ?? ''} disabled={!entry?.speciesFilter}
                onChange={(e) => setFilters({ ...filters, speciesId: e.target.value })}>
                <MenuItem value="">Todas</MenuItem>
                {(species.data ?? []).map((item) => <MenuItem key={item.id} value={item.id}>{item.commonName}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, xl: 3 }}>
              <Stack direction="row" spacing={1} justifyContent={{ xs: 'flex-end' }}>
                <Button variant="contained" startIcon={<TableViewOutlinedIcon />} disabled={!valid || exportMutation.isPending}
                  onClick={() => exportMutation.mutate('CSV')}>CSV</Button>
                <Button variant="outlined" startIcon={<PictureAsPdfOutlinedIcon />} disabled={!valid || exportMutation.isPending}
                  onClick={() => exportMutation.mutate('PDF')}>PDF</Button>
              </Stack>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {!valid && <Alert severity="warning" sx={{ mb: 2 }}>La fecha inicial no puede ser posterior a la final.</Alert>}
      {error && <Alert severity="error" sx={{ mb: 2 }}>{toAppError(error).message}</Alert>}
      {notice && <Alert severity="info" icon={<HourglassTopIcon />} sx={{ mb: 2 }} onClose={() => setNotice(null)}>{notice}</Alert>}
      {preview.data?.background && !notice && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Este periodo supera un año: la exportación se generará en segundo plano y te avisaremos cuando esté lista.
        </Alert>
      )}

      <Card sx={{ mb: 2.5 }}>
        <CardContent>
          {preview.isLoading && <CircularProgress size={28} />}
          {table && (
            <>
              <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={1} sx={{ mb: 1.5 }}>
                <Box>
                  <Typography variant="overline" color="text.secondary">SIGEPES · Vista previa</Typography>
                  <Typography variant="h6" component="h2">{table.title}</Typography>
                  <Typography variant="caption" color="text.secondary">{table.filters.join(' · ')}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ alignSelf: { sm: 'flex-end' } }}>
                  {preview.data?.totalRows} fila(s){preview.data?.truncated ? ' · se muestran las primeras 200' : ''}
                </Typography>
              </Stack>
              <TableContainer sx={{ maxHeight: 440, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                <Table size="small" stickyHeader aria-label={table.title}>
                  <TableHead>
                    <TableRow>{table.columns.map((column) => <TableCell key={column}>{column}</TableCell>)}</TableRow>
                  </TableHead>
                  <TableBody>
                    {table.rows.length === 0 && (
                      <TableRow><TableCell colSpan={table.columns.length}>Sin registros para los filtros elegidos.</TableCell></TableRow>
                    )}
                    {table.rows.map((row, index) => (
                      <TableRow key={index} hover>
                        {row.map((cell, cellIndex) => (
                          <TableCell key={cellIndex} align={/^-?\d+(\.\d+)?$/.test(cell) ? 'right' : 'left'} sx={{ fontVariantNumeric: 'tabular-nums' }}>
                            {cell}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <Stack spacing={0.5} sx={{ mt: 1.5 }}>
                {table.footnotes.map((note) => (
                  <Typography key={note} variant="caption" color="text.secondary">Nota: {note}</Typography>
                ))}
              </Stack>
            </>
          )}
        </CardContent>
      </Card>

      {(jobs.data ?? []).length > 0 && (
        <Card>
          <CardContent>
            <Typography variant="h6" component="h2" sx={{ fontSize: '1.05rem', mb: 1 }}>Exportaciones en segundo plano</Typography>
            <Stack component="ul" spacing={1} sx={{ listStyle: 'none', p: 0, m: 0 }}>
              {(jobs.data ?? []).map((job) => (
                <Stack component="li" key={job.id} direction="row" spacing={1.5} alignItems="center"
                  sx={{ p: 1.25, borderRadius: 2, bgcolor: 'rgba(11,59,92,.04)' }}>
                  <DescriptionOutlinedIcon sx={{ color: 'primary.main' }} />
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="body2" fontWeight={700}>{job.title} · {job.format}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {job.from} a {job.to} · pedido {dateTime.format(new Date(job.createdAt))}
                      {job.rowCount != null ? ` · ${job.rowCount} fila(s)` : ''}{job.error ? ` · ${job.error}` : ''}
                    </Typography>
                  </Box>
                  <Chip size="small" label={JOB_STATUS[job.status].label}
                    color={JOB_STATUS[job.status].color === 'default' ? undefined : JOB_STATUS[job.status].color} />
                  {job.status === 'DONE' && (
                    <Tooltip title="Descargar">
                      <IconButton aria-label={`Descargar ${job.title} ${job.format}`} onClick={() => downloadJob.mutate(job)}>
                        <DownloadIcon />
                      </IconButton>
                    </Tooltip>
                  )}
                </Stack>
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}
    </>
  );
}
