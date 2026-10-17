import type { AlertSeverity, AlertType } from '../api/domain.service';

export const ALERT_TYPE_LABEL: Record<AlertType, string> = {
  CLOSURE: 'Veda',
  QUOTA: 'Cuota',
  PERMIT: 'Permiso',
  ZONE: 'Zona restringida',
  REPORT: 'Denuncia',
  SECURITY: 'Seguridad',
  DOCUMENT: 'Documento',
  EXPORT: 'Reporte oficial',
};

/** AC-11.2 · Informativa / Atención / Crítica. */
export const ALERT_SEVERITY: Record<AlertSeverity, { label: string; color: 'info' | 'warning' | 'error' }> = {
  INFO: { label: 'Informativa', color: 'info' },
  WARNING: { label: 'Atención', color: 'warning' },
  CRITICAL: { label: 'Crítica', color: 'error' },
};
