import { useGoogleMapsAuthFailed, useTilesUnavailable } from '../components/map/mapsAuth';
import { useOnlineStatus } from './useOnlineStatus';

export const GOOGLE_MAPS_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

/** Qué mapa se muestra: Google (si hay clave válida), OpenStreetMap con Leaflet o el esquema sin conexión. */
export type MapMode = 'google' | 'leaflet' | 'schematic';

export function useMapMode(): MapMode {
  const online = useOnlineStatus();
  const authFailed = useGoogleMapsAuthFailed();
  const tilesUnavailable = useTilesUnavailable();
  if (!online) return 'schematic';
  if (GOOGLE_MAPS_API_KEY && !authFailed) return 'google';
  return tilesUnavailable ? 'schematic' : 'leaflet';
}

/** El mapa admite clic para elegir coordenadas: con cartografía real (Google o Leaflet), no con el esquema. */
export function useInteractiveMap(): boolean {
  return useMapMode() !== 'schematic';
}
