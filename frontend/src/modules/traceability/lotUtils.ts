import type { PartyType } from '../../shared/api/domain.service';

export const PARTY_LABEL: Record<PartyType, string> = {
  FISHER: 'Pescador (desembarco)',
  BUYER: 'Comprador',
  TRADER: 'Comercializador',
  PROCESSOR: 'Planta de proceso',
  EXPORTER: 'Exportador',
};

const day = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' });
export const formatDay = (iso: string) => day.format(new Date(`${iso}T12:00:00`));

/** URL pública que codifica el QR del lote. */
export function lotVerificationUrl(code: string): string {
  return `${window.location.origin}/verificar/${code}`;
}
