package pe.upc.peaceapp.trust.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "trust.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH = 50;

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;
    private final String exchange;

    public OutboxRelay(JdbcTemplate jdbc, RabbitTemplate rabbit, TransactionTemplate tx,
                       @Value("${trust.messaging.exchange}") String exchange) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.tx = tx;
        this.exchange = exchange;
    }

    record Pending(UUID id, String routingKey, String payload) { }

    @Scheduled(fixedDelayString = "${trust.outbox.poll-interval-ms:1000}")
    public void relay() {
        try {
            int published;
            do {
                published = tx.execute(status -> publishBatch());
            } while (published == BATCH);
        } catch (RuntimeException e) {
            log.warn("Outbox: no se pudo publicar ({}). Se reintenta.", e.getMessage());
        }
    }

    private int publishBatch() {
        List<Pending> batch = jdbc.query("select id, routing_key, payload from outbox_events "
                        + "where published_at is null order by created_at limit ? for update skip locked",
                (rs, i) -> new Pending(rs.getObject("id", UUID.class), rs.getString("routing_key"), rs.getString("payload")),
                BATCH);
        for (Pending p : batch) {
            var message = MessageBuilder.withBody(p.payload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(p.id().toString()).build();
            boolean confirmed = Boolean.TRUE.equals(rabbit.invoke(ops -> {
                ops.send(exchange, p.routingKey(), message);
                return ops.waitForConfirms(TimeUnit.SECONDS.toMillis(5));
            }));
            if (!confirmed) throw new IllegalStateException("El broker no confirmo el evento " + p.id());
            jdbc.update("update outbox_events set published_at = ? where id = ?", Timestamp.from(Instant.now()), p.id());
        }
        return batch.size();
    }
}

