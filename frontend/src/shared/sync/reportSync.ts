import axios from 'axios';
import { domainService } from '../api/domain.service';
import { toAppError } from '../errors/AppError';
import { enqueueReport, queuedReports, removeQueuedReport, saveReceipt } from '../storage/reportOutbox';

let running: Promise<number> | null = null;

/**
 * AC-10.6: envía las denuncias encoladas sin conexión. Devuelve cuántas se radicaron.
 *
 * <p>Si el servidor rechaza una (datos inválidos) no se borra: queda marcada con el motivo para que
 * quien denuncia la vea y decida. Si el fallo es de red, se reintenta en la próxima reconexión.
 */
export function flushReportOutbox(): Promise<number> {
  if (running) return running;
  running = (async () => {
    let sent = 0;
    for (const queued of (await queuedReports()).filter((item) => !item.rejection)) {
      try {
        const receipt = await domainService.submitReport(queued.payload, queued.photos);
        saveReceipt(receipt);
        await removeQueuedReport(queued.clientId);
        sent += 1;
      } catch (error) {
        const status = axios.isAxiosError(error) ? error.response?.status : undefined;
        if (status && status >= 400 && status < 500 && status !== 429) {
          await enqueueReport({ ...queued, rejection: toAppError(error).message });
        } else {
          break;
        }
      }
    }
    return sent;
  })().finally(() => {
    running = null;
  });
  return running;
}
