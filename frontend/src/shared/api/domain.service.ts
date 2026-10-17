import { httpClient } from './httpClient';
import type { Role } from '../../modules/auth/types/auth.types';
import { cacheSpecies, getCachedSpecies } from '../storage/speciesOfflineStore';
import { cacheQuotas, getCachedQuotas } from '../storage/quotaOfflineStore';
import { cacheZones, getCachedZones } from '../storage/zoneOfflineStore';

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
  hullType: string;
  enginePower: number;
  capacityTons: number;
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
  measurementMethod: string;
  region: string;
  imageUrl: string;
  status: string;
  underClosure?: boolean;
}

/** AC-06.4: una fila del historial de versiones de una especie. */
export interface SpeciesVersion {
  version: number;
  commonName: string;
  scientificName: string;
  minSizeCm: number;
  measurementMethod: string;
  region: string;
  imageUrl: string;
  status: string;
  changedByLabel: string;
  changedAt: string;
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
  resolutionNumber: string;
  closureType: string;
  notes?: string | null;
}

/** RF-07: cuota de captura con su saldo. */
export interface Quota {
  id: string;
  speciesId: string;
  species: string;
  zone: string;
  from: string;
  to: string;
  totalKg: number;
  consumedKg: number;
  remainingKg: number;
  consumedPercent: number;
  status: 'ACTIVE' | 'CLOSED';
  vesselId?: string | null;
  vessel?: string | null;
  resolutionNumber?: string | null;
  notes?: string | null;
}

/** RF-04: zona restringida (AMP o zona en veda) con su polígono [[lat, lon], ...]. */
export interface RestrictedZone {
  id: string;
  name: string;
  zoneType: 'CLOSURE_ZONE' | 'MPA' | 'OTHER';
  polygon: [number, number][];
  resolutionNumber: string;
  description?: string | null;
  active: boolean;
}

/** AC-04.1: posición GPS capturada en el dispositivo. El id lo genera el cliente (idempotencia). */
export interface PositionInput {
  id: string;
  latitude: number;
  longitude: number;
  accuracyMeters?: number | null;
  recordedAt: string;
}

export interface TripPosition extends PositionInput {
  insideZoneId?: string | null;
  insideZone?: string | null;
}

export interface ZoneEvent {
  id: string;
  tripId: string;
  zoneId: string;
  zone: string;
  zoneType: string;
  resolutionNumber: string;
  ownerName: string;
  vessel: string;
  latitude: number;
  longitude: number;
  occurredAt: string;
  status: 'OPEN' | 'REVIEWED';
}

export interface PositionBatchResult {
  stored: number;
  duplicates: number;
  intrusions: ZoneEvent[];
}

export type PermitStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | 'EXPIRED';

export interface PermitAttachment {
  kind: 'ID_COPY' | 'VESSEL_OWNERSHIP';
  originalFilename: string;
  contentType: string;
}

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
  attachments: PermitAttachment[];
}

export interface Capture {
  id: string;
  speciesId: string;
  species: string;
  quantity: number;
  weightKg: number;
  averageSizeCm: number;
  minSizeCmAtCapture?: number;
}

export interface Trip {
  id: string;
  vesselId: string;
  vessel: string;
  zone: string;
  originPort: string;
  date: string;
  status: string;
  nullDeclaration: boolean;
  captures: Capture[];
  ownerId: string;
  ownerName?: string;
  gear?: string | null;
  permitNumber?: string | null;
  departureTime?: string | null;
  arrivalTime?: string | null;
  notes?: string | null;
  landingLatitude?: number | null;
  landingLongitude?: number | null;
  totalWeightKg?: number;
  /** AC-04.6: recorrido capturado sin conexión, se envía al sincronizar. */
  positions?: PositionInput[];
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
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
}

/** Cuerpo de una faena pendiente de sincronizar (registro offline). */
export interface PendingTripPayload {
  id: string;
  vesselId: string;
  zone: string;
  originPort: string;
  date: string;
  status: string;
  nullDeclaration: boolean;
  captures: { speciesId: string; quantity: number; weightKg: number; averageSizeCm: number }[];
  landingLatitude: number;
  landingLongitude: number;
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
    hullType: string;
    enginePower: number;
    capacityTons: number;
    gears: string[];
    status?: string;
  }): Promise<Vessel> {
    return (await httpClient.post<Vessel>('/embarcaciones', payload)).data;
  },
  /** AC-02.5: perfil completo de una embarcación (usado por el escaneo QR de campo). */
  async vesselDetail(id: string): Promise<Vessel> {
    return (await httpClient.get<Vessel>(`/embarcaciones/${id}`)).data;
  },
  async gears(): Promise<Gear[]> {
    return (await httpClient.get<Gear[]>('/artes-pesca')).data;
  },
  async createGear(payload: { name: string; description?: string; destructive: boolean; status?: string }): Promise<Gear> {
    return (await httpClient.post<Gear>('/artes-pesca', payload)).data;
  },

  // --- Especies y vedas (RF-06 / RF-05) -------------------------------
  /** AC-06.3: cachea el catálogo en cada fetch exitoso y cae a la copia local sin conexión. */
  async species(): Promise<Species[]> {
    try {
      const data = (await httpClient.get<Species[]>('/especies')).data;
      cacheSpecies(data);
      return data;
    } catch (error) {
      const cached = getCachedSpecies();
      if (cached) return cached;
      throw error;
    }
  },
  async createSpecies(payload: {
    commonName: string;
    scientificName: string;
    minSizeCm: number;
    measurementMethod: string;
    region: string;
    imageUrl: string;
    status?: string;
  }): Promise<Species> {
    return (await httpClient.post<Species>('/especies', payload)).data;
  },
  async updateSpecies(
    id: string,
    payload: Partial<{
      commonName: string;
      scientificName: string;
      minSizeCm: number;
      measurementMethod: string;
      region: string;
      imageUrl: string;
      status: string;
    }>,
  ): Promise<Species> {
    return (await httpClient.patch<Species>(`/especies/${id}`, payload)).data;
  },
  /** AC-06.4: historial de versiones de una especie. */
  async speciesVersions(id: string): Promise<SpeciesVersion[]> {
    return (await httpClient.get<SpeciesVersion[]>(`/especies/${id}/versiones`)).data;
  },
  async closures(): Promise<Closure[]> {
    return (await httpClient.get<Closure[]>('/vedas')).data;
  },
  /** Solo las vedas vigentes hoy; se usa para avisar en el formulario de faena. */
  async activeClosures(): Promise<Closure[]> {
    return (await httpClient.get<Closure[]>('/vedas/activas')).data;
  },
  async createClosure(payload: {
    speciesId: string;
    zone: string;
    from: string;
    to: string;
    resolutionNumber: string;
    closureType: string;
    reason?: string;
    notes?: string;
  }): Promise<Closure> {
    return (await httpClient.post<Closure>('/vedas', payload)).data;
  },
  async updateClosure(
    id: string,
    payload: Partial<{
      status: string;
      from: string;
      to: string;
      resolutionNumber: string;
      closureType: string;
      notes: string;
    }>,
  ): Promise<Closure> {
    return (await httpClient.patch<Closure>(`/vedas/${id}`, payload)).data;
  },

  // --- Cuotas (RF-07) -------------------------------------------------
  /** AC-07.6: cachea las cuotas para validar capturas sin conexión con el último saldo conocido. */
  async quotas(): Promise<Quota[]> {
    try {
      const data = (await httpClient.get<Quota[]>('/cuotas')).data;
      cacheQuotas(data);
      return data;
    } catch (error) {
      const cached = getCachedQuotas();
      if (cached) return cached;
      throw error;
    }
  },
  async createQuota(payload: {
    speciesId: string;
    zone: string;
    from: string;
    to: string;
    totalKg: number;
    vesselId?: string;
    resolutionNumber?: string;
    notes?: string;
  }): Promise<Quota> {
    return (await httpClient.post<Quota>('/cuotas', payload)).data;
  },
  async updateQuota(
    id: string,
    payload: Partial<{ totalKg: number; to: string; status: 'ACTIVE' | 'CLOSED'; notes: string }>,
  ): Promise<Quota> {
    return (await httpClient.patch<Quota>(`/cuotas/${id}`, payload)).data;
  },

  // --- Geolocalización (RF-04) ----------------------------------------
  /** AC-04.6: las zonas se cachean para comprobar intrusiones en el propio dispositivo sin señal. */
  async zones(): Promise<RestrictedZone[]> {
    try {
      const data = (await httpClient.get<RestrictedZone[]>('/zonas-restringidas')).data;
      cacheZones(data);
      return data;
    } catch (error) {
      const cached = getCachedZones();
      if (cached) return cached;
      throw error;
    }
  },
  async createZone(payload: {
    name: string;
    zoneType: RestrictedZone['zoneType'];
    polygon: [number, number][];
    resolutionNumber: string;
    description?: string;
  }): Promise<RestrictedZone> {
    return (await httpClient.post<RestrictedZone>('/zonas-restringidas', payload)).data;
  },
  async updateZone(id: string, payload: { active?: boolean; description?: string }): Promise<RestrictedZone> {
    return (await httpClient.patch<RestrictedZone>(`/zonas-restringidas/${id}`, payload)).data;
  },
  async postPositions(tripId: string, positions: PositionInput[]): Promise<PositionBatchResult> {
    return (await httpClient.post<PositionBatchResult>(`/faenas/${tripId}/posiciones`, { positions })).data;
  },
  async tripPositions(tripId: string): Promise<TripPosition[]> {
    return (await httpClient.get<TripPosition[]>(`/faenas/${tripId}/posiciones`)).data;
  },
  async tripZoneEvents(tripId: string): Promise<ZoneEvent[]> {
    return (await httpClient.get<ZoneEvent[]>(`/faenas/${tripId}/eventos-zona`)).data;
  },
  async zoneEvents(): Promise<ZoneEvent[]> {
    return (await httpClient.get<ZoneEvent[]>('/eventos-zona')).data;
  },
  async reviewZoneEvent(id: string): Promise<ZoneEvent> {
    return (await httpClient.patch<ZoneEvent>(`/eventos-zona/${id}/revisar`)).data;
  },

  // --- Permisos (RF-08) -----------------------------------------------
  async permits(): Promise<Permit[]> {
    return (await httpClient.get<Permit[]>('/permisos')).data;
  },
  /** AC-08.5: la copia de cédula y el certificado de propiedad son obligatorios. */
  async createPermit(
    payload: { type: string; issued?: string; expires?: string },
    idDocument: File,
    vesselOwnershipDocument: File,
  ): Promise<Permit> {
    const form = new FormData();
    form.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    form.append('idDocument', idDocument);
    form.append('vesselOwnershipDocument', vesselOwnershipDocument);
    return (await httpClient.post<Permit>('/permisos', form)).data;
  },
  async evaluatePermit(
    permitId: string,
    status: PermitStatus,
    reason?: string,
  ): Promise<Permit> {
    return (await httpClient.patch<Permit>(`/permisos/${permitId}/evaluar`, { status, reason })).data;
  },
  /** AC-08.4: verifica un permiso por número o por el código escaneado en su QR. */
  async validatePermit(numberOrCode: string): Promise<Permit> {
    return (await httpClient.get<Permit>(`/permisos/validar/${encodeURIComponent(numberOrCode)}`)).data;
  },
  /**
   * AC-08.5: descarga un adjunto. El endpoint exige el token (como todo `/api/**`), así que no se
   * puede usar un `<a href>` plano — se trae como blob y se dispara la descarga en el navegador.
   */
  async downloadAttachment(permitId: string, kind: PermitAttachment['kind'], filename: string): Promise<void> {
    const response = await httpClient.get(`/permisos/${permitId}/adjuntos/${kind}`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(response.data as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  // --- Faenas y capturas (RF-03) --------------------------------------
  async trips(): Promise<Trip[]> {
    return (await httpClient.get<Trip[]>('/faenas')).data;
  },
  async startTrip(payload: { id?: string; vesselId: string; zone: string; originPort: string; date?: string }): Promise<Trip> {
    return (await httpClient.post<Trip>('/faenas', payload)).data;
  },
  async addCapture(
    tripId: string,
    payload: { speciesId: string; weightKg: number; averageSizeCm: number; quantity: number },
  ): Promise<Trip> {
    return (await httpClient.post<Trip>(`/faenas/${tripId}/capturas`, payload)).data;
  },
  async closeTrip(
    tripId: string,
    payload: { nullDeclaration: boolean; landingLatitude: number; landingLongitude: number },
  ): Promise<Trip> {
    return (await httpClient.post<Trip>(`/faenas/${tripId}/cerrar`, payload)).data;
  },
  async sync(pending: PendingTripPayload[]): Promise<Trip[]> {
    return (await httpClient.post<Trip[]>('/faenas/sincronizar', pending)).data;
  },
  /** AC-03.5: corrige una faena; si ya está cerrada, `reason` es obligatorio (lo valida el backend). */
  async correctTrip(
    id: string,
    payload: { zone?: string; arrivalTime?: string; notes?: string; reason?: string },
  ): Promise<Trip> {
    const { zone, ...body } = payload;
    const query = zone ? `?zone=${encodeURIComponent(zone)}` : '';
    return (await httpClient.patch<Trip>(`/faenas/${id}${query}`, body)).data;
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
