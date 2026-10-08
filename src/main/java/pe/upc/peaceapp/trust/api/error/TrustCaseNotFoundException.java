package pe.upc.peaceapp.trust.api.error;

import java.util.UUID;

public class TrustCaseNotFoundException extends RuntimeException {
    public TrustCaseNotFoundException(UUID id) {
        super("Caso de confiabilidad no encontrado: " + id);
    }
}
