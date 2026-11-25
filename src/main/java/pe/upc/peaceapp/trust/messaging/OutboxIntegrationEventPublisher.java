package pe.upc.peaceapp.trust.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OutboxIntegrationEventPublisher implements IntegrationEventPublisher {
    private final OutboxEventRepository repository;
    private final ObjectMapper json;

    public OutboxIntegrationEventPublisher(OutboxEventRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String routingKey, Object payload) {
        try {
            repository.save(new OutboxEvent(routingKey, json.writeValueAsString(enrich(payload))));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + routingKey, e);
        }
    }

    private static Object enrich(Object payload) {
        if (!(payload instanceof Map<?, ?> original)) return payload;
        var event = new LinkedHashMap<String, Object>();
        original.forEach((k, v) -> event.put(String.valueOf(k), v));
        event.putIfAbsent("schemaVersion", 1);
        String requestId = MDC.get("requestId");
        if (requestId != null) event.putIfAbsent("correlationId", requestId);
        return event;
    }
}
