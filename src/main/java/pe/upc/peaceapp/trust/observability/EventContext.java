package pe.upc.peaceapp.trust.observability;

import org.slf4j.MDC;

import java.util.UUID;

public final class EventContext {
    private EventContext() {
    }

    public static void run(String correlationId, UUID eventId, Runnable action) {
        String id = correlationId != null && !correlationId.isBlank() ? correlationId
                : eventId != null ? eventId.toString() : UUID.randomUUID().toString();
        MDC.put(RequestCorrelationFilter.REQUEST_ID_KEY, id);
        try {
            action.run();
        } finally {
            MDC.remove(RequestCorrelationFilter.REQUEST_ID_KEY);
        }
    }
}
