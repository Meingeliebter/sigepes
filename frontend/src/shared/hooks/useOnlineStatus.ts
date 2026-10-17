import { useEffect, useState } from 'react';

/**
 * Estado de conexión reactivo. `navigator.onLine` por sí solo no dispara
 * renders: hay que escuchar los eventos `online`/`offline` del `window`
 * para que la UI refleje el estado real en cuanto cambia.
 */
export function useOnlineStatus(): boolean {
  const [online, setOnline] = useState(() => navigator.onLine);

  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener('online', update);
    window.addEventListener('offline', update);
    return () => {
      window.removeEventListener('online', update);
      window.removeEventListener('offline', update);
    };
  }, []);

  return online;
}
