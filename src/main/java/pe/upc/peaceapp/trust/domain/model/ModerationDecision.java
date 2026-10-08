package pe.upc.peaceapp.trust.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** US-16: decision tomada por un moderador sobre un caso en la cola de moderacion. */
@Entity
@Table(name = "moderation_decisions")
@Getter
@Setter
@NoArgsConstructor
public class ModerationDecision {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trust_case_id", nullable = false)
    private TrustCase trustCase;

    @Column(nullable = false)
    private UUID moderatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision;

    private String reason;

    @Column(nullable = false)
    private Instant decidedAt = Instant.now();

    public ModerationDecision(TrustCase trustCase, UUID moderatorId, Decision decision, String reason) {
        this.trustCase = trustCase;
        this.moderatorId = moderatorId;
        this.decision = decision;
        this.reason = reason;
    }

    public enum Decision { APPROVE, REJECT }
}
