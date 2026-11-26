package pe.upc.peaceapp.trust.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxIntegrationEventPublisherTest {
    @Mock
    private OutboxEventRepository repository;

    @AfterEach
    void clear() {
        MDC.clear();
    }

    private String publishAndReadPayload(Object payload) {
        new OutboxIntegrationEventPublisher(repository, new ObjectMapper()).publish("trust.case", payload);
        var captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(ReflectionTestUtils.getField(captor.getValue(), "routingKey")).isEqualTo("trust.case");
        return (String) ReflectionTestUtils.getField(captor.getValue(), "payload");
    }

    @Test
    void addsSchemaVersionAndCorrelationId() {
        MDC.put("requestId", "req-12345678");
        var payload = publishAndReadPayload(Map.of("caseId", "c1"));
        assertThat(payload).contains("\"schemaVersion\":1").contains("\"correlationId\":\"req-12345678\"")
                .contains("\"caseId\":\"c1\"");
    }

    @Test
    void omitsCorrelationIdWhenThereIsNoRequest() {
        assertThat(publishAndReadPayload(Map.of("caseId", "c1"))).doesNotContain("correlationId");
    }

    @Test
    void serializesNonMapPayloadsAsIs() {
        assertThat(publishAndReadPayload(java.util.List.of("a", "b"))).isEqualTo("[\"a\",\"b\"]");
    }
}
