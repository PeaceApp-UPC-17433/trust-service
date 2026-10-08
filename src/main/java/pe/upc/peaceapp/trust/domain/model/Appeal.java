package pe.upc.peaceapp.trust.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** US-17: apelacion del autor sobre un caso rechazado. */
@Entity
@Table(name = "appeals")
@Getter
@Setter
@NoArgsConstructor
public class Appeal {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trust_case_id", nullable = false)
    private TrustCase trustCase;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 1000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Appeal(TrustCase trustCase, UUID authorId, String justification) {
        this.trustCase = trustCase;
        this.authorId = authorId;
        this.justification = justification;
    }

    public enum Status { PENDING, RESOLVED }
}
