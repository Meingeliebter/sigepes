import type { QueryClient } from '@tanstack/react-query';
import { domainService, type PendingTripPayload, type Trip } from '../api/domain.service';
import { toAppError } from '../errors/AppError';
import { clearPendingTrips, getPendingTrips } from '../storage/tripOfflineStore';

function toPendingTripPayload(trip: Trip): PendingTripPayload {
  return {
    id: trip.id,
    vesselId: trip.vesselId,
    zone: trip.zone,
    originPort: trip.originPort,
    date: trip.date,
    status: trip.status,
    nullDeclaration: trip.nullDeclaration,
    captures: trip.captures.map((capture) => ({
      speciesId: capture.speciesId,
      quantity: capture.quantity,
      weightKg: capture.weightKg,
      averageSizeCm: capture.averageSizeCm,
    })),
    landingLatitude: trip.landingLatitude ?? 0,
    landingLongitude: trip.landingLongitude ?? 0,
  };
}

export interface SyncResult {
  ok: boolean;
  error?: string;
  syncedCount: number;
}

/**
 * AC-03.1: sincroniza las faenas guardadas sin conexión. La usan tanto el botón manual de
 * `TripsPage` como el disparo automático al recuperar conectividad (`AppLayout`), para no
 * duplicar el mapeo `Trip -> PendingTripPayload` ni la limpieza del store.
 */
export async function syncPendingTrips(queryClient: QueryClient): Promise<SyncResult> {
  const pending = getPendingTrips();
  if (pending.length === 0) {
    return { ok: true, syncedCount: 0 };
  }

  try {
    await domainService.sync(pending.map(toPendingTripPayload));
    clearPendingTrips();
    await queryClient.invalidateQueries({ queryKey: ['trips'] });
    await queryClient.invalidateQueries({ queryKey: ['summary'] });
    return { ok: true, syncedCount: pending.length };
  } catch (error) {
    // Si el servidor rechaza la faena (veda, talla, permiso, campo faltante) se conserva en el
    // dispositivo para que el usuario pueda corregirla en lugar de perder el registro.
    return { ok: false, error: toAppError(error).message, syncedCount: 0 };
  }
}
