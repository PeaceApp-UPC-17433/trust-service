package pe.upc.peaceapp.trust.api.dto;

import pe.upc.peaceapp.trust.domain.model.TrustCase;
import pe.upc.peaceapp.trust.domain.model.TrustStatus;

import java.time.Instant;
import java.util.UUID;

public record TrustCaseResponse(
        UUID id,
        UUID incidentId,
        String type,
        TrustStatus status,
        double falsenessScore,
        UUID duplicateOfCaseId,
        Instant createdAt
) {
    public static TrustCaseResponse from(TrustCase trustCase) {
        return new TrustCaseResponse(
                trustCase.getId(),
                trustCase.getIncidentId(),
                trustCase.getType(),
                trustCase.getStatus(),
                trustCase.getFalsenessScore(),
                trustCase.getDuplicateOfCaseId(),
                trustCase.getCreatedAt());
    }
}
