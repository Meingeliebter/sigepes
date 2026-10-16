import type { RestrictedZone } from '../api/domain.service';

/**
 * AC-04.6: prueba de punto en polígono (cruce de rayos), la misma que hace el servidor, para que
 * el pescador reciba la alerta de zona restringida incluso sin señal.
 */
export function pointInPolygon(lat: number, lon: number, polygon: [number, number][]): boolean {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const [latI, lonI] = polygon[i];
    const [latJ, lonJ] = polygon[j];
    const crosses = latI > lat !== latJ > lat && lon < ((lonJ - lonI) * (lat - latI)) / (latJ - latI) + lonI;
    if (crosses) inside = !inside;
  }
  return inside;
}

/** Zona restringida activa que contiene el punto (con prefiltro de caja envolvente). */
export function findZoneAt(zones: RestrictedZone[] | undefined, lat: number, lon: number): RestrictedZone | undefined {
  return zones?.find((zone) => {
    if (!zone.active) return false;
    const lats = zone.polygon.map(([pLat]) => pLat);
    const lons = zone.polygon.map(([, pLon]) => pLon);
    if (lat < Math.min(...lats) || lat > Math.max(...lats) || lon < Math.min(...lons) || lon > Math.max(...lons)) {
      return false;
    }
    return pointInPolygon(lat, lon, zone.polygon);
  });
}

export const ZONE_TYPE_LABEL: Record<RestrictedZone['zoneType'], string> = {
  CLOSURE_ZONE: 'Zona en veda',
  MPA: 'Área Marina Protegida',
  OTHER: 'Otra restricción',
};

/**
 * Lee vértices escritos como "lat, lon" (uno por línea). Devuelve null si alguna línea no es
 * válida, para que el formulario pueda señalarla.
 */
export function parseVertices(text: string): [number, number][] | null {
  const lines = text.split('\n').map((line) => line.trim()).filter(Boolean);
  const vertices: [number, number][] = [];
  for (const line of lines) {
    const parts = line.split(/[,;\s]+/).filter(Boolean).map(Number);
    if (parts.length !== 2 || parts.some((value) => Number.isNaN(value))) return null;
    const [lat, lon] = parts;
    if (Math.abs(lat) > 90 || Math.abs(lon) > 180) return null;
    vertices.push([lat, lon]);
  }
  return vertices;
}
