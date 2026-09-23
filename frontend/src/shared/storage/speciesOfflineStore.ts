import type { Species } from '../api/domain.service';

const KEY = 'sigepes-cached-species';

/** AC-06.3: copia local del catálogo de especies para consulta sin conexión en el mar. */
export function cacheSpecies(species: Species[]): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(species));
  } catch {
    // Almacenamiento no disponible (modo privado, cuota agotada): no es crítico, se reintenta
    // en el próximo fetch exitoso.
  }
}

export function getCachedSpecies(): Species[] | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Species[]) : null;
  } catch {
    return null;
  }
}
