import { Chip } from '@mui/material';

type Tone = 'success' | 'warning' | 'error' | 'info' | 'default' | 'secondary';

const toneByStatus: Record<string, Tone> = {
  ACTIVE: 'success', VALID: 'success', APPROVED: 'success', SYNCHRONIZED: 'success', OPEN: 'success',
  PENDING: 'warning', UNDER_MAINTENANCE: 'warning', PENDING_SYNC: 'warning', EXPIRING: 'warning',
  DRAFT: 'default', CLOSED: 'error', REJECTED: 'error', BLOCKED: 'error', EXPIRED: 'error', INFO: 'info',
};

const labelByStatus: Record<string, string> = {
  ACTIVE: 'Activo', VALID: 'Vigente', APPROVED: 'Aprobado', SYNCHRONIZED: 'Sincronizado', OPEN: 'Abierta',
  PENDING: 'Pendiente', UNDER_MAINTENANCE: 'Mantenimiento', PENDING_SYNC: 'Por sincronizar', EXPIRING: 'Por vencer',
  DRAFT: 'Borrador', CLOSED: 'Cerrada', REJECTED: 'Rechazado', BLOCKED: 'Bloqueado', EXPIRED: 'Vencido', INFO: 'Informativo',
};

export function StatusChip({ status }: { status: string }) {
  const tone = toneByStatus[status] ?? 'default';
  const label = labelByStatus[status] ?? status;
  return <Chip size="small" color={tone === 'default' ? undefined : tone} label={label} variant={tone === 'default' ? 'outlined' : 'filled'} />;
}
