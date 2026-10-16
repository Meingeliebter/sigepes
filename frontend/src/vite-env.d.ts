/// <reference types="vite/client" />
/// <reference types="google.maps" />

interface ImportMetaEnv {
  readonly VITE_APP_NAME: string;
  readonly VITE_API_BASE_URL: string;
  readonly VITE_REQUEST_TIMEOUT_MS: string;
  /** Clave de navegador de Google Maps JavaScript API (restringida por dominio en Google Cloud). */
  readonly VITE_GOOGLE_MAPS_API_KEY?: string;
  /** Map ID de Google Cloud; por defecto `DEMO_MAP_ID`. */
  readonly VITE_GOOGLE_MAPS_MAP_ID?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
