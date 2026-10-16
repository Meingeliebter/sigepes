import type { ReportStatus, ReportType } from '../../shared/api/domain.service';

export const REPORT_TYPE_LABEL: Record<ReportType, string> = {
  CLOSURE_FISHING: 'Pesca en periodo de veda',
  UNDERSIZED_CATCH: 'Captura bajo la talla mínima',
  PROHIBITED_GEAR: 'Arte de pesca prohibido',
  MPA_INTRUSION: 'Pesca en Área Marina Protegida',
  OTHER: 'Otra infracción',
};

export const REPORT_STATUS_LABEL: Record<ReportStatus, string> = {
  RECEIVED: 'Recibida',
  INVESTIGATING: 'En investigación',
  CLOSED_FOUNDED: 'Cerrada · fundada',
  CLOSED_UNFOUNDED: 'Cerrada · no fundada',
};

export const REPORT_STATUS_COLOR: Record<ReportStatus, 'info' | 'warning' | 'success' | 'default'> = {
  RECEIVED: 'info',
  INVESTIGATING: 'warning',
  CLOSED_FOUNDED: 'success',
  CLOSED_UNFOUNDED: 'default',
};

/** AC-10.4: siguientes estados permitidos desde cada uno (mismo flujo que valida el servidor). */
export const NEXT_STATUSES: Record<ReportStatus, ReportStatus[]> = {
  RECEIVED: ['INVESTIGATING'],
  INVESTIGATING: ['CLOSED_FOUNDED', 'CLOSED_UNFOUNDED'],
  CLOSED_FOUNDED: [],
  CLOSED_UNFOUNDED: [],
};
