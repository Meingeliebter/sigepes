import type { RestrictedZone } from '../api/domain.service';

const KEY = 'sigepes-cached-zones';

/** AC-04.6: copia local de las zonas restringidas para alertar sin conexión. */
export function cacheZones(zones: RestrictedZone[]): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(zones));
  } catch {
    // Almacenamiento no disponible: se reintenta en el próximo fetch exitoso.
  }
}

export function getCachedZones(): RestrictedZone[] | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as RestrictedZone[]) : null;
  } catch {
    return null;
  }
}
