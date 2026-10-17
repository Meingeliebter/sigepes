import { Chip } from '@mui/material';

type Tone = 'success' | 'warning' | 'error' | 'info' | 'default' | 'secondary';

const toneByStatus: Record<string, Tone> = {
  ACTIVE: 'success', VALID: 'success', APPROVED: 'success', SYNCHRONIZED: 'success', OPEN: 'success',
  PENDING: 'warning', UNDER_MAINTENANCE: 'warning', PENDING_SYNC: 'warning', EXPIRING: 'warning',
  IN_PROGRESS: 'warning', SUSPENDED: 'warning',
  DRAFT: 'default', INACTIVE: 'default', FINISHED: 'default', CANCELLED: 'default',
  CLOSED: 'error', REJECTED: 'error', BLOCKED: 'error', EXPIRED: 'error', DECOMMISSIONED: 'error',
  SCHEDULED: 'info', INFO: 'info',
};

const labelByStatus: Record<string, string> = {
  ACTIVE: 'Activo', VALID: 'Vigente', APPROVED: 'Aprobado', SYNCHRONIZED: 'Sincronizado', OPEN: 'Abierta',
  PENDING: 'Pendiente', UNDER_MAINTENANCE: 'Mantenimiento', PENDING_SYNC: 'Por sincronizar', EXPIRING: 'Por vencer',
  IN_PROGRESS: 'En curso', SUSPENDED: 'Suspendido',
  DRAFT: 'Borrador', INACTIVE: 'Inactivo', FINISHED: 'Finalizada', CANCELLED: 'Anulada',
  CLOSED: 'Cerrada', REJECTED: 'Rechazado', BLOCKED: 'Bloqueado', EXPIRED: 'Vencido', DECOMMISSIONED: 'Dado de baja',
  SCHEDULED: 'Programada', INFO: 'Informativo',
};

export function StatusChip({ status }: { status: string }) {
  const tone = toneByStatus[status] ?? 'default';
  const label = labelByStatus[status] ?? status;
  return <Chip size="small" color={tone === 'default' ? undefined : tone} label={label} variant={tone === 'default' ? 'outlined' : 'filled'} />;
}
