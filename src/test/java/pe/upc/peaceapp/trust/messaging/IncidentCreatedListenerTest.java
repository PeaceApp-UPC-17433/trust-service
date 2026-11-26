package pe.upc.peaceapp.trust.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import pe.upc.peaceapp.trust.api.dto.EvaluateIncidentRequest;
import pe.upc.peaceapp.trust.application.TrustService;
import pe.upc.peaceapp.trust.messaging.IncidentCreatedListener.IncidentCreatedMessage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentCreatedListenerTest {
    @Mock
    private TrustService trustService;

    private IncidentCreatedListener listener() {
        return new IncidentCreatedListener(trustService);
    }

    private static IncidentCreatedMessage message(UUID incidentId, Boolean hasEvidence, Instant at) {
        return new IncidentCreatedMessage(UUID.randomUUID(), "corr-1", at, incidentId, UUID.randomUUID(),
                "ROBO", -12.05, -77.04, "detalle", hasEvidence);
    }

    @Test
    void evaluatesValidMessageWithDefaults() {
        var id = UUID.randomUUID();
        when(trustService.evaluateOnce(any())).thenReturn(Optional.empty());
        listener().on(message(id, null, null));
        var captor = ArgumentCaptor.forClass(EvaluateIncidentRequest.class);
        verify(trustService).evaluateOnce(captor.capture());
        assertThat(captor.getValue().incidentId()).isEqualTo(id);
        assertThat(captor.getValue().hasEvidence()).isFalse();
        assertThat(captor.getValue().occurredAt()).isNotNull();
    }

    @Test
    void keepsEvidenceFlagAndTimestamp() {
        var at = Instant.parse("2026-01-01T10:00:00Z");
        when(trustService.evaluateOnce(any())).thenReturn(Optional.empty());
        listener().on(message(UUID.randomUUID(), true, at));
        var captor = ArgumentCaptor.forClass(EvaluateIncidentRequest.class);
        verify(trustService).evaluateOnce(captor.capture());
        assertThat(captor.getValue().hasEvidence()).isTrue();
        assertThat(captor.getValue().occurredAt()).isEqualTo(at);
    }

    @Test
    void rejectsInvalidMessageWithoutRequeue() {
        assertThatThrownBy(() -> listener().on(message(null, false, null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verify(trustService, never()).evaluateOnce(any());
    }
}
