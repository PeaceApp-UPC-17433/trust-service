package pe.upc.peaceapp.trust.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AppealRequest(@NotBlank String justification) {
}
