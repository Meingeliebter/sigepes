import type { RestrictedZone } from '../../api/domain.service';

export interface MapPoint {
  id: string;
  latitude: number;
  longitude: number;
  /** Punto dentro de una zona restringida: se marca en rojo. */
  intrusion?: boolean;
}

export interface GeoMapProps {
  zones: RestrictedZone[];
  track?: MapPoint[];
  /** Zona a resaltar (p. ej. la que se está previsualizando en un formulario). */
  highlightZoneId?: string;
  height?: number;
  title: string;
  /**
   * Si se pasa, un clic en el mapa entrega la coordenada (ubicar una denuncia, trazar los vértices
   * de una zona). Solo el mapa de Google lo admite; el esquema sin conexión no es interactivo.
   */
  onPick?: (latitude: number, longitude: number) => void;
}

/**
 * Colores por tipo de zona. Trazo opaco y relleno translúcido para que se distingan tanto sobre el
 * esquema claro como sobre la imagen satelital oscura.
 */
export const ZONE_STYLE: Record<RestrictedZone['zoneType'], { fill: string; stroke: string; fillOpacity: number }> = {
  CLOSURE_ZONE: { fill: 'rgba(168, 90, 0, 0.16)', stroke: '#E07B00', fillOpacity: 0.28 },
  MPA: { fill: 'rgba(27, 107, 63, 0.16)', stroke: '#1FA35A', fillOpacity: 0.28 },
  OTHER: { fill: 'rgba(61, 110, 146, 0.16)', stroke: '#3D8FD1', fillOpacity: 0.26 },
};

export const TRACK_COLOR = '#0B3B5C';
export const TRACK_START_COLOR = '#0E7C86';
export const INTRUSION_COLOR = '#B3261E';
