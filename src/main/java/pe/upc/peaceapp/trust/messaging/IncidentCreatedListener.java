package pe.upc.peaceapp.trust.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.upc.peaceapp.trust.api.dto.EvaluateIncidentRequest;
import pe.upc.peaceapp.trust.application.TrustService;
import pe.upc.peaceapp.trust.observability.EventContext;

import java.time.Instant;
import java.util.UUID;

@Component
public class IncidentCreatedListener {
    private static final Logger log = LoggerFactory.getLogger(IncidentCreatedListener.class);

    private final TrustService trustService;

    public IncidentCreatedListener(TrustService trustService) {
        this.trustService = trustService;
    }

    public record IncidentCreatedMessage(UUID eventId, String correlationId, Instant occurredAt, UUID incidentId,
                                         UUID authorId, String type, Double latitude, Double longitude,
                                         String description, Boolean hasEvidence) { }

    @RabbitListener(queues = RabbitConfig.INCIDENT_CREATED_QUEUE)
    public void on(IncidentCreatedMessage m) {
        EventContext.run(m.correlationId(), m.eventId(), () -> {
            if (m.incidentId() == null || m.authorId() == null || m.type() == null || m.latitude() == null
                    || m.longitude() == null) {
                log.warn("incident.created invalido eventId={}", m.eventId());
                throw new AmqpRejectAndDontRequeueException("incident.created invalido");
            }
            var request = new EvaluateIncidentRequest(m.incidentId(), m.authorId(), m.type(), m.latitude(),
                    m.longitude(), m.occurredAt() != null ? m.occurredAt() : Instant.now(), m.description(),
                    Boolean.TRUE.equals(m.hasEvidence()), 0);
            var evaluated = trustService.evaluateOnce(request);
            log.info("incident.created incidentId={} resultado={}", m.incidentId(),
                    evaluated.map(c -> c.getStatus().name()).orElse("ya evaluado"));
        });
    }
}
