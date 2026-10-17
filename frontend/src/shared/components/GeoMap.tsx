import { GOOGLE_MAPS_API_KEY, useMapMode } from '../hooks/useInteractiveMap';
import { GoogleGeoMap } from './map/GoogleGeoMap';
import { LeafletGeoMap } from './map/LeafletGeoMap';
import { markTilesUnavailable } from './map/mapsAuth';
import { SchematicMap } from './map/SchematicMap';
import type { GeoMapProps } from './map/mapStyles';

export type { GeoMapProps, MapPoint } from './map/mapStyles';

/**
 * Mapa de zonas restringidas, recorridos y puntos, siempre sobre cartografía real cuando hay señal:
 *
 * <ul>
 *   <li>Google Maps si hay una clave válida configurada.</li>
 *   <li>Si no, OpenStreetMap con Leaflet (sin clave), con vista de mapa, satélite y relieve.</li>
 *   <li>Sin señal en el mar, o si las teselas no cargan, el esquema propio: no descarga nada y
 *       mantiene visibles zonas, alertas y recorridos (AC-04.6).</li>
 * </ul>
 */
export function GeoMap(props: GeoMapProps) {
  const mode = useMapMode();
  if (mode === 'google' && GOOGLE_MAPS_API_KEY) {
    return <GoogleGeoMap apiKey={GOOGLE_MAPS_API_KEY} {...props} />;
  }
  if (mode === 'leaflet') {
    return <LeafletGeoMap {...props} onTilesUnavailable={markTilesUnavailable} />;
  }
  return <SchematicMap {...props} />;
}
