import { useCallback, useEffect, useMemo, useState } from 'react';
import type { PositionInput, RestrictedZone } from '../api/domain.service';
import { findZoneAt } from '../geo/geo';

/** AC-04.1: una posición cada 5 minutos mientras la faena está abierta. */
export const TRACKING_INTERVAL_MS = 5 * 60 * 1000;

export type TrackerStatus = 'idle' | 'tracking' | 'denied' | 'unavailable';

function newPositionId(): string {
  const random = typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID().replace(/-/g, '').slice(0, 12)
    : Math.random().toString(36).slice(2, 14);
  return `pos-${random}`;
}

const geolocationSupported = typeof navigator !== 'undefined' && 'geolocation' in navigator;

/**
 * Seguimiento GPS de la faena en el dispositivo.
 *
 * <p>Las posiciones se guardan en memoria (y con la faena pendiente si no hay señal) y la última se
 * compara contra las zonas restringidas cacheadas, de modo que la alerta llega aunque no haya
 * conexión (AC-04.6). El servidor repite la comprobación al recibirlas.
 */
export function useTripTracker(zones: RestrictedZone[] | undefined, enabled: boolean) {
  const [positions, setPositions] = useState<PositionInput[]>([]);
  const [status, setStatus] = useState<TrackerStatus>(geolocationSupported ? 'idle' : 'unavailable');

  const capture = useCallback((): Promise<PositionInput | null> => {
    if (!geolocationSupported) return Promise.resolve(null);
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (result) => {
          const position: PositionInput = {
            id: newPositionId(),
            latitude: Number(result.coords.latitude.toFixed(6)),
            longitude: Number(result.coords.longitude.toFixed(6)),
            accuracyMeters: Math.round(result.coords.accuracy),
            recordedAt: new Date(result.timestamp).toISOString(),
          };
          setPositions((previous) => [...previous, position]);
          setStatus('tracking');
          resolve(position);
        },
        (error) => {
          setStatus(error.code === error.PERMISSION_DENIED ? 'denied' : 'unavailable');
          resolve(null);
        },
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 60000 },
      );
    });
  }, []);

  useEffect(() => {
    if (!enabled || !geolocationSupported) return undefined;
    void capture();
    const timer = window.setInterval(() => void capture(), TRACKING_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [enabled, capture]);

  const last = positions[positions.length - 1];
  const currentZone = useMemo(
    () => (last ? findZoneAt(zones, last.latitude, last.longitude) : undefined),
    [zones, last],
  );

  return { positions, status, currentZone, capture };
}
