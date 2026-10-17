import AddIcon from '@mui/icons-material/Add';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import GavelIcon from '@mui/icons-material/Gavel';
import PrintIcon from '@mui/icons-material/Print';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  IconButton, Snackbar, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip,
  Typography,
} from '@mui/material';
import { QRCodeSVG } from 'qrcode.react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { EmptyState } from '../../../shared/components/EmptyState';
import { PageHeader } from '../../../shared/components/PageHeader';
import { StatusChip } from '../../../shared/components/StatusChip';
import { domainService, type Permit, type PermitStatus } from '../../../shared/api/domain.service';
import { toAppError } from '../../../shared/errors/AppError';
import { useAuthStore } from '../../auth/store/authStore';
import { isAuthority } from '../../auth/types/auth.types';

/** Resolución pendiente de confirmar: rechazo y suspensión exigen motivo. */
interface Resolution {
  permit: Permit;
  status: PermitStatus;
}

const ATTACHMENT_LABEL: Record<string, string> = {
  ID_COPY: 'Copia de cédula',
  VESSEL_OWNERSHIP: 'Propiedad de la embarcación',
};

const FILTERS = [
  { value: 'PENDING', label: 'Pendientes' },
  { value: 'APPROVED', label: 'Aprobados' },
  { value: 'REJECTED', label: 'Rechazados' },
  { value: 'SUSPENDED', label: 'Suspendidos' },
  { value: 'ALL', label: 'Todos' },
] as const;

export function PermitsPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const authority = isAuthority(user?.role);

  const permitsQuery = useQuery({ queryKey: ['permits'], queryFn: domainService.permits });
  const [requestOpen, setRequestOpen] = useState(false);
  const [type, setType] = useState('Pesca artesanal');
  const [idDocument, setIdDocument] = useState<File | null>(null);
  const [vesselOwnershipDocument, setVesselOwnershipDocument] = useState<File | null>(null);
  const [resolution, setResolution] = useState<Resolution | null>(null);
  const [reason, setReason] = useState('');
  const [toast, setToast] = useState('');
  const [certificate, setCertificate] = useState<Permit | null>(null);
  // AC-08.3: la autoridad aterriza en la cola de pendientes, no en un listado plano.
  const [filter, setFilter] = useState<(typeof FILTERS)[number]['value']>(authority ? 'PENDING' : 'ALL');

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['permits'] });
    await queryClient.invalidateQueries({ queryKey: ['summary'] });
  };

  const requestMutation = useMutation({
    mutationFn: () => domainService.createPermit({ type }, idDocument!, vesselOwnershipDocument!),
    onSuccess: async (permit) => {
      setRequestOpen(false);
      setIdDocument(null);
      setVesselOwnershipDocument(null);
      setToast(`Solicitud ${permit.number} registrada.`);
      await refresh();
    },
  });

  const evaluateMutation = useMutation({
    mutationFn: ({ id, status, motive }: { id: string; status: PermitStatus; motive?: string }) =>
      domainService.evaluatePermit(id, status, motive),
    onSuccess: async (permit) => {
      setResolution(null);
      setReason('');
      setToast(`Permiso ${permit.number} actualizado.`);
      await refresh();
    },
  });

  const permits = permitsQuery.data ?? [];
  const counts = useMemo(
    () => Object.fromEntries(FILTERS.map((f) => [f.value, f.value === 'ALL' ? permits.length : permits.filter((p) => p.status === f.value).length])),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- `permitsQuery.data` es la identidad estable; `permits` solo agrega el fallback `[]`.
    [permitsQuery.data],
  );
  const visiblePermits = filter === 'ALL' ? permits : permits.filter((p) => p.status === filter);
  const requestError = requestMutation.error ? toAppError(requestMutation.error) : null;
  const evaluateError = evaluateMutation.error ? toAppError(evaluateMutation.error) : null;

  const resolve = (permit: Permit, status: PermitStatus) => {
    if (status === 'APPROVED') {
      evaluateMutation.mutate({ id: permit.id, status });
      return;
    }
    setReason('');
    setResolution({ permit, status });
  };

  const canSubmitRequest = !!type && !!idDocument && !!vesselOwnershipDocument;

  return (
    <>
      <PageHeader
        title="Permisos"
        subtitle={authority ? 'Cola de revisión de solicitudes de permiso' : 'Solicitud, seguimiento y verificación de permisos de pesca'}
        actionLabel="Solicitar permiso"
        actionIcon={<AddIcon />}
        onAction={() => setRequestOpen(true)}
      />
      {permitsQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar los permisos.</Alert>}
      {evaluateError && <Alert severity="error" sx={{ mb: 2 }}>{evaluateError.message}</Alert>}

      {authority && (
        <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
          {FILTERS.map((f) => (
            <Chip
              key={f.value}
              label={`${f.label} (${counts[f.value] ?? 0})`}
              color={filter === f.value ? 'primary' : 'default'}
              variant={filter === f.value ? 'filled' : 'outlined'}
              onClick={() => setFilter(f.value)}
            />
          ))}
        </Stack>
      )}

      <Card>
        <CardContent>
          {permitsQuery.isLoading && <CircularProgress size={28} />}
          {!permitsQuery.isLoading && !permitsQuery.isError && visiblePermits.length === 0 && (
            <EmptyState
              icon={<GavelIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
              title={permits.length === 0 ? 'No tienes permisos registrados' : 'Nada en este filtro'}
              description={
                permits.length === 0
                  ? 'Solicita tu primer permiso de pesca para poder abrir faenas.'
                  : 'No hay permisos con este estado por ahora.'
              }
              actionLabel={permits.length === 0 ? 'Solicitar permiso' : undefined}
              onAction={permits.length === 0 ? () => setRequestOpen(true) : undefined}
            />
          )}
          {!permitsQuery.isLoading && visiblePermits.length > 0 && (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Titular</TableCell>
                  <TableCell>Emisión</TableCell>
                  <TableCell>Vencimiento</TableCell>
                  <TableCell>Adjuntos</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell align="right">Acción</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {visiblePermits.map((permit) => (
                  <TableRow key={permit.id} hover>
                    <TableCell><Typography fontWeight={700}>{permit.number}</Typography></TableCell>
                    <TableCell>{permit.type}</TableCell>
                    <TableCell>{permit.holder}</TableCell>
                    <TableCell>{permit.issued}</TableCell>
                    <TableCell>
                      {permit.expires}
                      {permit.expiringSoon && (
                        <Typography variant="caption" color="warning.main" display="block">
                          Vence en {permit.daysToExpire} días
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.5}>
                        {permit.attachments.map((att) => (
                          <Tooltip key={att.kind} title={`Descargar: ${ATTACHMENT_LABEL[att.kind] ?? att.kind}`}>
                            <IconButton
                              size="small"
                              onClick={() => void domainService.downloadAttachment(permit.id, att.kind, att.originalFilename)}
                              aria-label={`Descargar ${ATTACHMENT_LABEL[att.kind] ?? att.kind}`}
                            >
                              <AttachFileIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        ))}
                      </Stack>
                    </TableCell>
                    <TableCell><StatusChip status={permit.status} /></TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end" flexWrap="wrap" useFlexGap>
                        {authority && permit.status !== 'APPROVED' && (
                          <Button size="small" color="success" onClick={() => resolve(permit, 'APPROVED')} disabled={evaluateMutation.isPending}>Aprobar</Button>
                        )}
                        {authority && permit.status === 'PENDING' && (
                          <Button size="small" color="error" onClick={() => resolve(permit, 'REJECTED')} disabled={evaluateMutation.isPending}>Rechazar</Button>
                        )}
                        {authority && permit.status === 'APPROVED' && (
                          <Button size="small" color="warning" onClick={() => resolve(permit, 'SUSPENDED')} disabled={evaluateMutation.isPending}>Suspender</Button>
                        )}
                        {permit.status === 'APPROVED' && (
                          <Tooltip title="Ver certificado digital">
                            <Button size="small" startIcon={<QrCode2Icon />} onClick={() => setCertificate(permit)}>Certificado</Button>
                          </Tooltip>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          )}
          {permits.length > 0 && (
          <Stack sx={{ mt: 1 }}>
            <Typography variant="caption" color="text.secondary">Mostrando {visiblePermits.length} de {permits.length} permisos</Typography>
          </Stack>
          )}
        </CardContent>
      </Card>

      <Dialog open={requestOpen} onClose={() => setRequestOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Solicitar permiso</DialogTitle>
        <DialogContent>
          {requestError && <Alert severity="error" sx={{ mb: 2 }}>{requestError.message}</Alert>}
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Tipo de permiso"
              value={type}
              onChange={(event) => setType(event.target.value)}
              fullWidth
              helperText="La solicitud queda pendiente de revisión por la autoridad."
            />
            <Button component="label" variant="outlined" startIcon={<AttachFileIcon />}>
              {idDocument ? idDocument.name : 'Adjuntar copia de cédula'}
              <input
                hidden
                type="file"
                accept="application/pdf,image/jpeg,image/png"
                onChange={(e) => setIdDocument(e.target.files?.[0] ?? null)}
              />
            </Button>
            <Button component="label" variant="outlined" startIcon={<AttachFileIcon />}>
              {vesselOwnershipDocument ? vesselOwnershipDocument.name : 'Adjuntar propiedad de la embarcación'}
              <input
                hidden
                type="file"
                accept="application/pdf,image/jpeg,image/png"
                onChange={(e) => setVesselOwnershipDocument(e.target.files?.[0] ?? null)}
              />
            </Button>
            <Typography variant="caption" color="text.secondary">
              Ambos adjuntos son obligatorios (PDF, JPG o PNG, máx. 8 MB).
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRequestOpen(false)}>Cancelar</Button>
          <Button variant="contained" disabled={requestMutation.isPending || !canSubmitRequest} onClick={() => requestMutation.mutate()}>
            {requestMutation.isPending ? 'Enviando...' : 'Solicitar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={resolution !== null} onClose={() => setResolution(null)} fullWidth maxWidth="xs">
        <DialogTitle>{resolution?.status === 'REJECTED' ? 'Rechazar permiso' : 'Suspender permiso'}</DialogTitle>
        <DialogContent>
          <TextField
            label="Motivo"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            fullWidth
            multiline
            minRows={2}
            sx={{ mt: 1 }}
            helperText="El motivo es obligatorio y queda registrado en la auditoría."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResolution(null)}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={!reason.trim() || evaluateMutation.isPending}
            onClick={() => resolution && evaluateMutation.mutate({ id: resolution.permit.id, status: resolution.status, motive: reason })}
          >
            Confirmar
          </Button>
        </DialogActions>
      </Dialog>

      {/* AC-08.4: certificado digital descargable — se imprime/guarda como PDF con el diálogo nativo del navegador. */}
      <Dialog open={!!certificate} onClose={() => setCertificate(null)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ '@media print': { display: 'none' } }}>Certificado digital</DialogTitle>
        <DialogContent>
          {certificate && (
            <Box sx={{ p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2, textAlign: 'center' }}>
              <Typography variant="overline" color="text.secondary">República de Colombia · AUNAP</Typography>
              <Typography variant="h5" sx={{ mb: 2 }}>Constancia de permiso de pesca</Typography>
              <Stack spacing={0.5} sx={{ mb: 3, textAlign: 'left', display: 'inline-block' }}>
                <Typography><strong>Número:</strong> {certificate.number}</Typography>
                <Typography><strong>Titular:</strong> {certificate.holder}</Typography>
                <Typography><strong>Tipo:</strong> {certificate.type}</Typography>
                <Typography><strong>Vigencia:</strong> {certificate.issued} al {certificate.expires}</Typography>
                <Typography><strong>Código de verificación:</strong> {certificate.verificationCode}</Typography>
              </Stack>
              <Box>
                {certificate.verificationCode && <QRCodeSVG value={certificate.verificationCode} size={180} />}
              </Box>
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 2 }}>
                Escanea este código para verificar la vigencia del permiso en campo.
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ '@media print': { display: 'none' } }}>
          <Button onClick={() => setCertificate(null)}>Cerrar</Button>
          <Button variant="contained" startIcon={<PrintIcon />} onClick={() => window.print()}>
            Descargar / Imprimir
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={toast !== ''} autoHideDuration={4000} onClose={() => setToast('')} message={toast} />
    </>
  );
}
