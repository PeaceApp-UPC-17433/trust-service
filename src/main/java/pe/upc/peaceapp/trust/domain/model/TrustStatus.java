package pe.upc.peaceapp.trust.domain.model;

/**
 * US-14, US-15, US-16: resultado de la evaluacion de confiabilidad de un reporte.
 * CLEARED = sigue su flujo normal de validacion comunitaria (fuera de este servicio).
 */
public enum TrustStatus {
    CLEARED,
    DUPLICATE,
    HELD_FOR_MODERATION,
    APPROVED,
    REJECTED
}
