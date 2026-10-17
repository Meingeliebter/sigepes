package com.sigepes.backend.audit;

/**
 * AC-20.2 · Clasificación del evento auditado.
 *
 * <p>{@code SECURITY} cubre eventos de autenticación (login/logout) que no son un CRUD sobre una
 * entidad de negocio, pero que igual deben quedar clasificados de forma explícita en vez de
 * forzarlos incorrectamente a CREATE/UPDATE/DELETE.
 */
public enum AuditActionType {
    CREATE,
    UPDATE,
    DELETE,
    SECURITY
}
