import { useGoogleMapsAuthFailed } from '../components/map/mapsAuth';
import { useOnlineStatus } from './useOnlineStatus';

export const GOOGLE_MAPS_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

/**
 * El mapa admite clic para elegir coordenadas: requiere Google Maps (clave configurada y válida, y
 * señal). En cualquier otro caso se muestra el esquema sin conexión, que no es interactivo.
 */
export function useInteractiveMap(): boolean {
  const online = useOnlineStatus();
  const authFailed = useGoogleMapsAuthFailed();
  return !!GOOGLE_MAPS_API_KEY && online && !authFailed;
}
