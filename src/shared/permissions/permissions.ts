/**
 * Modelo RBAC de SIGEPES.
 * Roles y capacidades derivados de los actores y reglas de negocio del proyecto.
 */
export type Role =
  | 'FISHER' // Pescador artesanal
  | 'SHIPOWNER' // Armador
  | 'INSPECTOR' // Inspector AUNAP
  | 'ANALYST' // Analista de sostenibilidad
  | 'BUYER' // Comprador / comercializador
  | 'ADMIN'; // Administrador del sistema

export type Permission =
  | 'trip:create'
  | 'trip:update'
  | 'trip:close'
  | 'vessel:manage'
  | 'permit:request'
  | 'permit:review'
  | 'permit:approve'
  | 'permit:reject'
  | 'species:manage'
  | 'closure:manage'
  | 'quota:manage'
  | 'traceability:read'
  | 'complaint:manage'
  | 'dashboard:read'
  | 'report:read'
  | 'audit:read'
  | 'user:manage';

const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  FISHER: [
    'trip:create',
    'trip:update',
    'trip:close',
    'vessel:manage',
    'permit:request',
    'traceability:read',
  ],
  SHIPOWNER: [
    'trip:create',
    'trip:update',
    'trip:close',
    'vessel:manage',
    'permit:request',
    'quota:manage',
    'traceability:read',
  ],
  INSPECTOR: [
    'permit:review',
    'permit:approve',
    'permit:reject',
    'complaint:manage',
    'dashboard:read',
    'report:read',
  ],
  ANALYST: [
    'species:manage',
    'closure:manage',
    'quota:manage',
    'dashboard:read',
    'report:read',
  ],
  BUYER: ['traceability:read'],
  ADMIN: [
    'user:manage',
    'species:manage',
    'closure:manage',
    'quota:manage',
    'audit:read',
    'report:read',
    'dashboard:read',
  ],
};

export function getPermissionsForRoles(roles: Role[]): Set<Permission> {
  const result = new Set<Permission>();
  for (const role of roles) {
    for (const permission of ROLE_PERMISSIONS[role] ?? []) {
      result.add(permission);
    }
  }
  return result;
}

export function hasPermission(
  roles: Role[],
  permission: Permission,
): boolean {
  return getPermissionsForRoles(roles).has(permission);
}
