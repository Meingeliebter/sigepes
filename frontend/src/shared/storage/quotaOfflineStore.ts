import type { Quota } from '../api/domain.service';

const KEY = 'sigepes-cached-quotas';

/** AC-07.6: último saldo de cuotas conocido, para validar capturas en el mar sin conexión. */
export function cacheQuotas(quotas: Quota[]): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(quotas));
  } catch {
    // Almacenamiento no disponible: se reintenta en el próximo fetch exitoso.
  }
}

export function getCachedQuotas(): Quota[] | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Quota[]) : null;
  } catch {
    return null;
  }
}

/**
 * Cuota que aplica a una captura, con la misma regla que el backend: especie, zona (o «TODAS»),
 * fecha dentro del periodo, cuota activa y, si está asignada, la misma embarcación. Gana la más
 * específica (por embarcación y por zona concreta).
 */
export function findApplicableQuota(
  quotas: Quota[] | undefined,
  speciesId: string,
  zone: string,
  vesselId: string,
  date: string = new Date().toISOString().slice(0, 10),
): Quota | undefined {
  if (!quotas || !speciesId) return undefined;
  const normalizedZone = zone.trim().toLowerCase();
  const specificity = (quota: Quota) =>
    (quota.vesselId ? 2 : 0) + (quota.zone.toUpperCase() === 'TODAS' ? 0 : 1);
  return quotas
    .filter(
      (quota) =>
        quota.status === 'ACTIVE' &&
        quota.speciesId === speciesId &&
        (quota.zone.toUpperCase() === 'TODAS' || quota.zone.toLowerCase() === normalizedZone) &&
        quota.from <= date &&
        quota.to >= date &&
        (!quota.vesselId || quota.vesselId === vesselId),
    )
    .sort((a, b) => specificity(b) - specificity(a))[0];
}
