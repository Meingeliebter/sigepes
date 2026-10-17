import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useAuthStore } from '../../modules/auth/store/authStore';
import type { AppAlert } from '../api/domain.service';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
const MAX_BACKOFF_MS = 60_000;

/** Interpreta un bloque de Server-Sent Events ("event: x\ndata: y"). */
function parseEvent(block: string): { event: string; data: string } | null {
  let event = 'message';
  const data: string[] = [];
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
  }
  return data.length ? { event, data: data.join('\n') } : null;
}

/**
 * AC-11.6 · Recibe las alertas nuevas en segundos por Server-Sent Events.
 *
 * <p>Se usa `fetch` en lugar de `EventSource` porque este último no deja enviar la cabecera
 * Authorization. Si el canal se cae, reintenta con espera creciente; mientras tanto la campana
 * sigue consultando el buzón cada minuto, que es la fuente de verdad (AC-11.5).
 */
export function useAlertStream(onAlert?: (alert: AppAlert) => void) {
  const token = useAuthStore((s) => s.accessToken);
  const queryClient = useQueryClient();
  const callback = useRef(onAlert);

  useEffect(() => {
    callback.current = onAlert;
  });

  useEffect(() => {
    if (!token) return undefined;
    const controller = new AbortController();
    let retry: number | undefined;
    let backoff = 2_000;

    const connect = async () => {
      try {
        const response = await fetch(`${BASE_URL}/alertas/stream`, {
          headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
          signal: controller.signal,
        });
        if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`);
        backoff = 2_000;
        const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
        let buffer = '';
        for (;;) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += value.replace(/\r\n/g, '\n');
          let boundary = buffer.indexOf('\n\n');
          while (boundary >= 0) {
            const parsed = parseEvent(buffer.slice(0, boundary));
            buffer = buffer.slice(boundary + 2);
            if (parsed?.event === 'alert') {
              const alert = JSON.parse(parsed.data) as AppAlert;
              void queryClient.invalidateQueries({ queryKey: ['alerts'] });
              callback.current?.(alert);
            }
            boundary = buffer.indexOf('\n\n');
          }
        }
      } catch {
        if (controller.signal.aborted) return;
      }
      if (controller.signal.aborted) return;
      // Al reconectar puede haber alertas que llegaron mientras tanto: se refresca el buzón.
      void queryClient.invalidateQueries({ queryKey: ['alerts'] });
      retry = window.setTimeout(() => void connect(), backoff);
      backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
    };

    void connect();
    return () => {
      controller.abort();
      window.clearTimeout(retry);
    };
  }, [token, queryClient]);
}
