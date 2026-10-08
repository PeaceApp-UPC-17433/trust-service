package pe.upc.peaceapp.trust.api.error;

/** US-16: se intenta decidir un caso que no esta en la cola de moderacion. */
public class InvalidModerationStateException extends RuntimeException {
    public InvalidModerationStateException(String message) {
        super(message);
    }
}
