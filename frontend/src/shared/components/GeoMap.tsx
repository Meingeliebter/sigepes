import { GOOGLE_MAPS_API_KEY } from '../hooks/useInteractiveMap';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { GoogleGeoMap } from './map/GoogleGeoMap';
import { SchematicMap } from './map/SchematicMap';
import type { GeoMapProps } from './map/mapStyles';

export type { GeoMapProps, MapPoint } from './map/mapStyles';

/**
 * Mapa de zonas restringidas, recorridos y puntos.
 *
 * <p>Con clave de Google Maps y conexión se muestra la cartografía real (satélite, calles,
 * relieve). Sin señal en el mar o sin clave configurada se usa el esquema propio, que no descarga
 * teselas, de modo que alertas y recorridos siguen visibles sin conexión (AC-04.6).
 */
export function GeoMap(props: GeoMapProps) {
  const online = useOnlineStatus();
  if (GOOGLE_MAPS_API_KEY && online) {
    return <GoogleGeoMap apiKey={GOOGLE_MAPS_API_KEY} {...props} />;
  }
  return <SchematicMap {...props} />;
}
