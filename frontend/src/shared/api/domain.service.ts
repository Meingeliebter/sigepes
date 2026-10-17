import { httpClient } from './httpClient';
import type { Role } from '../../modules/auth/types/auth.types';

export interface Vessel {
  id: string;
  name: string;
  registration: string;
  port: string;
  gears: string[];
  gearIds?: string[];
  status: string;
  ownerId: string;
  ownerName?: string;
  type?: string | null;
  capacityTons?: number | null;
}

export interface Gear {
  id: string;
  name: string;
  description?: string | null;
  destructive: boolean;
  status: string;
}

export interface Species {
  id: string;
  commonName: string;
  scientificName: string;
  minSizeCm: number;
  status: string;
  underClosure?: boolean;
}

export interface Closure {
  id: string;
  speciesId: string;
  species: string;
  zone: string;
  from: string;
  to: string;
  status: string;
  inForce?: boolean;
  reason?: string | null;
  notes?: string | null;
}

export type PermitStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | 'EXPIRED';

export interface Permit {
  id: string;
  number: string;
  type: string;
  holder: string;
  issued: string;
  expires: string;
  status: string;
  ownerId: string;
  reason?: string | null;
  verificationCode?: string;
  expiringSoon?: boolean;
  daysToExpire?: number;
}

export interface Capture {
  id: string;
  speciesId: string;
  species: string;
  quantity?: number | null;
  weightKg: number;
  averageSizeCm: number;
}

export interface Trip {
  id: string;
  vesselId: string;
  vessel: string;
  zone: string;
  date: string;
  status: string;
  nullDeclaration: boolean;
  captures: Capture[];
  ownerId: string;
  ownerName?: string;
  gear?: string | null;
  permitNumber?: string | null;
  notes?: string | null;
  totalWeightKg?: number;
}

export interface AuditEvent {
  id: string;
  timestamp: string;
  occurredAt?: string;
  actor: string;
  actorId?: string | null;
  action: string;
  actionType: 'CREATE' | 'UPDATE' | 'DELETE' | 'SECURITY';
  module?: string;
  entity: string;
  entityId?: string | null;
  detail: string;
  ip?: string | null;
  beforeState?: string | null;
  afterState?: string | null;
}

export interface AuditFilters {
  module?: string;
  actionType?: string;
  actorId?: string;
  from?: string;
  to?: string;
  limit?: number;
}

export interface UserRow {
  id: string;
  fullName: string;
  email: string;
  nationalId: string;
  role: Role;
  status: string;
}

export interface Stat {
  value: number;
  helper: string;
}

export interface Activity {
  id: string;
  title: string;
  status: string;
  date: string;
}

export interface ComplianceRow {
  label: string;
  status: string;
}

export interface DashboardSummary {
  fullName: string;
  alerts: string[];
  vessels: Stat;
  permits: Stat;
  trips: Stat;
  warnings: Stat;
  recent: Activity[];
  compliance: ComplianceRow[];
}

export interface Notification {
  id: string;
  type: 'CLOSURE' | 'PERMIT' | 'TRIP' | 'INFO';
  title: string;
  body: string;
  date: string;
}

/** Cuerpo de una faena pendiente de sincronizar (registro offline). */
export interface PendingTripPayload {
  id: string;
  vesselId: string;
  zone: string;
  date: string;
  status: string;
  nullDeclaration: boolean;
  captures: { speciesId: string; weightKg: number; averageSizeCm: number }[];
}

export const domainService = {
  // --- Embarcaciones y artes (RF-02) ---------------------------------
  async vessels(): Promise<Vessel[]> {
    return (await httpClient.get<Vessel[]>('/embarcaciones')).data;
  },
  async createVessel(payload: {
    name: string;
    registration: string;
    port: string;
    gears: string[];
    status?: string;
  }): Promise<Vessel> {
    return (await httpClient.post<Vessel>('/embarcaciones', payload)).data;
  },
  async gears(): Promise<Gear[]> {
    return (await httpClient.get<Gear[]>('/artes-pesca')).data;
  },

  // --- Especies y vedas (RF-06 / RF-05) -------------------------------
  async species(): Promise<Species[]> {
    return (await httpClient.get<Species[]>('/especies')).data;
  },
  async closures(): Promise<Closure[]> {
    return (await httpClient.get<Closure[]>('/vedas')).data;
  },
  /** Solo las vedas vigentes hoy; se usa para avisar en el formulario de faena. */
  async activeClosures(): Promise<Closure[]> {
    return (await httpClient.get<Closure[]>('/vedas/activas')).data;
  },

  // --- Permisos (RF-08) -----------------------------------------------
  async permits(): Promise<Permit[]> {
    return (await httpClient.get<Permit[]>('/permisos')).data;
  },
  async createPermit(payload: { type: string; issued?: string; expires?: string }): Promise<Permit> {
    return (await httpClient.post<Permit>('/permisos', payload)).data;
  },
  async evaluatePermit(
    permitId: string,
    status: PermitStatus,
    reason?: string,
  ): Promise<Permit> {
    return (await httpClient.patch<Permit>(`/permisos/${permitId}/evaluar`, { status, reason })).data;
  },

  // --- Faenas y capturas (RF-03) --------------------------------------
  async trips(): Promise<Trip[]> {
    return (await httpClient.get<Trip[]>('/faenas')).data;
  },
  async startTrip(payload: { id?: string; vesselId: string; zone: string; date?: string }): Promise<Trip> {
    return (await httpClient.post<Trip>('/faenas', payload)).data;
  },
  async addCapture(
    tripId: string,
    payload: { speciesId: string; weightKg: number; averageSizeCm: number; quantity?: number },
  ): Promise<Trip> {
    return (await httpClient.post<Trip>(`/faenas/${tripId}/capturas`, payload)).data;
  },
  async closeTrip(tripId: string, nullDeclaration: boolean): Promise<Trip> {
    return (await httpClient.post<Trip>(`/faenas/${tripId}/cerrar`, { nullDeclaration })).data;
  },
  async sync(pending: PendingTripPayload[]): Promise<Trip[]> {
    return (await httpClient.post<Trip[]>('/faenas/sincronizar', pending)).data;
  },

  // --- Administración y auditoría (RF-20) -----------------------------
  async users(): Promise<UserRow[]> {
    return (await httpClient.get<UserRow[]>('/usuarios')).data;
  },
  async createUser(payload: {
    nationalId: string;
    fullName: string;
    email: string;
    role: Role;
    password: string;
  }): Promise<UserRow> {
    return (await httpClient.post<UserRow>('/usuarios', payload)).data;
  },
  /** AC-20.4: filtros opcionales de fecha, tipo de acción, módulo y usuario. */
  async audit(filters?: AuditFilters): Promise<AuditEvent[]> {
    return (await httpClient.get<AuditEvent[]>('/auditoria', { params: filters })).data;
  },

  // --- Inicio y notificaciones ----------------------------------------
  async summary(): Promise<DashboardSummary> {
    return (await httpClient.get<DashboardSummary>('/resumen')).data;
  },
  async notifications(): Promise<Notification[]> {
    return (await httpClient.get<Notification[]>('/notificaciones')).data;
  },
};
