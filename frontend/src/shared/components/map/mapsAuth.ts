import { useSyncExternalStore } from 'react';

declare global {
  interface Window {
    gm_authFailure?: () => void;
  }
}

let failed = false;
const listeners = new Set<() => void>();

/**
 * Google Maps no rechaza la carga cuando la clave es inválida, no tiene facturación o el dominio no
 * está autorizado: carga y luego llama a `window.gm_authFailure`. Ese es el único aviso, así que se
 * registra aquí para que el mapa caiga al esquema propio en vez de mostrar la pantalla de error.
 */
if (typeof window !== 'undefined') {
  const previous = window.gm_authFailure;
  window.gm_authFailure = () => {
    failed = true;
    listeners.forEach((listener) => listener());
    previous?.();
  };
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useGoogleMapsAuthFailed(): boolean {
  return useSyncExternalStore(subscribe, () => failed, () => false);
}

// --- Teselas de mapa base (OpenStreetMap/Esri) -------------------------------------------------

let tilesUnavailable = false;
const tileListeners = new Set<() => void>();

/** Las teselas del mapa base no cargan (red que las bloquea): se usa el esquema sin conexión. */
export function markTilesUnavailable(): void {
  if (tilesUnavailable) return;
  tilesUnavailable = true;
  tileListeners.forEach((listener) => listener());
}

function subscribeTiles(listener: () => void) {
  tileListeners.add(listener);
  return () => tileListeners.delete(listener);
}

export function useTilesUnavailable(): boolean {
  return useSyncExternalStore(subscribeTiles, () => tilesUnavailable, () => false);
}
