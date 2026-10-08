package pe.upc.peaceapp.trust.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.trust.api.dto.AppealRequest;
import pe.upc.peaceapp.trust.api.dto.EvaluateIncidentRequest;
import pe.upc.peaceapp.trust.api.dto.ModerationDecisionRequest;
import pe.upc.peaceapp.trust.api.dto.TrustCaseResponse;
import pe.upc.peaceapp.trust.application.TrustService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trust")
public class TrustController {

    private final TrustService trustService;

    public TrustController(TrustService trustService) {
        this.trustService = trustService;
    }

    /** US-14, US-15: evaluar un incidente recien reportado (lo invoca incident-service). */
    @PostMapping("/evaluate")
    @ResponseStatus(HttpStatus.CREATED)
    public TrustCaseResponse evaluate(@Valid @RequestBody EvaluateIncidentRequest request) {
        return TrustCaseResponse.from(trustService.evaluate(request));
    }

    @GetMapping("/cases/{id}")
    public TrustCaseResponse getById(@PathVariable UUID id) {
        return TrustCaseResponse.from(trustService.getById(id));
    }

    /** US-16: cola de moderacion (rol MODERADOR o ADMINISTRADOR). */
    @GetMapping("/moderation-queue")
    public List<TrustCaseResponse> getModerationQueue() {
        return trustService.getModerationQueue().stream().map(TrustCaseResponse::from).toList();
    }

    /** US-16: decision de moderacion. */
    @PostMapping("/moderation-queue/{id}/decision")
    public TrustCaseResponse decide(Authentication authentication, @PathVariable UUID id,
                                     @Valid @RequestBody ModerationDecisionRequest request) {
        var trustCase = trustService.decide(id, currentUserId(authentication), request.decision(), request.reason());
        return TrustCaseResponse.from(trustCase);
    }

    /** US-17: apelacion del autor sobre un caso rechazado. */
    @PostMapping("/cases/{id}/appeal")
    @ResponseStatus(HttpStatus.CREATED)
    public TrustCaseResponse appeal(Authentication authentication, @PathVariable UUID id,
                                     @Valid @RequestBody AppealRequest request) {
        trustService.appeal(id, currentUserId(authentication), request.justification());
        return TrustCaseResponse.from(trustService.getById(id));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
