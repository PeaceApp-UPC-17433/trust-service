package pe.upc.peaceapp.trust.api.error;

/** US-17: la apelacion se presenta fuera de la ventana de 7 dias. */
public class AppealWindowClosedException extends RuntimeException {
    public AppealWindowClosedException() {
        super("La ventana para apelar esta decision ya expiro");
    }
}
