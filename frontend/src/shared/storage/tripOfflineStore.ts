import type { Trip } from '../api/domain.service';

const KEY = 'sigepes-pending-trips';

export function getPendingTrips(): Trip[] {
  try {
    return JSON.parse(localStorage.getItem(KEY) ?? '[]') as Trip[];
  } catch {
    return [];
  }
}

export function savePendingTrip(trip: Trip): void {
  const trips = getPendingTrips().filter((item) => item.id !== trip.id);
  localStorage.setItem(KEY, JSON.stringify([...trips, trip]));
}

export function clearPendingTrips(): void {
  localStorage.removeItem(KEY);
}

export function pendingTripCount(): number {
  return getPendingTrips().length;
}
