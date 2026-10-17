import type { ReportInput, ReportReceipt } from '../api/domain.service';

/**
 * AC-10.6: cola local de denuncias redactadas sin señal.
 *
 * <p>Va en IndexedDB y no en localStorage porque guarda las fotos como Blob (localStorage solo
 * admite texto y se queda corto con tres fotos de teléfono). El `clientId` de cada denuncia hace
 * que el reenvío sea idempotente en el servidor.
 */
export interface QueuedReport {
  clientId: string;
  payload: ReportInput;
  photos: Blob[];
  queuedAt: string;
  /** Motivo por el que el servidor la rechazó; queda a la vista para corregirla o descartarla. */
  rejection?: string;
}

const DB_NAME = 'sigepes-reports';
const STORE = 'outbox';
const RECEIPTS_KEY = 'sigepes-report-receipts';
export const OUTBOX_EVENT = 'sigepes-report-outbox';

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB no disponible'));
      return;
    }
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE, { keyPath: 'clientId' });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error('No fue posible abrir IndexedDB'));
  });
}

async function withStore<T>(mode: IDBTransactionMode, run: (store: IDBObjectStore) => IDBRequest<T>): Promise<T> {
  const db = await openDb();
  try {
    return await new Promise<T>((resolve, reject) => {
      const transaction = db.transaction(STORE, mode);
      const request = run(transaction.objectStore(STORE));
      transaction.oncomplete = () => resolve(request.result);
      transaction.onerror = () => reject(transaction.error ?? new Error('Error de IndexedDB'));
    });
  } finally {
    db.close();
  }
}

function notify() {
  window.dispatchEvent(new Event(OUTBOX_EVENT));
}

export async function enqueueReport(report: QueuedReport): Promise<void> {
  await withStore('readwrite', (store) => store.put(report));
  notify();
}

export async function queuedReports(): Promise<QueuedReport[]> {
  try {
    return await withStore<QueuedReport[]>('readonly', (store) => store.getAll() as IDBRequest<QueuedReport[]>);
  } catch {
    return [];
  }
}

export async function removeQueuedReport(clientId: string): Promise<void> {
  await withStore('readwrite', (store) => store.delete(clientId));
  notify();
}

/**
 * Códigos de seguimiento de las denuncias hechas desde este dispositivo. Solo viven aquí: el
 * servidor no guarda nada que vincule una denuncia anónima con su autor.
 */
export function savedReceipts(): ReportReceipt[] {
  try {
    const raw = localStorage.getItem(RECEIPTS_KEY);
    return raw ? (JSON.parse(raw) as ReportReceipt[]) : [];
  } catch {
    return [];
  }
}

export function saveReceipt(receipt: ReportReceipt): void {
  try {
    const others = savedReceipts().filter((item) => item.trackingCode !== receipt.trackingCode);
    localStorage.setItem(RECEIPTS_KEY, JSON.stringify([receipt, ...others].slice(0, 20)));
  } catch {
    // Sin almacenamiento: el código se mostró en pantalla y el usuario puede anotarlo.
  }
  notify();
}

export function forgetReceipt(trackingCode: string): void {
  try {
    localStorage.setItem(RECEIPTS_KEY, JSON.stringify(savedReceipts().filter((item) => item.trackingCode !== trackingCode)));
  } catch {
    // Nada que olvidar si el almacenamiento no está disponible.
  }
  notify();
}
