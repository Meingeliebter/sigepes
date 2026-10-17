import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DownloadIcon from '@mui/icons-material/Download';
import FolderOpenOutlinedIcon from '@mui/icons-material/FolderOpenOutlined';
import HistoryIcon from '@mui/icons-material/History';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  Divider, Grid, IconButton, List, ListItem, ListItemButton, ListItemIcon, ListItemText, MenuItem, Stack, TextField,
  Tooltip, Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import {
  domainService, type DocEntityType, type DocExpiryStatus, type DocType, type DocumentRecord,
} from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';

const MAX_BYTES = 5 * 1024 * 1024;
const ACCEPTED = ['application/pdf', 'image/jpeg', 'image/png'];

const DOC_TYPE_LABEL: Record<DocType, string> = {
  ID_DOCUMENT: 'Documento de identidad',
  VESSEL_CERTIFICATE: 'Certificado de embarcación',
  PERMIT: 'Permiso',
  RESOLUTION: 'Resolución',
  OTHER: 'Otro',
};
const ENTITY_LABEL: Record<DocEntityType, string> = { USER: 'Titular', VESSEL: 'Embarcación', PERMIT: 'Permiso', CLOSURE: 'Veda' };
const STATUS: Record<DocExpiryStatus, { label: string; color: 'success' | 'warning' | 'error' | 'default' }> = {
  VALID: { label: 'Vigente', color: 'success' },
  EXPIRING: { label: 'Por vencer', color: 'warning' },
  EXPIRED: { label: 'Vencido', color: 'error' },
  NO_EXPIRY: { label: 'Sin vencimiento', color: 'default' },
};

const day = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });
const formatDay = (iso: string) => day.format(new Date(`${iso}T12:00:00`));
const size = (bytes: number) => (bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`);

function StatusChip({ status }: { status: DocExpiryStatus }) {
  const config = STATUS[status];
  return <Chip size="small" label={config.label} color={config.color === 'default' ? undefined : config.color} variant={config.color === 'default' ? 'outlined' : 'filled'} />;
}

/** Validación previa en el navegador; el servidor vuelve a revisar el contenido real del archivo. */
function fileProblem(file: File | null): string | null {
  if (!file) return null;
  if (!ACCEPTED.includes(file.type)) return 'Solo se admiten archivos PDF, JPG o PNG.';
  if (file.size > MAX_BYTES) return 'El archivo supera el máximo de 5 MB.';
  return null;
}

function FilePicker({ file, onChange }: { file: File | null; onChange: (file: File | null) => void }) {
  const problem = fileProblem(file);
  return (
    <Box>
      <Button component="label" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
        {file ? 'Cambiar archivo' : 'Elegir archivo'}
        <input hidden type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
          onChange={(e) => { onChange(e.target.files?.[0] ?? null); e.target.value = ''; }} />
      </Button>
      <Typography variant="caption" color={problem ? 'error' : 'text.secondary'} sx={{ display: 'block', mt: 0.75 }} role={problem ? 'alert' : undefined}>
        {problem ?? (file ? `${file.name} · ${size(file.size)}` : 'PDF, JPG o PNG, hasta 5 MB.')}
      </Typography>
    </Box>
  );
}

const EMPTY_FORM = { docType: 'VESSEL_CERTIFICATE' as DocType, title: '', entityType: 'VESSEL' as DocEntityType, entityId: '', expiresOn: '', note: '' };

function UploadDialog({ onClose, onDone }: { onClose: () => void; onDone: (doc: DocumentRecord) => void }) {
  const user = useAuthStore((s) => s.user);
  const authority = isAuthority(user?.role);
  const [form, setForm] = useState(EMPTY_FORM);
  const [file, setFile] = useState<File | null>(null);
  const vessels = useQuery({ queryKey: ['vessels'], queryFn: domainService.vessels });
  const permits = useQuery({ queryKey: ['permits'], queryFn: domainService.permits });
  const closures = useQuery({ queryKey: ['closures'], queryFn: domainService.closures, enabled: authority });

  const options: { id: string; label: string }[] =
    form.entityType === 'USER' ? (user ? [{ id: user.id, label: user.fullName }] : [])
      : form.entityType === 'VESSEL' ? (vessels.data ?? [])
        .filter((vessel) => authority || vessel.ownerId === user?.id)
        .map((vessel) => ({ id: vessel.id, label: `${vessel.name} (${vessel.registration})` }))
        : form.entityType === 'PERMIT' ? (permits.data ?? []).map((permit) => ({ id: permit.id, label: `Permiso ${permit.number}` }))
          : (closures.data ?? []).map((closure) => ({ id: closure.id, label: `${closure.species} · ${closure.zone} · ${closure.resolutionNumber}` }));

  const upload = useMutation({
    mutationFn: () => domainService.uploadDocument({
      docType: form.docType, title: form.title.trim(), entityType: form.entityType, entityId: form.entityId,
      expiresOn: form.expiresOn || undefined, note: form.note.trim() || undefined,
    }, file!),
    onSuccess: onDone,
  });
  const error = upload.error ? toAppError(upload.error) : null;
  const ready = !!form.title.trim() && !!form.entityId && !!file && !fileProblem(file);
  const entityTypes: DocEntityType[] = authority ? ['USER', 'VESSEL', 'PERMIT', 'CLOSURE'] : ['USER', 'VESSEL', 'PERMIT'];

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm" aria-labelledby="upload-title">
      <DialogTitle id="upload-title">Cargar documento</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2.25}>
          {error && <Alert severity="error">{error.message}</Alert>}
          <TextField select label="Tipo de documento" value={form.docType} onChange={(e) => setForm({ ...form, docType: e.target.value as DocType })}>
            {(Object.keys(DOC_TYPE_LABEL) as DocType[]).map((type) => <MenuItem key={type} value={type}>{DOC_TYPE_LABEL[type]}</MenuItem>)}
          </TextField>
          <TextField label="Título" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
            placeholder="Ej.: Certificado de navegabilidad 2026" slotProps={{ htmlInput: { maxLength: 160 } }} />
          <Grid container spacing={1.5}>
            <Grid size={{ xs: 12, sm: 5 }}>
              <TextField select fullWidth label="Pertenece a" value={form.entityType}
                onChange={(e) => setForm({ ...form, entityType: e.target.value as DocEntityType, entityId: '' })}>
                {entityTypes.map((type) => <MenuItem key={type} value={type}>{ENTITY_LABEL[type]}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 7 }}>
              <TextField select fullWidth label="Registro" value={form.entityId}
                onChange={(e) => setForm({ ...form, entityId: e.target.value })}
                helperText={options.length === 0 ? 'No hay registros disponibles de este tipo.' : ' '}>
                {options.map((option) => <MenuItem key={option.id} value={option.id}>{option.label}</MenuItem>)}
              </TextField>
            </Grid>
          </Grid>
          <TextField type="date" label="Vence el (opcional)" value={form.expiresOn}
            onChange={(e) => setForm({ ...form, expiresOn: e.target.value })} slotProps={{ inputLabel: { shrink: true } }}
            helperText="Te avisaremos 30, 15 y 5 días antes." />
          <TextField label="Nota (opcional)" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })}
            slotProps={{ htmlInput: { maxLength: 300 } }} />
          <FilePicker file={file} onChange={setFile} />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={() => upload.mutate()} disabled={!ready || upload.isPending}>
          {upload.isPending ? 'Cargando…' : 'Cargar'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/** Detalle con historial de versiones (RN-30) y descarga por enlace firmado (AC-15.5). */
function DocumentDialog({ id, onClose }: { id: string; onClose: () => void }) {
  const queryClient = useQueryClient();
  const detail = useQuery({ queryKey: ['document', id], queryFn: () => domainService.document(id) });
  const [file, setFile] = useState<File | null>(null);
  const [expiresOn, setExpiresOn] = useState('');
  const [note, setNote] = useState('');
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const addVersion = useMutation({
    mutationFn: () => domainService.uploadDocumentVersion(id, { expiresOn: expiresOn || undefined, note: note.trim() || undefined }, file!),
    onSuccess: async (updated) => {
      queryClient.setQueryData(['document', id], updated);
      setFile(null);
      setExpiresOn('');
      setNote('');
      await queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });
  const download = async (version: number) => {
    setDownloadError(null);
    try {
      window.location.assign(await domainService.documentDownloadUrl(id, version));
    } catch (error) {
      setDownloadError(toAppError(error).message);
    }
  };
  const doc = detail.data;
  const versionError = addVersion.error ? toAppError(addVersion.error) : null;

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm" aria-labelledby="doc-title">
      <DialogTitle id="doc-title">{doc?.title ?? 'Documento'}</DialogTitle>
      <DialogContent dividers>
        {detail.isLoading && <CircularProgress size={28} />}
        {doc && (
          <Stack spacing={2}>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Chip size="small" variant="outlined" label={DOC_TYPE_LABEL[doc.docType]} />
              <StatusChip status={doc.expiryStatus} />
              {doc.expiresOn && <Chip size="small" variant="outlined" label={`Vence ${formatDay(doc.expiresOn)}`} />}
            </Stack>
            <Typography variant="body2" color="text.secondary">{doc.entityLabel} · titular {doc.ownerName}</Typography>
            {downloadError && <Alert severity="error">{downloadError}</Alert>}

            <Box>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Versiones</Typography>
              <List dense disablePadding aria-label="Historial de versiones">
                {doc.versions.map((version) => (
                  <ListItem
                    key={version.version}
                    disableGutters
                    secondaryAction={
                      <Tooltip title={`Descargar versión ${version.version}`}>
                        <IconButton edge="end" aria-label={`Descargar versión ${version.version}`} onClick={() => void download(version.version)}>
                          <DownloadIcon />
                        </IconButton>
                      </Tooltip>
                    }
                  >
                    <ListItemIcon sx={{ minWidth: 36 }}>
                      {version.contentType === 'application/pdf' ? <DescriptionOutlinedIcon /> : <ImageOutlinedIcon />}
                    </ListItemIcon>
                    <ListItemText
                      primary={`v${version.version}${version.version === doc.currentVersion ? ' · vigente' : ''} · ${version.originalFilename}`}
                      secondary={`${day.format(new Date(version.uploadedAt))} · ${size(version.sizeBytes)}${version.expiresOn ? ` · vence ${formatDay(version.expiresOn)}` : ''}${version.note ? ` · ${version.note}` : ''}`}
                      slotProps={{ primary: { fontWeight: version.version === doc.currentVersion ? 700 : 500, noWrap: true } }}
                    />
                  </ListItem>
                ))}
              </List>
              <Typography variant="caption" color="text.secondary">
                Las versiones anteriores nunca se reemplazan. Cada descarga usa un enlace que caduca en 5 minutos y queda registrada.
              </Typography>
            </Box>

            <Divider />
            <Typography variant="subtitle2">Cargar nueva versión</Typography>
            {versionError && <Alert severity="error">{versionError.message}</Alert>}
            <Grid container spacing={1.5}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField fullWidth size="small" type="date" label="Vence el (opcional)" value={expiresOn}
                  onChange={(e) => setExpiresOn(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField fullWidth size="small" label="Nota" value={note} onChange={(e) => setNote(e.target.value)}
                  placeholder="Ej.: renovación 2027" slotProps={{ htmlInput: { maxLength: 300 } }} />
              </Grid>
            </Grid>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'flex-start' }} justifyContent="space-between">
              <FilePicker file={file} onChange={setFile} />
              <Button variant="contained" startIcon={<HistoryIcon />} onClick={() => addVersion.mutate()}
                disabled={!file || !!fileProblem(file) || addVersion.isPending}>
                {addVersion.isPending ? 'Cargando…' : 'Agregar versión'}
              </Button>
            </Stack>
          </Stack>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button variant="contained" onClick={onClose}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}

/** RF-15 · Gestión documental versionada. */
export function DocumentsPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<{ tipo: DocType | ''; entidad: DocEntityType | ''; estado: DocExpiryStatus | '' }>({ tipo: '', entidad: '', estado: '' });
  const query = useQuery({ queryKey: ['documents', filters], queryFn: () => domainService.documents(filters) });
  const [uploading, setUploading] = useState(false);
  const [openId, setOpenId] = useState<string | null>(null);
  const documents = query.data ?? [];
  const filtered = !!(filters.tipo || filters.entidad || filters.estado);

  return (
    <>
      <PageHeader
        title="Documentos"
        subtitle="Cédula, certificados de embarcación, permisos y resoluciones, con su historial de versiones y aviso antes de vencer."
        actionLabel="Cargar documento"
        actionIcon={<CloudUploadOutlinedIcon />}
        onAction={() => setUploading(true)}
      />

      <Card sx={{ mb: 2.5 }}>
        <CardContent sx={{ py: 2, '&:last-child': { pb: 2 } }}>
          <Grid container spacing={1.5} aria-label="Filtros de documentos">
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField select fullWidth size="small" label="Tipo" value={filters.tipo} onChange={(e) => setFilters({ ...filters, tipo: e.target.value as DocType | '' })}>
                <MenuItem value="">Todos</MenuItem>
                {(Object.keys(DOC_TYPE_LABEL) as DocType[]).map((type) => <MenuItem key={type} value={type}>{DOC_TYPE_LABEL[type]}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, sm: 4 }}>
              <TextField select fullWidth size="small" label="Pertenece a" value={filters.entidad} onChange={(e) => setFilters({ ...filters, entidad: e.target.value as DocEntityType | '' })}>
                <MenuItem value="">Todos</MenuItem>
                {(Object.keys(ENTITY_LABEL) as DocEntityType[]).map((type) => <MenuItem key={type} value={type}>{ENTITY_LABEL[type]}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, sm: 4 }}>
              <TextField select fullWidth size="small" label="Estado" value={filters.estado} onChange={(e) => setFilters({ ...filters, estado: e.target.value as DocExpiryStatus | '' })}>
                <MenuItem value="">Todos</MenuItem>
                {(Object.keys(STATUS) as DocExpiryStatus[]).map((status) => <MenuItem key={status} value={status}>{STATUS[status].label}</MenuItem>)}
              </TextField>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {query.isLoading && <CircularProgress size={28} />}
      {query.isError && <Alert severity="error">No fue posible cargar los documentos.</Alert>}
      {query.isSuccess && documents.length === 0 && (
        <EmptyState
          icon={<FolderOpenOutlinedIcon sx={{ fontSize: 48, color: 'secondary.light' }} />}
          title={filtered ? 'Ningún documento con esos filtros' : 'Aún no hay documentos'}
          description={filtered ? 'Prueba con otros filtros.' : 'Carga tu cédula, el certificado de tu embarcación o tu permiso para tenerlos siempre a mano.'}
          actionLabel={filtered ? undefined : 'Cargar el primero'}
          onAction={filtered ? undefined : () => setUploading(true)}
        />
      )}

      {documents.length > 0 && (
        <Card>
          <List disablePadding aria-label="Documentos">
            {documents.map((doc, index) => (
              <ListItem key={doc.id} disablePadding divider={index < documents.length - 1}>
                <ListItemButton onClick={() => setOpenId(doc.id)} sx={{ py: 1.5 }}>
                  <ListItemIcon><DescriptionOutlinedIcon sx={{ color: 'primary.main' }} /></ListItemIcon>
                  <ListItemText
                    disableTypography
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                        <Typography fontWeight={700}>{doc.title}</Typography>
                        <StatusChip status={doc.expiryStatus} />
                      </Stack>
                    }
                    secondary={
                      <Typography variant="body2" color="text.secondary">
                        {DOC_TYPE_LABEL[doc.docType]} · {doc.entityLabel} · v{doc.currentVersion}
                        {doc.expiresOn ? ` · vence ${formatDay(doc.expiresOn)}` : ''}
                      </Typography>
                    }
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Card>
      )}

      {uploading && (
        <UploadDialog
          onClose={() => setUploading(false)}
          onDone={async (doc) => {
            setUploading(false);
            setOpenId(doc.id);
            await queryClient.invalidateQueries({ queryKey: ['documents'] });
          }}
        />
      )}
      {openId && <DocumentDialog id={openId} onClose={() => setOpenId(null)} />}
    </>
  );
}
