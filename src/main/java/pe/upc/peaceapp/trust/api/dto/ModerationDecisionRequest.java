package pe.upc.peaceapp.trust.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.upc.peaceapp.trust.domain.model.ModerationDecision;

public record ModerationDecisionRequest(@NotNull ModerationDecision.Decision decision, String reason) {
}
