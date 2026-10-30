package pe.upc.peaceapp.trust.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.upc.peaceapp.trust.api.dto.EvaluateIncidentRequest;
import pe.upc.peaceapp.trust.api.error.AppealWindowClosedException;
import pe.upc.peaceapp.trust.api.error.InvalidModerationStateException;
import pe.upc.peaceapp.trust.domain.model.ModerationDecision;
import pe.upc.peaceapp.trust.domain.model.TrustCase;
import pe.upc.peaceapp.trust.domain.model.TrustStatus;
import pe.upc.peaceapp.trust.domain.repository.AppealRepository;
import pe.upc.peaceapp.trust.domain.repository.ModerationDecisionRepository;
import pe.upc.peaceapp.trust.domain.repository.TrustCaseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustServiceTest {

    private static final double RADIUS_METERS = 100;
    private static final long WINDOW_MINUTES = 30;
    private static final double FALSENESS_THRESHOLD = 0.6;
    private static final long APPEAL_WINDOW_DAYS = 7;

    @Mock
    private TrustCaseRepository trustCaseRepository;
    @Mock
    private ModerationDecisionRepository moderationDecisionRepository;
    @Mock
    private AppealRepository appealRepository;

    private TrustService newTrustService() {
        return new TrustService(trustCaseRepository, moderationDecisionRepository, appealRepository,
                RADIUS_METERS, WINDOW_MINUTES, FALSENESS_THRESHOLD, APPEAL_WINDOW_DAYS);
    }

    @Test
    void evaluateMarksAsDuplicateWhenCloseInSpaceAndTime() {
        TrustService service = newTrustService();
        Instant now = Instant.now();
        TrustCase existing = new TrustCase(UUID.randomUUID(), UUID.randomUUID(), "ROBO", -12.05, -77.03, now);
        when(trustCaseRepository.findByTypeAndOccurredAtAfter(eq("ROBO"), any())).thenReturn(List.of(existing));
        when(trustCaseRepository.save(any(TrustCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new EvaluateIncidentRequest(UUID.randomUUID(), UUID.randomUUID(), "ROBO",
                -12.05, -77.03, now.plusSeconds(60), "otro reporte del mismo hecho", true, 5);

        TrustCase result = service.evaluate(request);

        assertThat(result.getStatus()).isEqualTo(TrustStatus.DUPLICATE);
        assertThat(result.getDuplicateOfCaseId()).isEqualTo(existing.getId());
    }

    @Test
    void evaluateHoldsForModerationWhenFalsenessScoreIsHigh() {
        TrustService service = newTrustService();
        when(trustCaseRepository.findByTypeAndOccurredAtAfter(any(), any())).thenReturn(List.of());
        when(trustCaseRepository.save(any(TrustCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new EvaluateIncidentRequest(UUID.randomUUID(), UUID.randomUUID(), "ROBO",
                -12.05, -77.03, Instant.now(), null, false, -1);

        TrustCase result = service.evaluate(request);

        assertThat(result.getStatus()).isEqualTo(TrustStatus.HELD_FOR_MODERATION);
    }

    @Test
    void evaluateClearsWellSupportedReport() {
        TrustService service = newTrustService();
        when(trustCaseRepository.findByTypeAndOccurredAtAfter(any(), any())).thenReturn(List.of());
        when(trustCaseRepository.save(any(TrustCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new EvaluateIncidentRequest(UUID.randomUUID(), UUID.randomUUID(), "ROBO",
                -12.05, -77.03, Instant.now(), "descripcion detallada", true, 10);

        TrustCase result = service.evaluate(request);

        assertThat(result.getStatus()).isEqualTo(TrustStatus.CLEARED);
    }

    @Test
    void decideFailsWhenCaseIsNotInModerationQueue() {
        TrustService service = newTrustService();
        TrustCase clearedCase = new TrustCase(UUID.randomUUID(), UUID.randomUUID(), "ROBO", -12.05, -77.03, Instant.now());
        clearedCase.setStatus(TrustStatus.CLEARED);
        when(trustCaseRepository.findById(any())).thenReturn(Optional.of(clearedCase));

        assertThatThrownBy(() -> service.decide(UUID.randomUUID(), UUID.randomUUID(),
                ModerationDecision.Decision.APPROVE, "ok"))
                .isInstanceOf(InvalidModerationStateException.class);
    }

    @Test
    void appealFailsWhenWindowExpired() {
        TrustService service = newTrustService();
        UUID authorId = UUID.randomUUID();
        TrustCase rejectedCase = new TrustCase(UUID.randomUUID(), authorId, "ROBO", -12.05, -77.03, Instant.now());
        rejectedCase.setStatus(TrustStatus.REJECTED);
        rejectedCase.setDecidedAt(Instant.now().minusSeconds((APPEAL_WINDOW_DAYS + 1) * 24 * 3600));
        when(trustCaseRepository.findById(any())).thenReturn(Optional.of(rejectedCase));

        assertThatThrownBy(() -> service.appeal(UUID.randomUUID(), authorId, "no fue asi"))
                .isInstanceOf(AppealWindowClosedException.class);
    }

    private TrustCase rejectedCase(UUID author) {
        TrustCase value = new TrustCase(UUID.randomUUID(), author, "ROBO", -12.05, -77.03, Instant.now());
        value.setStatus(TrustStatus.REJECTED);
        value.setDecidedAt(Instant.now().minusSeconds(60));
        return value;
    }

    @Test
    void approvalChangesStatusAndPersistsDecision() {
        TrustCase value = rejectedCase(UUID.randomUUID());
        value.setStatus(TrustStatus.HELD_FOR_MODERATION);
        when(trustCaseRepository.findById(any())).thenReturn(Optional.of(value));
        when(trustCaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(newTrustService().decide(UUID.randomUUID(), UUID.randomUUID(), ModerationDecision.Decision.APPROVE, "validado").getStatus())
                .isEqualTo(TrustStatus.APPROVED);
        org.mockito.Mockito.verify(moderationDecisionRepository).save(any());
        assertThat(value.getDecidedAt()).isNotNull();
    }

    @Test
    void rejectionChangesStatus() {
        TrustCase value = rejectedCase(UUID.randomUUID());
        value.setStatus(TrustStatus.HELD_FOR_MODERATION);
        when(trustCaseRepository.findById(any())).thenReturn(Optional.of(value));
        when(trustCaseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(newTrustService().decide(UUID.randomUUID(), UUID.randomUUID(), ModerationDecision.Decision.REJECT, "sin sustento").getStatus())
                .isEqualTo(TrustStatus.REJECTED);
    }

    @Test
    void timelyAppealReopensModerationAndPersistsJustification() {
        UUID author = UUID.randomUUID();
        TrustCase value = rejectedCase(author);
        when(trustCaseRepository.findById(any())).thenReturn(Optional.of(value));
        when(appealRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var appeal = newTrustService().appeal(UUID.randomUUID(), author, "adjunto evidencia");
        assertThat(value.getStatus()).isEqualTo(TrustStatus.HELD_FOR_MODERATION);
        assertThat(appeal.getAuthorId()).isEqualTo(author);
        assertThat(appeal.getJustification()).isEqualTo("adjunto evidencia");
        org.mockito.Mockito.verify(trustCaseRepository).save(value);
    }

    @Test
    void appealRejectsAnotherAuthorWithoutWriting() {
        when(trustCaseRepository.findById(any())).thenReturn(Optional.of(rejectedCase(UUID.randomUUID())));
        assertThatThrownBy(() -> newTrustService().appeal(UUID.randomUUID(), UUID.randomUUID(), "motivo"))
                .isInstanceOf(pe.upc.peaceapp.trust.api.error.ForbiddenOperationException.class);
        org.mockito.Mockito.verifyNoInteractions(appealRepository);
        org.mockito.Mockito.verify(trustCaseRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void appealRequiresRejectedState() {
        UUID author=UUID.randomUUID();
        TrustCase value=rejectedCase(author);
        value.setStatus(TrustStatus.APPROVED);
        when(trustCaseRepository.findById(any())).thenReturn(Optional.of(value));
        assertThatThrownBy(() -> newTrustService().appeal(UUID.randomUUID(), author, "motivo"))
                .isInstanceOf(InvalidModerationStateException.class);
    }

    @Test
    void missingCaseIsReported() {
        when(trustCaseRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> newTrustService().getById(UUID.randomUUID()))
                .isInstanceOf(pe.upc.peaceapp.trust.api.error.TrustCaseNotFoundException.class);
    }

    @Test
    void queueContainsOnlyCasesAwaitingModeration() {
        TrustCase value=rejectedCase(UUID.randomUUID());
        value.setStatus(TrustStatus.HELD_FOR_MODERATION);
        when(trustCaseRepository.findByStatus(TrustStatus.HELD_FOR_MODERATION)).thenReturn(List.of(value));
        assertThat(newTrustService().getModerationQueue()).containsExactly(value);
    }
}
