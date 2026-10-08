package pe.upc.peaceapp.trust.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.trust.api.dto.EvaluateIncidentRequest;
import pe.upc.peaceapp.trust.api.error.AppealWindowClosedException;
import pe.upc.peaceapp.trust.api.error.ForbiddenOperationException;
import pe.upc.peaceapp.trust.api.error.InvalidModerationStateException;
import pe.upc.peaceapp.trust.api.error.TrustCaseNotFoundException;
import pe.upc.peaceapp.trust.domain.model.*;
import pe.upc.peaceapp.trust.domain.repository.AppealRepository;
import pe.upc.peaceapp.trust.domain.repository.ModerationDecisionRepository;
import pe.upc.peaceapp.trust.domain.repository.TrustCaseRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** BC-03 Trust & Moderation: US-14 a US-17. */
@Service
public class TrustService {

    private final TrustCaseRepository trustCaseRepository;
    private final ModerationDecisionRepository moderationDecisionRepository;
    private final AppealRepository appealRepository;
    private final double duplicateRadiusMeters;
    private final long duplicateWindowMinutes;
    private final double falsenessThreshold;
    private final long appealWindowDays;

    public TrustService(TrustCaseRepository trustCaseRepository,
                         ModerationDecisionRepository moderationDecisionRepository,
                         AppealRepository appealRepository,
                         @Value("${trust.duplicate.radius-meters}") double duplicateRadiusMeters,
                         @Value("${trust.duplicate.window-minutes}") long duplicateWindowMinutes,
                         @Value("${trust.moderation.falseness-threshold}") double falsenessThreshold,
                         @Value("${trust.appeal.window-days}") long appealWindowDays) {
        this.trustCaseRepository = trustCaseRepository;
        this.moderationDecisionRepository = moderationDecisionRepository;
        this.appealRepository = appealRepository;
        this.duplicateRadiusMeters = duplicateRadiusMeters;
        this.duplicateWindowMinutes = duplicateWindowMinutes;
        this.falsenessThreshold = falsenessThreshold;
        this.appealWindowDays = appealWindowDays;
    }

    /** US-14: agrupa como duplicado si hay un caso del mismo tipo a menos de 100 m y 30 min. */
    @Transactional
    public TrustCase evaluate(EvaluateIncidentRequest request) {
        Instant windowStart = request.occurredAt().minusSeconds(duplicateWindowMinutes * 60);
        Instant windowEnd = request.occurredAt().plusSeconds(duplicateWindowMinutes * 60);

        TrustCase duplicateOf = trustCaseRepository.findByTypeAndOccurredAtAfter(request.type(), windowStart)
                .stream()
                .filter(candidate -> !candidate.getOccurredAt().isAfter(windowEnd))
                .filter(candidate -> !candidate.getIncidentId().equals(request.incidentId()))
                .filter(candidate -> GeoDistance.meters(candidate.getLatitude(), candidate.getLongitude(),
                        request.latitude(), request.longitude()) <= duplicateRadiusMeters)
                .findFirst()
                .orElse(null);

        TrustCase trustCase = new TrustCase(request.incidentId(), request.authorId(), request.type(),
                request.latitude(), request.longitude(), request.occurredAt());

        if (duplicateOf != null) {
            trustCase.setStatus(TrustStatus.DUPLICATE);
            trustCase.setDuplicateOfCaseId(duplicateOf.getId());
            return trustCaseRepository.save(trustCase);
        }

        double score = falsenessScore(request);
        trustCase.setFalsenessScore(score);
        trustCase.setStatus(score >= falsenessThreshold ? TrustStatus.HELD_FOR_MODERATION : TrustStatus.CLEARED);
        return trustCaseRepository.save(trustCase);
    }

    /**
     * US-15: heuristica provisional mientras no exista el modelo de clasificacion real
     * (similitud de contenido + reputacion, mencionados en 1.1.4 del informe).
     */
    private double falsenessScore(EvaluateIncidentRequest request) {
        double score = 0.0;
        if (request.description() == null || request.description().isBlank()) {
            score += 0.3;
        }
        if (!request.hasEvidence()) {
            score += 0.3;
        }
        if (request.authorReputationScore() < 0) {
            score += 0.4;
        }
        return Math.min(score, 1.0);
    }

    public TrustCase getById(UUID caseId) {
        return trustCaseRepository.findById(caseId).orElseThrow(() -> new TrustCaseNotFoundException(caseId));
    }

    /** US-16: cola de moderacion. */
    public List<TrustCase> getModerationQueue() {
        return trustCaseRepository.findByStatus(TrustStatus.HELD_FOR_MODERATION);
    }

    /** US-16: aprobar o rechazar un caso en la cola de moderacion. */
    @Transactional
    public TrustCase decide(UUID caseId, UUID moderatorId, ModerationDecision.Decision decision, String reason) {
        TrustCase trustCase = getById(caseId);
        if (trustCase.getStatus() != TrustStatus.HELD_FOR_MODERATION) {
            throw new InvalidModerationStateException("El caso no esta en la cola de moderacion");
        }

        moderationDecisionRepository.save(new ModerationDecision(trustCase, moderatorId, decision, reason));

        trustCase.setStatus(decision == ModerationDecision.Decision.APPROVE
                ? TrustStatus.APPROVED : TrustStatus.REJECTED);
        trustCase.setDecidedAt(Instant.now());
        return trustCaseRepository.save(trustCase);
    }

    /** US-17: apelacion del autor dentro de los 7 dias siguientes al rechazo. */
    @Transactional
    public Appeal appeal(UUID caseId, UUID requesterId, String justification) {
        TrustCase trustCase = getById(caseId);

        if (!trustCase.getAuthorId().equals(requesterId)) {
            throw new ForbiddenOperationException("Solo el autor del reporte puede apelar esta decision");
        }
        if (trustCase.getStatus() != TrustStatus.REJECTED) {
            throw new InvalidModerationStateException("Solo se puede apelar un caso rechazado");
        }
        if (trustCase.getDecidedAt() == null
                || Instant.now().isAfter(trustCase.getDecidedAt().plusSeconds(appealWindowDays * 24 * 3600))) {
            throw new AppealWindowClosedException();
        }

        trustCase.setStatus(TrustStatus.HELD_FOR_MODERATION);
        trustCaseRepository.save(trustCase);
        return appealRepository.save(new Appeal(trustCase, requesterId, justification));
    }
}
