import AddIcon from '@mui/icons-material/Add';
import GavelIcon from '@mui/icons-material/Gavel';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import {
  Alert, Button, Card, CardContent, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle,
  Snackbar, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
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

export function PermitsPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const authority = isAuthority(user?.role);

  const permitsQuery = useQuery({ queryKey: ['permits'], queryFn: domainService.permits });
  const [requestOpen, setRequestOpen] = useState(false);
  const [type, setType] = useState('Pesca artesanal');
  const [resolution, setResolution] = useState<Resolution | null>(null);
  const [reason, setReason] = useState('');
  const [toast, setToast] = useState('');

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['permits'] });
    await queryClient.invalidateQueries({ queryKey: ['summary'] });
  };

  const requestMutation = useMutation({
    mutationFn: domainService.createPermit,
    onSuccess: async (permit) => {
      setRequestOpen(false);
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

  return (
    <>
      <PageHeader
        title="Permisos"
        subtitle="Solicitud, seguimiento y verificación de permisos de pesca"
        actionLabel="Solicitar permiso"
        actionIcon={<AddIcon />}
        onAction={() => setRequestOpen(true)}
      />
      {permitsQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>No fue posible cargar los permisos.</Alert>}
      {evaluateError && <Alert severity="error" sx={{ mb: 2 }}>{evaluateError.message}</Alert>}
      <Card>
        <CardContent>
          {permitsQuery.isLoading && <CircularProgress size={28} />}
          {!permitsQuery.isLoading && !permitsQuery.isError && permits.length === 0 && (
            <EmptyState
              icon={<GavelIcon sx={{ fontSize: 48, color: 'text.disabled' }} />}
              title="No tienes permisos registrados"
              description="Solicita tu primer permiso de pesca para poder abrir faenas."
              actionLabel="Solicitar permiso"
              onAction={() => setRequestOpen(true)}
            />
          )}
          {!permitsQuery.isLoading && permits.length > 0 && (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Titular</TableCell>
                  <TableCell>Emisión</TableCell>
                  <TableCell>Vencimiento</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell align="right">Acción</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {permits.map((permit) => (
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
                        <Tooltip title={permit.verificationCode ?? 'Sin constancia'}>
                          <span>
                            <Button size="small" startIcon={<QrCode2Icon />} onClick={() => setToast(`Constancia: ${permit.verificationCode ?? '—'}`)}>QR</Button>
                          </span>
                        </Tooltip>
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
            <Typography variant="caption" color="text.secondary">Mostrando {permits.length} permisos</Typography>
          </Stack>
          )}
        </CardContent>
      </Card>

      <Dialog open={requestOpen} onClose={() => setRequestOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Solicitar permiso</DialogTitle>
        <DialogContent>
          {requestError && <Alert severity="error" sx={{ mb: 2 }}>{requestError.message}</Alert>}
          <TextField
            label="Tipo de permiso"
            value={type}
            onChange={(event) => setType(event.target.value)}
            fullWidth
            sx={{ mt: 1 }}
            helperText="La solicitud queda pendiente de revisión por la autoridad."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRequestOpen(false)}>Cancelar</Button>
          <Button variant="contained" disabled={requestMutation.isPending || !type} onClick={() => requestMutation.mutate({ type })}>
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

      <Snackbar open={toast !== ''} autoHideDuration={4000} onClose={() => setToast('')} message={toast} />
    </>
  );
}
