package pe.upc.peaceapp.trust.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * BC-03 Trust &amp; Moderation: evaluacion de confiabilidad de un incidente reportado
 * en incident-service (US-14 a US-17). No hay base de datos compartida entre servicios
 * (CA-02), por eso incidentId/type/latitude/longitude se reciben duplicados al evaluar.
 */
@Entity
@Table(name = "trust_cases")
@Getter
@Setter
@NoArgsConstructor
public class TrustCase {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID incidentId;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustStatus status = TrustStatus.CLEARED;

    /** US-15: probabilidad heuristica de falsedad (0 a 1) mientras no exista el modelo de clasificacion. */
    private double falsenessScore;

    private UUID duplicateOfCaseId;

    /** US-17: fecha de la ultima decision de moderacion, usada para calcular la ventana de apelacion. */
    private Instant decidedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public TrustCase(UUID incidentId, UUID authorId, String type, double latitude, double longitude,
                      Instant occurredAt) {
        this.incidentId = incidentId;
        this.authorId = authorId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.occurredAt = occurredAt;
    }
}
