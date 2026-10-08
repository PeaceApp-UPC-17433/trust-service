package pe.upc.peaceapp.trust.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * US-14, US-15: datos del incidente a evaluar. incident-service la invoca al crear un
 * reporte (sincrono, como sustituto del broker de eventos de CA-01 aun no definido).
 */
public record EvaluateIncidentRequest(
        @NotNull UUID incidentId,
        @NotNull UUID authorId,
        @NotBlank String type,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Instant occurredAt,
        String description,
        boolean hasEvidence,
        int authorReputationScore
) {
}
